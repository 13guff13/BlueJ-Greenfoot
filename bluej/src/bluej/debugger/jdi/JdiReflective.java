/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.*;

import bluej.debugger.gentype.*;
import bluej.utility.Debug;

import com.sun.jdi.*;

/**
 * A Reflective for Jdi classes.
 * 
 * @see Reflective.
 * 
 * @author Davin McCall
 */
public class JdiReflective extends Reflective
{
    // For a loaded type, we have a ReferenceType.
    private ReferenceType rclass = null;

    // If the type has not been loaded, we know only it's name, and the
    // type from which we discovered the reference to it:
    protected String name = null;
    private ClassLoaderReference sourceLoader = null;
    private VirtualMachine sourceVM = null;

    /**
     * Constructor - loaded type
     */
    public JdiReflective(ReferenceType rclass)
    {
        super();
        this.rclass = rclass;
        if (rclass == null) {
            Debug.message("JdiReflective: null ReferenceType?");
            throw new NullPointerException();
        }
    }

    /**
     * Constructor - type not loaded yet; source (parent) object available.
     * 
     * @param name
     *            The fully qualified type name
     * @param sourceType
     *            The type from which a reference to this type was obtained
     */
    public JdiReflective(String name, ReferenceType sourceType)
    {
        this.name = name;
        this.sourceLoader = sourceType.classLoader();
        this.sourceVM = sourceType.virtualMachine();
    }

    /**
     * Constructor - type may not yet be loaded; class loader available.
     * 
     * @param name
     *            The fully qualified type name
     * @param classLoader
     *            The class loader used to load this type
     * @param vm
     *            The virtual machine reference
     */
    public JdiReflective(String name, ClassLoaderReference classLoader, VirtualMachine vm)
    {
        this.name = name;
        this.sourceLoader = classLoader;
        this.sourceVM = vm;
    }
    
    /**
     * Get the JDI ReferenceType this reflective represents.
     */
    public ReferenceType getJdiType()
    {
        checkLoaded();
        return rclass;
    }
    
    @Override
    public Reflective getRelativeClass(String name)
    {
        if (rclass != null) {
            return new JdiReflective(name, rclass);
        }
        else {
            return new JdiReflective(name, sourceLoader, sourceVM);
        }
    }

    /**
     * Try to make sure we have a valid reference to the actual type
     */
    protected void checkLoaded()
    {
        if (rclass == null) {
            rclass = findClass(name, sourceLoader, sourceVM);
            if (rclass == null) {
                Debug.message("Attempt to use unloaded type: " + name);
                Debug.message("  name = " +  name + ", sourceLoader = " + sourceLoader);
                Debug.message("  (in JdiReflective.checkLoader()");
                return;
            }
            name = null;
            sourceLoader = null;
            sourceVM = null;
        }
    }

    public String getName()
    {
        if (name != null) {
            return name;
        }
        
        if (rclass.signature().startsWith("[")) {
            return rclass.signature().replace('/', '.');
        }
        else {
            return rclass.name();
        }
    }
    
    @Override
    public boolean isInterface()
    {
        checkLoaded();
        return rclass instanceof InterfaceType;
    }
    
    @Override
    public boolean isStatic()
    {
        checkLoaded();
        return rclass.isStatic();
    }
    
    @Override
    public boolean isPublic()
    {
        checkLoaded();
        return rclass.isPublic();
    }

    public Reflective getArrayOf()
    {
        if (rclass != null) {
            return new JdiArrayReflective(new GenTypeClass(this), rclass);
        }
        else {
            return new JdiArrayReflective(new GenTypeClass(this), sourceLoader, sourceVM);
        }
    }
    
    public List<GenTypeDeclTpar> getTypeParams()
    {
        // Make sure we are loaded and a generic signature is present.
        checkLoaded();
        String gensig = JdiUtils.getJdiUtils().genericSignature(rclass);
        if (gensig == null)
            return Collections.emptyList();
        
        // Read the type parameters from the generic signature.
        StringIterator s = new StringIterator(gensig);
        return getTypeParams(s);
    }

