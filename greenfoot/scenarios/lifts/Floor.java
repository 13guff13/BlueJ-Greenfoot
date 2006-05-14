import greenfoot.World;
import greenfoot.Actor;

import java.util.Random;
import java.util.List;
import java.util.Iterator;

public class Floor extends Actor
{
    private static final Random random = Building.getRandomizer();

    private int floorNumber;
    private Button button;
    
    public Floor()
    {
        this(0);
    }

    public Floor(int floorNumber)
    {
        this.floorNumber = floorNumber;
    }
    
    public void addedToWorld(World world)
    {
        button = new Button();
        world.addObject(button, getX()+78, getY());
    }
    
    /**
     * Do the regular simulation action. For a floor, that is: produce a new
     * person every now and then.
     */
    public void act()
    {
        if(random.nextFloat() < 0.005) {
            Person p = new Person(this,
                                  (Building)getWorld());
            getWorld().addObject(p,getX() + random.nextInt(68),getY() + 8);
        }
    }
    
    /**
     * Return this floor's number.
     */
    public int getFloorNumber()
    {
        return floorNumber;
    }
    
    /**
     * Press a button to call a lift to this floor.
     */
    public void pressButton(int direction)
    {
        button.press(direction);
    }

    /**
     * Press a button to call a lift to this floor.
     */
    public void clearButton(int direction)
    {
        button.clear(direction);
    }

}