/*
 * $Id: CsvReaderTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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

package org.esa.beam.util.io;

import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.util.ArrayUtils;

public class CsvReaderTest extends TestCase {

    private CsvReader _reader;
    private Vector _expectedVector;

    public CsvReaderTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CsvReaderTest.class);
    }

    protected void setUp() {
        _expectedVector = new Vector();
        _expectedVector.add(
                new String[]{"radiance_4",
                             "Radiance_MDS(4).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.4",
                             "*",
                             "*"});
        _expectedVector.add(
                new String[]{"radiance_5",
                             "Radiance_MDS(5).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.5",
                             "*",
                             "*"});
        _expectedVector.add(
                new String[]{"radiance_6",
                             "Radiance_MDS(6).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.6",
                             "*",
                             "*"});
        _expectedVector.add(
                new String[]{"radiance_7",
                             "Radiance_MDS(7).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.7",
                             "*",
                             "*"});

        Reader reader = new StringReader("radiance_4   |Radiance_MDS(4).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.4 |*|*\n" +
                                         "radiance_5   |Radiance_MDS(5).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.5 |*|*\n" +
                                         "radiance_6   |Radiance_MDS(6).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.6 |*|*\n" +
                                         "radiance_7   |Radiance_MDS(7).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.7 |*|*\n");
        _reader = new CsvReader(reader, new char[]{'|'});
    }

    protected void tearDown() {
    }

    public void testGetSeparators() {
        Reader reader = new StringReader("testreader");
        CsvReader csvReader;
        char[] seperator;

        seperator = new char[]{'|'};
        csvReader = new CsvReader(reader, seperator);
        assertEquals(seperator, csvReader.getSeparators());

        seperator = new char[]{'|', ','};
        csvReader = new CsvReader(reader, seperator);
        assertEquals(seperator, csvReader.getSeparators());
    }

    public void testReadAllRecords() {
        Vector actualVector = null;
        try {
            actualVector = _reader.readAllRecords();
        } catch (java.io.IOException e) {
            fail("no java.io.IOException expected");
        }

        assertEquals(actualVector.size(), _expectedVector.size());
        for (int i = 0; i < actualVector.size(); i++) {
            ArrayUtils.equalArrays((String[]) actualVector.get(i), (String[]) _expectedVector.get(i));
        }
    }

    public void testReadRecord() {
        String[] expStrArray;

        for (int i = 0; i < _expectedVector.size(); i++) {
            expStrArray = (String[]) _expectedVector.get(i);
            try {
                assertEquals(true, ArrayUtils.equalArrays(expStrArray, _reader.readRecord()));
            } catch (java.io.IOException e) {
                fail("no java.io.IOException expected");
            }
        }

        try {
            assertNull(_reader.readRecord());
        } catch (java.io.IOException ex) {
            fail("no java.io.IOException expected");
        }
    }
}
