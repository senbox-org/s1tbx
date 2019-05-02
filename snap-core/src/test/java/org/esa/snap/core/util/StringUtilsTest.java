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

import org.junit.Assert;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {


    @Test
    public void testNullOrEmpty() {
        assertTrue(StringUtils.isNullOrEmpty(null));
        assertTrue(StringUtils.isNullOrEmpty(""));
        assertFalse(StringUtils.isNullOrEmpty("a"));
    }

    @Test
    public void testNotNullAndNotEmpty() {
        assertFalse(StringUtils.isNotNullAndNotEmpty(null));
        assertFalse(StringUtils.isNotNullAndNotEmpty(""));
        assertTrue(StringUtils.isNotNullAndNotEmpty("a"));
    }

    @Test
    public void testJoinAndSplit() {

        Object[] tokensOrig = {"Fischer's", "Fritz", "fischt", "frische", "", "Fische"};
        String textOrig1 = "Fischer's Fritz fischt frische  Fische";
        String textOrig2 = "Fischer's|Fritz|fischt|frische||Fische";

        assertEquals(StringUtils.join(tokensOrig, " "), textOrig1);
        assertEquals(StringUtils.join(tokensOrig, "|"), textOrig2);

        assertTrue(ArrayUtils.equalArrays(StringUtils.split(textOrig1, new char[]{' '}, false),
                                                 tokensOrig));
        assertTrue(ArrayUtils.equalArrays(StringUtils.split(textOrig2, new char[]{'|'}, false),
                                                 tokensOrig));
    }

    @Test
    public void testIsIntegerString() {
        assertTrue(StringUtils.isIntegerString("234567", 8));
        assertTrue(StringUtils.isIntegerString("-437543", 8));
        assertTrue(StringUtils.isIntegerString("abcdef", 16));
        assertTrue(StringUtils.isIntegerString("-abcdef", 16));
        assertTrue(StringUtils.isIntegerString("278495", 10));
        assertTrue(StringUtils.isIntegerString("-278495", 10));
        assertTrue(StringUtils.isIntegerString("278495"));
        assertTrue(StringUtils.isIntegerString("-278495"));
        assertTrue(StringUtils.isIntegerString("00001"));
        assertTrue(StringUtils.isIntegerString("-00001"));
        assertFalse(StringUtils.isIntegerString(null));
        assertFalse(StringUtils.isIntegerString(""));
        assertFalse(StringUtils.isIntegerString("?"));
        assertFalse(StringUtils.isIntegerString("0000x"));
        assertFalse(StringUtils.isIntegerString("2784A5"));
        assertFalse(StringUtils.isIntegerString("   278495  "));
        assertFalse(StringUtils.isIntegerString("   -278495 "));
    }

    @Test
    public void testToStringArray() {
        Object[] objArray;
        String[] strArray;

        // null --> null
        objArray = null;
        strArray = StringUtils.toStringArray(objArray);
        Assert.assertNull(strArray);

        // {type1, type2, type3} --> {str1, str2, str3}
        objArray = new Object[]{"Zorro!", 17, 'X'};
        strArray = StringUtils.toStringArray(objArray);
        Assert.assertNotNull(strArray);
        assertEquals(3, strArray.length);
        assertEquals("Zorro!", strArray[0]);
        assertEquals("17", strArray[1]);
        assertEquals("X", strArray[2]);

        // {type1, null, type3} --> {str1, null, str3}
        objArray = new Object[]{"Zorro!", null, 'X'};
        strArray = StringUtils.toStringArray(objArray);
        Assert.assertNotNull(strArray);
        assertEquals(3, strArray.length);
        assertEquals("Zorro!", strArray[0]);
        Assert.assertNull(strArray[1]);
        assertEquals("X", strArray[2]);

        // {str1, str2, str3} == {str1, str2, str3}
        objArray = new String[]{"Zorro", "Robin Hood", "Tommy Lee Jones"};
        strArray = StringUtils.toStringArray(objArray);
        Assert.assertNotNull(strArray);
        Assert.assertSame(objArray, strArray);
    }

    @Test
    public void testAddToArray() {
        String[] array = new String[]{"ab", "cd", "ef"};

        String[] newArray = StringUtils.addToArray(array, "gh");

        assertEquals(4, newArray.length);
        assertEquals("ab", newArray[0]);
        assertEquals("cd", newArray[1]);
        assertEquals("ef", newArray[2]);
        assertEquals("gh", newArray[3]);

        array = new String[]{"12", "34", "56", "78"};

        newArray = StringUtils.addToArray(array, "90");

        assertEquals(5, newArray.length);
        assertEquals("12", newArray[0]);
        assertEquals("34", newArray[1]);
        assertEquals("56", newArray[2]);
        assertEquals("78", newArray[3]);
        assertEquals("90", newArray[4]);

        try {
            StringUtils.addToArray(null, "er");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.addToArray(array, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.addToArray(null, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testRemoveFromArray() {
        String[] strings;
        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, "A");
        assertTrue("String[]{\"B\",\"C\"} expected", ArrayUtils.equalArrays(new String[]{"B", "C"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, "B");
        assertTrue("String[]{\"A\",\"C\"} expected", ArrayUtils.equalArrays(new String[]{"A", "C"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, "C");
        assertTrue("String[]{\"A\",\"B\"} expected", ArrayUtils.equalArrays(new String[]{"A", "B"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, "D");
        assertTrue("String[]{\"A\",\"B\",\"C\"} expected",
                          ArrayUtils.equalArrays(new String[]{"A", "B", "C"}, strings));
    }

    @Test
    public void testFail() {
        String[] strings;
        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, new String[]{"A"});
        assertTrue("String[]{\"B\",\"C\"} expected", ArrayUtils.equalArrays(new String[]{"B", "C"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, new String[]{"B"});
        assertTrue("String[]{\"A\",\"C\"} expected", ArrayUtils.equalArrays(new String[]{"A", "C"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, new String[]{"C"});
        assertTrue("String[]{\"A\",\"B\"} expected", ArrayUtils.equalArrays(new String[]{"A", "B"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C"}, new String[]{"D"});
        assertTrue("String[]{\"A\",\"B\",\"C\"} expected",
                          ArrayUtils.equalArrays(new String[]{"A", "B", "C"}, strings));

        strings = StringUtils.removeFromArray(new String[]{"A", "B", "C", "D"}, new String[]{"D", "B"});
        assertTrue("String[]{\"A\",\"C\"} expected", ArrayUtils.equalArrays(new String[]{"A", "C"}, strings));
    }

    @Test
    public void testAddArrays() {
        String[] array1 = new String[]{"a", "b"};
        String[] array2 = new String[]{"c", "d", "e"};

        String[] newArray = StringUtils.addArrays(array1, array2);

        assertEquals(5, newArray.length);
        assertEquals("a", newArray[0]);
        assertEquals("b", newArray[1]);
        assertEquals("c", newArray[2]);
        assertEquals("d", newArray[3]);
        assertEquals("e", newArray[4]);

        array1 = new String[]{"d", "e", "f", "g"};
        array2 = new String[]{"a", "b", "c"};

        newArray = StringUtils.addArrays(array1, array2);

        assertEquals(7, newArray.length);
        assertEquals("d", newArray[0]);
        assertEquals("e", newArray[1]);
        assertEquals("f", newArray[2]);
        assertEquals("g", newArray[3]);
        assertEquals("a", newArray[4]);
        assertEquals("b", newArray[5]);
        assertEquals("c", newArray[6]);

        try {
            StringUtils.addArrays(null, array1);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.addArrays(array2, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.addArrays(null, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testIsInArray() {
        String[] array = new String[]{"abc", "bcd", "cde"};
        assertTrue(StringUtils.contains(array, "abc"));
        assertTrue(StringUtils.contains(array, "bcd"));
        assertTrue(StringUtils.contains(array, "cde"));
        assertFalse(StringUtils.contains(array, "ace"));

        try {
            StringUtils.contains(null, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.contains(array, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            StringUtils.contains(null, "ac");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
    }

//    public void testReplaceSubstring() {
//        String original_1 = "Meine Uhr ist alt";
//        String toReplace_1 = "alt";
//        String replaceWith_1 = "antik!";
//        String expected_1 = "Meine Uhr ist antik!";
//
//        // none of the arguments shall be null
//        try {
//            StringUtils.replaceSubstring(null, null, null);
//            fail("no null arguments allowed");
//        } catch (IllegalArgumentException e) {
//        }
//
//        // check for valid replaces
//        assertEquals(expected_1, StringUtils.replaceSubstring(original_1, toReplace_1, replaceWith_1));
//    }

    @Test
    public void testToIntArray() {
        int[] current;
        String values;
        int[] expected = new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

        //default separator
        values = "" + Integer.MIN_VALUE + ",-1,0,1," + Integer.MAX_VALUE;
        current = StringUtils.toIntArray(values, null);
        assertTrue(ObjectUtils.equalObjects(expected, current));

        //specified separator
        values = "" + Integer.MIN_VALUE + "|-1|0|1|" + Integer.MAX_VALUE;
        current = StringUtils.toIntArray(values, "|");
        assertTrue(ObjectUtils.equalObjects(expected, current));

        //ignore not integer values
        values = "" + Integer.MIN_VALUE + ",-1,0,a,1," + Integer.MAX_VALUE;
        try {
            StringUtils.toIntArray(values, null);
            Assert.fail("NumberFormatException expected");
        } catch (NumberFormatException e) {
            // exception expected
        }

        //ignore values out of range
        values = "" + ((long) Integer.MIN_VALUE - 1) +
                "," + Integer.MIN_VALUE + ",-1,0,1," + Integer.MAX_VALUE +
                "," + ((long) Integer.MAX_VALUE + 1);
        try {
            StringUtils.toIntArray(values, null);
            Assert.fail("NumberFormatException expected");
        } catch (RuntimeException e) {
            // exception expected
        }

        //IllegalArgumentException
        try {
            int[] ints = StringUtils.toIntArray("", null);
            Assert.assertNotNull(ints);
            assertEquals(0, ints.length);
        } catch (IllegalArgumentException e) {
            Assert.fail("IllegalArgumentException not expected");
        }

        //IllegalArgumentException
        try {
            StringUtils.toIntArray(null, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testGetCSVString() {
        final byte[] bytes = new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE, 0, 1, -1};
        String csvString = StringUtils.arrayToCsv(bytes);
        assertEquals("127,-128,0,1,-1", csvString);
    }


    @Test
    public void testGetSeparateString() {

        final byte[] bytes = new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE, 0, 1, -1};
        assertEquals("127_-128_0_1_-1", StringUtils.arrayToString(bytes, "_"));

        final short[] shorts = new short[]{Short.MAX_VALUE, Short.MIN_VALUE, 0, 1, -1};
        assertEquals("32767/-32768/0/1/-1", StringUtils.arrayToString(shorts, "/"));

        final int[] ints = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 1, -1};
        assertEquals("2147483647\t-2147483648\t0\t1\t-1", StringUtils.arrayToString(ints, "\t"));

        final long[] longs = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, 0, 1, -1};
        assertEquals("9223372036854775807=-9223372036854775808=0=1=-1", StringUtils.arrayToString(longs, "="));

        final float[] floats = new float[]{Float.MAX_VALUE, Float.MIN_VALUE, 0, 1, -1};
        assertEquals("3.4028235E38k1.4E-45k0.0k1.0k-1.0", StringUtils.arrayToString(floats, "k"));

        final double[] doubles = new double[]{Double.MAX_VALUE, Double.MIN_VALUE, 0, 1, -1};
        assertEquals("1.7976931348623157E308" +
                                    "4.9E-324" +
                                    "0.0" +
                                    "1.0" +
                                    "-1.0", StringUtils.arrayToString(doubles, ""));

        final String[] strings = new String[]{"a", "b", "string", "noch einer", "a_%&§$\"!"};
        assertEquals("a,b,string,noch einer,a_%&§$\"!", StringUtils.arrayToString(strings, ","));
        assertEquals("abstringnoch einera_%&§$\"!", StringUtils.arrayToString(strings, ""));

        final boolean[] booleans = new boolean[]{false, true, true, false, true, false};
        assertEquals("false,true,true,false,true,false", StringUtils.arrayToString(booleans, ","));

        final HashMap<String, java.io.Serializable> hashMap = new HashMap<>();
        hashMap.put("wert1", 8);
        hashMap.put("wert2", "string");

        final Object[] objects = new Object[]{new Rectangle(3, 4, 5, 6), new Point(12, 13), hashMap};
        assertEquals("java.awt.Rectangle[x=3,y=4,width=5,height=6]," +
                                    "java.awt.Point[x=12,y=13]," +
                                    "{wert1=8, wert2=string}", StringUtils.arrayToString(objects, ","));

        try {
            // the parameter is not an array
            StringUtils.arrayToString("string", ",");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected exception
        }

        try {
            // the parameter is null
            StringUtils.arrayToString(null, ",");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected exception
        }
    }

    @Test
    public void testCsvToStringArray() {
        String[] strings = StringUtils.csvToArray("4, b ,  ,2,(-:  :-),8");
        String[] expected = new String[]{"4", " b ", "  ", "2", "(-:  :-)", "8"};
        assertEquals(expected.length, strings.length);
        for (int i = 0; i < expected.length; i++) {
            final String exp = expected[i];
            final String curr = strings[i];
            assertEquals("at index " + i, exp, curr);
        }

        strings = StringUtils.csvToArray("42");
        expected = new String[]{"42"};
        assertEquals(expected.length, strings.length);
        for (int i = 0; i < expected.length; i++) {
            final String exp = expected[i];
            final String curr = strings[i];
            assertEquals("at index " + i, exp, curr);
        }

        try {
            StringUtils.csvToArray(null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected exception
        }

        try {
            StringUtils.csvToArray("");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected exception
        }
    }

    @Test
    public void testCreateValidName() {
        char[] validChars;
        String validName;
        String expectedName;

        validChars = null;
        validName = StringUtils.createValidName("Ha123:A*A+A#A-A!A$A", validChars, '_');
        expectedName = "Ha123_A_A_A_A_A_A_A";
        assertEquals(expectedName, validName);

        validChars = new char[]{'+', '-', '$'};
        validName = StringUtils.createValidName("Ha123:A*A+A#A-A!A$A", validChars, '_');
        expectedName = "Ha123_A_A+A_A-A_A$A";
        assertEquals(expectedName, validName);

        validChars = new char[]{':', '*', '+', '-', '$', '#', '!'};
        validName = StringUtils.createValidName("Ha123:A*A+A#A-A!A$A", validChars, '_');
        expectedName = "Ha123:A*A+A#A-A!A$A";
        assertEquals(expectedName, validName);
    }

    @Test
    public void testAreEntriesUnique() {
        String[] expFail_1 = {"bla", "blubber", "bla"};
        String[] expFail_2 = {"laber", "schwafel", "sülz", "schwafel"};
        String[] expPass_1 = {"laber", "schwafel", "sülz"};
        String[] expPass_2 = {"laber", "test", "schwafel", "sülz", "anotherTest"};

        // null not allowed as parameter
        try {
            StringUtils.areEntriesUnique(null);
            Assert.fail("IllegalArgumentException expected");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        assertFalse(StringUtils.areEntriesUnique(expFail_1));
        assertFalse(StringUtils.areEntriesUnique(expFail_2));
        assertTrue(StringUtils.areEntriesUnique(expPass_1));
        assertTrue(StringUtils.areEntriesUnique(expPass_2));

        final String[] strings1 = new String[]{"alois", "blois", "alois"};
        assertFalse(StringUtils.areEntriesUnique(strings1));

        final String[] strings2 = new String[]{"alois", "blois", null};
        assertTrue(StringUtils.areEntriesUnique(strings2));

        final String[] strings3 = new String[]{"alois", "blois", null, null};
        assertFalse(StringUtils.areEntriesUnique(strings3));
    }

    @Test
    public void testSplit() {
        try {
            StringUtils.split(null, new char[]{','}, true);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected Exception because the text is null
        }

        final String[] strings1 = StringUtils.split("", new char[]{','}, true);
        Assert.assertNotNull(strings1);
        assertEquals(0, strings1.length);

        final String[] strings2 = StringUtils.split("  ", new char[]{','}, true);
        Assert.assertNotNull(strings2);
        assertEquals(1, strings2.length);

        final String[] strings3 = StringUtils.split(",", new char[]{','}, true);
        Assert.assertNotNull(strings3);
        assertEquals(2, strings3.length);

        final String[] strings4 = StringUtils.split("  aaa , , bbb ,  ccc ", new char[]{','}, true);
        Assert.assertNotNull(strings4);
        assertEquals(4, strings4.length);
        assertEquals("aaa", strings4[0]);
        assertEquals("", strings4[1]);
        assertEquals("bbb", strings4[2]);
        assertEquals("ccc", strings4[3]);
    }

    @Test
    public void testReplaceIdentifier() {
        String string;
        String regex;
        String replacement;

        string = null;
        regex = "as";
        replacement = "";
        try {
            StringUtils.replaceWord(string, regex, replacement);
            Assert.fail("IllegalArgumentException expected because string is null");
        } catch (IllegalArgumentException expected) {
        }

        string = "";
        regex = "";
        replacement = "";
        try {
            StringUtils.replaceWord(string, regex, replacement);
            Assert.fail("IllegalArgumentException expected because regex is empty");
        } catch (IllegalArgumentException expected) {
        }

        string = "";
        regex = null;
        replacement = "";
        try {
            StringUtils.replaceWord(string, regex, replacement);
            Assert.fail("IllegalArgumentException expected because regex is null");
        } catch (IllegalArgumentException expected) {
        }

        string = "";
        regex = "as";
        replacement = null;
        try {
            StringUtils.replaceWord(string, regex, replacement);
            Assert.fail("IllegalArgumentException expected because replacement is null");
        } catch (IllegalArgumentException expected) {
        }


        string = "band+band_1 - band2 * band.flag + _band + asdbandfer and band>120";
        regex = "\\bband\\b";
        replacement = "raster";

        StringUtils.replaceWord(string, regex, replacement);

        assertEquals("raster+band_1 - band2 * raster.flag + _band + asdbandfer and raster>120",
                            StringUtils.replaceWord(string, regex, replacement));
    }

    @Test
    public void testIsNumeric() {

        assertFalse(StringUtils.isNumeric("nan", Byte.class));
        assertFalse(StringUtils.isNumeric("nan", Short.class));
        assertFalse(StringUtils.isNumeric("nan", Integer.class));
        assertFalse(StringUtils.isNumeric("nan", Float.class));
        assertFalse(StringUtils.isNumeric("nan", Double.class));

        assertTrue(StringUtils.isNumeric("123", Byte.class));
        assertTrue(StringUtils.isNumeric("123", Short.class));
        assertTrue(StringUtils.isNumeric("123", Integer.class));
        assertTrue(StringUtils.isNumeric("123", Float.class));
        assertTrue(StringUtils.isNumeric("123", Double.class));

        assertFalse(StringUtils.isNumeric("123.69", Byte.class));
        assertFalse(StringUtils.isNumeric("123.69", Short.class));
        assertFalse(StringUtils.isNumeric("123.69", Integer.class));
        assertTrue(StringUtils.isNumeric("123.69", Float.class));
        assertTrue(StringUtils.isNumeric("123.69", Double.class));

    }

    @Test
    public void testIndexOfSpecificOccurrence() {
        String a = "ababab";
        assertEquals(-1, StringUtils.indexOfSpecificOccurrence(a, "a", 0));
        assertEquals(-1, StringUtils.indexOfSpecificOccurrence(a, "b", 0));
        assertEquals(0, StringUtils.indexOfSpecificOccurrence(a, "a", 1));
        assertEquals(1, StringUtils.indexOfSpecificOccurrence(a, "b", 1));
        assertEquals(-1, StringUtils.indexOfSpecificOccurrence(a, "c", 1));
        assertEquals(2, StringUtils.indexOfSpecificOccurrence(a, "a", 2));
        assertEquals(3, StringUtils.indexOfSpecificOccurrence(a, "b", 2));
        assertEquals(4, StringUtils.indexOfSpecificOccurrence(a, "a", 3));
        assertEquals(5, StringUtils.indexOfSpecificOccurrence(a, "b", 3));
        assertEquals(-1, StringUtils.indexOfSpecificOccurrence(a, "a", 4));
        assertEquals(-1, StringUtils.indexOfSpecificOccurrence(a, "b", 4));
    }

    @Test
    public void testMakestringsUnique() {
        String[] inputNames = {"abc", "duplicate", "def", "twin", "ghj", "duplicate", "duplicate", "twin"};

        String[] actualNames = StringUtils.makeStringsUnique(inputNames);
        String[] expectedNames = {"abc", "duplicate_1", "def", "twin_1", "ghj", "duplicate_2", "duplicate_3", "twin_2"};

        Assert.assertArrayEquals(expectedNames, actualNames);
    }

}

