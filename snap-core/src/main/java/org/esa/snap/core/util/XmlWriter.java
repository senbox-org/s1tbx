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

import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Utility class for writing XML.
 * @deprecated since BEAM 4.2, XML shall only be written via a DOM (e.g. JDOM). Used by BEAM-DIMAP product writer.
 */
@Deprecated
public class XmlWriter {

    public final static String XML_HEADER_LINE = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";

    private final PrintWriter _pWriter;
    private static XMLOutputter _xmlOutputter;

    public XmlWriter(File file) throws IOException {
        this(new FileWriter(file), true);
    }

    public XmlWriter(Writer writer, boolean initHeadline) {
        Guardian.assertNotNull("writer", writer);
        if (writer instanceof PrintWriter) {
            _pWriter = (PrintWriter) writer;
        } else {
            _pWriter = new PrintWriter(writer);
        }

        _xmlOutputter = new XMLOutputter();
        final Format format = _xmlOutputter.getFormat();
        format.setIndent("    ");   // four spaces
        _xmlOutputter.setFormat(format);
        if (initHeadline) {
            init();
        }
    }

    private void init() {
        _pWriter.println(XML_HEADER_LINE);
    }

    public void println(String str) {
        _pWriter.println(str);
    }

    public void print(String str) {
        _pWriter.print(str);
    }

    public void printElement(int indent, Element element) {
        if (element != null) {
            try {
                final StringWriter sw = new StringWriter();
                _xmlOutputter.output(element, sw);
                final StringBuffer buffer = sw.getBuffer();
                final BufferedReader br = new BufferedReader(new StringReader(buffer.toString()));
                String line;
                while ((line = br.readLine()) != null) {
                    _pWriter.write(getIndentWhiteSpace(indent));
                    _pWriter.write(line);
                    _pWriter.println();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        _pWriter.close();
    }

    public static String[] createTags(int indent, String name) {
        final String[] tags = new String[2];
        final String ws = getIndentWhiteSpace(indent);
        tags[0] = ws + "<" + name + ">";
        tags[1] = ws + "</" + name + ">";
        return tags;
    }

    public static String[] createTags(int indent, String name, String[][] attributes) {
        Debug.assertNotNullOrEmpty(name);
        Debug.assertNotNull(attributes);
        final StringBuffer tag = new StringBuffer();
        final String indentWs = getIndentWhiteSpace(indent);
        tag.append(indentWs);
        tag.append("<");
        tag.append(name);
        for (int i = 0; i < attributes.length; i++) {
            final String[] att_val = attributes[i];
            if (att_val.length > 1) {
                final String attribute = att_val[0];
                final String value = att_val[1];
                if (attribute != null && attribute.length() > 0) {
                    tag.append(" " + attribute + "=\"");
                    if (value != null) {
                        tag.append(encode(value));
                    }
                    tag.append("\"");
                }
            }
        }
        tag.append(">");
        final String[] tags = new String[2];
        tags[0] = tag.toString();
        tags[1] = indentWs + "</" + name + ">";
        return tags;
    }

    public void printLine(int indent, String tagName, boolean b) {
        printLine(indent, tagName, String.valueOf(b));
    }

    public void printLine(int indent, String tagName, int i) {
        printLine(indent, tagName, String.valueOf(i));
    }

    public void printLine(int indent, String tagName, float f) {
        printLine(indent, tagName, String.valueOf(f));
    }

    public void printLine(int indent, String tagName, double d) {
        printLine(indent, tagName, String.valueOf(d));
    }

    public void printLine(int indent, String tagName, String text) {
        final String[] tags = createTags(indent, tagName);
        printLine(tags, text);
    }

    public void printLine(int indent, String tagName, String[][] attributes, String text) {
        final String[] tags = createTags(indent, tagName, attributes);
        printLine(tags, text);
    }

    public void printLine(String[] tags, String text) {
        if (text != null && text.trim().length() > 0) {
            _pWriter.print(tags[0]);
            _pWriter.print(encode(text));
            _pWriter.println(tags[1].trim());
        } else {
            _pWriter.println(tags[0].substring(0, tags[0].length() - 1).concat(" />"));
        }
    }

    private static String getIndentWhiteSpace(int indent) {
        final int length = indent * 4;
        char newStr[] = new char[length];
        for(int i=0; i<newStr.length; i++) newStr[i]=' ';
        return new String(newStr);
    }

    private static String encode(String text) {
        if (text != null) {
            text = _xmlOutputter.outputString(new Text(text.trim()));
        }
        return text;
    }

}
