/*
 * $Id: XmlWriterTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

public class XmlWriterTest extends TestCase {

    public XmlWriterTest(String s) {
        super(s);
    }

    public void testPrintLineWidthText() {
        StringWriter stringWriter = new StringWriter();
        XmlWriter xmlWriter = new XmlWriter(stringWriter, false);
        xmlWriter.printLine(XmlWriter.createTags(1, "tag"), "name");
        String current = stringWriter.toString();

        String expected = "    <tag>name</tag>";
        stringWriter = new StringWriter();
        new PrintWriter(stringWriter).println();
        expected = expected + stringWriter.toString();

        assertEquals(expected, current);
    }

    public void testPrintLineEmptyText() {
        StringWriter stringWriter = new StringWriter();
        XmlWriter xmlWriter = new XmlWriter(stringWriter, false);
        xmlWriter.printLine(XmlWriter.createTags(1, "tag"), "");
        String current = stringWriter.toString();

        String expected = "    <tag />";
        stringWriter = new StringWriter();
        new PrintWriter(stringWriter).println();
        expected = expected + stringWriter.toString();

        assertEquals(expected, current);
    }

    public void testPrintLineNullText() {
        StringWriter stringWriter = new StringWriter();
        XmlWriter xmlWriter = new XmlWriter(stringWriter, false);
        xmlWriter.printLine(XmlWriter.createTags(1, "tag"), null);
        String current = stringWriter.toString();

        String expected = "    <tag />";
        stringWriter = new StringWriter();
        new PrintWriter(stringWriter).println();
        expected = expected + stringWriter.toString();

        assertEquals(expected, current);
    }

    public void testPrintLineText_ampersand() {
        StringWriter stringWriter = new StringWriter();
        XmlWriter xmlWriter = new XmlWriter(stringWriter, false);
        xmlWriter.printLine(XmlWriter.createTags(1, "tag"), "ampersand & ampersand");
        String current = stringWriter.toString();

        String expected = "    <tag>ampersand &amp; ampersand</tag>";
        stringWriter = new StringWriter();
        new PrintWriter(stringWriter).println();
        expected = expected + stringWriter.toString();

        assertEquals(expected, current);
    }

    public void testPrintLineText_parentheses() {
        StringWriter stringWriter = new StringWriter();
        XmlWriter xmlWriter = new XmlWriter(stringWriter, false);
        xmlWriter.printLine(XmlWriter.createTags(1, "tag"), "test < test > test");
        String current = stringWriter.toString();

        String expected = "    <tag>test &lt; test &gt; test</tag>";
        stringWriter = new StringWriter();
        new PrintWriter(stringWriter).println();
        expected = expected + stringWriter.toString();

        assertEquals(expected, current);
    }

    public void testCreateTags() {
        String[] tags = XmlWriter.createTags(0, "name");

        assertEquals(2, tags.length);
        assertEquals("<name>", tags[0]);
        assertEquals("</name>", tags[1]);
    }

    public void testCreateTagsWithAtribs() {
        String[] tags = XmlWriter.createTags(2, "name", new String[][]{{"a", "b"}, {"c", "d"}});

        assertEquals(2, tags.length);
        assertEquals("        <name a=\"b\" c=\"d\">", tags[0]);
        assertEquals("        </name>", tags[1]);
    }

    public void testCreateTagsWithAtribs_ampersand_and_parentheses() {
        String[] tags = XmlWriter.createTags(2, "name", new String[][]{{"a", "a & b"}, {"c", "d < e > f"}});

        assertEquals(2, tags.length);
        assertEquals("        <name a=\"a &amp; b\" c=\"d &lt; e &gt; f\">", tags[0]);
        assertEquals("        </name>", tags[1]);
    }
}
