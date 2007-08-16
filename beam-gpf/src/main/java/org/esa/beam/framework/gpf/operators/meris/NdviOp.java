package org.esa.beam.framework.gpf.operators.meris;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.support.CachingOperator;
import org.esa.beam.framework.gpf.support.ProductDataCache;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.io.IOException;

/**
 * The <code>NdviOp</code> uses MERIS Level-1b TOA radiances of bands 6 and 10
 * to retrieve the Normalized Difference Vegetation Index (NDVI).
 *
 * @author Maximilian Aulinger
 */
public class NdviOp extends CachingOperator {

    // constants
    public static final String NDVI_PRODUCT_TYPE = "MER_NDVI2P";
    public static final String NDVI_BAND_NAME = "ndvi";
    public static final String NDVI_FLAGS_BAND_NAME = "ndvi_flags";
    public static final String NDVI_ARITHMETIC_FLAG_NAME = "NDVI_ARITHMETIC";
    public static final String NDVI_LOW_FLAG_NAME = "NDVI_NEGATIVE";
    public static final String NDVI_HIGH_FLAG_NAME = "NDVI_SATURATION";
    public static final int NDVI_ARITHMETIC_FLAG_VALUE = 1;
    public static final int NDVI_LOW_FLAG_VALUE = 1 << 1;
    public static final int NDVI_HIGH_FLAG_VALUE = 1 << 2;
    public static final String L1FLAGS_INPUT_BAND_NAME = "l1_flags";
    public static final String LOWER_INPUT_BAND_NAME = "radiance_6";
    public static final String UPPER_INPUT_BAND_NAME = "radiance_10";
    private Band _lowerInputBand;
    private Band _upperInputBand;
    private Product product;


    public NdviOp(OperatorSpi spi) {
        super(spi);
    }

    @Override
    public boolean isComputingAllBandsAtOnce() {
        return false;
    }

    @Override
    public Product createTargetProduct(ProgressMonitor pm) throws OperatorException {
        Product inputProduct = getContext().getSourceProduct("input");
        loadSourceBands(inputProduct);
        int sceneWidth = inputProduct.getSceneRasterWidth();
        int sceneHeight = inputProduct.getSceneRasterHeight();
        // create the in memory represenation of the output product
        // ---------------------------------------------------------
        // the product itself
        product = new Product("ndvi", NDVI_PRODUCT_TYPE, sceneWidth, sceneHeight);

        // create and add the NDVI band
        Band ndviOutputBand = new Band(NDVI_BAND_NAME, ProductData.TYPE_FLOAT32, sceneWidth,
                                       sceneHeight);
        product.addBand(ndviOutputBand);

        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(inputProduct, product);

        // copy geo-coding and the lat/lon tiepoints to the output product
        ProductUtils.copyGeoCoding(inputProduct, product);

        ProductUtils.copyFlagBands(inputProduct, product);

        // create and add the NDVI flags coding
        FlagCoding ndviFlagCoding = createNdviFlagCoding();
        product.addFlagCoding(ndviFlagCoding);

        // create and add the NDVI flags band
        Band ndviFlagsOutputBand = new Band(NDVI_FLAGS_BAND_NAME, ProductData.TYPE_INT32,
                                            sceneWidth, sceneHeight);
        ndviFlagsOutputBand.setDescription("NDVI specific flags");
        ndviFlagsOutputBand.setFlagCoding(ndviFlagCoding);
        product.addBand(ndviFlagsOutputBand);

        // Copy predefined bitmask definitions
        ProductUtils.copyBitmaskDefs(inputProduct, product);
        product.addBitmaskDef(new BitmaskDef(NDVI_ARITHMETIC_FLAG_NAME.toLowerCase(),
                                             "An arithmetic exception occured.", NDVI_FLAGS_BAND_NAME + "."
                + NDVI_ARITHMETIC_FLAG_NAME, Color.red.brighter(), 0.7f));
        product.addBitmaskDef(new BitmaskDef(NDVI_LOW_FLAG_NAME.toLowerCase(),
                                             "NDVI value is too low.", NDVI_FLAGS_BAND_NAME + "." + NDVI_LOW_FLAG_NAME,
                                             Color.red, 0.7f));
        product.addBitmaskDef(new BitmaskDef(NDVI_HIGH_FLAG_NAME.toLowerCase(),
                                             "NDVI value is too high.", NDVI_FLAGS_BAND_NAME + "." + NDVI_HIGH_FLAG_NAME,
                                             Color.red.darker(), 0.7f));

        return product;
    }

