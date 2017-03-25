import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;
import greenfoot.Greenfoot;
import greenfoot.MouseInfo;

import java.awt.Color;

public class Paddle extends Actor
{
    public Paddle()
    {
        GreenfootImage pic = new GreenfootImage(60, 10);
        pic.setColor(Color.GREEN);
        pic.fill();
        setImage(pic);
    }
    
    public void setLocation(int x, int y)
    {
        y = BrickWorld.SIZEY - 20;
        super.setLocation(x,y);
    }

    public void act()
    {
        // The paddle can be dragged with the mouse, but it can also be
        // controlled by the directional arrow keys
        
        int xdir = 0;
        if (Greenfoot.isKeyDown("left")) {
            xdir = -3;
        }
        if (Greenfoot.isKeyDown("right")) {
            xdir = 3;
        }
        
        int newx = getX() + xdir;
        if (newx >= 0 && newx < getWorld().getWidth()) {
            super.setLocation(newx,getY());
        }
        
        MouseInfo minfo = Greenfoot.getMouseInfo();
        if (minfo != null) {
            super.setLocation(minfo.getX(), getY());
        }
    }
}
