package bluej.debugger.jdi;

import java.io.File;
import java.util.*;

import javax.swing.event.EventListenerList;

import bluej.*;
import bluej.debugger.*;
import bluej.debugmgr.Invoker;
import bluej.runtime.ExecServer;
import bluej.utility.*;

import com.sun.jdi.*;

/**
 * A class implementing the execution and debugging primitives needed by
 * BlueJ.
 * 
 * This class is tightly coupled with the classes VMReference and
 * VMEventHandler. JdiDebugger is the half of the debugger that is
 * persistent across debugger sessions. VMReference and VMEventHandler
 * will be constructed anew each time a remote VM is started.
 * VMReference handles most of the work of making the remote VM
 * do things. VMEventHandler starts a new thread that listens for
 * remote VM events and calls back into VMReference on reciept of
 * these events.
 * 
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: JdiDebugger.java 2371 2003-11-19 03:39:06Z ajp $
 */
public class JdiDebugger extends Debugger
{
    private static final int loaderPriority = Thread.currentThread().getPriority() - 2;
    
	// the synch object for loading.
	// See the inner class MachineLoaderThread (at the bottom of this source)
	volatile private boolean vmRunning = false;
	
	private boolean autoRestart = true;

	// the reference to the current remote VM handler
	private VMReference vmRef;
	
	// the thread that we spawn to load the current remote VM
	private MachineLoaderThread machineLoader;

	// a set holding all the JdiThreads in the VM
	private JdiThreadSet allThreads;
	
	// a TreeModel exposing selected JdiThreads in the VM
	private JdiThreadTreeModel treeModel;
	
	// listeners for events that occur in the debugger	
	private EventListenerList listenerList = new EventListenerList();

	// the directory to launch the VM in
	private File startingDirectory;
	
    // terminal to use for all VM input and output
    private DebuggerTerminal terminal;
    
	// a Set of strings which have been used as names on the
	// object bench. We endeavour to not reuse them.
	private Set usedNames;
	
    // indicate whether we want to see system threads
    private boolean hideSystemThreads;

	/**
	 * Construct an instance of the debugger.
	 *
	 * This constructor should not be used by the main part of BlueJ. Access
	 * should be through Debugger.getDebuggerImpl().
	 *  
	 * @param startingDirectory  a File representing the directory
	 *                           we should launch the debug VM in.
     * @param terminal           a Terminal where we can do input/output.
	 */	
    public JdiDebugger(File startingDirectory, DebuggerTerminal terminal)
    {
		this.startingDirectory = startingDirectory;
        this.terminal = terminal;
		
        allThreads = new JdiThreadSet();
		treeModel = new JdiThreadTreeModel(new JdiThreadNode());
		usedNames = new TreeSet();
        hideSystemThreads = true;
    }

	/**
	 * Start debugging.
	 */
	public synchronized void launch()
	{
		if(vmRunning)
			throw new IllegalStateException("JdiDebugger.launch() was called but the debugger was already loaded");

		if(machineLoader != null)
			 // Attempt to restart VM while already restarting - ignored.
            return;
		
		autoRestart = true;
		
		raiseStateChangeEvent(Debugger.UNKNOWN, Debugger.NOTREADY);

		// start the MachineLoader (a separate thread) to load the
		// remote virtual machine in the background
		machineLoader = new MachineLoaderThread();
		// lower priority to improve GUI response time
        machineLoader.setPriority(loaderPriority);
		machineLoader.start();	
	}
	
    
	/**
	 * Close this VM, possibly restart it.
	 */
	public synchronized void close(boolean restart)
	{
        if (vmRunning) {
            autoRestart = restart;
            vmRunning = false;

            // kill the remote debugger process
            vmRef.close();
            
            // we will eventually get a vmDisconnect event and end up in
            // method vmDisconnect() (below)
        }
        else {
            // if we are not running, just check whether we want a restart
            if(restart)
                launch();
        }
	}

    
	/**
	 * Add a listener for DebuggerEvents
	 * 
	 * @param l  the DebuggerListener to add
	 */
	public void addDebuggerListener(DebuggerListener l)
	{
		listenerList.add(DebuggerListener.class, l);
	}

