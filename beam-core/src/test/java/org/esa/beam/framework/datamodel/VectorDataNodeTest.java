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

package org.esa.beam.framework.datamodel;

import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.*;


public class VectorDataNodeTest {
    @Test
    public void testVectorData() throws TransformException, FactoryException {
        SimpleFeatureType pinType = Placemark.createPinFeatureType();
        SimpleFeatureType gcpType = Placemark.createGcpFeatureType();
        SimpleFeatureType unknownType = PlacemarkDescriptorRegistryTest.createYetUnknownFeatureType();
        testVectorData(new VectorDataNode("Pins", pinType), "Pins", pinType);
        testVectorData(new VectorDataNode("GCPs", gcpType), "GCPs", gcpType);
        testVectorData(new VectorDataNode("Imported", unknownType), "Imported", unknownType);
    }

    @Test
    public void testVectorDataGroup() throws TransformException, FactoryException {
        Product p = new Product("p", "pt", 512, 512);
        assertEquals(0, p.getVectorDataGroup().getNodeCount());

        SimpleFeatureType pinType = Placemark.createPinFeatureType();
        SimpleFeatureType gcpType = Placemark.createGcpFeatureType();

        p.getVectorDataGroup().add(new VectorDataNode("Pins", pinType));
        p.getVectorDataGroup().add(new VectorDataNode("GCPs", gcpType));
        assertEquals(2, p.getVectorDataGroup().getNodeCount());

        testVectorData(p, "Pins", pinType);
        testVectorData(p, "GCPs", gcpType);
    }

    private static void testVectorData(Product p, String expectedName, SimpleFeatureType expectedType) {
        VectorDataNode pins = p.getVectorDataGroup().get(expectedName);
        assertNotNull(pins);
        testVectorData(pins, expectedName, expectedType);
    }

    private static void testVectorData(VectorDataNode vectorDataNode, String expectedName, SimpleFeatureType expectedType) {
        assertNotNull(vectorDataNode.getPlacemarkDescriptor());
        assertEquals(expectedName, vectorDataNode.getName());
        assertNotNull(vectorDataNode.getFeatureCollection());
        assertSame(expectedType, vectorDataNode.getFeatureType());
        assertSame(expectedType, vectorDataNode.getFeatureCollection().getSchema());
    }

}
