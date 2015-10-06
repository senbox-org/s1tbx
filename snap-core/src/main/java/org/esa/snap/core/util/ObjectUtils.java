/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.util;

import java.util.Arrays;

/**
 * This utility class provides several useful <code>Object</code>-related methods.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public final class ObjectUtils {

    /**
     * Indicates whether the given objects are "equal to" each other.
     * <p> This method should be used in place of the <code>Object.equals</code> if one ore both arguments can be
     * <code>null</code>.
     * <p> If both objects are arrays of the same primitive types the comparision is delegated to the corresponding
     * <code>java.util.Arrays.equals</code> method.
     * <p> If both objects are object arrays with equal lengths, the method is recursively called for each array element
     * pair. If the first pair is not equal, the method immediately returns <code>false</code>.
     *
     * @param object1 the first object, can be <code>null</code>
     * @param object2 the second object, can also be <code>null</code>
     *
     * @return <code>true</code> if this first object equals the second; <code>false</code> otherwise.
     *
     * @see java.util.Arrays#equals
     */
    public static boolean equalObjects(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null) {
            return false;
        }
        if (object1.getClass().isArray()) {
            if ((object1 instanceof byte[]) && (object2 instanceof byte[])) {
                return Arrays.equals((byte[]) object1, (byte[]) object2);
            } else if ((object1 instanceof short[]) && (object2 instanceof short[])) {
                return Arrays.equals((short[]) object1, (short[]) object2);
            } else if ((object1 instanceof int[]) && (object2 instanceof int[])) {
                return Arrays.equals((int[]) object1, (int[]) object2);
            } else if ((object1 instanceof long[]) && (object2 instanceof long[])) {
                return Arrays.equals((long[]) object1, (long[]) object2);
            } else if ((object1 instanceof float[]) && (object2 instanceof float[])) {
                return Arrays.equals((float[]) object1, (float[]) object2);
            } else if ((object1 instanceof double[]) && (object2 instanceof double[])) {
                return Arrays.equals((double[]) object1, (double[]) object2);
            } else if ((object1 instanceof boolean[]) && (object2 instanceof boolean[])) {
                return Arrays.equals((boolean[]) object1, (boolean[]) object2);
            } else if ((object1 instanceof Object[]) && (object2 instanceof Object[])) {
                Object[] array1 = (Object[]) object1;
                Object[] array2 = (Object[]) object2;
                if (array1.length != array2.length) {
                    return false;
                }
                for (int i = 0; i < array1.length; i++) {
                    if (!equalObjects(array1[i], array2[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return object1.equals(object2);
    }

    /**
     * Private constructor in order to disable instantiation and inheritance.
     */
    private ObjectUtils() {
    }
}
