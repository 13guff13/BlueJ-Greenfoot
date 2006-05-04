package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.GreenfootUtil.ImageWaiter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * A dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 * 
 * @author Davin McCall
 * @version $Id: ImageLibFrame.java 4085 2006-05-04 14:39:17Z davmac $
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener
{
    /** label displaying the currently selected image */
    private JLabel imageLabel;
    private GClass gclass;
    private Icon defaultIcon;
    
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    private Action okAction;
    
    private File selectedImageFile;
    private File projImagesDir;
    private String className;
    
    public static int OK = 0;
    public static int CANCEL = 1;
    private int result = CANCEL;
    
    private Image generatedImage;
    private boolean showingGeneratedImage;
    
    private int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

    
    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     * 
     * @param owner      The parent frame
     * @param classView  The ClassView of the existing class
     */
    public ImageLibFrame(JFrame owner, ClassView classView)
    {
        // TODO i18n
        super(owner, "Select class image: " + classView.getClassName(), true);
        // setIconImage(BlueJTheme.getIconImage());
        
        this.gclass = classView.getGClass();
        generatedImage = renderImage();
        if (generatedImage != null) {
            showingGeneratedImage = true;
            defaultIcon = new ImageIcon(GreenfootUtil.getScaledImage(generatedImage, dpi/2, dpi/2));
        }
        else {
            showingGeneratedImage = false;
            defaultIcon = getPreviewIcon(new File(new File("images"), "greenfoot-logo.png"));
        }
        
        buildUI(false);
    }
    
    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     * 
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     */
    public ImageLibFrame(JFrame owner, GClass superClass)
    {
        super(owner, "New class", true);
        
        defaultIcon = getClassIcon(superClass, getPreviewIcon(new File(new File("images"), "greenfoot-logo.png")));
        showingGeneratedImage = false;
        
        // this.classView = new ClassView()
        buildUI(true);
    }
    
    private void buildUI(boolean includeClassNameField)
    {
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        
        okAction = new AbstractAction("Ok") {
            public void actionPerformed(ActionEvent e)
            {
                result = OK;
                if (showingGeneratedImage) {
                    try {
                        selectedImageFile = writeGeneratedImage();
                        if (selectedImageFile == null) {
                            // cancelled by user.
                            return;
                        }
                    }
                    catch (IOException ioe) {
                        // TODO: report with dialog
                        ioe.printStackTrace();
                    }
                }
                setVisible(false);
                dispose();
            }
        };
        
        {
            JPanel classDetailsPanel = new JPanel();
            classDetailsPanel.setLayout(new BoxLayout(classDetailsPanel, BoxLayout.Y_AXIS));
            
            // Show current image
            {
                JPanel currentImagePanel = new JPanel();
                currentImagePanel.setLayout(new BoxLayout(currentImagePanel, BoxLayout.X_AXIS));
                
                if (includeClassNameField) {
                    Box b = new Box(BoxLayout.X_AXIS);
                    JLabel classNameLabel = new JLabel("New class name:");
                    b.add(classNameLabel);
                    
                    // "ok" button should be disabled until class name entered
                    okAction.setEnabled(false);
                    
                    final JTextField classNameField = new JTextField(12);
                    classNameField.getDocument().addDocumentListener(new DocumentListener() {
                        private void change()
                        {
                            int length = classNameField.getDocument().getLength();
                            okAction.setEnabled(length != 0);
                            try {
                                className = classNameField.getDocument().getText(0, length);
                            }
                            catch (BadLocationException ble) {}
                        }
                        
                        public void changedUpdate(DocumentEvent e)
                        {
                            // Nothing to do
                        }
                        
                        public void insertUpdate(DocumentEvent e)
                        {
                            change();
                        }
                        
                        public void removeUpdate(DocumentEvent e)
                        {
                            change();
                        }
                    });
                    
                    b.add(Box.createHorizontalStrut(spacingLarge));
                    b.add(fixHeight(classNameField));
                    b.setAlignmentX(0.0f);
                    
                    classDetailsPanel.add(b);
                    classDetailsPanel.add(Box.createVerticalStrut(spacingLarge));
                }
                
                // help label
                JLabel helpLabel = new JLabel();
                if (showingGeneratedImage) {
                    helpLabel.setText("Click Ok to accept the auto-generated image,"
                            + " or select an image from the list below.");
                }
                else {
                    helpLabel.setText("Select an image for the class from the list below.");
                }
                Font smallFont = helpLabel.getFont().deriveFont(Font.ITALIC, 11.0f);
                helpLabel.setFont(smallFont);
                classDetailsPanel.add(fixHeight(helpLabel));

                classDetailsPanel.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        
                classDetailsPanel.add(fixHeight(new JSeparator()));

                // new class image display 
                JLabel classImageLabel = new JLabel("New class image:");
                currentImagePanel.add(classImageLabel);
                
                Icon icon;
                if (showingGeneratedImage) {
                    icon = defaultIcon;
                }
                else {
                    icon = getClassIcon(gclass, defaultIcon);
                }
                imageLabel = new JLabel(icon);
                if (showingGeneratedImage) {
                    imageLabel.setText("(auto-generated)");
                }
                currentImagePanel.add(imageLabel);
                currentImagePanel.setAlignmentX(0.0f);
                
                classDetailsPanel.add(fixHeight(currentImagePanel));
            }
            
            classDetailsPanel.setAlignmentX(0.0f);
            contentPane.add(fixHeight(classDetailsPanel));
        }
        
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        
        // Image selection panels - project and greenfoot image library
        {
            JPanel imageSelPanels = new JPanel();
            imageSelPanels.setLayout(new GridLayout(1, 2, BlueJTheme.componentSpacingSmall, 0));
            
            // Project images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel("Project images:");
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                try {
                    GreenfootMain greenfootInstance = GreenfootMain.getInstance();
                    File projDir = greenfootInstance.getProject().getDir();
                    projImagesDir = new File(projDir, "images");
                    projImageList = new ImageLibList(projImagesDir);
                    jsp.getViewport().setView(projImageList);
                }
                catch (ProjectNotOpenException pnoe) {}
                catch (RemoteException re) { re.printStackTrace(); }
                
                jsp.setBorder(Config.normalBorder);
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            // Category selection panel
            ImageCategorySelector imageCategorySelector;
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel("Image Categories:");
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                File imageDir = Config.getBlueJLibDir();
                imageDir = new File(imageDir, "imagelib");
                imageCategorySelector = new ImageCategorySelector(imageDir);
                
                jsp.getViewport().setView(imageCategorySelector);
                
                jsp.setBorder(Config.normalBorder);
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            // Greenfoot images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel("Library images:");
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                greenfootImageList = new ImageLibList();
                jsp.getViewport().setView(greenfootImageList);
                
                jsp.setBorder(Config.normalBorder);
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            imageSelPanels.setAlignmentX(0.0f);
            contentPane.add(imageSelPanels);
            
            projImageList.addListSelectionListener(this);
            greenfootImageList.addListSelectionListener(this);
            imageCategorySelector.setImageLibList(greenfootImageList);
        }

        // Browse button. Select image file from arbitrary location.
        JButton browseButton = new JButton("Browse for more images ...");
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser();
                new ImageFilePreview(chooser);
                int choice = chooser.showDialog(ImageLibFrame.this, "Select");
                if (choice == JFileChooser.APPROVE_OPTION) {
                    selectedImageFile = chooser.getSelectedFile();
                    imageLabel.setIcon(getPreviewIcon(selectedImageFile));
                }
            }
        });
        browseButton.setAlignmentX(0.0f);
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(browseButton));
        
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(new JSeparator()));
        
        // Ok and cancel buttons
        {
            JPanel okCancelPanel = new JPanel();
            okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.setAction(okAction);
            
            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    result = CANCEL;
                    selectedImageFile = null;
                    setVisible(false);
                    dispose();
                }
            });
            
            okCancelPanel.add(Box.createHorizontalGlue());
            okCancelPanel.add(okButton);
            okCancelPanel.add(Box.createHorizontalStrut(spacingLarge));
            okCancelPanel.add(cancelButton);
            okCancelPanel.setAlignmentX(0.0f);
            okCancelPanel.validate();
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(okCancelPanel));
            
            getRootPane().setDefaultButton(okButton);
        }
        
        pack();
        DialogManager.centreDialog(this);
        setVisible(true);
    }
    
    /*
     * A new image was selected in one of the ImageLibLists
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            imageLabel.setText("");
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedEntry();
            
            if (ile != null) {
                showingGeneratedImage = false;
                imageLabel.setIcon(getPreviewIcon(ile.imageFile));
                selectedImageFile = ile.imageFile;
            }
        }
    }
    
    /**
     * Get a preview icon for a class. This is a fixed size image.
     * 
     * @param gclass   The class whose icon to get
     */
    private static Icon getClassIcon(GClass gclass, Icon defaultIcon)
    {
        String imageName = null;
        
        if (gclass == null) {
            return defaultIcon;
        }
        
        while (gclass != null) {
            imageName = gclass.getClassProperty("image");
            
            // If an image is specified for this class, and we can read it, return
            if (imageName != null) {
                File imageFile = new File(new File("images"), imageName);
                if (imageFile.canRead()) {
                    return getPreviewIcon(imageFile);
                }
            }
            
            gclass = gclass.getSuperclass();
        }
        
        return defaultIcon;
    }
    
    /**
     * Load an image from a file and scale it to preview size.
     * @param fname  The file to load the image from
     */
    private static Icon getPreviewIcon(File fname)
    {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        
        try {
            BufferedImage bi = ImageIO.read(fname);
            return new ImageIcon(GreenfootUtil.getScaledImage(bi, dpi/2, dpi/2));
        }
        catch (IOException ioe) {
            BufferedImage bi = new BufferedImage(dpi/2, dpi/2, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(bi);
        }
    }
    
    /**
     * Fix the maxiumum height of the component equal to its preferred size, and
     * return the component.
     */
    private static Component fixHeight(Component src)
    {
        Dimension d = src.getMaximumSize();
        d.height = src.getPreferredSize().height;
        src.setMaximumSize(d);
        return src;
    }
    
    /**
     * Get the selected image file (null if dialog was canceled)
     */
    public File getSelectedImageFile()
    {
        if (result == OK) {
            return selectedImageFile;
        }
        else {
            return null;
        }
    }
    
    /**
     * Get the result from the dialog: OK or CANCEL
     */
    public int getResult()
    {
        return result;
    }
    
    /**
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * Try to get an image for the class by instantiating it, and grabbing the image from
     * the resulting object. Returns null if unsuccessful (or if the "generated" image is
     * really the same as the current class image).
     */
    private Image renderImage()
    {
        Object object = null;
        Class cls;
        try {
            cls = gclass.getJavaClass();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
            cls = null;
        }
        catch (RemoteException re) {
            re.printStackTrace();
            cls = null;
        }
        catch (bluej.extensions.ClassNotFoundException cnfe) {
            cls = null;
        }
        
        if (cls == null) {
            return null;
        }
        try {
            Constructor constructor = cls.getConstructor(new Class[]{});

            if (!Modifier.isAbstract(cls.getModifiers())) {
                object =  constructor.newInstance((Object []) null);
            }
        }
        catch (SecurityException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        catch (NoSuchMethodException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Throwable t) {
            // *Whatever* is thrown by user code, we want to catch it.
            t.printStackTrace();
        }
            
        if (object == null) {
            return null;
        }
        else if (object instanceof Actor) {
            Actor so = (Actor) object;
            GreenfootImage image = ActorVisitor.getDisplayImage(so);

            if (image != null) {
                Image awtImage = image.getAWTImage();
                //rotate it.
                int rotation = so.getRotation();
                if (image != null && rotation != 0) {
                    BufferedImage bImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = (Graphics2D) bImg.getGraphics();

                    double rotateX = image.getWidth() / 2.;
                    double rotateY = image.getHeight() / 2.;
                    g2.rotate(Math.toRadians(so.getRotation()), rotateX, rotateY);

                    ImageWaiter imageWaiter = new ImageWaiter(image.getAWTImage());
                    imageWaiter.drawWait(g2, 0, 0);

                    awtImage = bImg;
                }
                World world = so.getWorld();
                if(world != null) {
                    world.removeObject(so);
                } 

                GreenfootImage classImage = GreenfootMain.getProjectProperties().getImage(gclass.getQualifiedName());
                if (classImage != null && classImage.getAWTImage().equals(awtImage)) {
                    // "generated" image is actually just the class image
                    return null;
                }
                else {
                    return awtImage;
                }
            }
        }
        return null;
    }
    
    /**
     * Write the generate image to a file, and return the filename used.
     * 
     * @return The filename the image was written to, or null if cancelled.
     * @throws IOException
     */
    private File writeGeneratedImage()
        throws IOException
    {
        File f = new File(new File("images"), gclass.getName() + ".png");
        if (f.exists()) {
            int r = JOptionPane.showOptionDialog(this, "The file \"" + f + "\" already exists."
                    + "Do you want to overwrite it?", "Confirm file replace",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, null, null);
            if (r != JOptionPane.OK_OPTION) {
                return null;
            }
        }
        
        ImageIO.write((RenderedImage) generatedImage, "png", new FileOutputStream(f));
        
        return f;
    }

}