    /**
     * Get the type parameters of this class, as a list of GenTypeDeclTpar,
     * using a pre-existing string iterator to process the generic signature.
     *  
     * @param s  A string iterator, currently at the beginning of the generic
     *           signature for this class. On return, the iterator will be
     *           positioned after the type parameters in the signature, at the
     *           beginning of the class name.
     * 
     * @return  A list of GenTypeDeclTpar, representing the type parameters of
     *          this class.
     */
    private List<GenTypeDeclTpar> getTypeParams(StringIterator s)
    {
        List<GenTypeDeclTpar> rlist = new ArrayList<GenTypeDeclTpar>();

        char c = s.peek();
        if (c != '<')
            return rlist;

        // go through each type parameter, assign it the type from our
        // params list.
        s.next();
        while (c != '>') {
            String paramName = readClassName(s);
            if (s.current() != ':') {
                Debug.message("getTypeParams : no ':' following type parameter name in super signature?? got "
                        + s.current());
                Debug.message("signature was: " + s.getString());
                return Collections.emptyList();
            }
            // '::' indicates lower bound is an interface. Ignore.
            if (s.peek() == ':')
                s.next();

            // multiple bounds appear as T:bound1;:bound2; ... etc
            ArrayList<GenTypeSolid> bounds = new ArrayList<GenTypeSolid>(3);
            while (s.current() == ':') {
                bounds.add((GenTypeSolid) typeFromSignature(s, null, rclass));
                
                //we don't want the next char to be eaten...
                if (s.peek() == ':')
                    s.next();
            }
            rlist.add(new GenTypeDeclTpar(paramName, bounds.toArray(new GenTypeSolid [0])));
            c = s.peek();
        }
        s.next();
        return rlist;
    }
    
    public List<Reflective> getSuperTypesR()
    {
        checkLoaded();
        if (rclass instanceof ClassType) {
            List<Reflective> l = new LinkedList<Reflective>();
            Iterator<InterfaceType> i = ((ClassType) rclass).interfaces().iterator();
            while (i.hasNext())
                l.add(new JdiReflective(i.next()));
            if (((ClassType) rclass).superclass() != null)
                l.add(new JdiReflective(((ClassType) rclass).superclass()));
            return l;
        }
        else if (rclass instanceof InterfaceType) {
            // interface
            List<Reflective> l = new LinkedList<Reflective>();
            Iterator<InterfaceType> i = ((InterfaceType) rclass).superinterfaces().iterator();
            while (i.hasNext())
                l.add(new JdiReflective((ReferenceType) i.next()));
            
            // interfaces with no direct superinterfaces have a supertype of Object
            if (l.isEmpty()) {
                l.add(new JdiReflective("java.lang.Object", this.rclass));
            }
            
            return l;
        }
        else
            return new LinkedList<Reflective>();
    }

    public List<GenTypeClass> getSuperTypes()
    {
        checkLoaded();
        List<GenTypeClass> rlist = new ArrayList<GenTypeClass>();

        if (JdiUtils.getJdiUtils().genericSignature(rclass) == null) {
            if (rclass instanceof ClassType) {
                ClassType ctClass = (ClassType) rclass;

                // superclass
                ClassType superclass = ctClass.superclass();
                if (superclass != null) {
                    Reflective r = new JdiReflective(ctClass.superclass());
                    rlist.add(new GenTypeClass(r));
                }

                // interfaces
                List<InterfaceType> interfaces = ctClass.interfaces();
                for (Iterator<InterfaceType> i = interfaces.iterator(); i.hasNext();) {
                    Reflective r = new JdiReflective(i.next());
                    rlist.add(new GenTypeClass(r));
                }
                return rlist;
            }
            else {
                // rclass must be an InterfaceType
                InterfaceType itClass = (InterfaceType) rclass;

                List<InterfaceType> interfaces = itClass.superinterfaces();
                for (Iterator<InterfaceType> i = interfaces.iterator(); i.hasNext();) {
                    Reflective r = new JdiReflective(i.next());
                    rlist.add(new GenTypeClass(r));
                }
                
                // interfaces with no direct superinterfaces have a supertype of Object
                if (rlist.isEmpty()) {
                    rlist.add(new GenTypeClass(new JdiReflective("java.lang.Object", this.rclass)));
                }

                return rlist;
            }
        }

        // A generic signature for a type looks something like:
        //    <..type params..>Lbase/class<...>;...interfaces...
        // First, skip over the type params in the supertype:

        StringIterator s = new StringIterator(JdiUtils.getJdiUtils().genericSignature(rclass));
        List<GenTypeDeclTpar> l = getTypeParams(s);
        Map<String,GenTypeDeclTpar> declTpars = new HashMap<String,GenTypeDeclTpar>();
        for (Iterator<GenTypeDeclTpar> i = l.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar declTpar = (GenTypeDeclTpar) i.next();
            declTpars.put(declTpar.getTparName(), declTpar); 
        }
        
        // go through each base type in turn.
        while (s.hasNext()) {
            // We now have a base.
            GenTypeClass t = typeFromSignature(s, declTpars, rclass).asClass();
            rlist.add(t);
        }
        return rlist;
    }
    
