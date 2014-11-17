/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Stx;
import org.junit.Test;

import javax.media.jai.Histogram;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class HistogramPanelModelTest {

    @Test
    public void testRemoveStxFromProduct() throws Exception {

        HistogramPanelModel model = new HistogramPanelModel();
        Band band = new Band("name", ProductData.TYPE_UINT32, 10, 10);
        Product product = new Product("dummy", "dummy", 10, 10);
        product.addBand(band);
        HistogramPanelModel.HistogramConfig config = new HistogramPanelModel.HistogramConfig(
                band,
                "Roy Mask",
                10,
                true
        );
        model.setStx(config, arbitraryStx());

        assertTrue(model.hasStx(config));
        model.removeStxFromProduct(product);
        assertFalse(model.hasStx(config));
    }

    private static Stx arbitraryStx() {
        return new Stx(10, 20, 15, 2, 0, 0, true, true, new Histogram(10, 10, 20, 1), 12);
    }
}
