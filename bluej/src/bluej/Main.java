package bluej;

import bluej.utility.Debug;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.debugger.Debugger;
import bluej.debugger.MachineLoader;
import bluej.classmgr.ClassMgrPrefPanel;
import bluej.prefmgr.MiscPrefPanel;
import bluej.prefmgr.PrefMgr;

import java.io.File;
import java.util.StringTokenizer;

/**
 * This is the main entry point to BlueJ. Normal invocations start
 * in this class's main method.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Main.java 839 2001-04-12 04:55:46Z mik $
 */
public class Main
{
    public static int BLUEJ_VERSION_MAJOR = 1;
    public static int BLUEJ_VERSION_MINOR = 1;
    public static int BLUEJ_VERSION_RELEASE = 3;
    public static String BLUEJ_VERSION_SUFFIX = "";


    public static int BLUEJ_VERSION_NUMBER = BLUEJ_VERSION_MAJOR * 1000 +
                                             BLUEJ_VERSION_MINOR * 100 +
                                             BLUEJ_VERSION_RELEASE;

    public static String BLUEJ_VERSION = "" + BLUEJ_VERSION_MAJOR + "." +
                                         BLUEJ_VERSION_MINOR + "." +
                                         BLUEJ_VERSION_RELEASE +
                                         BLUEJ_VERSION_SUFFIX;


    public static String BLUEJ_VERSION_TITLE = "BlueJ " + BLUEJ_VERSION;

    public static String BLUEJ_JAR = "bluej.jar";

    /**
     * Entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public static void main(String[] args) {

        if((args.length >= 1) && "-version".equals(args[0])) {
            System.out.println("BlueJ version " + BLUEJ_VERSION
                               + " (Java version "
                               + System.getProperty("java.version")
                               + ")");
            System.out.println("--");

            System.out.println("virtual machine: "
                               + System.getProperty("java.vm.name")
                               + " "
                               + System.getProperty("java.vm.version")
                               + " ("
                               + System.getProperty("java.vm.vendor")
                               + ")");

            System.out.println("running on: "
                               + System.getProperty("os.name")
                               + " "
                               + System.getProperty("os.version")
                               + " ("
                               + System.getProperty("os.arch")
                               + ")");
            System.exit(-1);
        }

        String bluejLib = findBlueJLib();
        if(bluejLib == null) {
            Debug.reportError("Cannot find lib directory");
            Debug.reportError("A file named " + BLUEJ_JAR + " should be in the classpath!");
            System.exit(-1);
        }

        SplashWindow splash = new SplashWindow(bluejLib);
        Config.initialise(bluejLib);
        PrefMgr.initialise();

        MiscPrefPanel.register();
        ClassMgrPrefPanel.register();

        // start the MachineLoader (a separate thread) to load the
        // remote virtual machine in the background

        MachineLoader machineLoader = new MachineLoader();
        // lower priority to improve GUI response time
        machineLoader.setPriority(Thread.currentThread().getPriority() - 1);
        machineLoader.start();

        processArgs(args);
        splash.remove();
    }

    /**
     * Start everything off. This is used to open the projects
     * specified on the command line when starting BlueJ.
     */
    public static void processArgs(String[] args)
    {
        boolean oneOpened = false;
        if(args.length > 0) {
            for(int i = 0; i < args.length; i++) {
                Project openProj;
                if((openProj = Project.openProject(args[i])) != null) {
                    oneOpened = true;

                    Package pkg = openProj.getPackage(
                                    openProj.getInitialPackageName());

                    PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                    pmf.setLocation(i*30 + 20, i*30 + 20);
                    pmf.show();
                }
            }
        }

        if(args.length == 0 || !oneOpened) {
            // no arguments, so start an empty package manager window
            PkgMgrFrame frame = PkgMgrFrame.createFrame();
            frame.setLocation(20, 20);
            frame.show();
        }
    }

    /**
     * Find out and return the directory path of BlueJ's lib directory,
     * including trailing slash or backslash.
     * Return null, if we can't find it.
     */
    private static String findBlueJLib()
    {
        String bluejLib = null;
        String classpath = System.getProperty("java.class.path");

        StringTokenizer st = new StringTokenizer(classpath, 
                                                 File.pathSeparator);
        while(st.hasMoreTokens()) {
            String entry = st.nextToken();
            if(entry.endsWith(BLUEJ_JAR)) {
                bluejLib = entry.substring(0, 
                                     entry.length() - BLUEJ_JAR.length());
                break;
            }
        }
        return bluejLib;
    }

    /**
     * Exit BlueJ.
     *
     * The open frame count should be zero by this point as PkgMgrFrame
     * is responsible for cleaning itself up before getting here.
     */
    public static void exit()
    {
        if (PkgMgrFrame.frameCount() > 0)
            Debug.reportError("Frame count was not zero when exiting. Work may not have been saved");

        // save configuration properties
        Config.handleExit();
        // free resources for debugger
        Debugger.handleExit();
        // exit with success status
        System.exit(0);
    }
}
