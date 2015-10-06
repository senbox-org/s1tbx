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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

public class AbstractNetcdfMultiLevelImageTest {

    @Test
    public void testCreatedImageHasSampleModel() {
        final Product product = new Product("product", "type", 101, 101);
        final Band rasterDataNode = product.addBand("name", ProductData.TYPE_INT32);
        final AbstractNetcdfMultiLevelImage multiLevelImage = new AbstractNetcdfMultiLevelImage(rasterDataNode) {
            @Override
            protected RenderedImage createImage(int level) {
                return null;
            }
        };
        assertNotNull("SampleModel is null", multiLevelImage.getSampleModel());
    }

}
