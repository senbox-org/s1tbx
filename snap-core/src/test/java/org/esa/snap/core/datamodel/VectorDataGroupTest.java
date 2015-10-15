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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VectorDataGroupTest {

    private ProductNodeGroup<VectorDataNode> vectorDataGroup;
    private ProductNodeGroup<Mask> maskGroup;

    @Before
    public void setup() {
        final Product product = new Product("P", "T", 10, 10);
        vectorDataGroup = product.getVectorDataGroup();
        maskGroup = product.getMaskGroup();
    }

    @Test
    public void initialState() {
        assertEquals(2, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
    }

    @Test
    public void maskGroupIsNotCoupledWithEmptyVectorDataGroups() {

        final VectorDataNode v = new VectorDataNode("V", Placemark.createGeometryFeatureType());

        vectorDataGroup.add(v);
        assertEquals(3, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
        assertSame(v, vectorDataGroup.get(2));
        assertFalse(maskGroup.contains(v.getName()));

        final VectorDataNode u = new VectorDataNode("U", Placemark.createGeometryFeatureType());
        vectorDataGroup.add(0, u);
        assertEquals(4, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
        assertSame(u, vectorDataGroup.get(0));
        assertSame(v, vectorDataGroup.get(3));
        assertFalse(maskGroup.contains(u.getName()));
        assertFalse(maskGroup.contains(v.getName()));

        vectorDataGroup.remove(u);
        assertEquals(3, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
        assertFalse(vectorDataGroup.contains(u));
        assertSame(v, vectorDataGroup.get(2));
        assertFalse(maskGroup.contains(u.getName()));
        assertFalse(maskGroup.contains(v.getName()));

        vectorDataGroup.removeAll();
        assertEquals(2, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
    }

    @Test
    public void cannotAddNullToVectorDataGroup() {
        try {
            vectorDataGroup.add(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(2, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
    }
}
