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
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jdoris.core.Orbit;
import org.jdoris.core.SLCImage;
import org.jdoris.core.filtering.PhaseFilter;
import org.jdoris.nest.utils.BandUtilsDoris;
import org.jdoris.nest.utils.CplxContainer;
import org.jdoris.nest.utils.ProductContainer;
import org.jdoris.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.jdoris.core.utils.MathUtils.isPower2;

@OperatorMetadata(alias = "PhaseFilter",
        category = "InSAR\\Tools",
        description = "Interferometric phase filtering",
        internal = false)
public class PhaseFilterOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"goldstein", "convolution"},
            description = "Select filtering method",
            defaultValue = "goldstein",
            label = "Method")
    private String method = "goldstein";

    @Parameter(valueSet = {"0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1"},
            description = "Smoothing parameter, 0 (no filtering) -> 1 (most filtering)",
            defaultValue = "0.5",
            label = "Alpha")
    private float alpha;

    @Parameter(valueSet = {"16", "32", "64"},
            description = "Size of data blocks for filtering",
            defaultValue = "32",
            label = "Blocksize")
    private int blockSize;

    @Parameter(valueSet = {"2", "4", "8", "10", "12","16","20"},
            description = "Half of overlap between consecutive filtering tiles",
            defaultValue = "8",
            label = "Overlap")
    private int overlap;

    @Parameter(valueSet = {"none", "1 1 1 1 1", "1 2 2 2 1", "1 2 3 2 1",
                                   "1 1 1", "1 2 1"},
            description = "Set of pre-defined kernels for filtering",
            defaultValue = "1 2 3 2 1",
            label = "Kernel")
    private String kernelInput;
    private double[] kernelArray;

    // source maps
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target maps
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

    // operator tags
    private static final boolean CREATE_VIRTUAL_BAND = true;
    private static final String PRODUCT_NAME = "filt_ifgs";
    public static final String PRODUCT_TAG = "_ifg_filt";


    @Override
    public void initialize() throws OperatorException {
        try {
            checkUserInput();
            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void checkUserInput() {
        
        if (!isPower2(blockSize)) {
            System.out.println("BlockSize is not power of two!");
        }

        if (kernelInput.equals("1 1 1 1 1")) {
            kernelArray = new double[]{1. / 5, 1. / 5, 1. / 5, 1. / 5, 1. / 5};
        } else if (kernelInput.equals("1 2 2 2 1")) {
            kernelArray = new double[]{1. / 5, 2. / 5, 2. / 5, 2. / 5, 1. / 5};
        } else if (kernelInput.equals("1 2 3 2 1")) {
            kernelArray = new double[]{1. / 5, 2. / 5, 3. / 5, 2. / 5, 1. / 5};
        } else if (kernelInput.equals("1 1 1")) {
            kernelArray = new double[]{1. / 3, 1. / 3, 1./ 3};
        } else if (kernelInput.equals("1 2 1")) {
            kernelArray = new double[]{1. / 5, 2. / 5, 1. / 5};
        } else if (kernelInput.equals("none")) {
            kernelArray = null;
        } else { // default
            kernelArray = new double[]{1 / 5, 1 / 5, 1 / 5, 1 / 5, 1 / 5};
        }

        if (method.contains("convolution")) {
            blockSize = kernelArray.length;
            overlap = (int) Math.floor(blockSize / 2);
        }

    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "ifg";
        final String slaveTag = "dummy";

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

        // pull out band names for this product
        final String[] bandNames = product.getBandNames();
        final int numOfBands = bandNames.length;

        // map key: ORBIT NUMBER
        int mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT);

        // metadata: construct classes and define bands
        final String date = OperatorUtils.getAcquisitionDate(root);
        final SLCImage meta = new SLCImage(root);
        final Orbit orbit = null; // ORBIT not needed

        // TODO: resolve multilook factors
        meta.setMlAz(1);
        meta.setMlRg(1);

        Band bandReal = null;
        Band bandImag = null;

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

    private void constructTargetMetadata() {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                // generate name for product bands
                String productName = keyMaster.toString() + "_" + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, true);

                product.targetBandName_I = "i" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;
                product.targetBandName_Q = "q" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;

                // put ifg-product bands into map
                targetMap.put(productName, product);

            }

        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(PRODUCT_NAME,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for (final Band band : targetProduct.getBands()) {
            targetProduct.removeBand(band);
        }

        for (String key : targetMap.keySet()) {

            String targetBandName_I = targetMap.get(key).targetBandName_I;
            String targetBandName_Q = targetMap.get(key).targetBandName_Q;
            targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_I).setUnit(Unit.REAL);
            targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_Q).setUnit(Unit.IMAGINARY);

            final String tag0 = targetMap.get(key).sourceMaster.date;
            final String tag1 = targetMap.get(key).sourceSlave.date;

            if (CREATE_VIRTUAL_BAND) {
                String countStr = PRODUCT_TAG + "_" + tag0 + "_" + tag1;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
            }

        }

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

//            String filterType = "goldstein";
//            final float alpha = (float) 0.75;
//            final int overlap = 8;
//            final double[] kernel = new double[]{1. / 5, 1. / 5, 1. / 5, 1. / 5, 1. / 5};
//            final int blockSize = 64;

            Band targetBand_I;
            Band targetBand_Q;

            final Rectangle rectIn = new Rectangle(targetRectangle);
            final Rectangle rectOut = new Rectangle(targetRectangle);

            rectIn.y -= (overlap);
            rectIn.height += (2*overlap);
            rectIn.x -= (overlap);
            rectIn.width += (2*overlap);

            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                Tile tileReal = getSourceTile(product.sourceMaster.realBand, rectIn);
                Tile tileImag = getSourceTile(product.sourceMaster.imagBand, rectIn);

                // put interferogram together
                ComplexDoubleMatrix complexIfg = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                PhaseFilter phaseFilter = new PhaseFilter(method, complexIfg, blockSize, overlap, kernelArray, alpha);
                phaseFilter.filter();
                complexIfg = phaseFilter.getData();

                // commit to target [note the tile overlap and boundary because of filter]
                // output [x0,y0] is shifted because of the tile overlap
                targetBand_I = targetProduct.getBand(product.targetBandName_I);
                Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.real(), tileOutReal, rectOut, overlap, overlap);

                targetBand_Q = targetProduct.getBand(product.targetBandName_Q);
                Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.imag(), tileOutImag, rectOut, overlap, overlap);

            }

        } catch (Exception e) {
            throw new OperatorException(e);
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
            super(PhaseFilterOp.class);
        }
    }

}
