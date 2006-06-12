package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;

import rmiextension.ProjectManager;
import rmiextension.wrappers.event.*;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.ClassListener;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJImpl.java 4350 2006-06-12 03:56:19Z davmac $
 */
public class RBlueJImpl extends java.rmi.server.UnicastRemoteObject
    implements RBlueJ
{
    BlueJ blueJ;

    Map listeners = new Hashtable();

    
    public RBlueJImpl(BlueJ blueJ)
        throws RemoteException
    {
        super();
        this.blueJ = blueJ;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addCompileListener(rmiextension.wrappers.event.RCompileListener, java.lang.String)
     */
    public void addCompileListener(RCompileListener listener, String projectName)
    {
        BProject[] projects = blueJ.getOpenProjects();
        BProject project = null;
        // TODO this is not robust if more than one project with the
        // same name is open
        for (int i = 0; i < projects.length; i++) {
            BProject prj = projects[i];
            try {
                if(prj.getName().equals(projectName)) {
                    project = prj;
                }
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
        }
        RCompileListenerWrapper wrapper = new RCompileListenerWrapper(listener, project);
        listeners.put(listener, wrapper);
        blueJ.addCompileListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addInvocationListener(rmiextension.wrappers.event.RInvocationListener)
     */
    public void addInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = new RInvocationListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addInvocationListener(wrapper);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void addClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = new RClassListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addClassListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getBlueJPropertyString(java.lang.String, java.lang.String)
     */
    public String getBlueJPropertyString(String property, String def)
    {
        return blueJ.getBlueJPropertyString(property, def);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getExtensionPropertyString(java.lang.String, java.lang.String)
     */
    public String getExtensionPropertyString(String property, String def)
    {
        return blueJ.getExtensionPropertyString(property, def);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getLabel(java.lang.String)
     */
    public String getLabel(String key)
    {
        return blueJ.getLabel(key);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getOpenProjects()
     */
    public RProject[] getOpenProjects()
        throws RemoteException
    {

        BProject[] bProjects = blueJ.getOpenProjects();
        int length = bProjects.length;
        RProject[] rProjects = new RProject[length];
        for (int i = 0; i < length; i++) {
            rProjects[i] = WrapperPool.instance().getWrapper(bProjects[i]);
        }

        return rProjects;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getSystemLibDir()
     */
    public File getSystemLibDir()
    {
        File f = blueJ.getSystemLibDir();
        //The getAbsoluteFile() fixes a weird bug on win using jdk1.4.2_06
        return f.getAbsoluteFile();
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#newProject(java.io.File)
     */
    public RProject newProject(File directory)
        throws RemoteException
    {
        ProjectManager.instance().addNewProject(directory);
        BProject wrapped = blueJ.newProject(directory);
        RProject wrapper = WrapperPool.instance().getWrapper(wrapped);
        ProjectManager.instance().removeNewProject(directory);
        return wrapper;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#openProject(java.lang.String)
     */
    public RProject openProject(String directory)
        throws RemoteException
    {

        return WrapperPool.instance().getWrapper(blueJ.openProject(new File(directory)));
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeCompileListener(rmiextension.wrappers.event.RCompileListener)
     */
    public void removeCompileListener(RCompileListener listener)
    {
        RCompileListenerWrapper wrapper = (RCompileListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeCompileListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeInvocationListener(rmiextension.wrappers.event.RInvocationListener)
     */
    public void removeInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = (RInvocationListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeInvocationListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void removeClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = (ClassListener) listeners.remove(listener);
        blueJ.removeClassListener(wrapper);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#setExtensionPropertyString(java.lang.String, java.lang.String)
     */
    public void setExtensionPropertyString(String property, String value)
    {
        blueJ.setExtensionPropertyString(property, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.remote.RBlueJ#exit()
     */
    public void exit()
        throws RemoteException
    {
        BProject[] bProjects = blueJ.getOpenProjects();
        int length = bProjects.length;
        for (int i = 0; i < length; i++) {
            RProjectImpl rpImpl = WrapperPool.instance().getWrapper(bProjects[i]);
            rpImpl.notifyClosing();
        }
        
        PkgMgrFrame [] frames = PkgMgrFrame.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].doClose(false);
        }
    }
}