	/**
	 * Remove a listener for DebuggerEvents.
	 * 
	 * @param l  the DebuggerListener to remove
	 */
	public void removeDebuggerListener(DebuggerListener l)
	{
		listenerList.remove(DebuggerListener.class, l);
	}


	/**
	 * Guess a suitable name for an object about to be put on the object bench.
	 * 
	 * @param	className	the fully qualified name of the class of object
	 * @return				a String suitable as a name for an object on the
	 * 						object bench. 
	 */
	public String guessNewName(String className)
	{
        // className can have array brackets at the end which is not suitable
        // for an identifier. We'll strip them out
        className = className.replace('[',' ').replace(']',' ').trim();
        
		String baseName = JavaNames.getBase(className);

		// truncate long names to  OBJ_NAME_LENGTH plus _instanceNum
		int stringEndIndex =
			baseName.length() > Invoker.OBJ_NAME_LENGTH ? Invoker.OBJ_NAME_LENGTH : baseName.length();

		String newName = Character.toLowerCase(baseName.charAt(0)) +
			baseName.substring(1, stringEndIndex);

		int num = 1;
		
		while(usedNames.contains(newName + num))
			num++;
			
		return newName + num;
	}
	
    
	/**
	 * Create a class loader in the debugger.
	 */
    public void newClassLoader(String classPath)
    {
		if (!vmRunning)
			return;

		usedNames.clear();
		getVM().newClassLoader(classPath);
    }

    
	/**
	 * Create a class loader in the debugger but retain
	 * any user created breakpoints.
	 */
	public void newClassLoaderLeavingBreakpoints(String classPath)
	{
		// a list of Location's representing a temporarily
		// saved list of breakpoints we want to keep
		List savedBreakpoints;

		savedBreakpoints = getVM().getBreakpoints();
		newClassLoader(classPath);
		getVM().restoreBreakpoints(savedBreakpoints);
	}


