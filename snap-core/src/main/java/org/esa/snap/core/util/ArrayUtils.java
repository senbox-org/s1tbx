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

/**
 * A utility class providing a set of static functions frequently used when working with basic Java arrays.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ArrayUtils {

    /**
     * Indicates whether the given arrays arrays are "equal to" each other.
     * <p>
     *
     * @param array1 the first array, can be <code>null</code>
     * @param array2 the second array, can also be <code>null</code>
     * @param eps    the maximum allowed absolute difference between the elements in both arrays
     *
     * @return <code>true</code> if each element in the first object array equals each element in the second, in fact in
     *         the same order; <code>false</code> otherwise.
     */
    public static boolean equalArrays(final float[] array1, final float[] array2, float eps) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether the given arrays arrays are "equal to" each other.
     * <p>
     *
     * @param array1 the first array, can be <code>null</code>
     * @param array2 the second array, can also be <code>null</code>
     * @param eps    the maximum allowed absolute difference between the elements in both arrays
     *
     * @return <code>true</code> if each element in the first object array equals each element in the second, in fact in
     *         the same order; <code>false</code> otherwise.
     */
    public static boolean equalArrays(final double[] array1, final double[] array2, double eps) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether the given objects arrays are "equal to" each other.
     * <p> This method should be used in place of the <code>Object.equals</code> if one ore both arguments can be
     * <code>null</code> and if an element-by-element comparision shall be performed, since this is what this method
     * does: for each element pair the <code>ObjectUtils.equalObjects</code> method is called.
     *
     * @param array1 the first object array, can be <code>null</code>
     * @param array2 the second object array, can also be <code>null</code>
     *
     * @return <code>true</code> if each element in the first object array equals each element in the second, in fact in
     *         the same order; <code>false</code> otherwise.
     */
    public static boolean equalArrays(Object[] array1, Object[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (!ObjectUtils.equalObjects(array1[i], array2[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the specified object within the given object array.
     * <p> The method calls the <code>ObjectUtils.equalObjects</code> with the specified element on each of the array's
     * elements. If both are equal, the index is immediately returned.
     *
     * @param element the element to be searched
     * @param array   the array in which to search the element, must not be <code>null</code>
     *
     * @return the array index in the range <code>0</code> to <code>array.length - 1</code> if the element was found,
     *         <code>-1</code> otherwise
     */
    public static int getElementIndex(Object element, Object[] array) {
        Guardian.assertNotNull("array", array);
        for (int i = 0; i < array.length; i++) {
            if (ObjectUtils.equalObjects(element, array[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if the given object is member of the specified array.
     * <p> The method simply returns <code>getElementIndex(element, array) &gt;= 0</code>.
     *
     * @param element the element to be searched
     * @param array   the array in which to search the element, must not be <code>null</code>
     *
     * @return <code>true</code> if the given object is member of the specified array
     */
    public static boolean isMemberOf(Object element, Object[] array) {
        return getElementIndex(element, array) >= 0;
    }

    /**
     * Swaps the content of the given array of <code>byte</code>s.
     *
     * @param array the array of <code>byte</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(byte[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        byte temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>char</code>s.
     *
     * @param array the array of <code>char</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(char[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        char temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }


    /**
     * Swaps the content of the given array of <code>short</code>s.
     *
     * @param array the array of <code>short</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(short[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        short temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>int</code>s.
     *
     * @param array the array of <code>int</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(int[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        int temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>long</code>s.
     *
     * @param array the array of <code>long</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(long[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        long temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>float</code>s.
     *
     * @param array the array of <code>float</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(float[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        float temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>double</code>s.
     *
     * @param array the array of <code>double</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(double[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        double temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array of <code>Object</code>s.
     *
     * @param array the array of <code>Object</code>s
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static void swapArray(Object[] array) {
        Guardian.assertNotNull("array", array);
        final int n = array.length;
        Object temp;
        for (int i1 = 0, i2 = n - 1; i1 < i2; i1++, i2--) {
            temp = array[i1];
            array[i1] = array[i2];
            array[i2] = temp;
        }
    }

    /**
     * Swaps the content of the given array.
     *
     * @param array the array, must be an instance of a native array type such as <code>float[]</code>
     *
     * @throws IllegalArgumentException if the given array is <code>null</code> or does not have a native array type
     */
    public static void swapArray(Object array) {
        Guardian.assertNotNull("array", array);
        if (array instanceof byte[]) {
            swapArray((byte[]) array);
        } else if (array instanceof char[]) {
            swapArray((char[]) array);
        } else if (array instanceof short[]) {
            swapArray((short[]) array);
        } else if (array instanceof int[]) {
            swapArray((int[]) array);
        } else if (array instanceof long[]) {
            swapArray((long[]) array);
        } else if (array instanceof float[]) {
            swapArray((float[]) array);
        } else if (array instanceof double[]) {
            swapArray((double[]) array);
        } else if (array instanceof Object[]) {
            swapArray((Object[]) array);
        } else {
            throw new IllegalArgumentException(UtilConstants.MSG_OBJ_NO_ARRAY);
        }
    }


    /**
     * Recycles or creates a new <code>int</code> array of the given length.
     *
     * @param array  an array which can possibly be recycled
     * @param length the requested array length
     *
     * @return if the given array is not null and has exactly the requested length the given array is returned,
     *         otherwise a new array is created
     */
    public static int[] recycleOrCreateArray(int[] array, int length) {
        if (array != null && array.length == length) {
            return array;
        }
        return new int[length];
    }

    /**
     * Recycles or creates a new <code>float</code> array of the given length.
     *
     * @param array  an array which can possibly be recycled
     * @param length the requested array length
     *
     * @return if the given array is not null and has exactly the requested length the given array is returned,
     *         otherwise a new array is created
     */
    public static float[] recycleOrCreateArray(float[] array, int length) {
        if (array != null && array.length == length) {
            return array;
        }
        return new float[length];
    }

    /**
     * Recycles or creates a new <code>double</code> array of the given length.
     *
     * @param array  an array which can possibly be recycled
     * @param length the requested array length
     *
     * @return if the given array is not null and has exactly the requested length the given array is returned,
     *         otherwise a new array is created
     */
    public static double[] recycleOrCreateArray(double[] array, int length) {
        if (array != null && array.length == length) {
            return array;
        }
        return new double[length];
    }

    /**
     * Gives a new int array who contains both, all ints form the given array and the given new value. The given value
     * was added to the end of array
     *
     * @return new <code>int[]</code> with all ints and the new value
     *
     * @throws IllegalArgumentException if the given array is <code>null</code>
     */
    public static int[] addToArray(int[] array, int value) throws IllegalArgumentException {
        Guardian.assertNotNull("array", array);

        final int[] newArray = new int[array.length + 1];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = array[i];
        }
        newArray[array.length] = value;
        return newArray;
    }

    /**
     * Gives a new int array who contains both, all ints form all the given arrays. The given second array was added to
     * the end of th first array.
     *
     * @return new <code>int[]</code> with all ints of both arrays
     *
     * @throws IllegalArgumentException if any of the given arrays are <code>null</code>
     */
    public static int[] addArrays(int[] firstArray, int[] secondArray) throws IllegalArgumentException {
        Guardian.assertNotNull("firstArray", firstArray);
        Guardian.assertNotNull("secondArray", secondArray);

        final int firstArrayLength = firstArray.length;
        final int secondArrayLength = secondArray.length;
        final int newLength = firstArrayLength + secondArrayLength;

        final int[] newArray = new int[newLength];
        for (int i = 0; i < firstArrayLength; i++) {
            newArray[i] = firstArray[i];
        }

        for (int i = 0, j = firstArrayLength; i < secondArrayLength; i++, j++) {
            newArray[j] = secondArray[i];
        }
        return newArray;
    }

    /**
     * Creates an int array which containes all values between the given min and the given max. If min and max are equal
     * an int[] which only contains one value was returned.
     *
     * @param min the given minimum
     * @param max the given maximum
     *
     * @return an int array which containes all values between the given min and the given max.
     */
    public static int[] createIntArray(final int min, final int max) {
        final int minInt;
        final int maxInt;
        if (min < max) {
            minInt = min;
            maxInt = max;
        } else {
            minInt = max;
            maxInt = min;
        }
        final int arraySize = maxInt - minInt + 1;
        final int[] ints = new int[arraySize];

        int values = minInt;
        for (int i = 0; i < ints.length; i++) {
            ints[i] = values++;
        }

        return ints;
    }
}
