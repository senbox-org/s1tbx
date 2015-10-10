/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.datamodel.metadata;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by lveci on 22/07/2014.
 */
public class TestAbstractMetadata {


    @Test
    public void TestAbstractMetadataSAR() throws IOException {
        final Product product = createTestProduct(10, 10);
        createMetadata(product);

        AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        String dem = sarMeta.getAttributeString(AbstractMetadataSAR.DEM);
        assertEquals(dem, "dem");

        AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        String productName = absMeta.getAttributeString(AbstractMetadata.product_name);
        assertEquals(productName, "name");
    }

    private static Product createProduct(final String type, final int w, final int h) {
        final Product product = new Product("name", type, w, h);

        product.setStartTime(AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683"));
        product.setEndTime(AbstractMetadata.parseUTC("10-MAY-2008 20:35:46.890683"));
        product.setDescription("description");

        return product;
    }

    private static Product createTestProduct(final int w, final int h) {

        final Product testProduct = createProduct("ASA_APG_1P", w, h);
        TestUtils.createBand(testProduct, "band1", w, h);

        return testProduct;
    }

    private static void createMetadata(final Product product) throws IOException {
        final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        sarMeta.setAttribute(AbstractMetadataSAR.DEM, "dem");

        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        absMeta.setAttribute(AbstractMetadata.product_name, "name");
    }
}
