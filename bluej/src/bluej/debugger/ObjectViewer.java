package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.Border;

/**
 * A window that displays the fields in an object (also know as an
 * "Inspect window") and method call results.
 *
 * @author     Michael Cahill
 * @author     Michael Kolling
 * @author     Duane Buck
 * @version    $Id: ObjectViewer.java 837 2001-04-04 12:27:53Z ajp $
 */
public class ObjectViewer extends JFrame
    implements ActionListener, ListSelectionListener, InspectorListener
{
    protected boolean isInspection;       // true if inspecting object, false if
    //  displaying result
    protected JList staticFieldList = null;
    protected JList objFieldList = null;

    protected TreeSet arraySet = null;  // array of Integers representing the array indexes from
    // a large array that have been selected for viewing

    protected List indexToSlotList = null;  // list which is built when viewing an array
    // that records the object slot corresponding to each
    // array index

    protected JScrollPane staticScrollPane = null;
    protected JScrollPane objectScrollPane = null;
    protected JSplitPane listPane = null; // non-null when inspecting non-array object
                                        // (this holds the two JLists)
    protected JButton inspectBtn;
    protected JButton getBtn;
    protected DebuggerObject obj;
    protected DebuggerObject selectedObject;
    protected String selectedObjectName;
    protected Package pkg;
    protected String pkgScopeId;
    protected boolean getEnabled;
    protected boolean isInScope;
    protected boolean queryArrayElementSelected;

    // either a tabbed pane or null if there is only the standard inspector
    protected JTabbedPane inspectorTabs = null;

    protected String viewerId;    // a unique ID used to enter the
                                // viewer's object into the package scope

    // === static variables ===

    protected static int count = 0;
    protected static Hashtable viewers = new Hashtable();
    protected final static Image iconImage =
            new ImageIcon(Config.getImageFilename("image.icon")).getImage();

    protected final static Color bgColor = new Color(208, 212, 208);

    protected final static String inspectorDirectoryName = "+inspector";

    protected final static String inspectTitle =
        Config.getString("debugger.objectviewer.title");
    protected final static String resultTitle =
        Config.getString("debugger.resultviewer.title");
    protected final static String staticListTitle =
        Config.getString("debugger.objectviewer.staticListTitle");
    protected final static String objListTitle =
        Config.getString("debugger.objectviewer.objListTitle");
    protected final static String inspectLabel =
        Config.getString("debugger.objectviewer.inspect");
    protected final static String getLabel =
        Config.getString("debugger.objectviewer.get");
    protected final static String close =
        Config.getString("close");
    protected final static String objectClassName =
        Config.getString("debugger.objectviewer.objectClassName");

    // array

    protected static Class[] insp = new Class[10];
    protected static int inspCnt = 0;
    protected static Set loadedProjects = new HashSet();


    // === static methods ===


   /**
     *  Return a ObjectViewer for an object. The viewer is visible.
     *  This is the only way to get access to viewers - they cannot be
     *  directly created.
     *
     * @param  inspection  True is this is an inspection, false for result
     *                     displays
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if it is not on the
     *                     object bench
     * @param  pkg         The package all this belongs to
     * @param  getEnabled  if false, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     * @return             The Viewer value
     */
    public static ObjectViewer getViewer(boolean inspection,
            DebuggerObject obj, String name,
            Package pkg, boolean getEnabled,
            JFrame parent)
    {
        ObjectViewer viewer = (ObjectViewer) viewers.get(obj);

        if (viewer == null) {
            String id;
            if (name == null) {
                id = "#viewer" + count;  // # marks viewer for internal object
                count++;  //  which is not on bench
            } else {
                id = name;
            }
            viewer = new ObjectViewer(inspection, obj, pkg, id, getEnabled, parent);
            viewers.put(obj, viewer);
        }
        viewer.update();

        viewer.setVisible(true);
        viewer.bringToFront();

        return viewer;
    }

    /**
     *  Update all open viewers to show up-to-date values.
     */
    public static void updateViewers()
    {
        for (Enumeration e = viewers.elements(); e.hasMoreElements(); ) {
            ObjectViewer viewer = (ObjectViewer) e.nextElement();
            viewer.update();
        }
    }


    // === object methods ===

    /**
     *  Constructor
     *  Note: protected -- Objectviewers can only be created with the static
     *  "getViewer" method. 'pkg' may be null if getEnabled is false.
     *
     *@param  inspect     Description of Parameter
     *@param  obj         Description of Parameter
     *@param  pkg         Description of Parameter
     *@param  id          Description of Parameter
     *@param  getEnabled  Description of Parameter
     *@param  parent      Description of Parameter
     */
    protected ObjectViewer(boolean inspect, DebuggerObject obj,
            Package pkg, String id, boolean getEnabled,
            JFrame parent)
    {
        super();

        setIconImage(iconImage);

        isInspection = inspect;
        this.obj = obj;
        this.pkg = pkg;
        viewerId = id;
        this.getEnabled = getEnabled;
        isInScope = false;
        if (pkg == null) {
            if (getEnabled) {
                Debug.reportError("cannot enable 'get' with null package");
            }
            pkgScopeId = "";
        } else {
            pkgScopeId = pkg.getId();
        }

        makeFrame(parent, isInspection, obj);
    }

    /**
     *  Return this viewer's ID.
     *
     *@return    The Id value
     */
    public String getId()
    {
        return viewerId;
    }

    /**
     *  If this is the display of a method call result, return a String with
     *  the result.
     *
     * @return    The Result value
     */
    public String getResult()
    {
        if (isInspection) {
            return "";
        } else {
            return (String) obj.getInstanceFields(false).get(0);
        }
    }

    public void getEvent(InspectorEvent e)
    {
    }

    /**
     *  De-iconify the window (if necessary) and bring it to the front.
     */
    public void bringToFront()
    {
        setState(Frame.NORMAL);  // de-iconify
        toFront();  // window to front
    }


    /**
     *  Update the field values shown in this viewer to show current
     *  object values.
     */
    public void update()
    {
        int maxRows = 7;
        int rows;

        // static fields only applicable if not an array and list not null
        if (isInspection && !obj.isArray() && staticFieldList != null) {
            staticFieldList.setListData(
                    obj.getStaticFields(true).toArray(new Object[0]));

            // set the list sizes according to number of fields in object
            rows = obj.getStaticFieldCount() + 2;
            if (rows > maxRows) {
                rows = maxRows;
            }
            staticFieldList.setVisibleRowCount(rows);
        }

        // if is an array (we potentially will compress the array if it is large)
        if (obj.isArray()) {
            objFieldList.setListData(
                    compressArrayList(
                    obj.getInstanceFields(isInspection)).toArray(new Object[0]));
        } else {
            objFieldList.setListData(
                    obj.getInstanceFields(isInspection).toArray(new Object[0]));
        }

        if (objFieldList != null) {
            rows = obj.getInstanceFieldCount() + 2;
            if (rows > maxRows) {
                rows = maxRows;
            }
            if (!isInspection) {
                rows = 2;
            }
            objFieldList.setVisibleRowCount(rows);
        }

        if (staticFieldList != null) {
            staticFieldList.revalidate();
        }
        if (objFieldList != null) {
            objFieldList.revalidate();
        }

        if (listPane != null)
            listPane.resetToPreferredSizes();

        // Ensure a minimum width for the lists: if both lists are narrower
        // than 200 pixels, set them to 200 (need to set only one - the other
        // will be resized as well.

        double width = objFieldList.getPreferredScrollableViewportSize().getWidth();
        if (staticFieldList != null) {
            width = Math.max(width,
               staticFieldList.getPreferredScrollableViewportSize().getWidth());
        }
        if(width <= 200)
            objFieldList.setFixedCellWidth(200);
        else
            objFieldList.setFixedCellWidth(-1);

        pack();

        if (inspectorTabs != null) {
            for (int i = 1; i < inspectorTabs.getTabCount(); i++) {
                ((Inspector) inspectorTabs.getComponentAt(i)).refresh();
            }
        }

        repaint();
    }


    /**
     *  actionPerformed - something was done in the viewer dialog.
     *  Find out what it was and act.
     *
     *@param  evt  Description of Parameter
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();

        // "Inspect" button
        if (inspectLabel.equals(cmd)) {
            // null objects checked for inside doInspect
            doInspect();
        }

        // "Get" button
        else if (getLabel.equals(cmd) && (selectedObject != null))
        {
            doGet();
        }

        // "Close" button
        else if (close.equals(cmd))
        {
            doClose();
        }
    }

    // ----- ListSelectionListener interface -----

    /**
     *  The value of the list selection has changed. Update the selected
     *  object. This needs some synchronisation, since we have two lists,
     *  and we only want one selection.
     *
     *@param  e  Description of Parameter
     */
    public void valueChanged(ListSelectionEvent e)
    {
        // ignore mouse down, dragging, etc.
        if (e.getValueIsAdjusting()) {
            return;
        }

        // click in static list
        if (e.getSource() == staticFieldList) {
            int slot = staticFieldList.getSelectedIndex();

            if (slot == -1) {
                return;
            }

            if (obj.staticFieldIsObject(slot)) {
                setCurrentObj(obj.getStaticFieldObject(slot),
                                obj.getStaticFieldName(slot));

                if (obj.staticFieldIsPublic(slot)) {
                    setButtonsEnabled(true, true);
                } else {
                    setButtonsEnabled(true, false);
                }
            } else {
                setCurrentObj(null, null);
                setButtonsEnabled(false, false);
            }

            objFieldList.clearSelection();
        }
        else if (e.getSource() == objFieldList) // click in object list
        {
            int slot = objFieldList.getSelectedIndex();

            // occurs if valueChanged picked up a clearSelection event from
            // the list
            if (slot == -1) {
                return;
            }

            // add index to slot method for truncated arrays
            if (obj.isArray()) {
                slot = indexToSlot(slot);
            }

            queryArrayElementSelected = (slot == ARRAY_QUERY_SLOT_VALUE);

            // for array compression..
            if (queryArrayElementSelected)
            {  // "..." in Array inspector
                setCurrentObj(null, null);  //  selected
                // check to see if elements are objects,
                // using the first item in the array
                if (obj.instanceFieldIsObject(0)) {
                    setButtonsEnabled(true, false);
                } else {
                    setButtonsEnabled(false, false);
                }
            }
            else if (obj.instanceFieldIsObject(slot))
            {
                setCurrentObj(obj.getInstanceFieldObject(slot),
                                obj.getInstanceFieldName(slot));

                if (obj.instanceFieldIsPublic(slot)) {
                    setButtonsEnabled(true, true);
                } else {
                    setButtonsEnabled(true, false);
                }
            }
            else
            {
                setCurrentObj(null, null);
                setButtonsEnabled(false, false);
            }

            if (staticFieldList != null)
                staticFieldList.clearSelection();
        }
    }

    public void inspectEvent(InspectorEvent e)
    {
        getViewer(true,
                e.getDebuggerObject(), null,
                pkg, false,
                this);
    }


    /**
     *  Store the object currently selected in the list.
     *
     *@param  object  The new CurrentObj value
     *@param  name    The new CurrentObj value
     */
    private void setCurrentObj(DebuggerObject object, String name)
    {
        selectedObject = object;
        selectedObjectName = name;
    }


    /**
     * Enable or disable the Inspect and Get buttons.
     *
     * @param  inspect  The new ButtonsEnabled value
     * @param  get      The new ButtonsEnabled value
     */
    private void setButtonsEnabled(boolean inspect, boolean get)
    {
        inspectBtn.setEnabled(inspect);
        if (getEnabled) {
            getBtn.setEnabled(get);
        }
    }

    // ----- end of ListSelectionListener interface -----

    private final static int VISIBLE_ARRAY_START = 40;  // show at least the first 40 elements
    private final static int VISIBLE_ARRAY_TAIL = 5;  // and the last five elements

    private final static int ARRAY_QUERY_SLOT_VALUE = -2;  // signal marker of the [...] slot in our

    /**
     * Compress a potentially large array into a more displayable
     * shortened form.
     *
     * Compresses an array field name list to a maximum of VISIBLE_ARRAY_START
     * which are guaranteed to be displayed at the start, then some [..] expansion
     * slots, followed by VISIBLE_ARRAY_TAIL elements from the end of the array.
     * When a selected element is chosen
     * indexToSlot allows the selection to be converted to the original
     * array element position.
     *
     * @param  fullArrayFieldList  the full field list for an array
     * @return                     the compressed array
     */
    private List compressArrayList(List fullArrayFieldList)
    {
        if (arraySet == null)
        {
            arraySet = new TreeSet();
        }

        indexToSlotList = new LinkedList();

        // the +1 here is due to the fact that if we do not have at least one more than
        // the sum of start elements and tail elements, then there is no point in displaying
        // the ... elements because there would be no elements for them to reveal
        if (fullArrayFieldList.size() > (VISIBLE_ARRAY_START + VISIBLE_ARRAY_TAIL + 1))
        {

            // the destination list
            List newArray = new ArrayList();

            // make a copy which we gradually destroy
            LinkedList arraySetAsList = new LinkedList(arraySet);

            for (int i = 0; i < VISIBLE_ARRAY_START; i++)
            {
                // first 40 elements are displayed as per normal
                newArray.add(fullArrayFieldList.get(i));
                indexToSlotList.add(new Integer(i));
            }

            // now the first of our expansion slots
            newArray.add("[...]");
            indexToSlotList.add(new Integer(ARRAY_QUERY_SLOT_VALUE));

            if (arraySetAsList.size() > 0) {
                // add all the elements which they have previously indicated they want to show
                while (arraySetAsList.size() > 0) {
                    Integer first = (Integer) arraySetAsList.removeFirst();

                    newArray.add(fullArrayFieldList.get(first.intValue()));
                    indexToSlotList.add(new Integer(first.intValue()));
                }

                // now the second and last of our expansion slots
                newArray.add("[...]");
                indexToSlotList.add(new Integer(ARRAY_QUERY_SLOT_VALUE));
            }

            for (int i = VISIBLE_ARRAY_TAIL; i > 0; i--) {
                // last 5 elements are displayed
                newArray.add(fullArrayFieldList.get(
                        fullArrayFieldList.size() - i));
                indexToSlotList.add(new Integer(
                        fullArrayFieldList.size() - i));
            }
            return newArray;
        }
        else
        {
            for (int i = 0; i < fullArrayFieldList.size(); i++) {
                indexToSlotList.add(new Integer(i));
            }
            return fullArrayFieldList;
        }
    }

    /**
     * Converts list index position to that of array element position in arrays.
     * Uses the List built in compressArrayList to do the mapping.
     *
     * @param   listIndexPosition   the position selected in the list
     * @return                      the translated index of field array element
     */
    private int indexToSlot(int listIndexPosition)
    {
        Integer slot = (Integer) indexToSlotList.get(listIndexPosition);

        return slot.intValue();
    }

    /**
     * Shows a dialog to select array element for inspection
     */
    private void selectArrayElement()
    {
        String response = DialogManager.askString(this, "ask-index");

        if (response != null) {
            try {
                int slot = Integer.parseInt(response);

                // check if within bounds of array
                if (slot >= 0 && slot < obj.getInstanceFieldCount()) {
                    // if its an object set as current object
                    if (obj.instanceFieldIsObject(slot)) {
                        setCurrentObj(obj.getInstanceFieldObject(slot),
                                obj.getInstanceFieldName(slot));
                        setButtonsEnabled(true, false);
                    } else {
                        // it is not an object - a primitive, so lets
                        // just display it in the array list display
                        setButtonsEnabled(false, false);
                        arraySet.add(new Integer(slot));
                        update();
                    }
                } else {  // not within array bounds
                    DialogManager.showError(this, "out-of-bounds");
                }
            }
            catch (NumberFormatException e) {
                // input could not be parsed, eg. non integer value
                setCurrentObj(null, null);
                DialogManager.showError(this, "cannot-access-element");
            }
        }
        else
        {
            // set current object to null to avoid re-inspection of
            // previously selected wildcard
            setCurrentObj(null, null);
        }
    }


    /**
     *  The "Inspect" button was pressed. Inspect the
     *  selected object.
     */
    private void doInspect()
    {
        // if need to query array element
        if (queryArrayElementSelected) {
            selectArrayElement();
        }

        if (selectedObject != null) {
            boolean isPublic = getBtn.isEnabled();
            ObjectViewer viewer = getViewer(true, selectedObject, null, pkg,
                    isPublic, this);

            // If the newly opened object is public, enter it into the
            // package scope, so that we can perform "Get" operations on it.
            if (isPublic) {
                viewer.addToScope(viewerId, selectedObjectName);
            }
        }
    }

    /**
     *  The "Get" button was pressed. Get the selected object on the
     *  object bench.
     */
    private void doGet()
    {
        pkg.getEditor().raisePutOnBenchEvent(selectedObject, viewerId,
                selectedObjectName);
    }

    /**
     *@param  parentViewerId  The feature to be added to the ToScope attribute
     *@param  objectName      The feature to be added to the ToScope attribute
     */
    protected void addToScope(String parentViewerId, String objectName)
    {
        Debugger.debugger.addObjectToScope(pkgScopeId, parentViewerId,
                objectName, viewerId);
        isInScope = true;
    }


    /**
     *  Close this viewer. Don't forget to remove it from the list of open
     *  viewers.
     */
    private void doClose()
    {
        setVisible(false);
        dispose();
        viewers.remove(obj);

        // if the object shown here is not on the object bench, also
        // remove it from the package scope

        if (isInScope && (viewerId.charAt(0) == '#')) {
            Debugger.debugger.removeObjectFromScope(pkgScopeId, viewerId);
        }
    }

    /**
     * Calculate and set the visible row counts for our lists
     */
    private void calculateListSize(DebuggerObject obj, JList staticFields, JList objFields)
    {
    }

    /**
     *  Build the GUI interface.
     *
     *@param  parent        Description of Parameter
     *@param  isInspection  Indicates if this is a result window or an inspector window
     *@param  obj           The debugger object we want to look at
     */
    private void makeFrame(JFrame parent, boolean isInspection,
                            DebuggerObject obj)
    {
        String className = "";

        if (isInspection) {
            className = JavaNames.stripPrefix(obj.getClassName());
            setTitle(inspectTitle + " " + className);
        } else {
            setTitle(resultTitle);
        }

        //	setFont(font);
        setBackground(bgColor);

        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent E)
                {
                    doClose();
                }
            });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(Config.generalBorder);

        // if we are doing an inspection, we construct a JList for the static
        // fields of the object
        if (isInspection) {
            JPanel titlePanel = new JPanel();
            titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            JLabel classNameLabel = new JLabel(objectClassName + " " + className);
            titlePanel.add(classNameLabel, BorderLayout.CENTER);
            mainPanel.add(titlePanel, BorderLayout.NORTH);

            // only non-array objects can have static fields
            if (!obj.isArray()) {
                staticFieldList = new JList(new DefaultListModel());
                staticFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                staticFieldList.addListSelectionListener(this);
                staticScrollPane = new JScrollPane(staticFieldList);
                staticScrollPane.setColumnHeaderView(new JLabel(staticListTitle));
            }
        }

        // the object field list is either the fields of an object, the elements of
        // an array, or if we are not doing an inspection, the result of a method call
        objFieldList = new JList(new DefaultListModel());
        objFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objFieldList.addListSelectionListener(this);
        objectScrollPane = new JScrollPane(objFieldList);

        // if we are inspecting, we need a header or if we are displaying a result
        // we override the previously calculated list size (we make it 4 always)
        if (isInspection) {
            objectScrollPane.setColumnHeaderView(new JLabel(objListTitle));
        } else {
            objFieldList.setVisibleRowCount(4);
        }

        // in the case of inspecting a non-array object, we now need a split pane to
        // separate the statics from the object fields
        if (isInspection && !obj.isArray()) {
            listPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

            listPane.setTopComponent(staticScrollPane);
            listPane.setBottomComponent(objectScrollPane);
            listPane.setDividerSize(Config.splitPaneDividerWidth);
            listPane.resetToPreferredSizes();
            mainPanel.add(listPane, BorderLayout.CENTER);
        } else {
            mainPanel.add(objectScrollPane);
        }

        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        MouseListener mouseListener =
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    // monitor for double clicks
                    if (e.getClickCount() == 2) {
                        doInspect();
                    }
                }
            };
        objFieldList.addMouseListener(mouseListener);

        if (staticFieldList != null) {
            staticFieldList.addMouseListener(mouseListener);
        }

        // Create panel with "inspect" and "get" buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1));

        inspectBtn = new JButton(inspectLabel);
        inspectBtn.addActionListener(this);
        inspectBtn.setEnabled(false);
        buttonPanel.add(inspectBtn);

        getBtn = new JButton(getLabel);
        getBtn.setEnabled(false);
        getBtn.addActionListener(this);
        buttonPanel.add(getBtn);

        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(buttonFramePanel, BorderLayout.EAST);

        inspectorTabs = new JTabbedPane();
        initInspectors(inspectorTabs);

        // if we have any non-standard inspectors then we add a tabbed pane to
        // hold them, otherwise we just add the one panel
        if (inspectorTabs.getTabCount() > 0) {
            inspectorTabs.insertTab("Standard", null, mainPanel, "Standard", 0);
            ((JPanel) getContentPane()).add(inspectorTabs, BorderLayout.CENTER);
        } else {
            inspectorTabs = null;
            ((JPanel) getContentPane()).add(mainPanel, BorderLayout.CENTER);
        }

        // create bottom button pane with "Close" button

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton button = new JButton(close);
        buttonPanel.add(button);
        button.addActionListener(this);
        getRootPane().setDefaultButton(button);
        ((JPanel) getContentPane()).add(buttonPanel, BorderLayout.SOUTH);

        if (isInspection) {
            DialogManager.tileWindow(this, parent);
        } else {
            DialogManager.centreWindow(this, parent);
        }

        button.requestFocus();
    }

    /**
     * intialise inspectors
     */
    private void initInspectors(JTabbedPane inspTabs)
    {
        if (inspCnt == 0) {
            loadInspectors(Config.getSystemInspectorDir());
        }

        Project proj = null;
        // most common case, pkg can be null if inspection call came from debugger
        if(pkg != null)
            proj = pkg.getProject();
        // 1 project open...
        else if(Project.getOpenProjectCount() == 1)
            proj = Project.getProject();

        if (proj != null && !loadedProjects.contains(proj.getProjectDir()))
        {
            loadedProjects.add(proj.getProjectDir());
            loadInspectors(new File(proj.getProjectDir(),
                                    inspectorDirectoryName));
        }

        addInspectors(inspTabs);
    }

    private void loadInspectors(File inspectorDir)
    {
        ClassLoader loader = new InspectorClassLoader(inspectorDir);
        String[] inspName = inspectorDir.list();
        if (inspName != null) {
            for (int i=0; i < inspName.length; i++) {  // Add inspectors (if any)
                try {
                    if (inspName[i].endsWith(".class")) {
                        try {
                            Class theInspClass = loader.loadClass(inspName[i].substring(0, inspName[i].length() - 6));
                            Inspector theInsp = ((Inspector) theInspClass.newInstance());
                            // If control gets here, the class implements Inspector!
                            int inspIdx = inspCnt;
                            inspCnt++;
                            if (inspCnt >= insp.length) {
                                Class[] temp = new Class[insp.length * 2];
                                System.arraycopy(insp, 0, temp, 0, insp.length);
                                insp = temp;
                            }
                            insp[inspIdx] = theInspClass;
                            //System.out.println(""+inspIdx+": "+theInspClass);
                        }
                        catch (ClassNotFoundException e) {
                        }
                        catch (InstantiationException e) {
                        }
                        catch (IllegalAccessException e) {
                        }
                        catch (ClassCastException e) {
                        }
                    }
                }
                catch (Exception catchalle) {
                    catchalle.printStackTrace();
                }
            }
        }
    }

    private void addInspectors(JTabbedPane inspTabs)
    {
        for (int i = 0; i < inspCnt; i++) {  // Add inspectors (if any)
            try {
                bluej.debugger.Inspector theInsp = ((Inspector)insp[i].newInstance());
                String[] ic = theInsp.getInspectedClassnames();

                for (int j = 0; j < ic.length; j++) {
                    if (obj.isAssignableTo(ic[j])) {
                        boolean initOK = theInsp.initialize(ObjectViewer.this.obj);
                        if (initOK) {  //Inspector makes final decision
                            theInsp.addInspectorListener(this);
                            inspTabs.add(theInsp.getInspectorTitle(), theInsp);
                        }
                        break;
                    }
                }
            }
            // we catch all exceptions silently.. if there is buggy
            // code in an inspector, it won't affect the rest of blueJ
            // (the main inspector panel will always come up)
            catch (Exception e) { }
        }
    }
}
