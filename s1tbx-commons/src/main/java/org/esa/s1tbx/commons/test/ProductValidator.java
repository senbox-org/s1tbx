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
    private final ValidationOptions validationOptions;
    private final InputProductValidator inputProductValidator;

    private static final ProductData.UTC NO_TIME = new ProductData.UTC();

    public static class ValidationOptions {
        public boolean verifyGeoCoding = true;
        public boolean verifyBands = true;
        public boolean verifyTimes = true;
        public boolean verifyTiePointGrids = true;
    }

    public ProductValidator(final Product product) {
        this(product, null);
    }

    public ProductValidator(final Product product, final ValidationOptions options) {
        this.product = product;
        this.validationOptions = options == null ? new ValidationOptions() : options;
        this.inputProductValidator = new InputProductValidator(product);
    }

    public void validate() throws Exception {
        if (product == null) {
            throw new Exception("product is null");
        }
        if (validationOptions.verifyGeoCoding && product.getSceneGeoCoding() == null) {
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

        verifyTimes();
        verifyBands();
        verifyTiePointGrids();
    }

    private boolean isNotValid(final String str) {
        return str == null || str.isEmpty() || str.equals(AbstractMetadata.NO_METADATA_STRING);
    }

    private void verifyTimes() throws Exception {
        if (!validationOptions.verifyTimes) {
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
        if (!validationOptions.verifyBands) {
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

            if (inputProductValidator.isSARProduct()) {
                validateSARBand(band);
            } else {
                validateOpticalBand(band);
            }
        }
    }

    private void validateSARBand(final Band band) throws Exception {

    }

    private void validateOpticalBand(final Band band) throws Exception {

    }

    private void verifyTiePointGrids() throws Exception {
        if (!validationOptions.verifyTiePointGrids) {
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
}
