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
package org.esa.s1tbx.commons.test;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProcessorTest {

    static {
        TestUtils.initTestEnvironment();
    }

    protected File createTmpFolder(final String folderName) throws IOException {
        File folder = Files.createTempDirectory(folderName).toFile();
        //File folder = new File("c:\\out\\" + folderName);
        folder.mkdirs();
        return folder;
    }

    protected void validateProduct(final Product product) throws Exception {
        validateProduct(product, null);
    }

    protected void validateProduct(final Product product, final ProductValidator.ValidationOptions options) throws Exception {
        if(product == null) {
            throw new Exception("Product is null");
        }
        final ProductValidator productValidator = new ProductValidator(product, options);
        productValidator.validate();
    }

    protected void validateMetadata(final Product product) throws Exception {
        validateMetadata(product, null);
    }

    protected void validateMetadata(final Product product, final MetadataValidator.ValidationOptions options) throws Exception {
        if(product == null) {
            throw new Exception("Product is null");
        }
        final MetadataValidator metadataValidator = new MetadataValidator(product, options);
        metadataValidator.validate();
    }

    protected void validateBands(final Product trgProduct, final String[] bandNames) throws Exception {
        final Band[] bands = trgProduct.getBands();
        if(bandNames.length != bands.length) {
            String expectedBandNames = "";
            for(String bandName : trgProduct.getBandNames()) {
                if(!expectedBandNames.isEmpty())
                    expectedBandNames += ", ";
                expectedBandNames += bandName;
            }
            String actualBandNames = "";
            for(String bandName : bandNames) {
                if(!actualBandNames.isEmpty())
                    actualBandNames += ", ";
                actualBandNames += bandName;
            }
            throw new Exception("Expecting "+bandNames.length + " bands "+actualBandNames+" but found "+ bands.length +" "+ expectedBandNames);
        }
        for(String bandName : bandNames) {
            Band band = trgProduct.getBand(bandName);
            if(band == null) {
                throw new Exception("Band "+ bandName +" not found");
            }
            if(band.getUnit() == null) {
                throw new Exception("Band "+ bandName +" is missing a unit");
            }
            if(!band.isNoDataValueUsed()) {
                throw new Exception("Band "+ bandName +" is not using a nodata value");
            }
        }
    }
}