    public boolean isAssignableFrom(Reflective r)
    {
        if (this.equals(r))
            return true;
        
        // Any reference type, including arrays, can be assigned to Object
        if (getName().equals("java.lang.Object"))
            return true;
        
        if (r instanceof JdiReflective) {
            JdiReflective jr = (JdiReflective) r;
            
            jr.checkLoaded();
            return checkAssignability(rclass, jr.rclass);
        }
        else
            return false;
    }
    
    /**
     * Check that type b is assingable to a variable of type a.
     */
    private static boolean checkAssignability(ReferenceType a, ReferenceType b)
    {
        while (true)
        {
            if (a instanceof ClassType) {
                if (b instanceof InterfaceType) {
                    List<InterfaceType> l = ((ClassType)a).allInterfaces();
                    return l.contains(b);
                }
                else if (b instanceof ClassType) {
                    ClassType classType = (ClassType) a;
                    
                    while (classType != null) {
                        if (b.equals(classType))
                            return true;
                        classType = classType.superclass();
                    }
                }
                return false;
            }
            else if (a instanceof InterfaceType) {
                if (! (b instanceof InterfaceType))
                    return false;
                
                List<InterfaceType> l = new LinkedList<InterfaceType>(); 
                l.addAll(((InterfaceType) a).superinterfaces());
                while (! l.isEmpty()) {
                    // get the first superinterface in the list
                    InterfaceType it = (InterfaceType) l.get(0);
                    if (a.equals(it))
                        return true;
                    
                    l.addAll(it.superinterfaces());
                    l.remove(0);
                }
                return false;
            }
            else if (a instanceof ArrayType) {
                if (! (b instanceof ArrayType))
                    return false;
                
                try {
                    Type an = ((ArrayType) a).componentType();
                    Type bn = ((ArrayType) b).componentType();
                
                    if (an instanceof ReferenceType && bn instanceof ReferenceType) {
                        a = (ReferenceType) an;
                        b = (ReferenceType) bn;
                    }
                    else
                        return false;
                }
                catch (ClassNotLoadedException cnle) {
                    return false;
                }
            }
            else
                // unknown type
                return false;
        }
    }

    /**
     * Find a class by name, using the given class loader.
     * 
     * @param name
     *            The name of the class to find
     * @param lclass
     *            The loading class. The class loader of this class is used to
     *            locate the desired class.
     * @return A ClassType object representing the found class (or null)
     */
    private static ReferenceType findClass(String name, ClassLoaderReference cl, VirtualMachine vm)
    {
        if (cl != null) {
            // See if the desired class was initiated by our class loader.
            Iterator<ReferenceType> i = cl.visibleClasses().iterator();
            while (i.hasNext()) {
                ReferenceType ct = (ReferenceType) i.next();
                if (ct.name().equals(name))
                    return ct;
            }
        }
        else {
            // A null classloader means the bootstrap class loader was used.
            // So look for the class with the correct name which was loaded
            // by the bootstrap loader.
            Iterator<ReferenceType> i = vm.classesByName(name).iterator();
            while (i.hasNext()) {
                ReferenceType ct = (ReferenceType) i.next();
                if (ct.classLoader() == null)
                    return ct;
            }
        }
        
        // Try and load the class.
        VMReference vmr = VMReference.getVmForMachine(vm);
        if (vmr != null) {
           return vmr.loadClass(name, cl);
        }
        
        return null;
    }

