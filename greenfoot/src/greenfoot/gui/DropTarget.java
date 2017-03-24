package greenfoot.gui;

import java.awt.Point;

/**
 * Interface that components can use for accepting dropping of objects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: DropTarget.java 3633 2005-10-03 09:37:21Z polle $
 */
public interface DropTarget
{
    /**
     * Tells this component to do whatever when a component is dragged along it.
     * 
     * The component is responsible for repainting itself.
     * 
     * @param o
     * @return true if the drag was processed, false otherwise
     */
    public boolean drag(Object o, Point p);

    /**
     * Drops the object to this component if possible.
     * 
     * @param o
     * @return true if the drop was succesfull, false otherwise
     */
    public boolean drop(Object o, Point p);

    /**
     * A drag has ended on this component (the object has been dragged off
     * the component) - do cleanup
     */
    public void dragEnded(Object o);
}
