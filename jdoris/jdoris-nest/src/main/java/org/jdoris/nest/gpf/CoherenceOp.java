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
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jdoris.core.Orbit;
import org.jdoris.core.SLCImage;
import org.jdoris.core.utils.SarUtils;
import org.jdoris.nest.utils.BandUtilsDoris;
import org.jdoris.nest.utils.CplxContainer;
import org.jdoris.nest.utils.ProductContainer;
import org.jdoris.nest.utils.TileUtilsDoris;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.HashMap;

@OperatorMetadata(alias = "Coherence",
        category = "InSAR\\Products",
        description = "Estimate coherence from stack of coregistered images", internal = false)
public class CoherenceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "10",
            label = "Coherence Window Size in Azimuth")
    private int winAz = 10;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Range direction",
            defaultValue = "2",
            label = "Coherence Window Size in Range")
    private int winRg = 10;

    // source
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

    private static final int ORBIT_DEGREE = 3; // hardcoded
    private static final boolean CREATE_VIRTUAL_BAND = false;

    private static final String PRODUCT_NAME = "coherence";
    private static final String PRODUCT_TAG = "coh";

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

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void checkUserInput() {
        // TODO: use jdoris input.coherence class to check user input
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

    private void constructTargetMetadata() {

        // this means there is only one slave! but still do it in the loop
        // loop through masters
        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                // generate name for product bands
                String productName = keyMaster.toString() + "_" + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, false);

                product.targetBandName_I = PRODUCT_TAG + "_" + master.date + "_" + slave.date;
//                product.targetBandName_Q = null;

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
            String bandName = targetMap.get(key).targetBandName_I;
            targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            targetProduct.getBand(bandName).setUnit(Unit.COHERENCE);
        }

    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {

            final Rectangle rect = targetTile.getRectangle();
//            System.out.println("Original: x0 = " + rect.x + ", y = " + rect.y + ", w = " + rect.width + ", h = " + rect.height);
            final int x0 = rect.x - (winRg - 1) / 2;
            final int y0 = rect.y - (winAz - 1) / 2;
            final int w = rect.width + winRg - 1;
            final int h = rect.height + winAz - 1;
            rect.x = x0;
            rect.y = y0;
            rect.width = w;
            rect.height = h;

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
//            Band targetBand;

            for (String cohKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(cohKey);

                if (targetBand.getName().equals(product.targetBandName_I)) {

                    // check out from source
                    Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rect, border);
                    Tile tileImagMaster = getSourceTile(product.sourceMaster.imagBand, rect, border);
                    final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(tileRealMaster, tileImagMaster);// check out from source

                    Tile tileRealSlave = getSourceTile(product.sourceSlave.realBand, rect, border);
                    Tile tileImagSlave = getSourceTile(product.sourceSlave.imagBand, rect, border);
                    final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(tileRealSlave, tileImagSlave);

                    for (int i = 0; i < dataMaster.length; i++) {
                        double tmp = norm(dataMaster.get(i));
                        dataMaster.put(i, dataMaster.get(i).mul(dataSlave.get(i).conj()));
                        dataSlave.put(i, new ComplexDouble(norm(dataSlave.get(i)), tmp));
                    }

                    DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster, dataSlave, winAz, winRg);

//                targetBand = targetProduct.getBand(product.targetBandName_I);
//                Tile tileCoherence = targetTileMap.get(targetBand);
//                TileUtilsDoris.pushDoubleMatrix(cohMatrix, tileCoherence, targetRectangle);
                    TileUtilsDoris.pushDoubleMatrix(cohMatrix, targetTile, targetTile.getRectangle());

                }

            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private double norm(ComplexDouble number) {
        return Math.pow(number.real(), 2) + Math.pow(number.imag(), 2);
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
            super(CoherenceOp.class);
        }
    }
}