    /**
     * Read a class name or type parameter name from a signature string.
     * 
     * @param i
     *            An iterator into the string
     * @return the fully qualified class name
     */
    private static String readClassName(StringIterator i)
    {
        char c = i.next();
        String r = new String();
        while (c != '<' && c != ';' && c != ':') {
            r += (c == '/') ? '.' : c;
            c = i.next();
        }
        return r;
    }

    /**
     * Generate a GenType structure from a type specification found in a
     * signature string, optionally mapping type parameter names to their actual
     * types.
     * 
     * @param i
     *            a StringIterator through the signature string
     * @param tparams
     *            a mapping of type parameter names to types (or null)
     * @param parent
     *            the type in which the signature string is embedded. The class
     *            loader of this type is used to locate embedded types.
     * @return The GenType structure determined from the signature.
     */
    private static GenTypeParameter fromSignature(StringIterator i,
            Map<String,? extends GenTypeParameter> tparams, ReferenceType parent)
    {
        char c = i.peek();
        if (c == '*') {
            // '*' represents an unbounded '?'. For instance, List<?>
            // has signature: Ljava/lang/list<*>;
            i.next();
            JdiReflective objRef = new JdiReflective("java.lang.Object", parent);
            GenTypeClass objClass = new GenTypeClass(objRef);
            return new GenTypeUnbounded(objClass);
        }
        if (c == '+') {
            // ? extends ...
            // The bound must be a solid, but it's possible that tparams map
            // a type parameter (solid) to a wildcard (non-solid).
            i.next();
            GenTypeSolid t = (GenTypeSolid) typeFromSignature(i, null, parent);
            if (tparams != null) {
                return new GenTypeExtends(t).mapTparsToTypes(tparams);
            }
            else {
                return new GenTypeExtends(t);
            }
        }
        if (c == '-') {
            // ? super ...
            // Likewise as for extends.
            i.next();
            GenTypeSolid t = (GenTypeSolid) fromSignature(i, null, parent);
            if (tparams != null) {
                return new GenTypeSuper(t).mapTparsToTypes(tparams);
            }
            else {
                return new GenTypeSuper(t);
            }
        }
        
        return typeFromSignature(i, tparams, parent);
    }
    
