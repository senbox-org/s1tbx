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
package org.esa.snap.core.util.geotiff;

import junit.framework.TestCase;
import org.jdom.Element;

import java.util.List;

public class GeoTIFFMetadataTest extends TestCase {

    public void testConstructors() {
        final GeoTIFFMetadata md1 = new GeoTIFFMetadata();
        assertEquals(1, md1.getGeoTIFFVersion());
        assertEquals(1, md1.getKeyRevisionMajor());
        assertEquals(2, md1.getKeyRevisionMinor());
        assertEquals(1, md1.getNumGeoKeyEntries());

        final GeoTIFFMetadata md2 = new GeoTIFFMetadata(2, 3, 4);
        assertEquals(2, md2.getGeoTIFFVersion());
        assertEquals(3, md2.getKeyRevisionMajor());
        assertEquals(4, md2.getKeyRevisionMinor());
        assertEquals(1, md2.getNumGeoKeyEntries());
    }

    public void testCreatedRootTree() {
        final GeoTIFFMetadata md = new GeoTIFFMetadata();

        md.addGeoShortParam(1001, 5784);
        md.addGeoDoubleParam(1002, 45.3);
        md.addGeoAscii(1003, "My Projection");
        md.addGeoDoubleParams(1004, new double[]{21.4, -2.1, 76.9, -0.6});
        md.addGeoShortParam(1005, 865);
        md.addGeoDoubleParam(1006, 76.4);
        md.addGeoAscii(1007, "My Datum");

        // md.dump();

        final Element rootTree = md.createRootTree("whatever");

        assertEquals("com_sun_media_imageio_plugins_tiff_image_1.0", rootTree.getName());

        final List ifds = rootTree.getChildren();
        assertEquals(1, ifds.size());

        final Element ifd = (Element) ifds.get(0);
        assertEquals("TIFFIFD", ifd.getName());

        final List tags = ifd.getChildren();
        assertEquals(3, tags.size());

        final List sList = testTiffTag((Element) tags.get(0), "34735", "GeoKeyDirectory", "TIFFShorts");
        final List dList = testTiffTag((Element) tags.get(1), "34736", "GeoDoubleParams", "TIFFDoubles");
        final List aList = testTiffTag((Element) tags.get(2), "34737", "GeoAsciiParams", "TIFFAsciis");

        testShorts(sList, 0, "1", "1", "2", "7");
        testShorts(sList, 4, "1001", "0", "1", "5784");
        testShorts(sList, 8, "1002", "34736", "1", "0");
        testShorts(sList, 12, "1003", "34737", "14", "0");
        testShorts(sList, 16, "1004", "34736", "4", "1");
        testShorts(sList, 20, "1005", "0", "1", "865");
        testShorts(sList, 24, "1006", "34736", "1", "5");
        testShorts(sList, 28, "1007", "34737", "9", "14");

        testDouble(dList, 0, "45.3");
        testDouble(dList, 1, "21.4");
        testDouble(dList, 2, "-2.1");
        testDouble(dList, 3, "76.9");
        testDouble(dList, 4, "-0.6");
        testDouble(dList, 5, "76.4");

        testAscii(aList, "My Projection|My Datum|");
    }

    private List testTiffTag(Element tag, String number, String name, String type) {

        assertEquals("TIFFField", tag.getName());
        assertEquals(number, tag.getAttributeValue("number"));
        assertEquals(name, tag.getAttributeValue("name"));

        final List children = tag.getChildren();
        assertEquals(1, children.size());

        final Element element = (Element) children.get(0);
        assertEquals(type, element.getName());

        return element.getChildren();
    }

    private void testShorts(List sList, int offset, String value1, String value2, String value3, String value4) {
        assertTrue(offset <= sList.size() - 4);

        Element data1 = (Element) sList.get(offset + 0);
        assertEquals("TIFFShort", data1.getName());
        assertEquals(value1, data1.getAttributeValue("value"));

        Element data2 = (Element) sList.get(offset + 1);
        assertEquals("TIFFShort", data2.getName());
        assertEquals(value2, data2.getAttributeValue("value"));

        Element data3 = (Element) sList.get(offset + 2);
        assertEquals("TIFFShort", data3.getName());
        assertEquals(value3, data3.getAttributeValue("value"));

        Element data4 = (Element) sList.get(offset + 3);
        assertEquals("TIFFShort", data4.getName());
        assertEquals(value4, data4.getAttributeValue("value"));
    }

    private void testDouble(List dList, int offset, String value) {
        assertTrue(offset < dList.size());
        Element data1 = (Element) dList.get(offset + 0);
        assertEquals("TIFFDouble", data1.getName());
        assertEquals(value, data1.getAttributeValue("value"));
    }

    private void testAscii(List aList, String value) {
        assertEquals(1, aList.size());
        Element data1 = (Element) aList.get(0);
        assertEquals("TIFFAscii", data1.getName());
        assertEquals(value, data1.getAttributeValue("value"));
    }

    public void testAddAndGetParams() {

        final GeoTIFFMetadata md = new GeoTIFFMetadata();

        md.addGeoShortParam(1001, 5784);
        md.addGeoDoubleParam(1002, 45.3);
        md.addGeoAscii(1003, "My Projection");
        md.addGeoDoubleParams(1004, new double[]{21.4, -2.1, 76.9, -0.6});
        md.addGeoShortParam(1005, 865);
        md.addGeoDoubleParam(1006, 76.4);
        md.addGeoAscii(1007, "My Datum");

        GeoTIFFMetadata.KeyEntry entry;
        int[] data;

        ////////////////////////////////////
        // Entry 1704 --> null?

        entry = md.getGeoKeyEntry(1704);
        assertNull(entry);

        ////////////////////////////////////
        // Entry 1001

        entry = md.getGeoKeyEntry(1001);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1001, data[0]);  // key ID
        assertEquals(0, data[1]); // GeoTIFF SHORT parameter tag
        assertEquals(1, data[2]); // count
        assertEquals(5784, data[3]); // value

