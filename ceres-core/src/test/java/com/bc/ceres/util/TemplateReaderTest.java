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

package com.bc.ceres.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

public class TemplateReaderTest extends TestCase {

    public void testNullArgConvention() {
        try {
            new TemplateReader(null, new Properties());
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TemplateReader(new StringReader(""), (Map) null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TemplateReader(new StringReader(""), (TemplateReader.Resolver) null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testWithoutProperties() throws IOException {
        Properties properties = new Properties();

        test(properties,
             "<module><symbolicName>my-module</symbolicName></module>",
             "<module><symbolicName>my-module</symbolicName></module>");

        test(properties,
             "<module><symbolicName>${id}</symbolicName></module>",
             "<module><symbolicName>${id}</symbolicName></module>");
    }

    public void testWithProperties() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", "com.bc.x");

        test(properties,
             "<module><symbolicName>my-module</symbolicName></module>",
             "<module><symbolicName>my-module</symbolicName></module>");

        test(properties,
             "<module><symbolicName>$id</symbolicName></module>",
             "<module><symbolicName>com.bc.x</symbolicName></module>");

        test(properties,
             "<module><symbolicName>${id}</symbolicName></module>",
             "<module><symbolicName>com.bc.x</symbolicName></module>");

        test(properties,
             "<module><symbolicName>${id}</symbolicName><version>${version}</version></module>",
             "<module><symbolicName>com.bc.x</symbolicName><version>${version}</version></module>");

        properties.setProperty("version", "1.0");

        test(properties,
             "<module><symbolicName>${id}</symbolicName><version>${version}</version></module>",
             "<module><symbolicName>com.bc.x</symbolicName><version>1.0</version></module>");
    }

    public void testReplacementAtEOF() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("bar", "foo");

        test(properties,
             "blah, blah, $bar",
             "blah, blah, foo");

        test(properties,
             "blah, blah, ${bar}",
             "blah, blah, foo");
    }

    public void testReplacementAtBOF() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("bar", "foo");

        test(properties,
             "$bar, blah, blah",
             "foo, blah, blah");

        test(properties,
             "${bar}, blah, blah",
             "foo, blah, blah");
    }

    public void testSubsequentReplacements() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("bar", "foo");

        test(properties,
             "$bar$bar, blah, blah",
             "$bar$bar, blah, blah");

        test(properties,
             "${bar} ${bar}, blah, blah",
             "foo foo, blah, blah");

        test(properties,
             "${bar}-${bar}, blah, blah",
             "foo-foo, blah, blah");

        // todo - this test fails (rq - 02.11.2007)
//        test(properties,
//             "${bar}${bar}, blah, blah",
//             "foofoo, blah, blah");
    }

    public void testNotReplaceable() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("bar", "foo");

        test(properties,
             "${bar, blah, blah",
             "${bar, blah, blah");

        test(properties,
             "blah, blah ${bar",
             "blah, blah ${bar");

        test(properties,
             "blah, blah $-bar",
             "blah, blah $-bar");
    }

    public void testEmptyValues() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("emptyValue", "");
        properties.setProperty("spaceValue", " ");
        properties.setProperty("nlValue", "\n");

        test(properties, "$emptyValue", "");
        test(properties, "$spaceValue", " ");
        test(properties, "$nlValue", "\n");

        test(properties, "${emptyValue}", "");
        test(properties, "${spaceValue}", " ");
        test(properties, "${nlValue}", "\n");

        test(properties, "$emptyValue more text", " more text");
        test(properties, "$spaceValue more text", "  more text");
        test(properties, "$nlValue more text", "\n more text");

        test(properties, "${emptyValue}_more text", "_more text");
        test(properties, "${spaceValue}_more text", " _more text");
        test(properties, "${nlValue}_more text", "\n_more text");
    }

    private static void test(Properties properties, String input, String expectedOutput) throws IOException {
        StringReader stringReader = new StringReader(input);
        TemplateReader templateReader = new TemplateReader(stringReader, properties);
        String actualOutput = templateReader.readAll();
        assertEquals(expectedOutput, actualOutput);
    }

}
