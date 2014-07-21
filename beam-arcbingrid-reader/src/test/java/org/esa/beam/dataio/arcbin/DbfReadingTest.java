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

package org.esa.beam.dataio.arcbin;

import jxl.read.biff.BiffException;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Map;

import static junit.framework.Assert.*;

public class DbfReadingTest {

    @Test
    public void testFirstCreateDbfDescriptionMap() throws URISyntaxException, IOException {
        File file = new File(getClass().getResource("test_legend_1.dbf").toURI());
        final Map<Integer, String> map = LegendFile.createDbfDescriptionMap(file);
        assertEquals("Tropical Broadleaved Forest", map.get(1));
        assertEquals("Fragmented Tropical Broadleav", map.get(2));
        assertEquals("Mangroves", map.get(5));
    }

    @Test
    public void testSecondCreateDbfDescriptionMap() throws URISyntaxException, IOException {
        File file = new File(getClass().getResource("test_legend_2.dbf").toURI());
        final Map<Integer, String> map = LegendFile.createDbfDescriptionMap(file);
        assertEquals("Unclassified", map.get(0));
        assertEquals("Mosaic Forest / shrub cover", map.get(9));
        assertEquals("", map.get(11));
        assertEquals("Artificial surfaces", map.get(22));
    }

    @Test
    public void testLegend1() throws BiffException, IOException, URISyntaxException {
        final FileChannel channel = new FileInputStream(
                new File(DbfReadingTest.class.getResource("test_legend_1.dbf").toURI())).getChannel();
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "No Data");
            testRow(reader, 1, "Tropical Broadleaved Forest");
            testRow(reader, 2, "Fragmented Tropical Broadleav");
            testRow(reader, 3, "Cultivated and Managed");
            testRow(reader, 4, "Water");
            testRow(reader, 5, "Mangroves");
        } finally {
            reader.close();
            channel.close();
        }
    }

    @Test
    public void testLegend2() throws BiffException, IOException, URISyntaxException {
        final FileChannel channel = new FileInputStream(
                new File(DbfReadingTest.class.getResource("test_legend_2.dbf").toURI())).getChannel();
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "Unclassified");
            testRow(reader, 1, "Evergreen forest");
            testRow(reader, 2, "");
            testRow(reader, 3, "");
            testRow(reader, 4, "");
            testRow(reader, 5, "");
        } finally {
            reader.close();
            channel.close();
        }
    }

    private void testRow(DbaseFileReader reader, int value, String name) throws IOException {
        DbaseFileReader.Row row = reader.readRow();
        final Object actual = row.read(0);
        assertEquals(Double.class, actual.getClass());
        assertEquals(value, ((Double) actual).intValue());
        assertEquals(name, (String) row.read(1));
    }
}
