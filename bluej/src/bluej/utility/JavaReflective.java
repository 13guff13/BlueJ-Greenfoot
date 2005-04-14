package bluej.utility;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import bluej.debugger.gentype.GenType;
import bluej.debugger.gentype.Reflective;

/**
 * A reflective for GenTypeClass which uses the standard java reflection API.  
 * 
 * @author Davin McCall
 * @version $Id: JavaReflective.java 3347 2005-04-14 02:00:15Z davmac $
 */
public class JavaReflective extends Reflective {

    private Class c;
    
    public JavaReflective(Class c)
    {
        this.c = c;
    }
    
    public String getName()
    {
        return c.getName();
    }

    public boolean isInterface()
    {
        return c.isInterface();
    }
    
    public boolean isStatic()
    {
        return (c.getModifiers() & Modifier.STATIC) != 0;
    }
    
    public List getTypeParams()
    {
        return JavaUtils.getJavaUtils().getTypeParams(c);
    }
    
    public Reflective getArrayOf()
    {
        String rname;
        if (c.isArray())
            rname = "[" + c.getName();
        else
            rname = "[L" + c.getName() + ";";
        
        try {
            Class arrClass = c.getClassLoader().loadClass(rname);
            return new JavaReflective(arrClass);
        }
        catch (ClassNotFoundException cnfe) {}
        
        return null;
    }
    
    public Reflective getRelativeClass(String name)
    {
        try {
            Class cr = c.getClassLoader().loadClass(name);
            return new JavaReflective(cr);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    public List getSuperTypesR() {
        List l = new ArrayList();
        
        Class superclass = c.getSuperclass();
        if( superclass != null )
            l.add(new JavaReflective(superclass));

        Class [] interfaces = c.getInterfaces();
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(new JavaReflective(interfaces[i]));
        }
        return l;
    }

    public List getSuperTypes() {
        List l = new ArrayList();
        
        GenType superclass = JavaUtils.getJavaUtils().getSuperclass(c);
        if( superclass != null )
            l.add(superclass);
        
        GenType [] interfaces = JavaUtils.getJavaUtils().getInterfaces(c);
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(interfaces[i]);
        }
        return l;
    }
    
    /**
     * Get the underlying class (as a java.lang.Class object) that this
     * reflective represents.
     */
    public Class getUnderlyingClass()
    {
        return c;
    }

    public boolean isAssignableFrom(Reflective r)
    {
        if (r instanceof JavaReflective) {
            return c.isAssignableFrom(((JavaReflective)r).getUnderlyingClass());
        }
        else
            return false;
    }
}