    /**
     * Derive a type from a type signature.
     * @param i   An iterator through the signature
     * @param tparams  The type parameters of the parent type (the type from where this signature comes);
     *                 may be null.
     * @param parent   The parent type
     */
    public static JavaType typeFromSignature(StringIterator i,
            Map<String,? extends GenTypeParameter> tparams, ReferenceType parent)
    {
        char c = i.next();
        if (c == '[') {
            // array
            JavaType t = typeFromSignature(i, tparams, parent);
            JdiArrayReflective ar = new JdiArrayReflective(t, parent.classLoader(), parent.virtualMachine());
            return new GenTypeArrayClass(ar, t);
        }
        if (c == 'T') {
            // type parameter
            String tname = readClassName(i);
            if (tparams != null && tparams.get(tname) != null) {
                return tparams.get(tname).getCapture();
            }
            else {
                return new GenTypeTpar(tname);
            }
        }
        if (c == 'I') {
            // integer
            return JavaPrimitiveType.getInt();
        }
        if (c == 'C') {
            // character
            return JavaPrimitiveType.getChar();
        }
        if (c == 'Z') {
            // boolean
            return JavaPrimitiveType.getBoolean();
        }
        if (c == 'B') {
            // byte
            return JavaPrimitiveType.getByte();
        }
        if (c == 'S') {
            // short
            return JavaPrimitiveType.getShort();
        }
        if (c == 'J') {
            // long
            return JavaPrimitiveType.getLong();
        }
        if (c == 'F') {
            // float
            return JavaPrimitiveType.getFloat();
        }
        if (c == 'D') {
            // double
            return JavaPrimitiveType.getDouble();
        }
        if (c == 'V') {
            return JavaPrimitiveType.getVoid();
        }

        if (c != 'L') {
            Debug.message("Generic signature begins without 'L'?? (got " + c + ")");
        }

        String basename = readClassName(i);
        Reflective reflective = new JdiReflective(basename, parent);
        c = i.current();
        if (c == ';')
            return new GenTypeClass(reflective, (List<GenTypeParameter>) null);

        if (c != '<') {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }

        List<GenTypeParameter> params = new ArrayList<GenTypeParameter>();
        do {
            GenTypeParameter ptype = fromSignature(i, tparams, parent);
            if (ptype == null) {
                return null;
            }
            params.add(ptype);
        } while (i.peek() != '>');
        i.next(); // fetch the '>'
        c = i.next(); // fetch the trailing ';' or '.'
        
        GenTypeClass result = new GenTypeClass(reflective, params);

        // if c is now '.', we have an inner class
        if (c == '.') {
            return innerFromSignature(i, basename, result, tparams, parent);
        }
        
        // otherwise assume we have ';'
        return result;
    }

    
    private static GenTypeClass innerFromSignature(StringIterator i, String outerName, GenTypeClass outer,
            Map<String,? extends GenTypeParameter> tparams, ReferenceType parent)
    {
        String basename = readClassName(i);
        String innerName = outerName + '$' + basename;
        Reflective reflective = new JdiReflective(innerName, parent);
            
        char c = i.current();
        if (c == ';')
            return new GenTypeClass(reflective, (List<GenTypeParameter>) null, outer);
        
        if (c == '<') {
            List<GenTypeParameter> params = new ArrayList<GenTypeParameter>();
            do {
                GenTypeParameter ptype = fromSignature(i, tparams, parent);
                if (ptype == null) {
                    return null;
                }
                params.add(ptype);
            } while (i.peek() != '>');
            i.next(); // fetch the '>'
            c = i.next(); // fetch the trailing ';' or '.'
            
            GenTypeClass result = new GenTypeClass(reflective, params, outer);
            
            // if c is now '.', we have an inner class
            if (c == '.')
                return innerFromSignature(i, innerName, result, tparams, parent);

            return result;
        }
        else {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }
    }

    private static JavaType getNonGenericType(String typeName, Type t, ClassLoaderReference clr, VirtualMachine vm)
    {
        if (t instanceof BooleanType)
            return JavaPrimitiveType.getBoolean();
        else if (t instanceof ByteType)
            return JavaPrimitiveType.getByte();
        else if (t instanceof CharType)
            return JavaPrimitiveType.getChar();
        else if (t instanceof DoubleType)
            return JavaPrimitiveType.getDouble();
        else if (t instanceof FloatType)
            return JavaPrimitiveType.getFloat();
        else if (t instanceof IntegerType)
            return JavaPrimitiveType.getInt();
        else if (t instanceof LongType)
            return JavaPrimitiveType.getLong();
        else if (t instanceof ShortType)
            return JavaPrimitiveType.getShort();
        else {
            // Sometimes, we get a class which doesn't really exist
            // eg. as a type argument to a field.
            if (t == null) {
                // TODO specialise TextType a little - after all we know
                //  that this is an object type.
                return new TextType(typeName);
            }
            
            // The class may or may not be loaded.
            String tname = t.signature();
            if (tname.startsWith("[")) {
                JavaType arrayType = typeFromSignature(new StringIterator(tname), null, (ReferenceType) t);
                return arrayType;
            }
            else {
                tname = t.name();
            }
            Reflective ref = new JdiReflective(tname, clr, vm);
            return new GenTypeClass(ref, (List<GenTypeParameter>) null);
        }
    }

