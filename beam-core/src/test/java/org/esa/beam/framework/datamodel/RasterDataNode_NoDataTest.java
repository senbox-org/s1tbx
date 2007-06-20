package org.esa.beam.framework.datamodel;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * RasterDataNode Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>03/31/2005</pre>
 */

public class RasterDataNode_NoDataTest extends TestCase {

    private Product _product;
    private List _sourceNodes;
    private List _propertyNames;

    protected void setUp() throws Exception {
        _sourceNodes = new ArrayList();
        _propertyNames = new ArrayList();
        _product = new Product("name", "type", 10, 10) {
            protected void fireNodeChanged(ProductNode sourceNode, String propertyName, Object oldValue) {
                _sourceNodes.add(sourceNode);
                _propertyNames.add(propertyName);
            }
        };
    }

    public void testSetNoDataValue() {
        final RasterDataNode node = initNode(ProductData.TYPE_UINT16, 0.005, 4, false);
        clearEvents();

        final int rawValue = 2378;
        final double geoValue = 4 + 0.005 * rawValue;
        node.setNoDataValue(rawValue);

        assertEquals(rawValue, node.getNoDataValue(), 1e-10);
        assertEquals(geoValue, node.getGeophysicalNoDataValue(), 1e-10);
        assertEquals(1, _sourceNodes.size());
        assertSame(node, _sourceNodes.get(0));
        assertEquals(1, _propertyNames.size());
        assertEquals(RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE, _propertyNames.get(0));
    }

    public void testSetGeophysicalNoDataValue() {
        final RasterDataNode node = initNode(ProductData.TYPE_UINT16, 0.005, 4, false);
        clearEvents();

        final double geoValue = 32.237;
        final int rawValue = (int) ((geoValue - 4) / 0.005);
        node.setGeophysicalNoDataValue(geoValue);

        assertEquals(rawValue, node.getNoDataValue(), 1e-10);
        assertEquals(4 + 0.005 * rawValue, node.getGeophysicalNoDataValue(), 1e-10);
        assertEquals(1, _sourceNodes.size());
        assertSame(node, _sourceNodes.get(0));
        assertEquals(1, _propertyNames.size());
        assertEquals(RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE, _propertyNames.get(0));
    }

    public void testSetGeophysicalNoDataValueWithLogScaling() {
        final RasterDataNode node = initNode(ProductData.TYPE_UINT16, 2.3, -1.8, true);
        clearEvents();

        final int rawValue = 0;
        final double geoValue = Math.pow(10.0, -1.8 + 2.3 * rawValue);

        node.setNoDataValue(rawValue);
        assertEquals(rawValue, node.getNoDataValue(), 1e-10);
        assertEquals(geoValue, node.getGeophysicalNoDataValue(), 1e-10);

        node.setGeophysicalNoDataValue(geoValue);
        assertEquals(rawValue, node.getNoDataValue(), 1e-10);
        assertEquals(geoValue, node.getGeophysicalNoDataValue(), 1e-10);
    }

    private void clearEvents() {
        _propertyNames.clear();
        _sourceNodes.clear();
    }

    private RasterDataNode initNode(final int type,
                                    final double scalingFactor,
                                    final double scalingOffset,
                                    final boolean log10scaled) {
        final Band node = new Band("name", type, 10, 10);
        node.setScalingFactor(scalingFactor);
        node.setScalingOffset(scalingOffset);
        node.setLog10Scaled(log10scaled);
        _product.addBand(node);
        return node;
    }
}
