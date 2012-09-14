package org.jdoris.nest.gpf;

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
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jdoris.core.Orbit;
import org.jdoris.core.SLCImage;
import org.jdoris.core.Window;
import org.jdoris.core.filtering.AzimuthFilter;
import org.jdoris.nest.utils.BandUtilsDoris;
import org.jdoris.nest.utils.CplxContainer;
import org.jdoris.nest.utils.ProductContainer;
import org.jdoris.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@OperatorMetadata(alias = "AzimuthFilter",
        category = "InSAR\\Tools",
        description = "Azimuth Filter", internal = false)
public class AzimuthFilterOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"64", "128", "256", "512", "1024", "2048"},
            description = "Length of filtering window",
            defaultValue = "256",
            label = "FFT Window Length")
    private int fftLength = 256;

    @Parameter(valueSet = {"0", "8", "16", "32", "64", "128", "256"},
            description = "Overlap between filtering windows in azimuth direction [lines]",
            defaultValue = "0",
            label = "Azimuth Filter Overlap")
    private int aziFilterOverlap = 0;

    @Parameter(valueSet = {"0.5", "0.75", "0.8", "0.9", "1"},
            description = "Weight for Hamming filter (1 is rectangular window)",
            defaultValue = "0.75",
            label = "Hamming Alpha")
    private float alphaHamming = (float) 0.75;

    // source
    private LinkedHashMap<Integer, CplxContainer> masterMap = new LinkedHashMap<Integer, CplxContainer>();
    private LinkedHashMap<Integer, CplxContainer> slaveMap = new LinkedHashMap<Integer, CplxContainer>();

    // target
    private LinkedHashMap<String, ProductContainer> targetMap = new LinkedHashMap<String, ProductContainer>();

    private static final int ORBIT_DEGREE = 3; // hardcoded
    private static final boolean CREATE_VIRTUAL_BAND = true;

    private static boolean doFilterMaster = true;

    private static final String PRODUCT_NAME = "azimuth_filter";
    private static final String PRODUCT_TAG = "azifilt";

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

        if (doFilterMaster) {

            // this means there is only one slave! but still do it in the loop
            // loop through masters
            for (Integer keyMaster : masterMap.keySet()) {

                CplxContainer master = masterMap.get(keyMaster);
                String sourceName_I = master.realBand.getName();
                String sourceName_Q = master.imagBand.getName();

                String targetName_I = sourceName_I + "_" + PRODUCT_TAG;
                String targetName_Q = sourceName_Q + "_" + PRODUCT_TAG;

                // generate name for product bands
                final String productName = keyMaster.toString();

                for (Integer keySlave : slaveMap.keySet()) {

                    final CplxContainer slave = slaveMap.get(keySlave);
                    final ProductContainer product = new ProductContainer(productName, master, slave, false);

                    product.targetBandName_I = targetName_I;
                    product.targetBandName_Q = targetName_Q;

                    // put ifg-product bands into map
                    targetMap.put(productName, product);

                }

            }
        }

        // loop through slaves
        for (Integer key : slaveMap.keySet()) {

            CplxContainer slave = slaveMap.get(key);
            String sourceName_I = slave.realBand.getName();
            String sourceName_Q = slave.imagBand.getName();

            String targetName_I = sourceName_I + "_" + PRODUCT_TAG;
            String targetName_Q = sourceName_Q + "_" + PRODUCT_TAG;

            // generate name for product bands
            final String productName = key.toString();

            for (Integer keySlave : masterMap.keySet()) {

                final CplxContainer master = masterMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, slave, master, false);

                product.targetBandName_I = targetName_I;
                product.targetBandName_Q = targetName_Q;

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

        // check how many slaves
        if (slaveMap.keySet().toArray().length > 1) {
            doFilterMaster = false;
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

        // set prefered tile size : should be used only for testing and dev
        targetProduct.setPreferredTileSize(128,128);

        // copy product nodes
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {

            final ProductContainer product = targetMap.get(key);

            Band targetBandI;
            Band targetBandQ;

            // generate REAL band of master-sub-product
            targetBandI = targetProduct.addBand(product.targetBandName_I, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(product.sourceMaster.realBand, targetBandI);

            // generate IMAGINARY band of master-sub-product
            targetBandQ = targetProduct.addBand(product.targetBandName_Q, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(product.sourceMaster.imagBand, targetBandQ);

            // generate virtual bands
            if (CREATE_VIRTUAL_BAND) {
                final String tag = product.sourceMaster.date;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, ("_" + tag));
            }

        }
    }

    private void checkUserInput() throws OperatorException {

        // TILE_OVERLAP_X = aziFilterOverlap;

        // check for the logic in input paramaters
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final int isCoregStack = masterMeta.getAttributeInt(AbstractMetadata.coregistered_stack);
        if (isCoregStack != 1) {
            throw new OperatorException("Input should be a coregistered SLC stack");
        }
    }

    private void updateTargetProductMetadata() {
        // update metadata of target product for the estimated polynomial
    }

    private void updateTargetProductGeocoding() {
        // update metadata of target product for the estimated polynomial
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

            final Rectangle rectIn = new Rectangle(targetRectangle);

            rectIn.y -= aziFilterOverlap;
            rectIn.height += 2*aziFilterOverlap;

            // target
            Band targetBand;

            // loop over ifg(product)Container : both master and slave defined in container
            for (ProductContainer product : targetMap.values()) {

                // check out from source
                Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rectIn);
                Tile tileImagMaster = getSourceTile(product.sourceMaster.imagBand, rectIn);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(tileRealMaster, tileImagMaster);

                // construct azimuthfilter
                final AzimuthFilter azimuthMaster = new AzimuthFilter();

                // set filtering parameters
                azimuthMaster.setHammingAlpha(alphaHamming);
                azimuthMaster.setMetadata(product.sourceMaster.metaData);
                azimuthMaster.setMetadata1(product.sourceSlave.metaData);
                // TODO: variable constant hard-coded, further testing needed
                azimuthMaster.setVariableFilter(false); // hardcoded to const filtering!
                azimuthMaster.setTile(new Window(rectIn));

                // set data for filtering
                azimuthMaster.setData(dataMaster);

                // define parameters and filter
                azimuthMaster.defineParameters();
                azimuthMaster.defineFilter();
                azimuthMaster.applyFilter();

                // get data from filter
                // commit real() to target
                targetBand = targetProduct.getBand(product.targetBandName_I);
                tileRealMaster = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(azimuthMaster.getData().real(), tileRealMaster, targetRectangle, aziFilterOverlap, 0);

                // commit imag() to target
                targetBand = targetProduct.getBand(product.targetBandName_Q);
                tileImagMaster = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(azimuthMaster.getData().imag(), tileImagMaster, targetRectangle, aziFilterOverlap, 0);

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
            super(AzimuthFilterOp.class);
        }
    }
}
