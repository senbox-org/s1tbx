package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;


public class RasterDataNodeNoDataTest extends TestCase {
    public void testValidPixelExpression() {
        double z = 0;

        Product p = new Product("p", "t", 10, 10);
        Band b = p.addBand("b", ProductData.TYPE_FLOAT32);

        assertEquals(null, b.getValidMaskExpression());

        b.setNoDataValueUsed(true);
        b.setGeophysicalNoDataValue(-999.0);
        assertEquals("fneq(b,-999.0)", b.getValidMaskExpression());

        b.setGeophysicalNoDataValue(1.0 / z);
        assertEquals("!inf(b)", b.getValidMaskExpression());

        b.setGeophysicalNoDataValue(-1.0 / z);
        assertEquals("!inf(b)", b.getValidMaskExpression());

        b.setGeophysicalNoDataValue(Double.NaN);
        assertEquals("!nan(b)", b.getValidMaskExpression());

        b.setNoDataValueUsed(false);
        b.setValidPixelExpression("b > 0");
        assertEquals("b > 0", b.getValidMaskExpression());

        b.setNoDataValueUsed(true);
        assertEquals("(b > 0) && !nan(b)", b.getValidMaskExpression());

        b.setValidPixelExpression("b < -1 || b > 0");
        assertEquals("(b < -1 || b > 0) && !nan(b)", b.getValidMaskExpression());

        b.setValidPixelExpression("!nan(b)");
        assertEquals("!nan(b)", b.getValidMaskExpression());
    }

    public void testNodeDataChangedEventFired() {
        Product p = new Product("p", "t", 10, 10);
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
        Product p = new Product("p", "t", 10, 10);
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
        Product p = new Product("p", "t", 10, 10);
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
        Product p = new Product("p", "t", 10, 10);
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
        List<ProductNode> sourceNodes = new ArrayList<ProductNode>();
        List<String> propertyNames = new ArrayList<String>();

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
