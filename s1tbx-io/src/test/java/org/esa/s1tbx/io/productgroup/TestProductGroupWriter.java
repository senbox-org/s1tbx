/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;

public class TestProductGroupWriter extends ProcessorTest {

    @Test
    public void testWrite() throws Exception {
        Product product = createStackProduct();
        addMetadata(product);

        final File targetFolder = createTmpFolder("productgroups");
        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);
    }


    private Product createStackProduct() {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        for(int i=1; i < 5; ++i) {
            TestUtils.createBand(product, "band" + i, w, h);
        }

        return product;
    }

    private void addMetadata(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        MetadataAttribute attribute = AbstractMetadata.addAbstractedAttribute(absRoot, AbstractMetadata.collocated_stack, ProductData.TYPE_INT8, "", "");
        attribute.getData().setElemInt(1);

    }
}