        assertEquals(5784, md.getGeoShortParam(1001));

        ////////////////////////////////////
        // Entry 1002

        entry = md.getGeoKeyEntry(1002);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1002, data[0]);  // key ID
        assertEquals(34736, data[1]); // GeoTIFF DOUBLE parameter tag
        assertEquals(1, data[2]); // count
        assertEquals(0, data[3]); // offset

        assertEquals(45.3, md.getGeoDoubleParam(1002), 1e-10);

        ////////////////////////////////////
        // Entry 1003

        entry = md.getGeoKeyEntry(1003);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1003, data[0]);  // key ID
        assertEquals(34737, data[1]); // GeoTIFF ASCII parameter tag
        assertEquals(14, data[2]); // length
        assertEquals(0, data[3]); // offset

        assertEquals("My Projection", md.getGeoAsciiParam(1003));

        ////////////////////////////////////
        // Entry 1004

        entry = md.getGeoKeyEntry(1004);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1004, data[0]);  // key ID
        assertEquals(34736, data[1]); // GeoTIFF ASCII parameter tag
        assertEquals(4, data[2]); // length
        assertEquals(1, data[3]); // offset

        final double[] dparams = md.getGeoDoubleParams(1004);
        assertEquals(21.4, dparams[0], 1e-10);
        assertEquals(-2.1, dparams[1], 1e-10);
        assertEquals(76.9, dparams[2], 1e-10);
        assertEquals(-0.6, dparams[3], 1e-10);

        ////////////////////////////////////
        // Entry 1005

        entry = md.getGeoKeyEntry(1005);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1005, data[0]);  // key ID
        assertEquals(0, data[1]); // GeoTIFF SHORT parameter tag
        assertEquals(1, data[2]); // count
        assertEquals(865, data[3]); // value

        assertEquals(865, md.getGeoShortParam(1005));

        ////////////////////////////////////
        // Entry 1006

        entry = md.getGeoKeyEntry(1006);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1006, data[0]);  // key ID
        assertEquals(34736, data[1]); // GeoTIFF DOUBLE parameter tag
        assertEquals(1, data[2]); // count
        assertEquals(5, data[3]); // offset

        assertEquals(76.4, md.getGeoDoubleParam(1006), 1e-10);

        ////////////////////////////////////
        // Entry 1007

        entry = md.getGeoKeyEntry(1007);
        assertNotNull(entry);
        data = entry.getData();
        assertNotNull(data);
        assertEquals(1007, data[0]);  // key ID
        assertEquals(34737, data[1]); // GeoTIFF ASCII parameter tag
        assertEquals(9, data[2]); // length
        assertEquals(14, data[3]); // offset

        assertEquals("My Datum", md.getGeoAsciiParam(1007));
    }

    public void testAddEntrieysSorted() {
        final GeoTIFFMetadata md = new GeoTIFFMetadata();

        md.addGeoDoubleParams(1004, new double[]{21.4, -2.1, 76.9, -0.6});
        md.addGeoAscii(1003, "My Projection");
        md.addGeoAscii(1007, "My Datum");
        md.addGeoDoubleParam(1002, 45.3);
        md.addGeoShortParam(1001, 5784);
        md.addGeoDoubleParam(1006, 76.4);
        md.addGeoShortParam(1005, 865);

        assertEquals(8, md.getNumGeoKeyEntries());
        assertEquals(1, md.getGeoKeyEntryAt(0).getData()[0]);
        assertEquals(1001, md.getGeoKeyEntryAt(1).getData()[0]);
        assertEquals(1002, md.getGeoKeyEntryAt(2).getData()[0]);
        assertEquals(1003, md.getGeoKeyEntryAt(3).getData()[0]);
        assertEquals(1004, md.getGeoKeyEntryAt(4).getData()[0]);
        assertEquals(1005, md.getGeoKeyEntryAt(5).getData()[0]);
        assertEquals(1006, md.getGeoKeyEntryAt(6).getData()[0]);
        assertEquals(1007, md.getGeoKeyEntryAt(7).getData()[0]);
    }

    public void testGetGeoAscIIParams() {
        final String s1 = "My Projection";
        final String s2 = "My Datum";

        final GeoTIFFMetadata md = new GeoTIFFMetadata();

        md.addGeoAscii(1003, s1);
        md.addGeoAscii(1007, s2);

        assertEquals(s1 + "|" + s2 + "|", md.getGeoAsciiParams());
    }

    public void testGetGeoDoubleParams() {
        final GeoTIFFMetadata md = new GeoTIFFMetadata();

        md.addGeoDoubleParams(1004, new double[]{21.4, -2.1, 76.9, -0.6});
        md.addGeoDoubleParam(1002, 45.3);
        md.addGeoDoubleParam(1006, 76.4);

        final double[] doubleParams = md.getGeoDoubleParams();
        assertNotNull(doubleParams);
        assertEquals(6, doubleParams.length);
        assertEquals(21.4, doubleParams[0], 1e-12);
        assertEquals(-2.1, doubleParams[1], 1e-12);
        assertEquals(76.9, doubleParams[2], 1e-12);
        assertEquals(-0.6, doubleParams[3], 1e-12);
        assertEquals(45.3, doubleParams[4], 1e-12);
        assertEquals(76.4, doubleParams[5], 1e-12);
    }
}

