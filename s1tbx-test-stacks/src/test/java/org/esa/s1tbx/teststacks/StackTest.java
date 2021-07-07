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
package org.esa.s1tbx.teststacks;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.insar.gpf.InterferogramOp;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.s1tbx.insar.gpf.coregistration.CrossCorrelationOp;
import org.esa.s1tbx.insar.gpf.coregistration.WarpOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StackTest extends ProcessorTest {

    protected Product createStackProduct(final List<Product> products) {
        CreateStackOp createStackOp = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStackOp.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        return createStackOp.getTargetProduct();
    }

    protected Product coregister(final List<Product> products) {
        CreateStackOp createStack = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStack.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        CrossCorrelationOp crossCorrelation = new CrossCorrelationOp();
        crossCorrelation.setSourceProduct(createStack.getTargetProduct());

        WarpOp warp = new WarpOp();
        warp.setSourceProduct(crossCorrelation.getTargetProduct());
        warp.setParameter("openResidualsFile", true);

        return warp.getTargetProduct();
    }

    protected Product coregisterInterferogram(final List<Product> srcProducts, final File trgFolder, final String format) throws IOException {
        Product coregisteredStack = coregister(srcProducts);

        InterferogramOp interferogram = new InterferogramOp();
        interferogram.setSourceProduct(coregisteredStack);

        Product trgProduct = interferogram.getTargetProduct();

        ProductIO.writeProduct(trgProduct, trgFolder, format, true);

        return trgProduct;
    }

    protected void closeProducts(final List<Product> products) {
        for(Product product : products) {
            if(product != null)
                product.dispose();
        }
    }
}
