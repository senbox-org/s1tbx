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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;


public class RasterDataNodeNoDataTest extends TestCase {

    private Product p;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        p = new Product("p", "t", 10, 10);
    }

    public void testValidPixelExpression() {
        double z = 0;

        Band floatBand = p.addBand("b", ProductData.TYPE_FLOAT32);

        assertEquals(null, floatBand.getValidMaskExpression());

        floatBand.setNoDataValueUsed(true);
        floatBand.setGeophysicalNoDataValue(-999.0);
        assertEquals("fneq(b,-999.0)", floatBand.getValidMaskExpression());

        floatBand.setGeophysicalNoDataValue(1.0 / z);
        assertEquals("!inf(b)", floatBand.getValidMaskExpression());

        floatBand.setGeophysicalNoDataValue(-1.0 / z);
        assertEquals("!inf(b)", floatBand.getValidMaskExpression());

        floatBand.setGeophysicalNoDataValue(Double.NaN);
        assertEquals("!nan(b)", floatBand.getValidMaskExpression());

        floatBand.setNoDataValueUsed(false);
        floatBand.setValidPixelExpression("b > 0");
        assertEquals("b > 0", floatBand.getValidMaskExpression());

        floatBand.setNoDataValueUsed(true);
        assertEquals("(b > 0) && !nan(b)", floatBand.getValidMaskExpression());

        floatBand.setValidPixelExpression("b < -1 || b > 0");
        assertEquals("(b < -1 || b > 0) && !nan(b)", floatBand.getValidMaskExpression());

        floatBand.setValidPixelExpression("!nan(b)");
        assertEquals("!nan(b)", floatBand.getValidMaskExpression());


        Band intBand = p.addBand("i", ProductData.TYPE_INT32);
        assertEquals(null, intBand.getValidMaskExpression());

        intBand.setNoDataValueUsed(true);
        intBand.setNoDataValue(42);
        assertEquals("i.raw != 42.0", intBand.getValidMaskExpression());

        intBand.setNoDataValueUsed(false);
        intBand.setValidPixelExpression("i > 0");
        assertEquals("i > 0", intBand.getValidMaskExpression());

        intBand.setNoDataValueUsed(true);
        assertEquals("(i > 0) && i.raw != 42.0", intBand.getValidMaskExpression());

        intBand.setValidPixelExpression(null);
        intBand.setScalingFactor(10);
        intBand.setGeophysicalNoDataValue(40);
        assertEquals("i.raw != 4.0", intBand.getValidMaskExpression());

        Band uintBand = p.addBand("u", ProductData.TYPE_UINT8);
        uintBand.setNoDataValueUsed(true);
        uintBand.setNoDataValue(-1);
        assertEquals("u.raw != 255.0", uintBand.getValidMaskExpression());
    }

    public void testIsPixelValid() throws Exception {
        Band b = p.addBand("b", ProductData.TYPE_FLOAT32);

        assertTrue(b.isPixelValid(-10, -3));

        b.setNoDataValue(12.34);
        b.setNoDataValueUsed(true);
        assertFalse(b.isPixelValid(-10, -3));
    }

    public void testNodeDataChangedEventFired() {
        Band b = p.addBand("b", ProductData.TYPE_FLOAT32);

        assertEquals(false, b.isNoDataValueUsed());
        assertEquals(0.0, b.getNoDataValue(), 1e-15);
        assertEquals(null, b.getValidPixelExpression());
        assertEquals(null, b.getValidMaskExpression());

        ChangeDetector detector = new ChangeDetector();
        p.addProductNodeListener(detector);

        assertEquals(0, detector.nodeDataChanges);
        b.setNoDataValue(-1.0);
        assertEquals(0, detector.nodeDataChanges);

        b.setNoDataValueUsed(true);
        assertEquals(1, detector.nodeDataChanges);

        b.setNoDataValue(-1.0);
        assertEquals(1, detector.nodeDataChanges);

        b.setNoDataValueUsed(false);
        assertEquals(2, detector.nodeDataChanges);

        b.setNoDataValue(-999.0);
        assertEquals(2, detector.nodeDataChanges);

        b.setNoDataValueUsed(true);
        assertEquals(3, detector.nodeDataChanges);

        b.setValidPixelExpression("a >= 0 && a <= 1");
        assertEquals(4, detector.nodeDataChanges);
    }


    public void testSetNoDataValue() {
        final Band b = createBand(p, ProductData.TYPE_UINT16, 0.005, 4, false);

        ChangeDetector detector = new ChangeDetector();
        p.addProductNodeListener(detector);

        final int rawValue = 2378;
        final double geoValue = 4 + 0.005 * rawValue;
        b.setNoDataValue(rawValue);

        assertEquals(rawValue, b.getNoDataValue(), 1e-10);
        assertEquals(geoValue, b.getGeophysicalNoDataValue(), 1e-10);
        assertEquals(1, detector.sourceNodes.size());
        assertSame(b, detector.sourceNodes.get(0));
        assertEquals(1, detector.propertyNames.size());
        assertEquals(RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE, detector.propertyNames.get(0));
    }

    public void testSetGeophysicalNoDataValue() {
        final Band b = createBand(p, ProductData.TYPE_UINT16, 0.005, 4, false);
        ChangeDetector detector = new ChangeDetector();
        p.addProductNodeListener(detector);

        final double geoValue = 32.237;
        final int rawValue = (int) ((geoValue - 4) / 0.005);
        b.setGeophysicalNoDataValue(geoValue);

        assertEquals(rawValue, b.getNoDataValue(), 1e-10);
        assertEquals(4 + 0.005 * rawValue, b.getGeophysicalNoDataValue(), 1e-10);
        assertEquals(1, detector.sourceNodes.size());
        assertSame(b, detector.sourceNodes.get(0));
        assertEquals(1, detector.propertyNames.size());
        assertEquals(RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE, detector.propertyNames.get(0));
    }

    public void testSetGeophysicalNoDataValueWithLogScaling() {
        final Band b = createBand(p, ProductData.TYPE_UINT16, 2.3, -1.8, true);

        final int rawValue = 0;
        final double geoValue = Math.pow(10.0, -1.8 + 2.3 * rawValue);

        b.setNoDataValue(rawValue);
        assertEquals(rawValue, b.getNoDataValue(), 1e-10);
        assertEquals(geoValue, b.getGeophysicalNoDataValue(), 1e-10);

        b.setGeophysicalNoDataValue(geoValue);
        assertEquals(rawValue, b.getNoDataValue(), 1e-10);
        assertEquals(geoValue, b.getGeophysicalNoDataValue(), 1e-10);
    }

    public void testUsageOfRawSymbolForAllDataTypes() {
        testDataType(ProductData.TYPE_INT8, 4 ,127, "b.raw != 127.0");
        testDataType(ProductData.TYPE_INT8, 1 ,-1, "b.raw != -1.0");

        testDataType(ProductData.TYPE_UINT8, 4 ,-1, "b.raw != 255.0");
        testDataType(ProductData.TYPE_UINT8, 4 ,255, "b.raw != 255.0");
        testDataType(ProductData.TYPE_UINT8, 4 ,256, "b.raw != 0.0");

        testDataType(ProductData.TYPE_INT16, 4, 5, "b.raw != 5.0");

        testDataType(ProductData.TYPE_UINT16, 4, 5, "b.raw != 5.0");

        testDataType(ProductData.TYPE_INT32, 4, 5, "b.raw != 5.0");

        testDataType(ProductData.TYPE_UINT32, 4 , Integer.MAX_VALUE, "b.raw != 2.147483647E9");
        testDataType(ProductData.TYPE_UINT32, 4 ,Integer.MAX_VALUE + 1, "b.raw != 2.147483648E9");
        testDataType(ProductData.TYPE_UINT32, 4 ,-1, "b.raw != 4.294967295E9");

        testDataType(ProductData.TYPE_FLOAT32, 4 ,5, "fneq(b,5.0)");
        testDataType(ProductData.TYPE_FLOAT64, 4 ,5, "fneq(b,5.0)");
    }

    private void testDataType(int productDataType, double v1, double v2, String validMask) {
        Product p = new Product("p", "t", 1, 2);

        Band b = p.addBand("b", productDataType);
        ProductData pData = ProductData.createInstance(productDataType, 2);
        pData.setElemDoubleAt(0, v1);
        pData.setElemDoubleAt(1, v2);
        b.setData(pData);
        b.setNoDataValue(v2);
        b.setNoDataValueUsed(true);

        assertTrue(b.isPixelValid(0, 0));
        assertFalse(b.isPixelValid(0, 1));
        assertEquals(validMask, b.getValidMaskExpression());
    }

    private Band createBand(Product product,
                            int type,
                            double scalingFactor,
                            double scalingOffset,
                            boolean log10scaled) {
        final Band node = new Band("b", type, 10, 10);
        node.setScalingFactor(scalingFactor);
        node.setScalingOffset(scalingOffset);
        node.setLog10Scaled(log10scaled);
        product.addBand(node);
        return node;
    }

    private static class ChangeDetector extends ProductNodeListenerAdapter {
        int nodeDataChanges = 0;
        List<ProductNode> sourceNodes = new ArrayList<>();
        List<String> propertyNames = new ArrayList<>();

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            sourceNodes.add(event.getSourceNode());
            propertyNames.add(event.getPropertyName());
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeDataChanges++;
        }
    }


}
