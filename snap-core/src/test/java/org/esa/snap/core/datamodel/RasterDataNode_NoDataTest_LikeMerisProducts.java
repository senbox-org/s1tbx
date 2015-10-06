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

/**
 * RasterDataNode Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>03/31/2005</pre>
 */

public class RasterDataNode_NoDataTest_LikeMerisProducts extends TestCase {

    private RasterDataNode _node;

    public void testSetNoDataValue_SetNoDataValue_UByte_WithScalingLinkeAeroOptThick_MER_RR__2P() {
        short noDataValue;
        double geophysNoDataValue;
        initNode(ProductData.TYPE_UINT8, 0.0062992126, -0.0062992126, false);

        noDataValue = 87;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());

        noDataValue = 120;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());
    }

    public void testSetRawNoDataValue_SetNoDataValue_UByte_WithScalingLikeAlgal_MER_RR__2P() {
        short noDataValue;
        double geophysNoDataValue;
        initNode(ProductData.TYPE_UINT8, 0.023622047156095505, -3.0236220359802246, true);

        noDataValue = 214;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());

        noDataValue = 120;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());
    }

    public void testSetRawNoDataValue_SetNoDataValue_UShort_WithScalingLikeReflec_MER_RR__2P() {
        int noDataValue;
        double geophysNoDataValue;
        initNode(ProductData.TYPE_UINT16, 1.5259255E-5, -1.5259255E-5, false);

        noDataValue = 1214;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());

        noDataValue = 120;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());
    }

    public void testSetRawNoDataValue_SetNoDataValue_UShort_WithScalingLikeTotalSusp_MER_RR__2P() {
        int noDataValue;
        double geophysNoDataValue;
        initNode(ProductData.TYPE_UINT16, 0.015748031, -2.015748, true);

        noDataValue = 1214;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());

        noDataValue = 120;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(),true, noDataValue == _node.getNoDataValue());
    }

    public void testSetRawNoDataValue_SetNoDataValue_UShort_WithScalingLikeRadiance5_MER_RR__1P() {
        int noDataValue;
        double geophysNoDataValue;
        initNode(ProductData.TYPE_UINT16, 0.00940664, 0, false);

        noDataValue = 1214;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(), true, noDataValue == _node.getNoDataValue());

        noDataValue = 120;
        _node.setNoDataValue(noDataValue);
        geophysNoDataValue = _node.getGeophysicalNoDataValue();
        _node.setGeophysicalNoDataValue(4f);
        _node.setGeophysicalNoDataValue(geophysNoDataValue);
        assertEquals("Expected "+ noDataValue+" but was " + _node.getNoDataValue(),true, noDataValue == _node.getNoDataValue());
    }

    private void initNode(final int type,
                          final double scalingFactor,
                          final double scalingOffset,
                          final boolean log10scaled) {
        final Product product = new Product("X", "Y", 10,10);
        _node = product.addBand("name", type);
        _node.setScalingFactor(scalingFactor);
        _node.setScalingOffset(scalingOffset);
        _node.setLog10Scaled(log10scaled);
    }
}
