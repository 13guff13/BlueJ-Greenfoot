package bluej;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 2005 2003-06-03 06:10:09Z ajp $
 */

public class SplashWindow extends JWindow
    implements WindowListener
{
    private JLabel image;

    public SplashWindow()
    {
    	// must start with a forward slash or else Java converts the .
    	// to a /
    	URL iconURL = getClass().getResource("/bluej/splash.jpg");
    	
        ImageIcon icon = new ImageIcon(iconURL);
        // Note: it is intentional that the forward slash is used here
        // for all systems. See the documentation of 
        // getResource()

        image = new JLabel(icon);
        getContentPane().add(image);

        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width)/2,
                    (screenDim.height - getSize().height)/2);
        addWindowListener(this);
        setVisible(true);

        // for testing - if you want to look at it a bit longer...
        //try { Thread.sleep(4000); } catch (Exception e) {}
    }

    /**
     * Remove this splash screen from screen. Since we never need it again,
     * throw it away completely.
     */
    public void remove()
    {
        dispose();
    }

    // -------- WindowListener methods --------

    /**
     * Draw the version string on top of the picture.
     */
    public void windowOpened(WindowEvent e)
    {
        Graphics2D g = (Graphics2D)getGlassPane().getGraphics();
        g.setColor(new Color(96,0,0));
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("Version " + Boot.BLUEJ_VERSION, 20, 
                     image.getHeight()-20);
    }

    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
}

