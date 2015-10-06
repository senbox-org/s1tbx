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

package org.esa.snap.core.datamodel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ProductNodeListTest extends TestCase {

    private ProductNodeList<MetadataAttribute> _nodeList;
    private MetadataAttribute _attribute1;
    private MetadataAttribute _attribute2;
    private MetadataAttribute _attribute3;
    private MetadataAttribute _attribute4;

    public ProductNodeListTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductNodeListTest.class);
    }

    @Override
    protected void setUp() {
        _nodeList = new ProductNodeList<MetadataAttribute>();
        _attribute1 = new MetadataAttribute("attribute1", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute2 = new MetadataAttribute("attribute2", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute3 = new MetadataAttribute("attribute3", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute4 = new MetadataAttribute("attribute4", ProductData.createInstance(ProductData.TYPE_INT32), true);
    }

    @Override
    protected void tearDown() {
    }

    public void testGetAt() {
        addAllNodes();

        assertEquals(_nodeList.getAt(0), _attribute1);
        assertEquals(_nodeList.getAt(1), _attribute2);
        assertEquals(_nodeList.getAt(2), _attribute3);
        assertEquals(_nodeList.getAt(3), _attribute4);

        try {
            _nodeList.getAt(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (java.lang.IndexOutOfBoundsException e) {
        }

        try {
            _nodeList.getAt(4);
            fail("IndexOutOfBoundsException expected");
        } catch (java.lang.IndexOutOfBoundsException e) {
        }

        removeAllNodes();
    }

    public void testGet() {
        addAllNodes();
        assertEquals(_nodeList.get("attribute1"), _attribute1);
        assertEquals(_nodeList.get("attribute2"), _attribute2);
        assertEquals(_nodeList.get("attribute3"), _attribute3);
        assertEquals(_nodeList.get("attribute4"), _attribute4);
        assertEquals(_nodeList.get("ATTRIBUTE1"), _attribute1);
        assertEquals(_nodeList.get("ATTRIBUTE2"), _attribute2);
        assertEquals(_nodeList.get("ATTRIBUTE3"), _attribute3);
        assertEquals(_nodeList.get("ATTRIBUTE4"), _attribute4);
        assertEquals(_nodeList.get("ATTRIBUTEX"), null);
        removeAllNodes();
    }

    public void testContains() {
        addAllNodes();
        assertEquals(_nodeList.contains("attribute1"), true);
        assertEquals(_nodeList.contains("attribute2"), true);
        assertEquals(_nodeList.contains("attribute3"), true);
        assertEquals(_nodeList.contains("attribute4"), true);
        assertEquals(_nodeList.contains("ATTRIBUTE1"), true);
        assertEquals(_nodeList.contains("ATTRIBUTE2"), true);
        assertEquals(_nodeList.contains("ATTRIBUTE3"), true);
        assertEquals(_nodeList.contains("ATTRIBUTE4"), true);
        assertEquals(_nodeList.contains("ATTRIBUTEX"), false);
        removeAllNodes();
    }

    public void testGetNames() {
        addAllNodes();
        String[] names = _nodeList.getNames();
        assertEquals(names[0], "attribute1");
        assertEquals(names[1], "attribute2");
        assertEquals(names[2], "attribute3");
        assertEquals(names[3], "attribute4");
        removeAllNodes();
    }

    public void testToArray() {
        addAllNodes();
        ProductNode[] nodes = _nodeList.toArray();
        assertEquals(nodes[0], _attribute1);
        assertEquals(nodes[1], _attribute2);
        assertEquals(nodes[2], _attribute3);
        assertEquals(nodes[3], _attribute4);
        removeAllNodes();
    }

    public void testIndexOf() {
        addAllNodes();
        assertEquals(_nodeList.indexOf("attribute1"), 0);
        assertEquals(_nodeList.indexOf("attribute2"), 1);
        assertEquals(_nodeList.indexOf("attribute3"), 2);
        assertEquals(_nodeList.indexOf("attribute4"), 3);
        assertEquals(_nodeList.indexOf("ATTRIBUTE1"), 0);
        assertEquals(_nodeList.indexOf("ATTRIBUTE2"), 1);
        assertEquals(_nodeList.indexOf("ATTRIBUTE3"), 2);
        assertEquals(_nodeList.indexOf("ATTRIBUTE4"), 3);
        assertEquals(_nodeList.indexOf("ATTRIBUTEX"), -1);
        removeAllNodes();
    }

    public void testAddAndRemoveAndSize() {
        assertEquals(_nodeList.size(), 0);
        _nodeList.add(_attribute1);
        assertEquals(_nodeList.size(), 1);
        _nodeList.add(_attribute2);
        assertEquals(_nodeList.size(), 2);
        _nodeList.add(_attribute3);
        assertEquals(_nodeList.size(), 3);
        _nodeList.add(_attribute4);
        assertEquals(_nodeList.size(), 4);
        _nodeList.remove(_attribute1);
        assertEquals(_nodeList.size(), 3);
        _nodeList.removeAll();
        assertEquals(_nodeList.size(), 0);
    }

    private void addAllNodes() {
        _nodeList.add(_attribute1);
        _nodeList.add(_attribute2);
        _nodeList.add(_attribute3);
        _nodeList.add(_attribute4);

//        final ProductNode[] children = _owner.getChildren();
//        assertEquals(4, children.length);
//        assertSame(_attribute1, children[0]);
//        assertSame(_attribute2, children[1]);
//        assertSame(_attribute3, children[2]);
//        assertSame(_attribute4, children[3]);
    }

    private void removeAllNodes() {
        _nodeList.removeAll();

//        final ProductNode[] children = _owner.getChildren();
//        assertEquals(0, children.length);
    }

//    public void testChildren() {
//        final ProductNode[] empty = _owner.getChildren();
//        assertNotNull(empty);
//        assertEquals(0, empty.length);
//
//        addAllNodes();
//
//        final ProductNode[] fourChildren = _owner.getChildren();
//        assertNotNull(fourChildren);
//        assertEquals(4, fourChildren.length);
//        assertSame(_attribute1, fourChildren[0]);
//        assertSame(_attribute2, fourChildren[1]);
//        assertSame(_attribute3, fourChildren[2]);
//        assertSame(_attribute4, fourChildren[3]);
//
//        _nodeList.remove(_attribute2);
//
//        final ProductNode[] threeChildren = _owner.getChildren();
//        assertNotNull(threeChildren);
//        assertEquals(3, threeChildren.length);
//        assertSame(_attribute1, threeChildren[0]);
//        assertSame(_attribute3, threeChildren[1]);
//        assertSame(_attribute4, threeChildren[2]);
//
//        removeAllNodes();
//
//        final ProductNode[] secondEmpty = _owner.getChildren();
//        assertNotNull(secondEmpty);
//        assertEquals(0, secondEmpty.length);
//    }
}
