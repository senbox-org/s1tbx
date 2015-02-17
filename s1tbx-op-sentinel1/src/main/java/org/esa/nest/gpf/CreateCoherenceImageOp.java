package org.esa.nest.gpf;

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
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.HashMap;

@OperatorMetadata(alias = "Coherence",
        category = "SAR Processing/Interferometric/Products",
        authors = "Petar Marinkovic, Jun Lu",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Estimate coherence from stack of coregistered images")
public class CreateCoherenceImageOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "10",
            label = "Coherence Azimuth Window Size")
    private int cohWinAz = 10;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Range direction",
            defaultValue = "10",
            label = "Coherence Range Window Size")
    private int cohWinRg = 10;

    // source
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<>();

    // target
    private HashMap<String, ProductContainer> targetMap = new HashMap<>();

    private boolean isTOPSARBurstProduct = false;
    private String productName = null;
    private String productTag = null;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int numSubSwaths = 0;
    private int subSwathIndex = 0;

    private static final int ORBIT_DEGREE = 3; // hardcoded

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
            productName = "coherence";
            productTag = "coh";

            checkUserInput();

            constructSourceMetadata();

            constructTargetMetadata();

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void checkUserInput() {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCoregisteredStack();
            isTOPSARBurstProduct = validator.isTOPSARBurstProduct();

            if (isTOPSARBurstProduct) {
                su = new Sentinel1Utils(sourceProduct);
                subSwath = su.getSubSwath();
                numSubSwaths = su.getNumOfSubSwath();
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product

                final String topsarTag = CreateInterferogramOp.getTOPSARTag(sourceProduct);
                productTag = productTag + "_" + topsarTag;
            }
        } catch (Exception e) {
            throw new OperatorException(e);
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

        // plug sourceSlave metadata into slaveMap
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot);
        if(slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        MetadataElement[] slaveRoot = slaveElem.getElements();
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

                product.targetBandName_I = productTag + "_" + master.date + "_" + slave.date;

                // put ifg-product bands into map
                targetMap.put(productName, product);
            }
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(productName,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {
            final Band cohBand = targetProduct.addBand(targetMap.get(key).targetBandName_I, ProductData.TYPE_FLOAT32);
            cohBand.setUnit(Unit.COHERENCE);
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

        if (isTOPSARBurstProduct) {
            computeTileForTOPSARProduct(targetBand, targetTile, pm);
        } else {
            computeTileForNormalProduct(targetBand, targetTile, pm);
        }
    }

    private void computeTileForNormalProduct(Band targetBand, Tile targetTile, ProgressMonitor pm) {

        try {
            final Rectangle rect = targetTile.getRectangle();
            final int x0 = rect.x - (cohWinRg - 1) / 2;
            final int y0 = rect.y - (cohWinAz - 1) / 2;
            final int w = rect.width + cohWinRg - 1;
            final int h = rect.height + cohWinAz - 1;
            rect.x = x0;
            rect.y = y0;
            rect.width = w;
            rect.height = h;

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            for (String cohKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(cohKey);

                if (targetBand.getName().equals(product.targetBandName_I)) {

                    Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rect, border);
                    Tile tileImagMaster = getSourceTile(product.sourceMaster.imagBand, rect, border);
                    final ComplexDoubleMatrix dataMaster =
                            TileUtilsDoris.pullComplexDoubleMatrix(tileRealMaster, tileImagMaster);

                    Tile tileRealSlave = getSourceTile(product.sourceSlave.realBand, rect, border);
                    Tile tileImagSlave = getSourceTile(product.sourceSlave.imagBand, rect, border);
                    final ComplexDoubleMatrix dataSlave =
                            TileUtilsDoris.pullComplexDoubleMatrix(tileRealSlave, tileImagSlave);

                    for (int i = 0; i < dataMaster.length; i++) {
                        double tmp = norm(dataMaster.get(i));
                        dataMaster.put(i, dataMaster.get(i).mul(dataSlave.get(i).conj()));
                        dataSlave.put(i, new ComplexDouble(norm(dataSlave.get(i)), tmp));
                    }

                    DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster, dataSlave, cohWinAz, cohWinRg);

                    TileUtilsDoris.pushDoubleMatrix(cohMatrix, targetTile, targetTile.getRectangle());
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void computeTileForTOPSARProduct(Band targetBand, Tile targetTile, ProgressMonitor pm) {

        try {
            final Rectangle targetRectangle = targetTile.getRectangle();
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final int txMax = tx0 + tw;
            final int tyMax = ty0 + th;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            for (int burstIndex = 0; burstIndex < subSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + subSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (tyMax <= firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

                final int ntx0 = tx0;
                final int ntw = tw;
                final int nty0 = Math.max(ty0, firstLineIdx);
                final int ntyMax = Math.min(tyMax, lastLineIdx + 1);
                final int nth = ntyMax - nty0;
                final Rectangle partialTileRectangle = new Rectangle(ntx0, nty0, ntw, nth);
                System.out.println("burst = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                computePartialTile(subSwathIndex, burstIndex, targetBand, targetTile, partialTileRectangle, pm);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex, final Band targetBand,
                                    final Tile targetTile, final Rectangle targetRectangle, ProgressMonitor pm) {

        try {
            final int x0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int y0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int w = targetRectangle.width + cohWinRg - 1;
            final int h = targetRectangle.height + cohWinAz - 1;
            final Rectangle rect = new Rectangle(x0, y0, w, h);

            final int yMin = burstIndex * subSwath[subSwathIndex - 1].linesPerBurst;
            final int yMax = yMin + subSwath[subSwathIndex - 1].linesPerBurst - 1;

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            for (String cohKey : targetMap.keySet()) {
                final ProductContainer product = targetMap.get(cohKey);

                if (targetBand.getName().equals(product.targetBandName_I)) {

                    Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rect, border);
                    Tile tileImagMaster = getSourceTile(product.sourceMaster.imagBand, rect, border);
                    final ComplexDoubleMatrix dataMaster =
                            TileUtilsDoris.pullComplexDoubleMatrix(tileRealMaster, tileImagMaster);

                    Tile tileRealSlave = getSourceTile(product.sourceSlave.realBand, rect, border);
                    Tile tileImagSlave = getSourceTile(product.sourceSlave.imagBand, rect, border);
                    final ComplexDoubleMatrix dataSlave =
                            TileUtilsDoris.pullComplexDoubleMatrix(tileRealSlave, tileImagSlave);

                    for (int r = 0; r < dataMaster.rows; r++) {
                        final int y = y0 + r;
                        for (int c = 0; c < dataMaster.columns; c++) {
                            double tmp = norm(dataMaster.get(r, c));
                            if (y < yMin || y > yMax) {
                                dataMaster.put(r, c, 0.0);
                            } else {
                                dataMaster.put(r, c, dataMaster.get(r, c).mul(dataSlave.get(r,c).conj()));
                            }
                            dataSlave.put(r, c, new ComplexDouble(norm(dataSlave.get(r, c)), tmp));
                        }
                    }

                    DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster, dataSlave, cohWinAz, cohWinRg);

                    TileUtilsDoris.pushDoubleMatrix(cohMatrix, targetTile, targetRectangle);
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private static double norm(final ComplexDouble number) {
        return number.real()*number.real() + number.imag()*number.imag();
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
            super(CreateCoherenceImageOp.class);
        }
    }
}
