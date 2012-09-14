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
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.jblas.ComplexDoubleMatrix;
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
import java.util.Map;


@OperatorMetadata(alias = "GenericInSAROp",
        category = "InSAR Products",
        description = "Generic InSAR Operator", internal = false)
public class GenericInSAROp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();
    private HashMap<String, ProductContainer> ifgMap = new HashMap<String, ProductContainer>();

    private static final int ORBIT_DEGREE = 3; // hardcoded
    private static final boolean CREATE_VIRTUAL_BAND = true;

    private static final int TILE_OVERLAP_X = 0;
    private static final int TILE_OVERLAP_Y = 0;

    private static final String PRODUCT_NAME = "interferogram";
    private static final String PRODUCT_TAG = "ifg";

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

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            String nameMaster_I = master.realBand.getName();
            String nameMaster_Q = master.imagBand.getName();

            int counter = 0;

            for (Integer keySlave : slaveMap.keySet()) {

                // sourceSlave container
                final CplxContainer slave = slaveMap.get(keySlave);

                // get names of sourceSlave bands
                final String nameSlave_I = slave.realBand.getName();
                final String nameSlave_Q = slave.imagBand.getName();

                // generate name for product bands
                final String nameIfg = keyMaster.toString() + "_" + keySlave.toString();
                String iBandName = "i_" + PRODUCT_TAG + counter + "_" + nameMaster_I + "_" + nameSlave_I;
                String qBandName = "q_" + PRODUCT_TAG + counter + "_" + nameMaster_Q + "_" + nameSlave_Q;

                // initialize ifg-product container class
                final ProductContainer ifg = new ProductContainer(nameIfg, master, slave);

                // defined ifg-product bands
                ifg.targetBandName_I = iBandName;
                ifg.targetBandName_Q = qBandName;

                // put ifg-product bands into map
                ifgMap.put(nameIfg, ifg);

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
        // targetProduct.setPreferredTileSize(512, 512);

        // copy product nodes
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : ifgMap.keySet()) {

            // generate REAL band
            final ProductContainer ifg = ifgMap.get(key);
            final Band targetBandI = targetProduct.addBand(ifg.targetBandName_I, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.realBand, targetBandI);

            // generate IMAGINARY band
            final Band targetBandQ = targetProduct.addBand(ifg.targetBandName_Q, OUT_PRODUCT_DATA_TYPE);
            ProductUtils.copyRasterDataNodeProperties(ifg.sourceMaster.imagBand, targetBandQ);

            // generate virtual bands
            if (CREATE_VIRTUAL_BAND) {
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, ("_" + key));
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, ("_" + key));
            }

        }
    }

    private void checkUserInput() {
        // check for the logic in input paramaters
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

//            int x0 = targetRectangle.x;
//            int y0 = targetRectangle.y;
//            int w = targetRectangle.width;
//            int h = targetRectangle.height;
//            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            // target
            Band targetBand;

            // source
            Tile tileReal;
            Tile tileImag;

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final Rectangle rect = new Rectangle(targetRectangle);
            rect.width += TILE_OVERLAP_X;
            rect.height += TILE_OVERLAP_Y;

            // loop over ifg(product)Container
            for (String ifgTag : ifgMap.keySet()) {

                // get ifgContainer from pool
                final ProductContainer ifg = ifgMap.get(ifgTag);

                // check out from source
                tileReal = getSourceTile(ifg.sourceMaster.realBand, rect, border);
                tileImag = getSourceTile(ifg.sourceMaster.imagBand, rect, border);
                final ComplexDoubleMatrix masterMatrix = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                // check out from source
                tileReal = getSourceTile(ifg.sourceSlave.realBand, rect, border);
                tileImag = getSourceTile(ifg.sourceSlave.imagBand, rect, border);
                final ComplexDoubleMatrix slaveMatrix = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                // compute
                final ComplexDoubleMatrix cplxIfg = SarUtils.computeIfg(masterMatrix, slaveMatrix);

                // commit real() to target
                targetBand = targetProduct.getBand(ifg.targetBandName_I);
                tileReal = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(cplxIfg.real(), tileReal, targetRectangle);

                // commit imag() to target
                targetBand = targetProduct.getBand(ifg.targetBandName_Q);
                tileImag = targetTileMap.get(targetBand);
                TileUtilsDoris.pushFloatMatrix(cplxIfg.imag(), tileImag, targetRectangle);

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
            super(GenericInSAROp.class);
        }
    }
}