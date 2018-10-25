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
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;

/**

 */
public abstract class RecursiveProcessor {


    public int recurseProcessFolder(final File folder, int iterations, int maxIteration,
                                    final String[] productTypeExemptions,
                                    final String[] exceptionExemptions) throws Exception {
        final File[] fileList = folder.listFiles(new ProductFunctions.ValidProductFileFilter(true));
        if(fileList == null)
            return iterations;

        for (File file : fileList) {
            if (maxIteration > 0 && iterations >= maxIteration)
                break;

            if (file.isDirectory()) {
                if (!file.getName().contains(TestUtils.SKIPTEST)) {
                    iterations = recurseProcessFolder(file, iterations, maxIteration, productTypeExemptions, exceptionExemptions);
                }
            } else {
                try {
                    final Product sourceProduct = CommonReaders.readProduct(file);
                    if (sourceProduct != null) {
                        if (productTypeExemptions != null && TestUtils.containsProductType(productTypeExemptions, sourceProduct.getProductType()))
                            continue;

                        process(sourceProduct);

                        ++iterations;
                    }
                } catch (Exception e) {
                    boolean ok = false;
                    if (exceptionExemptions != null) {
                        for (String exemption : exceptionExemptions) {
                            if (e.getMessage().contains(exemption)) {
                                ok = true;
                                System.out.println("Exemption for " + e.getMessage());
                                break;
                            }
                        }
                    }
                    if (!ok) {
                        System.out.println("Failed to process " + file.toString());
                        throw e;
                    }
                }
            }
        }
        return iterations;
    }

    protected abstract void process(final Product sourceProduct);
}
