/*
 * $Id: BitmaskParserTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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

import junit.framework.TestCase;

/**
 * @deprecated
 */
public class BitmaskParserTest extends TestCase {

    public BitmaskParserTest(String s) {
        super(s);
    }


    public void testThatParserReturnsNullWithoutExceptionIfEmptyExpression() {
        try {
            assertEquals(null, BitmaskExpressionParser.parse(""));
        } catch (BitmaskExpressionParseException e) {
            fail("unexpected BitmaskExpressionParseException: " + e.getMessage());
        }
    }

    public void testEqualityForFlagRefTerms() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt2 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt3 = new BitmaskTerm.FlagReference("flags", "cloud");
        BitmaskTerm bt4 = new BitmaskTerm.FlagReference("flags", "Cloud");
        BitmaskTerm bt5 = new BitmaskTerm.FlagReference("flags", "LAND");
        BitmaskTerm bt6 = new BitmaskTerm.FlagReference("flags", "WATER");
        assertEquals(bt1, bt2);
        assertEquals(bt1, bt3);
        assertEquals(bt1, bt4);
        assertEquals(false, bt1.equals(bt5));
        assertEquals(false, bt1.equals(bt6));
    }

    public void testThatParserHandlesEOFCorrectly() {
        try {
            assertEquals(null, BitmaskExpressionParser.parse(""));
            assertEquals(null, BitmaskExpressionParser.parse(" "));
            assertEquals(null, BitmaskExpressionParser.parse("\n\t"));
            assertEquals(null, BitmaskExpressionParser.parse("  \n\n"));
            assertEquals(null, BitmaskExpressionParser.parse("\t\t\t   \t"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesFlagRefsCorrectly() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        try {
            assertEquals(bt1, BitmaskExpressionParser.parse("flags.CLOUD"));
            assertEquals(bt1, BitmaskExpressionParser.parse("flags.  CLOUD"));
            assertEquals(bt1, BitmaskExpressionParser.parse("FLAGS  .CLOUD"));
            assertEquals(bt1, BitmaskExpressionParser.parse("  flags  .cloud"));
            assertEquals(bt1, BitmaskExpressionParser.parse("  fLaGs  .  CLOUD "));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesSimpleNotTermsCorrectly() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt2 = new BitmaskTerm.Not(bt1);
        try {
            assertEquals(bt2, BitmaskExpressionParser.parse("!flags.CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("! flags.CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("\n!\tflags  .CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("  !flags.CLOUD\n"));
            assertEquals(bt2, BitmaskExpressionParser.parse("not flags.CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("NOT flags.CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("\nnOt\tflags  .CLOUD"));
            assertEquals(bt2, BitmaskExpressionParser.parse("   not flags.CLOUD\n"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesSimpleOrTermsCorrectly() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt2 = new BitmaskTerm.FlagReference("flags", "LAND");
        BitmaskTerm bt3 = new BitmaskTerm.Or(bt1, bt2);
        try {
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD|flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD  |  flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD | flags .  LAND "));
            assertEquals(bt3, BitmaskExpressionParser.parse("  \t   flags.CLOUD\n|\tflags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD or flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD  OR   flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD oR flags .  LAND "));
            assertEquals(bt3, BitmaskExpressionParser.parse("  \t   flags.CLOUD\nOR\tflags.LAND"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesSimpleAndTermsCorrectly() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt2 = new BitmaskTerm.FlagReference("flags", "LAND");
        BitmaskTerm bt3 = new BitmaskTerm.And(bt1, bt2);
        try {
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD&flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD  &  flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD & flags .  LAND "));
            assertEquals(bt3, BitmaskExpressionParser.parse("  \t   flags.CLOUD\n&\tflags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD and flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD  AND   flags.LAND"));
            assertEquals(bt3, BitmaskExpressionParser.parse("flags.CLOUD aNd flags .  LAND "));
            assertEquals(bt3, BitmaskExpressionParser.parse("  \t   flags.CLOUD\nAND\tflags.LAND"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesDiverseCompositeTermsCorrectly() {
        BitmaskTerm bt1 = new BitmaskTerm.FlagReference("flags", "WATER");
        BitmaskTerm bt2 = new BitmaskTerm.FlagReference("flags", "LAND");
        BitmaskTerm bt3 = new BitmaskTerm.FlagReference("flags", "CLOUD");
        BitmaskTerm bt4 = new BitmaskTerm.Not(bt3);
        BitmaskTerm bt5 = new BitmaskTerm.Or(bt1, new BitmaskTerm.And(bt2, bt4));
        try {
            assertEquals(bt5, BitmaskExpressionParser.parse("flags.WATER | flags.LAND & !flags.CLOUD"));
            assertEquals(bt5, BitmaskExpressionParser.parse("flags.WATER | (flags.LAND & !flags.CLOUD)"));
            assertEquals(bt5, BitmaskExpressionParser.parse("flags.WATER | (flags.LAND) & !(((flags.CLOUD)))"));
            assertEquals(bt5, BitmaskExpressionParser.parse("(flags.WATER | flags.LAND & !flags.CLOUD)"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
        BitmaskTerm bt6 = new BitmaskTerm.And(new BitmaskTerm.Or(bt1, bt2), bt4);
        try {
            assertEquals(bt6, BitmaskExpressionParser.parse("(flags.WATER | flags.LAND) & !flags.CLOUD"));
            assertEquals(bt6, BitmaskExpressionParser.parse("(flags.WATER | (flags.LAND)) & !flags.CLOUD"));
            assertEquals(bt6, BitmaskExpressionParser.parse("(flags.WATER | flags.LAND) & !(((flags.CLOUD)))"));
            assertEquals(bt6, BitmaskExpressionParser.parse("((flags.WATER | flags.LAND) & !flags.CLOUD)"));
        } catch (BitmaskExpressionParseException e) {
            fail();
        }
    }

    public void testThatParserHandlesDiverseSyntaxErrorsCorrectly() {
        try {
            BitmaskExpressionParser.parse("flags.WATER | flags.LAND & 4724");
            fail("BitmaskExpressionParseException expected, because of unknown token '4724'");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("flags.WATER | flags.LAND & 0xffff");
            fail("BitmaskExpressionParseException expected, because of unknown token '0xffff'");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("flags.WATER | flags.LAND & ");
            fail("BitmaskExpressionParseException expected, because term after '&' is missing");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("flags.WATER | flags & !flags.CLOUD");
            fail("BitmaskExpressionParseException expected, because '.' after band name is missing");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("flags.WATER | flags. & !flags.CLOUD");
            fail("BitmaskExpressionParseException expected, because flag name is missing");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("(flags.WATER | flags.LAND & !flags.CLOUD");
            fail("BitmaskExpressionParseException expected, because ')' is missing");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("(flags.WATER | flags.LAND & !flags.CLOUD))");
            fail("BitmaskExpressionParseException expected, because of superfluous ')'");
        } catch (BitmaskExpressionParseException e) {
        }
    }

    public void testThatDatasetNamesCanBeAnyJavaIdentifier() {

        try {
            BitmaskExpressionParser.parse("flags_1.CLOUD");
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            BitmaskExpressionParser.parse("_.CLOUD");
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            BitmaskExpressionParser.parse("_1.CLOUD");
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            BitmaskExpressionParser.parse("a1.CLOUD");
        } catch (BitmaskExpressionParseException e) {
            fail("No parsing errors expected: error was: '" + e.getMessage() + "'");
        }

        try {
            BitmaskExpressionParser.parse("1.CLOUD");
            fail("Parsing error expected");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("1_flags.CLOUD");
            fail("Parsing error expected");
        } catch (BitmaskExpressionParseException e) {
        }

        try {
            BitmaskExpressionParser.parse("1_.CLOUD");
            fail("Parsing error expected");
        } catch (BitmaskExpressionParseException e) {
        }
    }
}
