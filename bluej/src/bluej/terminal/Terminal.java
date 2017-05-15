/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.terminal;

import javax.swing.SwingUtilities;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bluej.BlueJTheme;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerTerminal;
import bluej.debugmgr.ExecutionEvent;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledText;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @author  Philip Stevens
 */
@SuppressWarnings("serial")
@OnThread(Tag.FXPlatform)
public final class Terminal
    implements BlueJEventListener, DebuggerTerminal
{
    private static final int MAX_BUFFER_LINES = 200;
    private VirtualizedScrollPane<?> errorScrollPane;

    private static interface TextAreaStyle
    {
        public String getCSSClass();
    }

    // The style for text in the stdout pane: was it output by the program, or input by the user?
    // Or third option: details about method recording
    private static enum StdoutStyle implements TextAreaStyle
    {
        OUTPUT("terminal-output"), INPUT("terminal-input"), METHOD_RECORDING("terminal-method-record");

        private final String cssClass;

        private StdoutStyle(String cssClass)
        {
            this.cssClass = cssClass;
        }

        public String getCSSClass()
        {
            return cssClass;
        }
    }
    // MOEFX TODO: add styles for formatting stack traces
    private static enum StderrStyleType
    {
        NORMAL("terminal-error"), LINKED_STACK_TRACE("terminal-stack-link"), FOREIGN_STACK_TRACE("terminal-stack-foreign");

        private final String cssClass;

        private StderrStyleType(String cssClass)
        {
            this.cssClass = cssClass;
        }

        public String getCSSClass()
        {
            return cssClass;
        }
    }

    private static class StderrStyle implements TextAreaStyle
    {
        private final StderrStyleType type;
        private final ExceptionSourceLocation exceptionSourceLocation;

        private StderrStyle(StderrStyleType type)
        {
            this.type = type;
            this.exceptionSourceLocation = null;
        }

        public StderrStyle(ExceptionSourceLocation exceptionSourceLocation)
        {
            this.type = StderrStyleType.LINKED_STACK_TRACE;
            this.exceptionSourceLocation = exceptionSourceLocation;
        }

        public static final StderrStyle NORMAL = new StderrStyle(StderrStyleType.NORMAL);
        public static final StderrStyle FOREIGN_STACK_TRACE = new StderrStyle(StderrStyleType.FOREIGN_STACK_TRACE);

        @Override
        public String getCSSClass()
        {
            return type.getCSSClass();
        }
    }

    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");
    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //private static final int ALT_SHORTCUT_MASK =
    //        SHORTCUT_MASK == Event.CTRL_MASK ? Event.CTRL_MASK : Event.META_MASK;

    private static final String RECORDMETHODCALLSPROPNAME = "bluej.terminal.recordcalls";
    private static final String CLEARONMETHODCALLSPROPNAME = "bluej.terminal.clearscreen";
    private static final String UNLIMITEDBUFFERINGCALLPROPNAME = "bluej.terminal.buffering";

    private final String title;

    // -- instance --

    private final Project project;
    
    private final StyledTextArea<Void, StdoutStyle> text;
    private StyledTextArea<Void, StderrStyle> errorText = null;
    private final TextField input;
    private final SplitPane splitPane;
    private boolean isActive = false;
    private static BooleanProperty recordMethodCalls =
            Config.getPropBooleanProperty(RECORDMETHODCALLSPROPNAME);
    private static BooleanProperty clearOnMethodCall =
            Config.getPropBooleanProperty(CLEARONMETHODCALLSPROPNAME);
    private static BooleanProperty unlimitedBufferingCall =
            Config.getPropBooleanProperty(UNLIMITEDBUFFERINGCALLPROPNAME);
    private boolean newMethodCall = true;
    private boolean errorShown = false;
    private final InputBuffer buffer;
    private final BooleanProperty showingProperty = new SimpleBooleanProperty(false);

    @OnThread(Tag.Any) private final Reader in = new TerminalReader();
    @OnThread(Tag.Any) private final Writer out = new TerminalWriter(false);
    @OnThread(Tag.Any) private final Writer err = new TerminalWriter(true);

    private Stage window;

    /**
     * Create a new terminal window with default specifications.
     */
    @OnThread(Tag.Swing)
    public Terminal(Project project)
    {
        this.title = WINDOWTITLE + " - " + project.getProjectName();
        this.project = project;

        buffer = new InputBuffer(256);
        int width = Config.isGreenfoot() ? 80 : Config.getPropInteger("bluej.terminal.width", 80);
        int height = Config.isGreenfoot() ? 10 : Config.getPropInteger("bluej.terminal.height", 22);
        text = new StyledTextArea<Void, StdoutStyle>(null, (t, v) -> {}, StdoutStyle.OUTPUT, this::applyStyle);
        //MOEFX: set size
                //height, width, buffer, this.project, this, false);

        VirtualizedScrollPane<?> scrollPane = new VirtualizedScrollPane<>(text);
        text.setEditable(false);
        text.getStyleClass().add("terminal");
        text.styleProperty().bind(PrefMgr.getEditorFontCSS(true));
        //MOEFX
        //text.setMargin(new Insets(6, 6, 6, 6));
        //MOEFX
        //text.addKeyListener(this);
        unlimitedBufferingCall.addListener(c -> {
            //MOEFX toggle unlimited buffering
        });

        input = new TextField();
        input.setOnAction(e -> {
            sendInput(false);
            e.consume();
        });
        input.styleProperty().bind(PrefMgr.getEditorFontCSS(true));

        Nodes.addInputMap(input, InputMap.sequence(
                // CTRL-D (unix/Mac EOF)
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)), e -> {sendInput(true); e.consume();}),
                // CTRL-Z (DOS/Windows EOF)
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN)), e -> {sendInput(true); e.consume();})
        ));

        splitPane = new SplitPane(new BorderPane(scrollPane, null, null, input, null));

        BorderPane mainPanel = new BorderPane();
        mainPanel.setCenter(splitPane);

        mainPanel.setTop(makeMenuBar());
        window = new Stage();
        window.setWidth(500);
        window.setHeight(500);
        BlueJTheme.setWindowIconFX(window);
        window.setTitle(title);
        Scene scene = new Scene(mainPanel);
        Config.addTerminalStylesheets(scene);
        window.setScene(scene);

        // Close Action when close button is pressed
        window.setOnCloseRequest(e -> {
            // We consume the event whatever happens, then decide if we can close:
            e.consume();

            // don't allow them to close the window if the debug machine
            // is running.. tries to stop them from closing down the
            // input window before finishing off input in the terminal
            if (project != null) {
                if (project.getDebugger().getStatus() == Debugger.RUNNING)
                    return;
            }
            showHide(false);
        });
        window.setOnShown(e -> {
            showingProperty.set(true);
            //org.scenicview.ScenicView.show(window.getScene());
        });
        window.setOnHidden(e -> showingProperty.set(false));

        JavaFXUtil.addChangeListenerPlatform(showingProperty, this::showHide);

        Config.rememberPositionAndSize(window, "bluej.terminal");
        //MOEFX
        //text.setUnlimitedBuffering(unlimitedBufferingCall);
        BlueJEvent.addListener(this);
    }

    private void sendInput(boolean eof)
    {
        String inputString = this.input.getText() + "\n";
        buffer.putString(inputString);
        if (eof)
        {
            buffer.signalEOF();
        }
        else
        {
            buffer.notifyReaders();
        }
        this.input.clear();
        writeToPane(text, inputString, StdoutStyle.INPUT);
    }

    private void applyStyle(TextExt t, TextAreaStyle s)
    {
        JavaFXUtil.addStyleClass(t, s.getCSSClass());
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        DataCollector.showHideTerminal(project, show);

        if (show)
        {
            window.show();
            input.requestFocus();
        }
        else
        {
            window.hide();
        }
    }
    
    public void dispose()
    {
        showHide(false);
        Platform.runLater(() -> {
            window = null;
        });
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return window.isShowing();
    }

    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
        if(active != isActive) {
            input.setEditable(active);
            isActive = active;
        }
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        text.replaceText("");
        if(errorText!=null) {
            errorText.replaceText("");
        }
        hideErrorPane();
    }

    /**
     * Save the terminal text to file.
     */
    public void save()
    {
        Platform.runLater(() -> {
            File fileName = FileUtility.getSaveFileFX(window,
                    Config.getString("terminal.save.title"),
                    null, false);
            if(fileName != null) {
                if (fileName.exists()){
                    if (DialogManager.askQuestionFX(window, "error-file-exists") != 0)
                        return;
                }
                SwingUtilities.invokeLater(() -> {
                    try
                    {
                        FileWriter writer = new FileWriter(fileName);
                        writer.write(text.getText());
                        writer.close();
                    } catch (IOException ex)
                    {
                        Platform.runLater(() -> DialogManager.showErrorFX(window, "error-save-file"));
                    }
                });
            }
        });
    }
    
    public void print()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        int printFontSize = Config.getPropInteger("bluej.fontsize.printText", 10);
        Font font = new Font("Monospaced", Font.PLAIN, printFontSize);
        if (job.printDialog()) {
            //MOEFX
            //TerminalPrinter.printTerminal(job, text, job.defaultPage(), font);
        }
    }

    /**
     * Write some text to the terminal.
     */
    private <S extends TextAreaStyle> void writeToPane(StyledTextArea<Void, S> pane, String s, S style)
    {
        prepare();
        if (pane == errorText)
            showErrorPane();
        
        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1) {
            clear();
            s = s.substring(n + 1);
        }

        pane.append(styled(s, style));

        if (!unlimitedBufferingCall.get() && pane.getParagraphs().size() >= MAX_BUFFER_LINES)
        {
            int newStart = pane.position(pane.getParagraphs().size() - MAX_BUFFER_LINES, 0).toOffset();
            pane.replaceText(0, newStart, "");
        }

        pane.end(SelectionPolicy.CLEAR);
    }

    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if (newMethodCall) {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
        else if (Config.isGreenfoot()) {
            // In greenfoot new output should always show the terminal
            if (!window.isShowing()) {
                showHide(true);
            }
        }
    }

    /**
     * An interactive method call has been made by a user.
     */
    private void methodCall(String callString)
    {
        newMethodCall = false;
        if(clearOnMethodCall.get()) {
            clear();
        }
        if(recordMethodCalls.get()) {
            text.append(styled(callString + "\n", StdoutStyle.METHOD_RECORDING));
        }
        newMethodCall = true;
    }

    private static <S> ReadOnlyStyledDocument<Void,StyledText<S>, S> styled(String text, S style)
    {
        return ReadOnlyStyledDocument.fromString(text, null, style, StyledText.textOps());
    }

    private void constructorCall(InvokerRecord ir)
    {
        newMethodCall = false;
        if(clearOnMethodCall.get()) {
            clear();
        }
        if(recordMethodCalls.get()) {
            String callString = ir.getResultTypeString() + " " + ir.getResultName() + " = " + ir.toExpression() + ";";
            text.append(styled(callString + "\n", StdoutStyle.METHOD_RECORDING));
        }
        newMethodCall = true;
    }
    
    private void methodResult(ExecutionEvent event)
    {
        if (recordMethodCalls.get()) {
            String result = null;
            String resultType = event.getResult();
            
            if (resultType == ExecutionEvent.NORMAL_EXIT) {
                DebuggerObject object = event.getResultObject();
                if (object != null) {
                    if (event.getClassName() != null && event.getMethodName() == null) {
                        // Constructor call - the result object is the created object.
                        // Don't display the result separately:
                        return;
                    }
                    else {
                        // if the method returns a void, we must handle it differently
                        if (object.isNullObject()) {
                            return; // Don't show result of void calls
                        }
                        else {
                            // other - the result object is a wrapper with a single result field
                            DebuggerField resultField = object.getField(0);
                            result = "    returned " + resultField.getType().toString(true) + " ";
                            result += resultField.getValueString();
                        }
                    }
                }
            }
            else if (resultType == ExecutionEvent.EXCEPTION_EXIT) {
                result = "    Exception occurred.";
            }
            else if (resultType == ExecutionEvent.TERMINATED_EXIT) {
                result = "    VM terminated.";
            }
            
            if (result != null) {
                text.append(styled(result + "\n", StdoutStyle.METHOD_RECORDING));
            }
        }
    }

    /**
     * Looks through the contents of the terminal for lines
     * that look like they are part of a stack trace.
     */
    private void scanForStackTrace()
    {
        try {
            String content = errorText.getText();

            Pattern p = java.util.regex.Pattern.compile("at (\\S+)\\((\\S+)\\.java:(\\d+)\\)");
            // Matches things like:
            // at greenfoot.localdebugger.LocalDebugger$QueuedExecution.run(LocalDebugger.java:267)
            //    ^--------------------group 1----------------------------^ ^--group 2--^      ^3^
            Matcher m = p.matcher(content);
            while (m.find())
            {
                String fullyQualifiedMethodName = m.group(1);
                String javaFile = m.group(2);
                int lineNumber = Integer.parseInt(m.group(3));

                // The fully qualified method name will end in ".method", so we can
                // definitely remove that:

                String fullyQualifiedClassName = JavaNames.getPrefix(fullyQualifiedMethodName);
                // The class name may be an inner class, so we want to take the package:
                String packageName = JavaNames.getPrefix(fullyQualifiedClassName);

                //Find out if that file is available, and only link if it is:
                Package pkg = project.getPackage(packageName);

                if (pkg != null && pkg.getAllClassnames().contains(javaFile))
                {
                    errorText.setStyle(m.start(1), m.end(), new StderrStyle(new ExceptionSourceLocation(m.start(1), m.end(), pkg, javaFile, lineNumber)));
                }
                else
                {
                    errorText.setStyle(m.start(), m.end(), StderrStyle.FOREIGN_STACK_TRACE);
                }
            }

            //Also mark up native method lines in stack traces with a marker for font colour:

            p = java.util.regex.Pattern.compile("at \\S+\\((Native Method|Unknown Source)\\)");
            // Matches things like:
            //  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            m = p.matcher(content);
            while (m.find())
            {
                errorText.setStyle(m.start(), m.end(), StderrStyle.FOREIGN_STACK_TRACE);
            }
        }
        catch (NumberFormatException e ) {
            //In case it looks like an exception but has a large line number:
            e.printStackTrace();
        }
    }



    /**
     * Return the input stream that can be used to read from this terminal.
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Reader getReader()
    {
        return in;
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getWriter()
    {
        return out;
    }


    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getErrorWriter()
    {
        return err;
    }

    // ---- BlueJEventListener interface ----

    /**
     * Called when a BlueJ event is raised. The event can be any BlueJEvent
     * type. The implementation of this method should check first whether
     * the event type is of interest an return immediately if it isn't.
     *
     * @param eventId  A constant identifying the event. One of the event id
     *                 constants defined in BlueJEvent.
     * @param arg      An event specific parameter. See BlueJEvent for
     *                 definition.
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        if(eventId == BlueJEvent.METHOD_CALL) {
            InvokerRecord ir = (InvokerRecord) arg;
            if (ir.getResultName() != null) {
                constructorCall(ir);
            }
            else {
                boolean isVoid = ir.hasVoidResult();
                if (isVoid) {
                    methodCall(ir.toStatement());
                }
                else {
                    methodCall(ir.toExpression());
                }
            }
        }
        else if (eventId == BlueJEvent.EXECUTION_RESULT) {
            methodResult((ExecutionEvent) arg);
        }
    }

    // ---- make window frame ----

    /**
     * Show the errorPane for error output
     */
    private void showErrorPane()
    {
        if(errorShown) {
            return;
        }

        if(errorText == null) {
            errorText = new StyledTextArea<Void, StderrStyle>(null, (t, v) -> {}, StderrStyle.NORMAL, this::applyStyle);
            //MOEFX: set size
            //TermTextArea(Config.isGreenfoot() ? 15 : 5, 80, null, project, this, true);
            errorScrollPane = new VirtualizedScrollPane<>(errorText);
            errorText.styleProperty().bind(PrefMgr.getEditorFontCSS(true));
            errorText.setEditable(false);
            errorText.plainTextChanges().subscribe(c -> scanForStackTrace());
            Consumer<MouseEvent> onClick = e ->
            {
                CharacterHit hit = errorText.hit(e.getX(), e.getY());

                StderrStyle style = errorText.getStyleAtPosition(hit.getInsertionIndex());

                if (style.exceptionSourceLocation != null)
                {
                    style.exceptionSourceLocation.showInEditor();
                }
                else
                {
                    // Default behaviour:
                    errorText.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                }
            };
            errorText.setOnOutsideSelectionMousePress(onClick);
            errorText.setOnInsideSelectionMousePressRelease(onClick);
            //MOEFX
            //errorText.setMargin(new Insets(6, 6, 6, 6));
            //MOEFX
            //errorText.setUnlimitedBuffering(true);
        }
        splitPane.getItems().add(errorScrollPane);
        errorShown = true;
    }
    
    /**
     * Hide the pane with the error output.
     */
    private void hideErrorPane()
    {
        if(!errorShown) {
            return;
        }
        splitPane.getItems().remove(errorScrollPane);
        errorShown = false;
    }


    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }
    
    /**
     * Create the terminal's menubar, all menus and items.
     */
    private MenuBar makeMenuBar()
    {
        MenuBar menubar = new MenuBar();
        menubar.setUseSystemMenuBar(true);
        Menu menu = new Menu(Config.getString("terminal.options"));
        MenuItem clearItem = new MenuItem(Config.getString("terminal.clear"));
        clearItem.setOnAction(e -> clear());
        clearItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));

        MenuItem copyItem = new MenuItem(Config.getString("terminal.copy"));
        copyItem.setOnAction(e -> text.copy());
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));

        MenuItem saveItem = new MenuItem(Config.getString("terminal.save"));
        saveItem.setOnAction(e -> save());
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));

        MenuItem printItem = new MenuItem("terminal.print");
        printItem.setOnAction(e -> print());
        printItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(clearItem, copyItem, saveItem, printItem, new SeparatorMenuItem());

        CheckMenuItem autoClear = new CheckMenuItem("terminal.clearScreen");
        autoClear.selectedProperty().bindBidirectional(clearOnMethodCall);

        CheckMenuItem recordCalls = new CheckMenuItem("terminal.recordCalls");
        recordCalls.selectedProperty().bindBidirectional(recordMethodCalls);

        CheckMenuItem unlimitedBuffering = new CheckMenuItem("terminal.buffering");
        unlimitedBuffering.selectedProperty().bindBidirectional(unlimitedBufferingCall);

        menu.getItems().addAll(autoClear, recordCalls, unlimitedBuffering);

        MenuItem closeItem = new MenuItem("terminal.close");
        closeItem.setOnAction(e -> showHide(false));
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(new SeparatorMenuItem(), closeItem);

        menubar.getMenus().add(menu);
        return menubar;
    }

    /**
     * Cleanup any resources or listeners the terminal has created/registered.
     * Called when the project is closing.
     */
    public void cleanup()
    {
        BlueJEvent.removeListener(this);
    }

    /**
     * A Reader which reads from the terminal.
     */
    @OnThread(Tag.Any)
    private class TerminalReader extends Reader
    {
        public int read(char[] cbuf, int off, int len)
        {
            int charsRead = 0;

            while(charsRead < len) {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                if(buffer.isEmpty())
                    break;
            }
            return charsRead;
        }

        @Override
        public boolean ready()
        {
            return ! buffer.isEmpty();
        }
        
        public void close() { }
    }

    /**
     * A writer which writes to the terminal. It can be flagged for error output.
     * The idea is that error output could be presented differently from standard
     * output.
     */
    @OnThread(Tag.Any)
    private class TerminalWriter extends Writer
    {
        private boolean isErrorOut;
        
        TerminalWriter(boolean isError)
        {
            super();
            isErrorOut = isError;
        }

        public void write(final char[] cbuf, final int off, final int len)
        {
            try {
                // We use invokeAndWait so that terminal output is limited to
                // the processing speed of the event queue. This means the UI
                // will still respond to user input even if the output is really
                // gushing.
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        String s = new String(cbuf, off, len);
                        if (isErrorOut)
                        {
                            showErrorPane();
                            writeToPane(errorText, s, StderrStyle.NORMAL);
                        }
                        else
                            writeToPane(text, s, StdoutStyle.OUTPUT);
                    }
                });
            }
            catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            catch (InterruptedException ie) {}
        }

        public void flush() { }

        public void close() { }
    }
}
