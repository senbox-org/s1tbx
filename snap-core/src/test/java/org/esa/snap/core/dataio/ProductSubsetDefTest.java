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

package org.esa.snap.core.dataio;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.awt.Dimension;
import java.awt.Rectangle;

public class ProductSubsetDefTest extends TestCase {

    private ProductSubsetDef _subset;
    private static final float EPS = 1e-5f;

    public ProductSubsetDefTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductSubsetDefTest.class);
    }

    @Override
    protected void setUp() {
        _subset = new ProductSubsetDef("undefined");
    }

    public void testAddBandName() {
        String[] names;

        //at start getNodeNames must return null
        names = _subset.getNodeNames();
        assertEquals("names must be null", null, names);

        //null parameter throws IllegalArgumentException
        //getNodeNames must still return null
        try {
            _subset.addNodeName(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        names = _subset.getNodeNames();
        assertEquals("names must be null", null, names);

        //enpty String parameter throws IllegalArgumentException
        //getNodeNames must still return null
        try {
            _subset.addNodeName("");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        names = _subset.getNodeNames();
        assertEquals("names must be null", null, names);

        //adds first band name and getNodeNames must return a filled
        //String[] with one entry
        _subset.addNodeName("band1");
        names = _subset.getNodeNames();
        assertEquals("length must be 1", 1, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);

        //adds second band name and getNodeNames must return a filled
        //String[] with two entrys
        _subset.addNodeName("band2");
        names = _subset.getNodeNames();
        assertEquals("length must be 2", 2, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);
        assertEquals("Index 1 contains", "band2", names[1]);

        //existing band should not be added and getNodeNames must
        //return a filled String[] with two entrys
        _subset.addNodeName("band2");
        names = _subset.getNodeNames();
        assertEquals("length must be 2", 2, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);
        assertEquals("Index 1 contains", "band2", names[1]);
    }

    public void testSetBandNames() {
        String[] names;

        try {
            //must NOT throw an IllegalArgumentException
            _subset.setNodeNames(null);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException expected");
        }

        // must add only two entries because band2 are twice String[]
        _subset.setNodeNames(new String[]{"band1", "band2", "band2"});
        names = _subset.getNodeNames();
        assertEquals("length must be two", 2, names.length);
        assertEquals("expected Name", "band1", names[0]);
        assertEquals("expected Name", "band2", names[1]);

    }

    public void testRemoveBandName() {
        String[] names;
        _subset.setNodeNames(new String[]{"band1", "band2"});
        names = _subset.getNodeNames();
        assertEquals("length must be two", 2, names.length);
        assertEquals("expected Name", "band1", names[0]);
        assertEquals("expected Name", "band2", names[1]);

        // removeNodeName "band1"
        assertEquals(true, _subset.removeNodeName("band1"));
        // second remove returns false because band1 already removed
        assertEquals(false, _subset.removeNodeName("band1"));
        names = _subset.getNodeNames();
        assertEquals("length must be two", 1, names.length);
        assertEquals("expected Name", "band2", names[0]);

        // removeNodeName "band2"
        assertEquals(true, _subset.removeNodeName("band2"));
        assertEquals("subset must be null", null, _subset.getNodeNames());
    }

    public void testGetAndSetRegion() {
        assertNull("initially, getRegion() should return null", _subset.getRegion());

        _subset.setRegion(new Rectangle(20, 30, 25, 35));
        assertNotNull(_subset.getRegion());
        assertEquals(20, _subset.getRegion().x);
        assertEquals(30, _subset.getRegion().y);
        assertEquals(25, _subset.getRegion().width);
        assertEquals(35, _subset.getRegion().height);

        _subset.setRegion(40, 45, 50, 55);
        assertNotNull(_subset.getRegion());
        assertEquals(40, _subset.getRegion().x);
        assertEquals(45, _subset.getRegion().y);
        assertEquals(50, _subset.getRegion().width);
        assertEquals(55, _subset.getRegion().height);

        // Check that getRegion() returns new rectangle instances each time it is called
        assertTrue(_subset.getRegion() != _subset.getRegion());

        // reset subset region
        _subset.setRegion(null);
        assertEquals(null, _subset.getRegion());

        // IllegalArgumentException if x is negative
        try {
            _subset.setRegion(-1, 2, 3, 4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if y is negative
        try {
            _subset.setRegion(1, -1, 3, 4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if width is zero
        try {
            _subset.setRegion(1, 2, 0, 4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if height is zero
        try {
            _subset.setRegion(1, 2, 3, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if width is negative
        try {
            _subset.setRegion(1, 2, -1, 4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if height is negative
        try {
            _subset.setRegion(1, 2, 3, -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if bad values in Rectangle
        try {
            _subset.setRegion(new Rectangle(12, 2, 3, -1));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetAndSetSubSampling() {
        assertEquals("initially, getSubSamplingX() should return 1", 1.0f, _subset.getSubSamplingX(), EPS);
        assertEquals("initially, getSubSamplingY() should return 1", 1.0f, _subset.getSubSamplingY(), EPS);

        _subset.setSubSampling(1, 10);
        assertEquals(1.0f, _subset.getSubSamplingX(), EPS);
        assertEquals(10.0f, _subset.getSubSamplingY(), EPS);

        _subset.setSubSampling(10, 1);
        assertEquals(10.0f, _subset.getSubSamplingX(), EPS);
        assertEquals(1.0f, _subset.getSubSamplingY(), EPS);

        // The value to be left unchanged in the following
        _subset.setSubSampling(11, 17);

        // IllegalArgumentException if x is less than 1
        try {
            _subset.setSubSampling(0, 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals(11.0f, _subset.getSubSamplingX(), EPS);
            assertEquals(17.0f, _subset.getSubSamplingY(), EPS);
        }

        // IllegalArgumentException if y is less than 1
        try {
            _subset.setSubSampling(1, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals(11.0f, _subset.getSubSamplingX(), EPS);
            assertEquals(17.0f, _subset.getSubSamplingY(), EPS);
        }

    }

    public void testGetRasterSize() {

        _subset.setSubSampling(1, 1);
        _subset.setRegion(new Rectangle(0, 0, 1, 1));
        assertEquals(new Dimension(1, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 9, 9));
        assertEquals(new Dimension(9, 9), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 10, 10));
        assertEquals(new Dimension(10, 10), _subset.getSceneRasterSize(100, 100));

        _subset.setSubSampling(2, 2);

        _subset.setRegion(new Rectangle(0, 0, 1, 1));
        assertEquals(new Dimension(1, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 90, 9));
        assertEquals(new Dimension(45, 5), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 100, 10));
        assertEquals(new Dimension(50, 5), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 110, 11));
        assertEquals(new Dimension(55, 6), _subset.getSceneRasterSize(100, 100));


        _subset.setSubSampling(3, 3);

        _subset.setRegion(new Rectangle(0, 0, 10, 1));
        assertEquals(new Dimension(4, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 90, 9));
        assertEquals(new Dimension(30, 3), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 100, 10));
        assertEquals(new Dimension(34, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 110, 11));
        assertEquals(new Dimension(37, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 120, 12));
        assertEquals(new Dimension(40, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setRegion(new Rectangle(0, 0, 130, 13));
        assertEquals(new Dimension(44, 5), _subset.getSceneRasterSize(100, 100));
    }

    public void testMetadataIgnored() {
        ProductSubsetDef subsetInfo = new ProductSubsetDef("undefined");
        //after creation isIgnoreMetadata must be false
        assertEquals(false, subsetInfo.isIgnoreMetadata());
        subsetInfo.setIgnoreMetadata(true);
        assertEquals(true, subsetInfo.isIgnoreMetadata());
        subsetInfo.setIgnoreMetadata(false);
        assertEquals(false, subsetInfo.isIgnoreMetadata());
    }
}