    /**
     * Convert a type name (as returned from JdiField.typeName() and so on) to a binary name
     * that can be passed to a classloader.
     */
    private static String typeNameToBinaryName(String typeName)
    {
        int arrIndex = typeName.indexOf('[');
        if (arrIndex == -1) {
            return typeName;
        }
        
        String binName = "L" + typeName.substring(0, arrIndex) + ";";
        do {
            binName = "[" + binName;
            arrIndex = typeName.indexOf('[', arrIndex + 1);
        } while (arrIndex != -1);
        
        return binName;
    }
    
    /**
     * Determine the complete declared type of an instance field.
     * 
     * @param f
     *            The field
     * @param parent
     *            The object in which the field is located
     * @return The type of the field value
     */
    public static JavaType fromField(Field f, JdiObject parent)
    {
        Type t = null;

        // For a field whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "f.type()" throws a
        // ClassNotLoadedException.

        try {
            t = f.type();
        }
        catch (ClassNotLoadedException cnle) {
            t = findClass(typeNameToBinaryName(f.typeName()), f.declaringType().classLoader(), f.virtualMachine());
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(f);

        // check for primitive type, or raw type
        //if (gensig == null && (rt == null || rt.genericSignature() != null))
        if (gensig == null) {
            return getNonGenericType(f.typeName(), t, parent.obj.referenceType().classLoader(), parent.obj
                    .virtualMachine());
        }
        
        // generic version.
        GenTypeClass genType = parent.getGenType();
        
        // Map from containing object type to the type in which the field was
        // declared. Then extract the type parameter mappings.
        Map<String,GenTypeParameter> tparams = genType.mapToSuper(f.declaringType().name()).getMap();
        if (tparams == null) {
            // raw parent
            Reflective r = new JdiReflective(f.typeName(), parent.obj.referenceType());
            return new GenTypeClass(r);
        }
        
        StringIterator iterator = new StringIterator(gensig);

        // Parse the signature, using the determined tpar mappings.
        return typeFromSignature(iterator, tparams, parent.obj.referenceType());
    }

    /**
     * Determine the complete type of a class field.
     * 
     * @param f
     *            The field
     * @param parent
     *            The object in which the field is located
     * @return The type of the field value
     */
    public static JavaType fromField(Field f)
    {
        Type t = null;
        ReferenceType parent = f.declaringType();

        // For a field whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "f.type()" throws a
        // ClassNotLoadedException.

        try {
            t = f.type();
        }
        catch (ClassNotLoadedException cnle) {
            t = findClass(typeNameToBinaryName(f.typeName()), f.declaringType().classLoader(), f.virtualMachine());
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(f);

        if (gensig == null) {
            return getNonGenericType(f.typeName(), t, parent.classLoader(), parent.virtualMachine());
        }

        // if the generic signature wasn't null, get the type from it.
        StringIterator iterator = new StringIterator(gensig);
        return typeFromSignature(iterator, null, parent);
    }

    /**
     * Determine the complete type of a local variable,
     * 
     * @param sf
     *            The stack frame containing the variable
     * @param var
     *            The local variable
     * @return The type of the field value
     */
    public static JavaType fromLocalVar(StackFrame sf, LocalVariable var)
    {
        Type t = null;
        
        // For a variable whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "var.type()" throws a
        // ClassNotLoadedException.

        Location l = sf.location();
        ReferenceType declType = l.declaringType();

        try {
            t = var.type();
        }
        catch (ClassNotLoadedException cnle) {
            t = findClass(typeNameToBinaryName(var.typeName()), declType.classLoader(), sf.virtualMachine());
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(var);

        if (gensig == null) {
            return getNonGenericType(var.typeName(), t, declType.classLoader(), sf.virtualMachine());
        }

        // if the generic signature wasn't null, get the type from it.
        StringIterator iterator = new StringIterator(gensig);
        Map<String,GenTypeParameter> tparams = new HashMap<String,GenTypeParameter>();
        addDefaultParamBases(tparams, new JdiReflective(declType));
        return typeFromSignature(iterator, tparams, declType);
    }

    /**
     * For all type parameters which don't already occur in the given Map,
     * add them in as the "default" given by the class definition. For instance
     * with List&lt;T&gt;, add in the mapping "T : ? extends Object".
     * @param tparams       The map (String -> GenTypeClass)
     * @param declaringType the type for which to add default mappings
     */
    private static void addDefaultParamBases(Map<String,GenTypeParameter> tparams,
            JdiReflective declaringType)
    {
        while (declaringType != null) {
            Iterator<GenTypeDeclTpar> i = declaringType.getTypeParams().iterator();
            
            while( i.hasNext() ) {
                GenTypeDeclTpar tpar = i.next();
                
                String paramName = tpar.getTparName();
                
                GenTypeSolid [] ubounds = tpar.upperBounds();
                GenTypeWildcard type = new GenTypeWildcard(ubounds, new GenTypeSolid[0]);
                if( ! tparams.containsKey(paramName)) {
                    tparams.put(paramName, type);
                }
            }
            declaringType = declaringType.getOuterType();
        }
    }
    
    /**
     * Get the reflective representing the outer class of this reflective,
     * or null if none.
     */
    private JdiReflective getOuterType()
    {
        checkLoaded();
        String myName = getName();
        int x = myName.indexOf('$');
        if (x != -1 && ! rclass.isStatic()) {
            String outerName = myName.substring(0, x);
            return (JdiReflective) getRelativeClass(outerName);
        }
        else
            return null;
    }
    
    @Override
    public Map<String,FieldReflective> getDeclaredFields()
    {
        checkLoaded();
        List<Field> fields = rclass.fields();
        Map<String,FieldReflective> rfields = new HashMap<String,FieldReflective>();
        
        for (Field field : fields) {
            String genSig = field.genericSignature();
            if (genSig == null) {
                genSig = field.signature();
            }
            
            StringIterator i = new StringIterator(genSig);
            JavaType ftype = typeFromSignature(i, null, rclass);
            FieldReflective fref = new FieldReflective(field.name(),
                    ftype, field.modifiers());
            rfields.put(field.name(), fref);
        }
        
        return rfields;
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        checkLoaded();
        List<Method> methods = rclass.methods();
        Map<String,Set<MethodReflective>> methodMap = new HashMap<String,Set<MethodReflective>>();
        
        for (Method method : methods) {
            if (method.isSynthetic()) {
                continue;
            }
            
            // Process the string signature to determine return and param types
            String genSig = method.genericSignature();
            if (genSig == null) {
                genSig = method.signature();
            }
            
            StringIterator i = new StringIterator(genSig);
            List<GenTypeDeclTpar> tparTypes = getTypeParams(i);

            char c = i.next();
            if (c != '(') {
                continue;
            }
            
            List<JavaType> paramTypes = new ArrayList<JavaType>();
            while (i.peek() != ')') {
                paramTypes.add(typeFromSignature(i, null, rclass));
            }
            
            i.next(); // skip ')'
            JavaType returnType = typeFromSignature(i, null, rclass);
            
            boolean isVarArgs = method.isVarArgs();
            int modifiers = method.modifiers();
            
            MethodReflective mr = new MethodReflective(method.name(), returnType, tparTypes,
                    paramTypes, this, isVarArgs, modifiers);
            
            Set<MethodReflective> mset = methodMap.get(mr.getName());
            if (mset == null) {
                mset = new HashSet<MethodReflective>();
                methodMap.put(mr.getName(), mset);
            }
            
            mset.add(mr);
        }
        
        return methodMap;
    }
    
    @Override
    public List<Reflective> getInners()
    {
        return Collections.emptyList(); // not implemented
    }
    
    static class StringIterator
    {
        int i = 0;
        String s;

        public StringIterator(String s)
        {
            this.s = s;
            if (s == null)
                Debug.message("StringIterator with null string??");
        }

        public char current()
        {
            return s.charAt(i - 1);
        }

        public char next()
        {
            return s.charAt(i++);
        }

        public char peek()
        {
            return s.charAt(i);
        }

        public boolean hasNext()
        {
            return i < s.length();
        }
        
        public String getString()
        {
            return s;
        }
    };
}