    @Override
    public void computeTile(Band band, Rectangle rectangle,
                            ProductDataCache cache, ProgressMonitor pm) throws OperatorException {


        if (band.getName().equals(L1FLAGS_INPUT_BAND_NAME)) {
            ProductData destBuffer = cache.createData(band);
            try {
                Band band2 = getContext().getSourceProduct("input").getBand(L1FLAGS_INPUT_BAND_NAME);
                getContext().getSourceProduct("input").getProductReader().readBandRasterData(band2, rectangle.x,
                                                                                rectangle.y, rectangle.width, rectangle.height, destBuffer, pm);
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        } else {
            // for all required bands loop over requested scanlines
            pm.beginTask("Computing NDVI", rectangle.height);

            ProductData ndviBuffer = cache.createData(product.getBand(NDVI_BAND_NAME));
            ProductData ndviFlagsBuffer = cache.createData(product.getBand(NDVI_FLAGS_BAND_NAME));

            int destOffsetX = rectangle.x;
            int destOffsetY = rectangle.y;
            int width = rectangle.width;
            int height = rectangle.height;

            // first of all - allocate memory for single scanline
            float[] lower = new float[width];
            float[] upper = new float[width];

            float ndviValue;
            int ndviFlagsValue;

            for (int y = 0; y < height; y++) {
                // read the input data scanline-wise
                try {
                    _lowerInputBand.readPixels(destOffsetX, destOffsetY + y, width, 1, lower,
                                               SubProgressMonitor.create(pm, 1));
                    _upperInputBand.readPixels(destOffsetX, destOffsetY + y, width, 1, upper,
                                               SubProgressMonitor.create(pm, 1));

                } catch (IOException e) {
                    throw new OperatorException(e);
                }

                // process the complete scanline
                for (int x = 0; x < width; x++) {
                    ndviValue = (upper[x] - lower[x]) / (upper[x] + lower[x]);
                    ndviFlagsValue = 0;
                    if (Float.isNaN(ndviValue) || Float.isInfinite(ndviValue)) {
                        ndviFlagsValue |= NDVI_ARITHMETIC_FLAG_VALUE;
                        ndviValue = 0f;
                    }
                    if (ndviValue < 0.0f) {
                        ndviFlagsValue |= NDVI_LOW_FLAG_VALUE;
                    }
                    if (ndviValue > 1.0f) {
                        ndviFlagsValue |= NDVI_HIGH_FLAG_VALUE;
                    }
                    ndviBuffer.setElemFloatAt(y * width + x, ndviValue);
                    ndviFlagsBuffer.setElemIntAt(y * width + x, ndviFlagsValue);
                }
                pm.worked(1);
            }
            pm.done();
        }
    }

    private void loadSourceBands(Product inputProduct) throws OperatorException {
        _lowerInputBand = inputProduct.getBand(LOWER_INPUT_BAND_NAME);
        if (_lowerInputBand == null) {
            throw new OperatorException("Can not load band " + LOWER_INPUT_BAND_NAME);
        }

        _upperInputBand = inputProduct.getBand(UPPER_INPUT_BAND_NAME);
        if (_upperInputBand == null) {
            throw new OperatorException("Can not load band " + UPPER_INPUT_BAND_NAME);
        }

    }

    private static FlagCoding createNdviFlagCoding() {

        FlagCoding ndviFlagCoding = new FlagCoding("ndvi_flags");
        ndviFlagCoding.setDescription("NDVI Flag Coding");

        MetadataAttribute attribute;

        attribute = new MetadataAttribute(NDVI_ARITHMETIC_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_ARITHMETIC_FLAG_VALUE);
        attribute.setDescription("NDVI value calculation failed due to an arithmetic exception");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_LOW_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_LOW_FLAG_VALUE);
        attribute.setDescription("NDVI value is too low");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_HIGH_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_HIGH_FLAG_VALUE);
        attribute.setDescription("NDVI value is too high");
        ndviFlagCoding.addAttribute(attribute);

        return ndviFlagCoding;
    }

    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(NdviOp.class, "SimpleNdvi");
        }

    }
}