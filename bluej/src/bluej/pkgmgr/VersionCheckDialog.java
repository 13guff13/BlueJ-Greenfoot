package bluej.pkgmgr;

import bluej.Main;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.io.*;
import java.net.URL;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog implementing version check functionality.
 *
 * @author  Michael Kolling
 * @version $Id: VersionCheckDialog.java 1077 2002-01-09 09:08:21Z mik $
 */

final class VersionCheckDialog extends JDialog
     implements ActionListener
{
    // Internationalisation
    private static final String close = Config.getString("close");
    private static final String check = Config.getString("pkgmgr.versionDlg.check");
    private static final String dialogTitle = Config.getString("pkgmgr.versionDlg.title");
    private static final String helpLine1 = Config.getString("pkgmgr.versionDlg.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.versionDlg.helpLine2");

    private static final String versionURL = Config.getPropString("bluej.url.versionCheck");

    private JTextArea textArea;
    private JButton closeButton;

    private String newVersion = null;
    private Thread versionThread = null;
    private boolean isClosed = false;

    /**
     * Create a new version check dialogue and make it visible.
     */
    public VersionCheckDialog(PkgMgrFrame parent)
    {
        super(parent, dialogTitle, true);
        makeDialog();
        setVisible(true);
    }

    /**
     * A button was pressed. Check which one and do the right thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();
        if(check.equals(cmd)) {
            doVersionCheck();
            getRootPane().setDefaultButton(closeButton);
        }
        else if(close.equals(cmd))
            doClose();
    }

    /**
     * Action when Close is pressed.
     */
    private void doClose()
    {
        isClosed = true;
        setVisible(false);
    }

    /**
     * Perform a version check. 
     */
    private void doVersionCheck()
    {
        versionThread = new VersionChecker();
        //versionThread.setPriority(Thread.MIN_PRIORITY);
        versionThread.start();
    }



    /**
     * Create the dialog interface.
     */
    private void makeDialog()
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(Config.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            mainPanel.add(helpText2);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            textArea = new JTextArea(14, 46);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            mainPanel.add(scrollPane);
            mainPanel.add(Box.createVerticalStrut(Config.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton checkButton = new JButton(check);
                checkButton.addActionListener(this);

                closeButton = new JButton(close);
                closeButton.addActionListener(this);

                buttonPanel.add(checkButton);
                buttonPanel.add(closeButton);

                getRootPane().setDefaultButton(checkButton);
                checkButton.requestFocus();

                // try to make the OK and cancel buttons have equal width
                closeButton.setPreferredSize(
                                 new Dimension(checkButton.getPreferredSize().width,
                                 closeButton.getPreferredSize().height));
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }


    /**
     * Private class to run the actual version checking in separate thread.
     */
    private class VersionChecker extends Thread
    {
        public VersionChecker()
        {
        }

        /**
         * Do a version check. That is: open a URL connection to the remote 
         * version file and read it. Display version info as appropriate.
         */
        public void run()
        {
            textArea.setText("Making connection to version server...");
            try {
                InputStream is = new URL(versionURL).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                
                if(isOutOfDate(reader)) {
                    if(!isClosed)
                        displayNewVersionInfo(reader);
                }
                else {
                    if(!isClosed)
                        textArea.setText(Config.getString("pkgmgr.versionDlg.upToDate"));
                }
            }
            catch(IOException exc) {
                if(!isClosed)
                    textArea.setText("Error: could not access remote version information");
                Debug.reportError("IO error when trying to access URL\n" + exc);
            }
        }

        /**
         * Given a reader for the (remote) version file, check whether this
         * version is out of date. We know that the first line of the version
         * file contains the up-to-date version number.
         */
        private boolean isOutOfDate(BufferedReader versionReader)
        {
            try {
                newVersion = versionReader.readLine();
                if(newVersion != null)
                    newVersion = newVersion.trim();
            }
            catch(IOException exc) {
                textArea.setText("Error: could not read remote version information");
                Debug.reportError("IO error when reading remote version info\n" + exc);
            }
            return ! Main.BLUEJ_VERSION.equals(newVersion);
        }

        /**
         * Given a reader for the (remote) version file, read the version
         * info text out of it and display it in the text area.
         */
        private void displayNewVersionInfo(BufferedReader versionReader)
        {
            if(newVersion == null)
                textArea.setText("Error: could not read remote version info");
            else {
                textArea.setText(Config.getString("pkgmgr.versionDlg.currentVersion"));
                textArea.append(" ");
                textArea.append(Main.BLUEJ_VERSION);
                textArea.append("\n");
                textArea.append(Config.getString("pkgmgr.versionDlg.newVersion"));
                textArea.append(" ");
                textArea.append(newVersion);
                textArea.append("\n");

                try {
                    String line = versionReader.readLine();
                    while(line != null) {
                        textArea.append(line);
                        textArea.append("\n");
                        line = versionReader.readLine();
                    }
                }
                catch(IOException exc) {
                    Debug.reportError("IO error when reading from version file");
                }
                textArea.setCaretPosition(0);
            }
        }

    } // end class VersionChecker

}
