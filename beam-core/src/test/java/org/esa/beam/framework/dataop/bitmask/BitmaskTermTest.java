/*
 * $Id: BitmaskTermTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataop.bitmask;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BitmaskTermTest extends TestCase {

    public BitmaskTermTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(BitmaskTermTest.class);
    }

    public void testToString() {
        BitmaskTerm bt;

        try {
            bt = BitmaskExpressionParser.parse("flags.CLOUD");
            assertEquals("flags.CLOUD", bt.toString());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("NOT flags.CLOUD");
            assertEquals("NOT flags.CLOUD", bt.toString());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("!!flags.CLOUD");
            assertEquals("NOT (NOT flags.CLOUD)", bt.toString());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("flags.CLOUD|flags.LAND");
            assertEquals("flags.CLOUD OR flags.LAND", bt.toString());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("flags.CLOUD&flags.LAND|!flags.LAND");
            assertEquals("(flags.CLOUD AND flags.LAND) OR (NOT flags.LAND)", bt.toString());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

    }

    public void testEqualsAndHashKey() {
        BitmaskTerm bt1, bt2;

        try {
            bt1 = BitmaskExpressionParser.parse("flags.CLOUD|flags.LAND");
            bt2 = BitmaskExpressionParser.parse("flags.CLOUD|   flags.LAND  ");
            assertEquals(bt1, bt2);
            assertEquals(bt1.hashCode(), bt2.hashCode());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt1 = BitmaskExpressionParser.parse("flags1.CLOUD&!flags2.LAND");
            bt2 = BitmaskExpressionParser.parse("flags1.CLOUD &  !flags2.LAND");
            assertEquals(bt1, bt2);
            assertEquals(bt1.hashCode(), bt2.hashCode());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt1 = BitmaskExpressionParser.parse("flags1.CLOUDO & !flags2.LAND");
            bt2 = BitmaskExpressionParser.parse("flags1.CLOUD  & !flags2.LAND");
            assertFalse(bt1.equals(bt2));
            assertFalse(bt1.hashCode() == bt2.hashCode());
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

    }

    public void testThatDatasetReferencesAreCollectedCorrectly() {
        BitmaskTerm bt;
        String[] names;

        try {
            bt = BitmaskExpressionParser.parse("flags.CLOUD|flags.LAND");
            names = bt.getReferencedDatasetNames();
            assertEquals(true, Arrays.equals(new String[]{"flags"}, names));
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("flags1.CLOUD|flags2.LAND");
            names = bt.getReferencedDatasetNames();
            assertEquals(true, Arrays.equals(new String[]{"flags1", "flags2"}, names));
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            bt = BitmaskExpressionParser.parse("flags1.CLOUD|flags2.LAND AND flags2.CLOUD");
            names = bt.getReferencedDatasetNames();
            assertEquals(true, Arrays.equals(new String[]{"flags1", "flags2"}, names));
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }
    }

}