	/**
	 * Add a debugger object into the project scope.
	 * 
	 * @param   newInstanceName  the name of the object
	 *          dob              the object itself
	 * @return  true if the object could be added with this name,
	 *          false if there was a name clash.
	 */
    public boolean addObject(String newInstanceName, DebuggerObject dob)
    {
        Object args[] = { newInstanceName, ((JdiObject)dob).getObjectReference() };

		getVM().invokeExecServerWorker( ExecServer.ADD_OBJECT, Arrays.asList(args));
		
		usedNames.add(newInstanceName);
		
		return true;
    }

    
    /**
     * Remove an object from a package scope (when removed from object bench).
     */
    public void removeObject(String instanceName)
    {
		if (!vmRunning)
			return;
    		
        Object args[] = { instanceName };

		try {
			getVM().invokeExecServerWorker( ExecServer.REMOVE_OBJECT, Arrays.asList(args) );
		}
		catch (VMDisconnectedException e) { }
    }

    
    /**
     * Set the class path of the remote VM
     */
    public void setLibraries(String classpath)
    {
        Object args[] = { classpath };

		getVM().invokeExecServerWorker( ExecServer.SET_LIBRARIES, Arrays.asList(args));
    }

    
	/**
	 * Return the debugger objects that exist in the
	 * debugger.
	 * 
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
	public Map getObjects()
	{
		throw new IllegalStateException("not implemented");
		// the returned array consists of double the number of objects
		// they alternate, name, object, name, object
		// ie.
		// arrayRef[0] = a field name 0 (StringReference)
		// arrayRef[1] = a field value 0 (ObjectReference)
		// arrayRef[2] = a field name 1 (StringReference)
		// arrayRef[3] = a field value 1 (ObjectReference)
		//
	}

	/**
	 * Run the setUp() method of a test class and return the created
	 * objects.
	 * 
	 * @param className	the fully qualified name of the class
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
    public Map runTestSetUp(String className)
    {
        Object args[] = { className };

        ArrayReference arrayRef = (ArrayReference) getVM().invokeExecServer(ExecServer.RUN_TEST_SETUP, Arrays.asList(args));
       
        // the returned array consists of double the number of fields created by running test setup
        // they alternate, fieldname, fieldvalue, fieldname, fieldvalue
        // ie.
        // arrayRef[0] = a field name 0 (StringReference)
        // arrayRef[1] = a field value 0 (ObjectReference)
        // arrayRef[2] = a field name 1 (StringReference)
        // arrayRef[3] = a field value 1 (ObjectReference)
        //
        // we could return a Map from RUN_TEST_SETUP but then we'd have to use JDI
        // reflection to make method calls on Map in order to extract the values
        Map returnMap = new HashMap();

        if (arrayRef != null) {
            for(int i=0; i<arrayRef.length(); i+=2)
                returnMap.put(((StringReference) arrayRef.getValue(i)).value(),
                                JdiObject.getDebuggerObject((ObjectReference)arrayRef.getValue(i+1)));
        }

        // the resulting map consists of entries (String fieldName, JdiObject obj)
        return returnMap;
    }

	/**
	 * Run a single test method in a test class and return the result.
	 * 
	 * @param  className  the fully qualified name of the class
	 * @param  methodName the name of the method
	 * @return            a DebuggerTestResult object
	 */
    public DebuggerTestResult runTestMethod(String className, String methodName)
    {
        Object args[] = { className, methodName };

		ArrayReference arrayRef = (ArrayReference) getVM().invokeExecServer(ExecServer.RUN_TEST_METHOD, Arrays.asList(args));
      
        if (arrayRef != null && arrayRef.length() > 2) {
			String failureType = ((StringReference) arrayRef.getValue(0)).value();
			String exMsg = ((StringReference) arrayRef.getValue(1)).value();
			String traceMsg = ((StringReference) arrayRef.getValue(2)).value();

			if (failureType.equals("failure"))
				return new JdiTestResultFailure(className, methodName, exMsg, traceMsg);
			else
				return new JdiTestResultError(className, methodName, exMsg, traceMsg);
        }
		
		// a null means that we had success. Return a success test result
		return new JdiTestResult(className, methodName);
    }    

	/**
	 * Return the machine status; one of the "machine state" constants:
	 * (IDLE, RUNNING, SUSPENDED).
	 */
	public int getStatus()
	{
		if (!vmRunning)
			return Debugger.NOTREADY;
		else
			return getVM().getStatus();
	}

    /**
     * Dispose all top level windows in the remote machine.
     */
    public void disposeWindows()
    {
		if (!vmRunning)
			return;

		try {
			getVM().invokeExecServerWorker(ExecServer.DISPOSE_WINDOWS, Collections.EMPTY_LIST);
		}
		catch (VMDisconnectedException e) { }
    }

    /**
     * Supress error output on the remote machine.
     */
    public void supressErrorOutput()
    {
		if (!vmRunning)
			return;

		getVM().invokeExecServerWorker( ExecServer.SUPRESS_OUTPUT, Collections.EMPTY_LIST );
    }

    /**
     * Restore error output on the remote machine.
     */
    public void restoreErrorOutput()
    {
		if (!vmRunning)
			return;

		getVM().invokeExecServerWorker( ExecServer.RESTORE_OUTPUT, Collections.EMPTY_LIST );
    }

	/**
	 * "Start" a class (i.e. invoke its main method)
	 *
	 * @param classname		the class to start
	 * @param eventParam	when a BlueJEvent is generated for a
	 *				breakpoint, this parameter is passed as the
	 *				event parameter
	 */
	public void runClassMain(String className)
		throws ClassNotFoundException
	{
		getVM().runShellClass(className);
	}

