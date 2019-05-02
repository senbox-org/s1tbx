/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.jexp.impl.Tokenizer;

import java.awt.Color;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The <code>StringUtils</code> class provides frequently used utility methods dealing with <code>String</code> values
 * and which are not found in the <code>java.lang.String</code> class.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Tom Block
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class StringUtils {

    /**
     * Splits the given text into a list of tokens by using the supplied separators. Empty tokens are created for
     * successive separators, or if the the supplied text starts with or ends with a separator. If the given text string
     * is empty, and empty list is returned, but never <code>null</code>. The tokens added to list will never contain
     * separators.
     *
     * @param text       the text to be splitted into tokens
     * @param separators the characters used to separate the tokens
     * @param trimTokens if true, white space characters are removed from both ends of each token
     * @param tokens     can be null. If not null, all tokens are added to this list and the method it, otherwise a new
     *                   list is created.
     * @return a list of tokens extracted from the given text, never <code>null</code>
     * @throws IllegalArgumentException if one of the arguments was null
     * @see java.util.StringTokenizer
     */
    public static List<String> split(String text, char[] separators, boolean trimTokens, List<String> tokens) {

        Guardian.assertNotNull("text", text);

        if (separators == null || separators.length == 0) {
            throw new IllegalArgumentException(UtilConstants.MSG_NULL_OR_EMPTY_SEPARATOR);
        }

        if (tokens == null) {
            tokens = new ArrayList<>();
        }

        String sepsStr = new String(separators);
        StringTokenizer st = new StringTokenizer(text, sepsStr, true);
        String token;
        String lastToken = null;
        while (st.hasMoreTokens()) {
            try {
                token = st.nextToken();
            } catch (Exception e) {
                break;
            }
            if (isSeparatorToken(token, sepsStr)) {
                // If text starts with a separator or two succesive separators
                // have been seen, add empty string
                if (lastToken == null || isSeparatorToken(lastToken, sepsStr)) {
                    tokens.add("");
                }
            } else {
                if (trimTokens) {
                    token = token.trim();
                }
                tokens.add(token);
            }
            lastToken = token;
        }
        // If text ends with a separator, add empty string
        if (lastToken != null && isSeparatorToken(lastToken, sepsStr)) {
            tokens.add("");
        }

        return tokens;
    }

    /**
     * Splits the given text into a list of tokens by using the supplied separators. Empty tokens are created for
     * successive separators, or if the the supplied text starts with or ends with a separator. If the given text string
     * is empty, and empty array is returned, but never <code>null</code>. The tokens in the returned array will never
     * contain separators.
     *
     * @param text       the text to be splitted into tokens
     * @param separators the characters used to separate the tokens
     * @param trimTokens if true, white space characters are removed from both ends of each token
     * @return an array of tokens extracted from the given text, never <code>null</code>
     * @see java.util.StringTokenizer
     */
    public static String[] split(String text, char[] separators, boolean trimTokens) {

        List tokens = split(text, separators, trimTokens, null);

        return (String[]) tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Joins the given array of tokens to a new text string. The given separator string is put between each of the
     * tokens. If a token in the array is <code>null</code> or empty, an empty string string is appended to the
     * resulting text. The resulting text string will always contain <code>tokens.length - 1</code> separators, if the
     * separator is not part of one of the tokens itself.
     *
     * @param tokens    the list of tokens to join, must not be null
     * @param separator the separator string, must not be null
     * @return the list of tokens as a text string
     * @throws IllegalArgumentException if one of the arguments was null
     * @see #split(String, char[], boolean)
     */
    public static String join(Object[] tokens, String separator) {

        if (tokens == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NULL_TOKEN);
        }

        if (separator == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NULL_SEPARATOR);
        }

        StringBuilder sb = new StringBuilder(tokens.length * 16);
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            if (tokens[i] != null) {
                sb.append(tokens[i].toString());
            }
        }

        return sb.toString();
    }

    /**
     * Joins the given array of tokens to a new text string. The given separator string is put between each of the
     * tokens. If a token in the array is <code>null</code> or empty, an empty string string is appended to the
     * resulting text. The resulting text string will always contain <code>tokens.length - 1</code> separators, if the
     * separator is not part of one of the tokens itself.
     *
     * @param tokens    the list of tokens to join, must not be null
     * @param separator the separator string, must not be null
     * @return the list of tokens as a text string
     * @throws IllegalArgumentException if one of the arguments was null
     * @see #join(Object[], String)
     */
    public static String join(List tokens, String separator) {
        if (tokens == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NULL_TOKEN);
        }
        return join(tokens.toArray(), separator);
    }

    /**
     * Checks whether the given token string represents an integer number or not.
     *
     * @param token the token string to be checked
     * @return <code>true</code> if the string represents an integer (radix=10)
     */
    public static boolean isIntegerString(String token) {
        return isIntegerString(token, 10);
    }

    /**
     * Checks whether the given token string represents an integer number or not.
     *
     * @param token the token string to be checked
     * @param radix the radix of the integer represented by the token string
     * @return <code>true</code> if the string represents an integer with tzhe given radix
     */
    public static boolean isIntegerString(String token, int radix) {
        if (token != null) {
            try {
                Integer.parseInt(token, radix);
                return true;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }


    /**
     * Converts the given text into an <code>int</code> array.
     * <p>The number values are expected to be separated by one of the characters given in the delimiter string.
     * <p>If the delimiter string is null or empty, the default delimiter "," will be used.
     *
     * @param text  the text to be converted
     * @param delim the delimiter between the number values
     * @return the <code>int</code> array parsed from the given text
     * @throws IllegalArgumentException if the text is null or cannot be converted to an array of the requested number
     *                                  type
     */
    public static int[] toIntArray(String text, String delim) {
        Guardian.assertNotNull("text", text);
        if (delim == null || delim.length() == 0) {
            delim = ",";
        }
        final String[] tokens = split(text, delim.toCharArray(), true);
        final int[] numbers = new int[tokens.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Integer.parseInt(tokens[i]);
        }
        return numbers;
    }

    /**
     * Converts the given text into an <code>float</code> array.
     * <p>The number values are expected to be separated by one of the characters given in the delimiter string.
     * <p>If the delimiter string is null or empty, the default delimiter "," will be used.
     *
     * @param text  the text to be converted
     * @param delim the delimiter between the number values
     * @return the <code>float</code> array parsed from the given text
     * @throws IllegalArgumentException if the text is null or cannot be converted to an array of the requested number
     *                                  type
     */
    public static float[] toFloatArray(String text, String delim) {
        Guardian.assertNotNull("text", text);
        if (delim == null || delim.length() == 0) {
            delim = ",";
        }
        final String[] tokens = split(text, delim.toCharArray(), true);
        final float[] numbers = new float[tokens.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Float.parseFloat(tokens[i]);
        }
        return numbers;
    }

    /**
     * Converts the given text into an <code>double</code> array.
     * <p>The number values are expected to be separated by one of the characters given in the delimiter string.
     * <p>If the delimiter string is null or empty, the default delimiter "," will be used.
     *
     * @param text  the text to be converted
     * @param delim the delimiter between the number values
     * @return the <code>double</code> array parsed from the given text
     * @throws IllegalArgumentException if the text is null or cannot be converted to an array of the requested number
     *                                  type
     */
    public static double[] toDoubleArray(String text, String delim) {
        Guardian.assertNotNull("text", text);
        if (delim == null || delim.length() == 0) {
            delim = ",";
        }
        final String[] tokens = split(text, delim.toCharArray(), true);
        final double[] numbers = new double[tokens.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Double.parseDouble(tokens[i]);
        }
        return numbers;
    }

    /**
     * Converts the given text into an array of <code>String</code> tokens.
     * <p>The number values are expected to be separated by one of the characters given in the delimiter string.
     * <p>If the delimiter string is null or empty, the default delimiter "," will be used.
     *
     * @param text   the text to be converted
     * @param delims the delimiter characters used between the tokens
     * @return the <code>String</code> array parsed from the given text, never null
     * @throws IllegalArgumentException if the text is null or cannot be converted to an array of the requested number
     *                                  type
     */
    public static String[] toStringArray(String text, String delims) {
        Guardian.assertNotNull("text", text);
        if (delims == null || delims.length() == 0) {
            delims = ",";
        }
        return split(text, delims.toCharArray(), true);
    }

    /**
     * Converts the given object array into a string array. If the given object array is already an instance of a string
     * array, it is simply type-casted and returned. Otherwise, a new string array is created and - for each non-null
     * object - the value of <code>object.toString()</code> is stored,  null-values remain null-values.
     *
     * @param objArray the object array to be converted, if <code>null</code> the method returns <code>null</code> too
     * @return the string array
     */
    public static String[] toStringArray(Object[] objArray) {
        if (objArray == null || objArray instanceof String[]) {
            return (String[]) objArray;
        }
        String[] strArray = new String[objArray.length];
        for (int i = 0; i < objArray.length; i++) {
            strArray[i] = (objArray[i] != null) ? objArray[i].toString() : null;
        }
        return strArray;
    }

    /**
     * Tests whether or not the given string is null or empty.
     *
     * @param str the string to be tested
     * @return <code>true</code> if so
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Tests whether or not the given string is not null and not empty.
     *
     * @param str the string to be tested
     * @return <code>true</code> if so
     */
    public static boolean isNotNullAndNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Gives a new StringArray who contains both, all Strings form the given Array and the given String. The given
     * String was added to the end of array
     *
     * @return new <code>String[]</code> with all Strings
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static String[] addToArray(String[] array, String toAdd) throws IllegalArgumentException {
        Guardian.assertNotNull("array", array);
        Guardian.assertNotNull("toAdd", toAdd);

        String[] newArray = new String[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = toAdd;
        return newArray;
    }

    /**
     * Gives a new StringArray who contains all Strings form the given Array excepting the given String. The first
     * occurrence of the given String was removed.
     *
     * @return new <code>String[]</code> without the first occurrence of the given String
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static String[] removeFromArray(String[] array, String toRemove) throws IllegalArgumentException {
        Guardian.assertNotNull("array", array);
        Guardian.assertNotNull("toRemove", toRemove);

        int index = indexOf(array, toRemove);
        if (index == -1) {
            return array;
        }

        String[] newArray = new String[array.length - 1];
        int offset = 0;
        for (int i = 0; i < array.length; i++) {
            if (i != index) {
                newArray[i + offset] = array[i];
            } else {
                offset = -1;
            }
        }
        return newArray;
    }

    /**
     * Gives a new StringArray who contains all Strings form the given Array excepting the String from the array to
     * remove. The first occurrence of the given Strings in the string array to remove was removed.
     *
     * @return new <code>String[]</code> without the first occurrence of the given Strings in the string array to
     *         remove
     * @throws IllegalArgumentException if array is <code>null</code>
     */
    public static String[] removeFromArray(String[] array, String[] toRemove) throws IllegalArgumentException {
        Guardian.assertNotNull("array", array);
        if (toRemove == null) {
            return array;
        }

        String[] newArray = array;
        for (String item : toRemove) {
            newArray = removeFromArray(newArray, item);
        }
        return newArray;
    }

    /**
     * Returns a string array which is a concatenation of the two given string arrays.
     *
     * @return a new <code>String[]</code> which is a concatenation of the two given string arrays.
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static String[] addArrays(String[] arr1, String[] arr2) throws IllegalArgumentException {
        Guardian.assertNotNull("arr1", arr1);
        Guardian.assertNotNull("arr2", arr2);

        int length1 = arr1.length;
        int length2 = arr2.length;
        String[] newArray = new String[length1 + length2];
        System.arraycopy(arr1, 0, newArray, 0, length1);
        System.arraycopy(arr2, 0, newArray, length1, length2);
        return newArray;
    }

    /**
     * Tests whether or not a given string is contained in a given string array.
     *
     * @param a the string array in which to search
     * @param s the string for which the search is performed
     * @return <code>true</code> if the string <code>s</code> is contained in the array <code>a</code>
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static boolean contains(String[] a, String s) {
        return indexOf(a, s) >= 0;
    }

    /**
     * Tests whether or not a given string is contained in a given string array.
     *
     * @param a the string array in which to search
     * @param s the string for which the search is performed
     * @return <code>true</code> if the string <code>s</code> is contained in the array <code>a</code>
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static boolean containsIgnoreCase(String[] a, String s) {
        return indexOfIgnoreCase(a, s) >= 0;
    }

    /**
     * Tests whether or not a given string is contained in a list.
     *
     * @param l the string list in which to search
     * @param s the string for which the search is performed
     * @return <code>true</code> if the string <code>s</code> is contained in the array <code>a</code>
     * @throws IllegalArgumentException if one of the arguments are <code>null</code>
     */
    public static boolean containsIgnoreCase(List l, String s) {
        return indexOfIgnoreCase(l, s) >= 0;
    }

    /**
     * Retrieves whether the entries in the string array are unique - or not.
     *
     * @param array
     * @return <code>true</code> if the entries in the string array are unique, otherwise <code>false</code>.
     */
    public static boolean areEntriesUnique(final String[] array) {
        Guardian.assertNotNull("array", array);

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[i] == array[j] || (array[i] != null && array[j] != null && array[i].equals(array[j]))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Gets the array index of a given string in a given string array or <code>-1</code> if the string could not be
     * found.
     *
     * @param a the string array in which to search
     * @param s the string for which the search is performed
     * @return the array index of the first occurence of <code>s</code> in <code>a</code> or <code>-1</code> if it is
     *         not cointained in the array
     * @throws IllegalArgumentException if one of the arguments is <code>null</code>
     */
    public static int indexOf(String[] a, String s) {
        Guardian.assertNotNull("a", a);
        Guardian.assertNotNull("s", s);
        for (int i = 0; i < a.length; i++) {
            if (s.equals(a[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the array index of a given string in a given string array or <code>-1</code> if the string could not be
     * found.
     *
     * @param a the string array in which to search
     * @param s the string for which the search is performed
     * @return the array index of the first occurence of <code>s</code> in <code>a</code> or <code>-1</code> if it is
     *         not cointained in the array
     * @throws IllegalArgumentException if one of the arguments is <code>null</code>
     */
    public static int indexOfIgnoreCase(String[] a, String s) {
        Guardian.assertNotNull("a", a);
        Guardian.assertNotNull("s", s);
        for (int i = 0; i < a.length; i++) {
            if (s.equalsIgnoreCase(a[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the list index of a given string in a given string list or <code>-1</code> if the string could not be
     * found.
     *
     * @param l the string list in which to search
     * @param s the string for which the search is performed
     * @return the array index of the first occurence of <code>s</code> in <code>a</code> or <code>-1</code> if it is
     *         not cointained in the array
     * @throws IllegalArgumentException if one of the arguments is <code>null</code>
     */
    public static int indexOfIgnoreCase(List l, String s) {
        Guardian.assertNotNull("l", l);
        Guardian.assertNotNull("s", s);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) != null && l.get(i).toString().equalsIgnoreCase(s)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets a comma separate value string for the given array object.
     *
     * @param array the array object of values.
     * @return a comma separate value string for the given array object.
     * @throws IllegalArgumentException if the given Object is not an <code>array</code> or <code>null</code>.
     */
    public static String arrayToCsv(final Object array) {
        return arrayToString(array, ",");
    }

    /**
     * Converts an array into a string.
     *
     * @param array the array object
     * @param s     the separator string, e.g. ","
     * @return a string represenation of the array
     * @throws IllegalArgumentException if the given Object is not an <code>array</code> or <code>null</code>.
     */
    public static String arrayToString(final Object array, final String s) {
        Guardian.assertNotNull("array", array);
        final int length = Array.getLength(array);
        if (length == 0) {
            return "";
        }
        if (length == 1) {
            return Array.get(array, 0).toString();
        }
        final StringBuilder sb = new StringBuilder(length * 8);
        sb.append(Array.get(array, 0));
        for (int i = 1; i < length; i++) {
            sb.append(s);
            sb.append(Array.get(array, i));
        }
        return sb.toString();
    }

    /**
     * Gets a String[] from the given comma separated value string.
     *
     * @param csvString the CSV (comma separated value) String.
     * @return an array of strings created from the given comma separated value string, never <code>null</code>
     * @throws IllegalArgumentException if the given csvString is <code>null</code> or <code>empty</code>.
     */
    public static String[] csvToArray(String csvString) {
        return stringToArray(csvString, ",");
    }

    /**
     * Gets a String[] from the given comma separated value string given a delimiter.
     *
     * @param csvString the delimited String.
     * @param delim     the separator string, e.g. ","
     * @return an array of strings created from the given comma separated value string, never <code>null</code>
     * @throws IllegalArgumentException if the given Object is not an <code>array</code> or <code>null</code>.
     */
    public static String[] stringToArray(final String csvString, final String delim) {
		Guardian.assertNotNullOrEmpty("csvString", csvString);
		Guardian.assertNotNullOrEmpty("delim", delim);
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        final List<String> strList = new ArrayList<>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            strList.add(tokenizer.nextToken());
        }
        return strList.toArray(new String[strList.size()]);
    }

//    /**
//     * Replaces all occurences of <code>toReplace</code> in the original string with
//     * <code>replaceWith</code> and returns the new string.
//     * When no occurrences are present, the original string is returned
//     */
//    public static String replaceSubstring(String original, String toReplace, String replaceWith) {
//        Guardian.assertNotNull("original", original);
//        Guardian.assertNotNull("toReplace", toReplace);
//        Guardian.assertNotNull("replaceWith", replaceWith);
//        StringBuffer buffer = new StringBuffer();
//        String strRet;
//
//        if (original.indexOf(toReplace) == -1) {
//            // no occurences, return original
//            strRet = original;
//        } else {
//            int start = 0;
//            int copyStart = 0;
//            int end = 0;
//
//            start = original.indexOf(toReplace, end);
//            end = start + toReplace.length();
//            buffer.append(original.substring(copyStart, start));
//            buffer.append(replaceWith);
//
//            strRet = buffer.toString();
//        }
//
//        return strRet;
//    }


    private static boolean isSeparatorToken(String token, String separators) {
        return token.length() == 1 && separators.contains(token);
    }

    protected StringUtils() {
    }

    /**
     * Converts a textual representation of an RGB(A) color to a <code>Color</code> object.
     */
    public static Color parseColor(String text) throws NumberFormatException {
        Color color = null;

        String trimedText = text.trim();
        String[] components = split(trimedText, new char[]{','}, true /*trim?*/);
        try {
            if (components.length == 1) {
                color = Color.decode(trimedText);
            }
            if (components.length >= 3) {
                int r = Integer.parseInt(components[0]);
                int g = Integer.parseInt(components[1]);
                int b = Integer.parseInt(components[2]);
                int a = 255;
                if (components.length > 3) {
                    a = Integer.parseInt(components[3]);
                }
                color = new Color(r, g, b, a);
            }
        } catch (NumberFormatException e) {
            color = null;
        }

        return color;
    }

    /**
     * Returns a string representation of the given color value.
     */
    public static String formatColor(Color c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getRed());
        sb.append(',');
        sb.append(c.getGreen());
        sb.append(',');
        sb.append(c.getBlue());
        if (c.getAlpha() != 255) {
            sb.append(',');
            sb.append(c.getAlpha());
        }
        return sb.toString();
    }

    /**
     * Creates a valid name for the given source name. The method returns a string which is the given name where each
     * occurence of a character which is not a letter, a digit or one of the given valid characters is replaced by the
     * given replace character. The returned string always has the same length as the source name.
     *
     * @param name        the source name, must not be  <code>null</code>
     * @param validChars  the array of valid characters
     * @param replaceChar the replace character
     */
    public static String createValidName(String name, char[] validChars, char replaceChar) {
        Guardian.assertNotNull("name", name);
        char[] sortedValidChars;
        if (validChars == null) {
            sortedValidChars = new char[0];
        } else {
            sortedValidChars = validChars.clone();
        }
        Arrays.sort(sortedValidChars);
        StringBuilder validName = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char ch = name.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                validName.append(ch);
            } else if (Arrays.binarySearch(sortedValidChars, ch) >= 0) {
                validName.append(ch);
            } else {
                validName.append(replaceChar);
            }
        }
        return validName.toString();
    }

    /**
     * Replaces all occurrences of the old word within the given string by the new word.
     *
     * @param string  the string within all occurrences of the old word are to be replaced.
     * @param oldWord the word to be replaced. Must not be <code>null</code> and must not be empty.
     * @param newWord the new word. Must not be <code> null</code>.
     * @return the resulting string, never <code>null</code>.
     */
    public static String replaceWord(final String string, final String oldWord, final String newWord) {
        Guardian.assertNotNull("string", string);
        Guardian.assertNotNullOrEmpty("oldWord", oldWord);
        Guardian.assertNotNull("newWord", newWord);
        // The "\\b" in a regular expression means "word boundary"
        return string.replaceAll("\\b" + oldWord + "\\b", newWord);
    }

    /**
     * Tests whether or not the given string is valid identifier. Valid identifiers have a length greater than zero,
     * start with a letter or underscore followed by letters, digits or underscores.
     *
     * @param s the string to test
     * @return <code>true</code> if the s is a valid node ifentifier, <code>false</code> otherwise
     */
    public static boolean isIdentifier(String s) {
        return Tokenizer.isExternalName(s);
    }

    /**
     * Checks if the string is numeric
     *
     * @param str   the String input
     * @param clazz the type to check against
     * @return true if numeric false if not
     */
    public static boolean isNumeric(String str, Class<? extends Number> clazz) {
        try {
            if (clazz.equals(Byte.class)) {
                Byte.parseByte(str);
            } else if (clazz.equals(Double.class)) {
                Double.parseDouble(str);
            } else if (clazz.equals(Float.class)) {
                Float.parseFloat(str);
            } else if (clazz.equals(Integer.class)) {
                Integer.parseInt(str);
            } else if (clazz.equals(Long.class)) {
                Long.parseLong(str);
            } else if (clazz.equals(Short.class)) {
                Short.parseShort(str);
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Turns the first letter of the given string to upper case.
     *
     * @param string the string to change
     * @return a changed string
     */
    public static String firstLetterUp(String string) {
        String firstChar = string.substring(0, 1).toUpperCase();
        return firstChar + string.substring(1);
    }

    /**
     * Gets the array index of the i-th occurrence of a given string in a string array or <code>-1</code> if
     * the string could not be found.
     *
     * @param a the string array in which to search
     * @param s the string for which the search is performed
     * @param i the index of the occurrence of the requested string
     * @return the array index of the <code>i-th</code> occurrence of <code>s</code> in <code>a</code> or
     *         <code>-1</code> if <code>s</code> is contained less than <code>i</code> times in the array or if
     *         <code>i</code> is less than 1
     * @throws IllegalArgumentException if <code>a</code> <code>s</code> are <code>null</code>
     */
    public static int indexOfSpecificOccurrence(String a, String s, int i) {
        int indexOfLastOccurrence = -1;
        for (int j = 0; j < i; j++) {
            indexOfLastOccurrence = a.indexOf(s, indexOfLastOccurrence + 1);
            if (indexOfLastOccurrence == -1 || indexOfLastOccurrence == a.length()) return -1;
        }
        return indexOfLastOccurrence;
    }

    /**
     * Adds padding to an integer
     * 1 becomes 001 or __1
     * @param num the integer value
     * @param max the desired string length
     * @param c the inserted character
     * @return padded number as string
     */
    public static String padNum(final int num, final int max, final char c) {
        final StringBuilder str = new StringBuilder(String.valueOf(num));
        while (str.length() < max) {
            str.insert(0, c);
        }
        return str.toString();
    }

    /**
     * Makes the strings in the given array unique.
     * Strings which occur more than once in the array get an appendix with the index of its occurrence.
     * Example:
     * <pre>
     *     'name', 'duplicate', 'other', 'duplicate'
     * --> 'name', 'duplicate_1', 'other', 'duplicate_2'
     *</pre>
     *
     * @param strings the strings to make unique
     * @return a new array with the changed names
     *
     */
    public static String[] makeStringsUnique(String[] strings) {
        List<String> nameList = new ArrayList<>();
        List<String> duplicated = new ArrayList<>();
        String[] clonedNames = strings.clone();
        for (String name : clonedNames) {
            if (!nameList.contains(name)) {
                nameList.add(name);
            } else {
                // duplicated
                duplicated.add(name);
            }
        }
        for (String duplicatedName : duplicated) {
            int index = 1;
            for (int i = 0; i < clonedNames.length; i++) {
                if (clonedNames[i].equals(duplicatedName)) {
                    clonedNames[i] = duplicatedName + "_" + index++;
                }
            }
        }
        return clonedNames;

    }
}
