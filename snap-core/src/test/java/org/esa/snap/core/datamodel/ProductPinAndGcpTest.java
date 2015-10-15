package org.esa.snap.core.datamodel;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the “permanent“ pin and GCP vector data nodes behave as expected.
 */
public class ProductPinAndGcpTest {

    @Test
    public void testInitialPinVectorDataNodeState() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getPinGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("pins");

        assertNotNull(node);
        assertTrue(node.getPlacemarkDescriptor() instanceof PinDescriptor);
        assertSame(p.getPinGroup(), p.getPinGroup());
        assertSame(p.getPinGroup(), node.getPlacemarkGroup());
        assertTrue(node.getFeatureCollection().isEmpty());
        assertEquals(0, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }

    @Test
    public void testAddingEmptyPinVectorDataNode() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getPinGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("pins");

        // add empty "pins" VDN
        final VectorDataNode node2 = new VectorDataNode("pins", node.getFeatureType());
        final boolean added = p.getVectorDataGroup().add(node2);

        assertFalse(added);
        assertSame(node, p.getVectorDataGroup().get("pins"));
        assertTrue(node.getFeatureCollection().isEmpty());
        assertEquals(0, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }

    @Test
    public void testAddingNonEmptyPinVectorDataNode() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getPinGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("pins");

        // add non-empty "pins" VDN
        final VectorDataNode node2 = new VectorDataNode("pins", node.getFeatureType());
        node2.getFeatureCollection().add(new SimpleFeatureBuilder(node.getFeatureType()).buildFeature("id"));
        final boolean added3 = p.getVectorDataGroup().add(node2);

        assertFalse(added3);
        assertSame(node, p.getVectorDataGroup().get("pins"));
        assertFalse(node.getFeatureCollection().isEmpty());
        assertEquals(1, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }

    @Test
    public void testInitialGcpVectorDataNodeState() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getGcpGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("ground_control_points");

        assertNotNull(node);
        assertTrue(node.getPlacemarkDescriptor() instanceof GcpDescriptor);
        assertSame(p.getGcpGroup(), p.getGcpGroup());
        assertSame(p.getGcpGroup(), node.getPlacemarkGroup());
        assertTrue(node.getFeatureCollection().isEmpty());
        assertEquals(0, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }

    @Test
    public void testAddingEmptyGcpVectorDataNode() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getGcpGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("ground_control_points");

        // add empty "GCP" VDN
        final VectorDataNode node2 = new VectorDataNode("ground_control_points", node.getFeatureType());
        final boolean added = p.getVectorDataGroup().add(node2);

        assertFalse(added);
        assertSame(node, p.getVectorDataGroup().get("ground_control_points"));
        assertTrue(node.getFeatureCollection().isEmpty());
        assertEquals(0, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }

    @Test
    public void testAddingNonEmptyGcpVectorDataNode() throws Exception {
        final Product p = new Product("n", "t", 1, 1);
        assertNotNull(p.getGcpGroup());
        final VectorDataNode node = p.getVectorDataGroup().get("ground_control_points");

        // add non-empty "pins" VDN
        final VectorDataNode node2 = new VectorDataNode("ground_control_points", node.getFeatureType());
        node2.getFeatureCollection().add(new SimpleFeatureBuilder(node.getFeatureType()).buildFeature("id"));
        final boolean added3 = p.getVectorDataGroup().add(node2);

        assertFalse(added3);
        assertSame(node, p.getVectorDataGroup().get("ground_control_points"));
        assertFalse(node.getFeatureCollection().isEmpty());
        assertEquals(1, node.getPlacemarkGroup().getNodeCount());
        assertTrue(node.isPermanent());
    }
}