    /**
     * Get a class from the virtual machine.
     * Return null if the class could not be found.
     */
    public DebuggerClass getClass(String className)
		throws ClassNotFoundException
    {
		ReferenceType classMirror = getVM().loadClass(className);

		return new JdiClass(classMirror);
    }

    /**
     * Get the value of a static field in a class.
     * Return null if the field or class could not be found.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
    	throws ClassNotFoundException
    {
		// Debug.message("[getStaticValue] " + className + ", " + fieldName);
		ObjectReference ob = getVM().getStaticValue(className, fieldName);
		
		if (ob != null)
			return JdiObject.getDebuggerObject(ob);
		else
			return null;
    }

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, TERMINATED).
     */
    public int getExitStatus()
    {
        return getVM().getExitStatus();
    }


    /**
     * Return the text of the last exception.
     */
    public ExceptionDescription getException()
    {
        return getVM().getException();
    }

	/**
	 * List all threads being debugged as a TreeModel.
	 *
	 * @return  A tree model of all the threads.
	 */
	public DebuggerThreadTreeModel getThreadTreeModel()
	{
		return treeModel; 
	}
	

	// notify all listeners that have registered interest for
	// notification on this event type.
	private void fireTargetEvent(DebuggerEvent ce)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i] == DebuggerListener.class) {
				((DebuggerListener)listeners[i+1]).debuggerEvent(ce);
			}
		}
	}

	void raiseStateChangeEvent(int oldState, int newState)
	{
		fireTargetEvent(new DebuggerEvent(this, DebuggerEvent.DEBUGGER_STATECHANGED, oldState, newState));
	}

	void raiseRemoveStepMarksEvent()
	{
		fireTargetEvent(new DebuggerEvent(this, DebuggerEvent.DEBUGGER_REMOVESTEPMARKS));
	}

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set/clear the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     *
     * @return  null if there was no problem, or an error string
     */
    public String toggleBreakpoint(String className, int line, boolean set)
    {
        // Debug.message("[toggleBreakpoint]: " + className + " line " + line);

        try {
            if(set) {
                return getVM().setBreakpoint(className, line);
            }
            else {
                return getVM().clearBreakpoint(className, line);
            }
        }
        catch(Exception e) {
            Debug.reportError("breakpoint error: " + e);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
        }
    }

	/**
	 * Called by VMReference when a HALT is encountered in the debugger VM.
	 */
	void halt(ThreadReference tr)
	{
		JdiThreadNode jtn = treeModel.findThreadNode(tr);

		if (jtn != null) {
			treeModel.nodeChanged(jtn);		

			fireTargetEvent(new DebuggerEvent(this,
												 DebuggerEvent.THREAD_HALT,
												 new JdiThread(treeModel, tr)));
		}
		else {
			Debug.message("Received HALT for unknown thread " + tr);
		}
	}
	
	/**
	 * Called by VMReference when a BREAKPOINT is encountered in the debugger VM.
	 */
	public void breakpoint(ThreadReference tr)
	{
        JdiThread breakThread = allThreads.find(tr);
		JdiThreadNode jtn = treeModel.findThreadNode(tr);
        // if the thread at the breakpoint is not currently displayed, display it now.
		if (jtn == null) {
            JdiThreadNode root = treeModel.getThreadRoot();
            treeModel.insertNodeInto(new JdiThreadNode(breakThread), root, 0);
        }
        else
            treeModel.nodeChanged(jtn);
        
        fireTargetEvent(new DebuggerEvent(this,
                                          DebuggerEvent.THREAD_BREAKPOINT,
                                          breakThread));
	}

	// - event handling

	/**
	 * Called by VMReference when the machine exits. The exit event
     * comes before a 'disconnect' event that will follow shortly.
	 */	
	void vmExit()
	{
	}

	/**
	 * Called by VMReference when the machine disconnects. The disconnect event
     * follows a machine 'exit' event.
	 */	
	void vmDisconnect()
	{
        // Debug.message("VM disconnect. Restart: " + autoRestart);

		if (autoRestart) {
			
            raiseRemoveStepMarksEvent();
            raiseStateChangeEvent(Debugger.IDLE, Debugger.NOTREADY);

            treeModel.setRoot(new JdiThreadNode());
            treeModel.reload();
            usedNames.clear();

            vmRef = null;

            // promote garbage collection but also indicate to the
            // launch procedure that we are not in a launch (see launch())
            machineLoader = null;

            vmRunning = false;
			launch();
        }
	}
	
	/**
	 * Called by VMReference when a thread is started in
	 * the debugger VM.
	 * 
	 * Use this event to keep our thread tree model up to date.
	 * Currently we ignore the thread group and construct all
	 * threads at the same level.
	 */
	void threadStart(ThreadReference tr)
	{
		synchronized(treeModel.getRoot()) {
            JdiThread newThread = new JdiThread(treeModel, tr);
            allThreads.add(newThread);
            
            displayThread(newThread);
		}
	}

	/**
	 * Called by VMReference when a thread dies in
	 * the debugger VM.
	 * 
	 * Use this event to keep our thread tree model up to date.
	 */
	void threadDeath(ThreadReference tr)
	{
		synchronized(treeModel.getRoot()) {
			JdiThreadNode jtn = treeModel.findThreadNode(tr);
			if (jtn != null) {
				treeModel.removeNodeFromParent(jtn);
			}
            
            allThreads.removeThread(tr);
		}
	}

    /**
     * Set or clear the option to hide system threads.
     * This method also updates the current display if necessary.
     */
    public void hideSystemThreads(boolean hide)
    {
        if(hideSystemThreads == hide)
            return;

        hideSystemThreads = hide;
        updateThreadDisplay();
    }
    
    /**
     * Re-build the treeModel for the currently displayed threads using
     * the allThreads set and the 'hideSystemThreads' flag.
     */
    private void updateThreadDisplay()
    {
   		treeModel.setRoot(new JdiThreadNode());
        
        for(Iterator it=allThreads.iterator(); it.hasNext(); ) {
            JdiThread currentThread = (JdiThread)it.next();
            displayThread(currentThread);
        }

		treeModel.reload();
    }

    /**
     * Add the given thread to the displayed threads if appropriate.
     * System threads are displayed conditional on the 'hideSystemThreads' flag.
     */
    private void displayThread(JdiThread newThread)
    {
        if(! (hideSystemThreads && (newThread.isKnownSystemThread() || newThread.isFinished()))) {
            JdiThreadNode root = treeModel.getThreadRoot();
            treeModel.insertNodeInto(new JdiThreadNode(newThread), root, 0);
        }
    }
    
    // -- support methods --

    public void dumpThreadInfo()
    {
    	getVM().dumpThreadInfo();
    }

	private VMReference getVM()
	{
		return machineLoader.getVM();
	}
	
	/**
	 * A thread which loads a new instance of the debugger.
	 */
	class MachineLoaderThread extends Thread
	{
		 MachineLoaderThread() { }
 
		 public synchronized void run()
		 {
             try{
    		 	// System.out.println("machine loader is running " + vmRunning);
    			vmRef = new VMReference(JdiDebugger.this, terminal, startingDirectory);
    			vmRef.waitForStartup();
    
    			vmRunning = true;
    
    			// System.out.println("machine loader is started " + vmRunning);
    
    			newClassLoader(startingDirectory.getAbsolutePath());
    
    			// wake any internal getVM() calls that
    			// are waiting for us to finish				
    			notifyAll();	
    
    			raiseStateChangeEvent(Debugger.NOTREADY, Debugger.IDLE);
             } catch(JdiVmCreationException e){
                 BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_FAILED, null);
             }
		 }
		 
		private synchronized VMReference getVM()
		{
			while(!vmRunning) 
				try {
					wait();
				}
			catch(InterruptedException e) { }

			return vmRef;
		}
	} 
}
