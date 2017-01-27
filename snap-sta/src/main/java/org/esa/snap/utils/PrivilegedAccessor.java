/*
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * a.k.a. The "ObjectMolester"
 * <p>
 * This class is used to access a method or field of an object no matter what
 * the access modifier of the method or field. The syntax for accessing fields
 * and methods is out of the ordinary because this class uses reflection to peel
 * away protection.
 * <p>
 * Here is an example of using this to access a private member.
 * <code>resolveName</code> is a private method of <code>Class</code>.
 *
 * <pre>
 * Class c = Class.class;
 * System.out.println(PrivilegedAccessor.invokeMethod(c, "resolveName","/ise/library/PrivilegedAccessor"));
 * </pre>
 *
 * @author Charlie Hubbard (chubbard@iss.net)
 * @author Prashant Dhokte (pdhokte@iss.net)
 * @author Dale Anson (danson@germane-software.com)
 */
public class PrivilegedAccessor {
 /**
     * Gets the value of the named field and returns it as an object.
     *
     * @param instance  the object instance
     * @param fieldName the name of the field
     * @return an object representing the value of the field
     */
    public static Object getValue(Object instance, String fieldName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    public static void setValue(Object instance, String fieldName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * Gets the value of the named static field and returns it as an object.
     *
     * @param c         the class containing the static field
     * @param fieldName the name of the field
     * @return an object representing the value of the field
     */
    public static Object getStaticValue(Class c, String fieldName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(c, fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    /**
     * Sets the value of the named static field.
     *
     * @param c         the class containing the static field
     * @param fieldName the name of the field
     */
    public static void setStaticValue(Class c, String fieldName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(c, fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * Calls a method on the given object instance with the given argument.
     *
     * @param instance      the object instance
     * @param methodName    the name of the method to invoke
     * @param arg           the argument to pass to the method
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object[])
     */
    public static Object invokeMethod(Object instance, String methodName, Object arg)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object[] args = new Object[1];
        args[0] = arg;
        return invokeMethod(instance, methodName, args);
    }

    /**
     * Calls a method on the given object instance with the given arguments.
     *
     * @param instance      the object instance
     * @param methodName    the name of the method to invoke
     * @param args          an array of objects to pass as arguments
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
     */
    public static Object invokeMethod(Object instance, String methodName, Object[] args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null)
            args = new Object[] {};
        Class[] classTypes = getClassArray(args);
        Method[] methods = instance.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class[] paramTypes = method.getParameterTypes();
            if (method.getName().equals(methodName) && compare(paramTypes, args)) {
                return method.invoke(instance, args);
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append("No method named ").append(methodName).append(" found in ")
                .append(instance.getClass().getName()).append(" with parameters (");
        for (int x = 0; x < classTypes.length; x++) {
            sb.append(classTypes[x].getName());
            if (x < classTypes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        throw new NoSuchMethodException(sb.toString());
    }

    /**
     * Converts the object array to an array of Classes.
     *
     * @param args  the object array to convert
     * @return a Class array
     */
    private static Class[] getClassArray(Object[] args) {
        Class[] classTypes = null;
        if (args != null) {
            classTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null)
                    classTypes[i] = args[i].getClass();
            }
        }
        return classTypes;
    }

    /**
     * Gets the named method with a method signature matching classTypes from the given insance
     * and returns it as an object.
     * @param instance      the object instance
     * @param methodName    the method name
     * @param classTypes    the method argument types
     */
    public static Method getMethod(Object instance, String methodName, Class[] classTypes)
            throws NoSuchMethodException {
        Method accessMethod = getMethod(instance.getClass(), methodName, classTypes);
        accessMethod.setAccessible(true);
        return accessMethod;
    }

    /**
     * Return the named field from the given class.
     * @param thisClass The class
     * @param fieldName The field name
     */
    public static Field getField(Class thisClass, String fieldName) throws NoSuchFieldException {
        if (thisClass == null)
            throw new NoSuchFieldException("Invalid field : " + fieldName);
        try {
            return thisClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getField(thisClass.getSuperclass(), fieldName);
        }
    }

    /**
     * Return the named method with a method signature matching classTypes from the given class.
     * @param thisClass the class containing the method
     * @param methodName the name of the method
     * @param classTypes the method argument types
     */
    public static Method getMethod(Class thisClass, String methodName, Class[] classTypes)
            throws NoSuchMethodException {
        if (thisClass == null)
            throw new NoSuchMethodException("Invalid method : " + methodName);
        try {
            return thisClass.getDeclaredMethod(methodName, classTypes);
        } catch (NoSuchMethodException e) {
            return getMethod(thisClass.getSuperclass(), methodName, classTypes);
        }
    }

    private static boolean compare(Class[] c, Object[] args) {
        if (c.length != args.length) {
            return false;
        }
        for (int i = 0; i < c.length; i++) {
            if (!c[i].isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new instance of the named class initialized with the given arguments.
     *
     * @param classname the name of the class to instantiate.
     * @param args      the arguments to pass as parameters to the constructor of the class.
     * @return  the instantiated object
     */
    public static Object getNewInstance(String classname, Object[] args)
            throws ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (classname == null)
            throw new ClassNotFoundException();
        if (args == null)
            args = new Object[] {};
        Class[] classTypes = getClassArray(args);
        Class c = Class.forName(classname);
        Constructor[] constructors = c.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            Class[] paramTypes = constructor.getParameterTypes();
            if (compare(paramTypes, args)) {
                return constructor.newInstance(args);
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append("No constructor found for ").append(classname).append(" with parameters (");
        for (int x = 0; x < classTypes.length; x++) {
            sb.append(classTypes[x].getName());
            if (x < classTypes.length - 1)
                sb.append(", ");
        }
        sb.append(")");
        throw new NoSuchMethodException(sb.toString());
    }
}
