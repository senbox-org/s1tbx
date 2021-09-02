/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

public class ProductValidator {

    private final Product product;
    private final Options productOptions;
    private final InputProductValidator inputProductValidator;
    private Expected expected;

    private static final ProductData.UTC NO_TIME = new ProductData.UTC();

    public static class Options {
        public boolean verifyGeoCoding = true;
        public boolean verifyBands = true;
        public boolean verifyTimes = true;
        public boolean verifyTiePointGrids = true;
    }

    public static class Expected {
        public Boolean isSAR = null;
        public Boolean isComplex = null;
        public String productType = null;
    }

    public ProductValidator(final Product product) throws Exception {
        this(product, null);
    }

    public ProductValidator(final Product product, final Options options) throws Exception {
        if(product == null) {
            throw new Exception("Product is null");
        }
        this.product = product;
        this.productOptions = options == null ? new Options() : options;
        this.inputProductValidator = new InputProductValidator(product);
        this.expected = new Expected();
    }

    public void setExpected(final Expected expected) {
        this.expected = expected;
    }

    public void validateProduct() throws Exception {
        if (product == null) {
            throw new Exception("product is null");
        }
        if (productOptions.verifyGeoCoding && product.getSceneGeoCoding() == null) {
            throw new Exception("geocoding is null");
        }
        if (product.getMetadataRoot() == null) {
            throw new Exception("metadataroot is null");
        }
        if (isNotValid(product.getProductType())) {
            throw new Exception("productType is invalid " + product.getProductType());
        }
        if(isNotValid(product.getName())) {
            throw new Exception("product name is invalid " + product.getName());
        }
        if(isNotValid(product.getDescription())) {
            throw new Exception("product description is invalid " + product.getDescription());
        }
        if (product.getSceneRasterWidth() == 0 || product.getSceneRasterHeight() == 0
                || product.getSceneRasterWidth() == AbstractMetadata.NO_METADATA || product.getSceneRasterHeight() == AbstractMetadata.NO_METADATA) {
            throw new Exception("product scene raster dimensions are " + product.getSceneRasterWidth() +" x "+ product.getSceneRasterHeight());
        }

        verifyExpected();

        verifyTimes();
        verifyBands();
        verifyTiePointGrids();
    }

    private boolean isNotValid(final String str) {
        return str == null || str.isEmpty() || str.equals(AbstractMetadata.NO_METADATA_STRING);
    }

    private void verifyExpected() throws Exception {
        if(expected.isSAR != null) {
            if(inputProductValidator.isSARProduct() != expected.isSAR) {
                throw new Exception("Expecting SAR product " + expected.isSAR);
            }
        }
        if(expected.isComplex != null) {
            if(inputProductValidator.isComplex() != expected.isComplex) {
                throw new Exception("Expecting complex data " + expected.isComplex);
            }
        }
        if(expected.productType != null) {
            if(!product.getProductType().equals(expected.productType)) {
                throw new Exception("Expecting productType "+ expected.productType + " but got " +product.getProductType());
            }
        }
    }

    private void verifyTimes() throws Exception {
        if (!productOptions.verifyTimes) {
            SystemUtils.LOG.warning("ProductValidator Skipping verify times");
            return;
        }
        if (product.getStartTime() == null || product.getStartTime().getMJD() == NO_TIME.getMJD()) {
            throw new Exception("startTime is null");
        }
        if (product.getEndTime() == null || product.getEndTime().getMJD() == NO_TIME.getMJD()) {
            throw new Exception("endTime is null");
        }
    }

    private void verifyBands() throws Exception {
        if (!productOptions.verifyBands) {
            SystemUtils.LOG.warning("ProductValidator Skipping verify bands");
            return;
        }
        if (product.getNumBands() == 0) {
            throw new Exception("number of bands are zero");
        }

        for (Band band : product.getBands()) {
            if (isNotValid(band.getName())) {
                throw new Exception("band " + band.getName() + " has invalid name");
            }
            if (isNotValid(band.getUnit())) {
                throw new Exception("band " + band.getName() + " has invalid unit " + band.getUnit());
            }
            if (!band.isNoDataValueUsed()) {
                throw new Exception("Band " + band.getName() + " is not using a nodata value");
            }
            if (band.getRasterWidth() == 0 || band.getRasterHeight() == 0) {
                throw new Exception("band " + band.getName() + " raster dimensions are "
                        + band.getRasterWidth() + " x " + band.getRasterHeight());
            }

            if (isSAR()) {
                validateSARBand(band);
            } else {
                if(!isFlagsBand(band)) {
                    validateOpticalBand(band);
                }
            }
        }
    }

    private boolean isSAR() {
        return inputProductValidator.isSARProduct();
    }

    private static boolean isFlagsBand(final Band srcBand) {
        return srcBand.isFlagBand() || (srcBand.getName().contains("flag") || srcBand.getName().contains("mask"));
    }

    private void validateSARBand(final Band band) throws Exception {

    }

    private void validateOpticalBand(final Band band) throws Exception {
        if(band.getSpectralWavelength() < 10) {
            //throw new Exception("Band " + band.getName() + " has invalid spectral wavelength " + band.getSpectralWavelength());
        }
        if(band.getSpectralBandwidth() < 1) {
            //throw new Exception("Band " + band.getName() + " has invalid spectral bandwidth " + band.getSpectralBandwidth());
        }
    }

    private void verifyTiePointGrids() throws Exception {
        if (!productOptions.verifyTiePointGrids) {
            SystemUtils.LOG.warning("ProductValidator Skipping verify tie point grids");
            return;
        }

        if (inputProductValidator.isSARProduct()) {
            TiePointGrid incidenceAngleTPG = product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE);
            if(incidenceAngleTPG == null) {
                //throw new Exception(OperatorUtils.TPG_INCIDENT_ANGLE + " tie point grid is missing");
            }
            // only used by GG EC
//            TiePointGrid slantRangeTimeTPG = product.getTiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME);
//            if(slantRangeTimeTPG == null) {
//                throw new Exception(OperatorUtils.TPG_SLANT_RANGE_TIME + " tie point grid is missing");
//            }
        }
    }

    public void validateMetadata() throws Exception {
        validateMetadata(null);
    }

    public void validateMetadata(final MetadataValidator.Options options) throws Exception {
        if(product == null) {
            throw new Exception("Product is null");
        }
        final MetadataValidator metadataValidator = new MetadataValidator(product, options);
        metadataValidator.validate();
    }

    public void validateBands(final String[] bandNames) throws Exception {
        final Band[] bands = product.getBands();
        if(bandNames.length != bands.length) {
            String expectedBandNames = "";
            for(String bandName : product.getBandNames()) {
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
            Band band = product.getBand(bandName);
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
