package org.jlinda.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.filtering.RangeFilter;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "RangeFilter",
        category = "InSAR\\Tools",
        description = "Range Filter", internal = false)
public class RangeFilterOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"8", "16", "32", "64", "128", "256", "512", "1024"},
            description = "Length of filtering window",
            defaultValue = "5",
            label = "FFT Window Length")
    private int fftLength = 64;

//    @Parameter(valueSet = {"5", "10", "15", "20", "25", "30", "35", "40"},
//            description = "Overlap between tiles in range direction [pixels]",
//            defaultValue = "10",
//            label = "Range Filter Overlap")
//    private int rangeTileOverlap = 10;

    @Parameter(valueSet = {"0.5", "0.75", "0.8", "0.9", "1"},
            description = "Weight for Hamming filter (1 is rectangular window)",
            defaultValue = "0.75",
            label = "Hamming Alpha")
    private float alphaHamming = (float) 0.75;

    @Parameter(valueSet = {"5", "10", "15", "20", "25"},
            description = "Input value for (walking) mean averaging to reduce noise.",
            defaultValue = "15",
            label = "Walking Mean Window") //has to be odd!
    private int nlMean = 15;

    @Parameter(valueSet = {"3", "4", "5", "6", "7"},
            description = "Threshold on SNR for peak estimation",
            defaultValue = "5",
            label = "SNR Threshold")
    private float snrThresh = 5;

    @Parameter(valueSet = {"1", "2", "4"},
            description = "Oversampling factor (in range only).",
            defaultValue = "1",
            label = "Oversampling factor")
    private int ovsmpFactor = 1;

    @Parameter(valueSet = {"true", "false"},
            description = "Use weight values to bias higher frequencies",
            defaultValue = "off",
            label = "De-weighting")
    private boolean doWeightCorrel = false;

    // source
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

    private static final int ORBIT_DEGREE = 3; // hardcoded
    private static final boolean CREATE_VIRTUAL_BAND = true;

    private static int TILE_OVERLAP_X;
    private static int TILE_OVERLAP_Y;
    private static int TILE_EXTENT_X;
    private static int TILE_EXTENT_Y;

    private static final String PRODUCT_NAME = "range_filter";
    private static final String PRODUCT_TAG = "rngfilt";

    private static final int OUT_PRODUCT_DATA_TYPE = ProductData.TYPE_FLOAT32;


    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {

            checkUserInput();
            constructSourceMetadata();
            constructTargetMetadata();

            // getSourceImageGeocodings();
            // estimateFlatEarthPolynomial();
            // updateTargetProductMetadata();
            // updateTargetProductGeocoding();

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void constructTargetMetadata() {

        // loop through masters
        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);
            String masterSourceName_I = master.realBand.getName();
            String masterSourceName_Q = master.imagBand.getName();

            String masterTargetName_I = masterSourceName_I + "_" + PRODUCT_TAG;
            String masterTargetName_Q = masterSourceName_Q + "_" + PRODUCT_TAG;

            // generate name for product bands
            final String productName = keyMaster.toString();

            for (Integer keySlave : slaveMap.keySet()) {

                final CplxContainer slave = slaveMap.get(keySlave);
                String slaveSourceName_I = slave.realBand.getName();
                String slaveSourceName_Q = slave.imagBand.getName();

                String slaveTargetName_I = slaveSourceName_I + "_" + PRODUCT_TAG;
                String slaveTargetName_Q = slaveSourceName_Q + "_" + PRODUCT_TAG;

                final ProductContainer product = new ProductContainer(productName, master, slaveMap.get(keySlave), true);

                product.masterSubProduct.targetBandName_I = masterTargetName_I;
                product.masterSubProduct.targetBandName_Q = masterTargetName_Q;

                product.slaveSubProduct.targetBandName_I = slaveTargetName_I;
                product.slaveSubProduct.targetBandName_Q = slaveTargetName_Q;

                // put ifg-product bands into map
                targetMap.put(productName, product);

            }

        }

    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "mst";
        final String slaveTag = "slv";

        // get sourceMaster & sourceSlave MetadataElement
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;

        /* organize metadata */
        // put sourceMaster metadata into the masterMap
        metaMapPut(masterTag, masterMeta, sourceProduct, masterMap);

        // pug sourceSlave metadata into slaveMap
        MetadataElement[] slaveRoot = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot).getElements();
        for (MetadataElement meta : slaveRoot) {
            metaMapPut(slaveTag, meta, sourceProduct, slaveMap);
        }

    }

    private void metaMapPut(final String tag,
                            final MetadataElement root,
                            final Product product,
                            final HashMap<Integer, CplxContainer> map) throws Exception {

        // TODO: include polarization flags/checks!
        // pull out band names for this product
        final String[] bandNames = product.getBandNames();
        final int numOfBands = bandNames.length;

        // map key: ORBIT NUMBER
        int mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT);

        // metadata: construct classes and define bands
        final String date = OperatorUtils.getAcquisitionDate(root);
        final SLCImage meta = new SLCImage(root);
        final Orbit orbit = new Orbit(root, ORBIT_DEGREE);
        Band bandReal = null;
        Band bandImag = null;

        // TODO: boy this is one ugly construction!?
        // loop through all band names(!) : and pull out only one that matches criteria
        for (int i = 0; i < numOfBands; i++) {
            String bandName = bandNames[i];
            if (bandName.contains(tag) && bandName.contains(date)) {
                final Band band = product.getBandAt(i);
                if (BandUtilsDoris.isBandReal(band)) {
                    bandReal = band;
                } else if (BandUtilsDoris.isBandImag(band)) {
                    bandImag = band;
                }
            }
        }

        try {
            map.put(mapKey, new CplxContainer(date, meta, orbit, bandReal, bandImag));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTargetProduct() throws Exception {

        // construct target product
        targetProduct = new Product(PRODUCT_NAME,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        /// set prefered tile size : should be used only for testing and dev
//        targetProduct.setPreferredTileSize(1024, 128);

        // copy product nodes
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {

            final ProductContainer ifg = targetMap.get(key);

            Band targetBandI;
            Band targetBandQ;

            // generate REAL band of master-sub-product
            targetBandI = targetProduct.addBand(ifg.masterSubProduct.targetBandName_I, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.realBand, targetBandI);

            // generate IMAGINARY band of master-sub-product
            targetBandQ = targetProduct.addBand(ifg.masterSubProduct.targetBandName_Q, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.imagBand, targetBandQ);

            // generate virtual bands
            if (CREATE_VIRTUAL_BAND) {
                final String tag = ifg.sourceMaster.date;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
            }

            // generate REAL band of master-sub-product
            targetBandI = targetProduct.addBand(ifg.slaveSubProduct.targetBandName_I, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.realBand, targetBandI);

            // generate IMAGINARY band
            targetBandQ = targetProduct.addBand(ifg.slaveSubProduct.targetBandName_Q, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.imagBand, targetBandQ);

            // generate virtual bands
            if (CREATE_VIRTUAL_BAND) {
                final String tag = ifg.sourceSlave.date;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
            }
        }
    }

    private void checkUserInput() throws OperatorException {
//        TILE_OVERLAP_X = rangeTileOverlap;
        // check for the logic in input paramaters

        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final int isCoregStack = masterMeta.getAttributeInt(AbstractMetadata.coregistered_stack);
        if(isCoregStack != 1) {
            throw new OperatorException("Input should be a coregistered SLC stack");
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {

            int w = targetRectangle.width;
//            int x0 = targetRectangle.x;
//            int y0 = targetRectangle.y;
//            int h = targetRectangle.height;
//            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            int extraRange = 0;

            if (!MathUtils.isPower2(w)) {

                double nextPow2 = Math.ceil(Math.log(w) / Math.log(2));
                int value = (int) Math.pow(2, nextPow2);
                extraRange = value - w;
//                targetRectangle.width = value;
            }

            // target
            Band targetBand;

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final Rectangle rect = new Rectangle(targetRectangle);
            rect.width += (TILE_OVERLAP_X + extraRange);
            rect.height += TILE_OVERLAP_Y;
            //System.out.println("x0 = " + rect.x + ", y0 = " + rect.y + ", w = " + rect.width + ", h = " + rect.height);

            boolean doFilterMaster = true;
            if (masterMap.keySet().toArray().length > 1) {
                doFilterMaster = false;
            }

            // loop over ifg(product)Container
            for (String ifgTag : targetMap.keySet()) {

                final RangeFilter rangeFilter = new RangeFilter();

                rangeFilter.setAlphaHamming(alphaHamming);
                rangeFilter.setDoWeightCorrelFlag(doWeightCorrel);
                rangeFilter.setOvsFactor(ovsmpFactor);
                rangeFilter.setFftLength(fftLength);
                rangeFilter.setNlMean(nlMean);
                rangeFilter.setSNRthreshold(snrThresh);

                // get ifgContainer from pool
                final ProductContainer ifg = targetMap.get(ifgTag);

                // check out from source
                Tile tileRealMaster = getSourceTile(ifg.sourceMaster.realBand, rect, border);
                Tile tileImagMaster = getSourceTile(ifg.sourceMaster.imagBand, rect, border);
                final ComplexDoubleMatrix masterMatrix = TileUtilsDoris.pullComplexDoubleMatrix(tileRealMaster, tileImagMaster);

                // check out from source
                Tile tileRealSlave = getSourceTile(ifg.sourceSlave.realBand, rect, border);
                Tile tileImagSlave = getSourceTile(ifg.sourceSlave.imagBand, rect, border);
                final ComplexDoubleMatrix slaveMatrix = TileUtilsDoris.pullComplexDoubleMatrix(tileRealSlave, tileImagSlave);

                rangeFilter.setMetadata(ifg.sourceMaster.metaData);
                rangeFilter.setData(masterMatrix);

                rangeFilter.setMetadata1(ifg.sourceSlave.metaData);
                rangeFilter.setData1(slaveMatrix);

                // compute
                rangeFilter.defineParameters();
                rangeFilter.defineFilter();

                if (doFilterMaster) {
                    rangeFilter.applyFilter();
                } else {
                    // apply only on slave data!
                    rangeFilter.applyFilterSlave();
                }

                /// MASTER
                ComplexDoubleMatrix filteredMaster;
                if (doFilterMaster) {
                    filteredMaster = rangeFilter.getData();
                } else {
                    filteredMaster = masterMatrix;
                }

                // commit real() to target
                targetBand = targetProduct.getBand(ifg.masterSubProduct.targetBandName_I);
                tileRealMaster = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(filteredMaster.real(), tileRealMaster, targetRectangle);

                // commit imag() to target
                targetBand = targetProduct.getBand(ifg.masterSubProduct.targetBandName_Q);
                tileImagMaster = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(filteredMaster.imag(), tileImagMaster, targetRectangle);

                /// SLAVE
                final ComplexDoubleMatrix filteredSlave = rangeFilter.getData1();
                // commit real() to target
                targetBand = targetProduct.getBand(ifg.slaveSubProduct.targetBandName_I);
                tileRealSlave = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(filteredSlave.real(), tileRealSlave, targetRectangle);

                // commit imag() to target
                targetBand = targetProduct.getBand(ifg.slaveSubProduct.targetBandName_Q);
                tileImagSlave = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(filteredSlave.imag(), tileImagSlave, targetRectangle);

//                // save imag band of computation : this is somehow too slow?
//                targetBand = targetProduct.getBand(ifg.targetBandName_Q);
//                tileReal = targetTileMap.get(targetBand);
//                targetBand = targetProduct.getBand(ifg.targetBandName_I);
//                tileImag = targetTileMap.get(targetBand);
//                TileUtilsDoris.pushComplexFloatMatrix(cplxIfg, tileReal, tileImag, targetRectangle);

            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RangeFilterOp.class);
        }
    }
}
