package org.esa.s1tbx.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.util.ProductUtils;
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
import java.awt.Rectangle;
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

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
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

    private void computeTileForNormalProduct(Band targetBand, Tile targetTile, ProgressMonitor pm)
            throws OperatorException {

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

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computeTileForNormalProduct2(Band targetBand, Tile targetTile, ProgressMonitor pm)
            throws OperatorException {

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

                    final Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rect, border);
                    final Tile tileImagMaster = getSourceTile(product.sourceMaster.imagBand, rect, border);

                    final Tile tileRealSlave = getSourceTile(product.sourceSlave.realBand, rect, border);
                    final Tile tileImagSlave = getSourceTile(product.sourceSlave.imagBand, rect, border);

                    final ProductData iMstDB = tileRealMaster.getDataBuffer();
                    final ProductData qMstDB = tileImagMaster.getDataBuffer();
                    final ProductData iSlvDB = tileRealSlave.getDataBuffer();
                    final ProductData qSlvDB = tileImagSlave.getDataBuffer();

                    final int numElems = iMstDB.getNumElems();
                    final double[] iMst = new double[numElems];
                    final double[] qMst = new double[numElems];
                    final double[] iSlv = new double[numElems];
                    final double[] qSlv = new double[numElems];
                    for (int i = 0; i < numElems; i++) {
                        double iM = iMstDB.getElemDoubleAt(i);
                        double qM = qMstDB.getElemDoubleAt(i);
                        double iS = iSlvDB.getElemDoubleAt(i);
                        double qS = qSlvDB.getElemDoubleAt(i);
                        double tmp = norm(iM, qM);
                        iMst[i] = iM * iS - qM * -qS;
                        qMst[i] = iM * -qS + qM * iS;

                        iSlv[i] = norm(iS, qS);
                        qSlv[i] = tmp;
                    }

                    DoubleMatrix cohMatrix = coherence(iMst, qMst, iSlv, qSlv, cohWinAz, cohWinRg,
                            tileRealMaster.getWidth(), tileRealMaster.getHeight());

                    TileUtilsDoris.pushDoubleMatrix(cohMatrix, targetTile, targetTile.getRectangle());
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computeTileForTOPSARProduct(final Band targetBand, final Tile targetTile, final ProgressMonitor pm)
            throws OperatorException {

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
                //System.out.println("burst = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                computePartialTile(subSwathIndex, burstIndex, targetBand, targetTile, partialTileRectangle);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex, final Band targetBand,
                                    final Tile targetTile, final Rectangle targetRectangle) {

        try {
            final int cohx0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int cohy0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle rect = new Rectangle(cohx0, cohy0, cohw, cohh);

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
                        final int y = cohy0 + r;
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

    private static double norm(final double real, final double imag) {
        return real*real + imag*imag;
    }

    public static DoubleMatrix coherence(final double[] iMst, final double[] qMst, final double[] iSlv, final double[] qSlv,
                                         final int winL, final int winP, int w, int h) {

        final ComplexDoubleMatrix input = new ComplexDoubleMatrix(h, w);
        final ComplexDoubleMatrix norms = new ComplexDoubleMatrix(h, w);
        for (int y = 0; y < h; y++) {
            final int stride = y * w;
            for (int x = 0; x < w; x++) {
                input.put(y, x, new ComplexDouble(iMst[stride + x],
                        qMst[stride + x]));
                norms.put(y, x, new ComplexDouble(iSlv[stride + x], qSlv[stride + x]));
            }
        }

        if (input.rows != norms.rows) {
            throw new IllegalArgumentException("coherence: not the same dimensions.");
        }

        // allocate output :: account for window overlap
        final int extent_RG = input.columns;
        final int extent_AZ = input.rows - winL + 1;
        final DoubleMatrix result = new DoubleMatrix(input.rows - winL + 1, input.columns - winP + 1);

        // temp variables
        int i, j, k, l;
        ComplexDouble sum;
        ComplexDouble power;
        final int leadingZeros = (winP - 1) / 2;  // number of pixels=0 floor...
        final int trailingZeros = (winP) / 2;     // floor...

        for (j = leadingZeros; j < extent_RG - trailingZeros; j++) {

            sum = new ComplexDouble(0);
            power = new ComplexDouble(0);

            //// Compute sum over first data block ////
            int minL = j - leadingZeros;
            int maxL = minL + winP;
            for (k = 0; k < winL; k++) {
                for (l = minL; l < maxL; l++) {
                    //sum.addi(input.get(k, l));
                    //power.addi(norms.get(k, l));
                    int inI = 2 * input.index(k, l);
                    sum.set(sum.real()+input.data[inI], sum.imag()+input.data[inI+1]);
                    power.set(power.real()+norms.data[inI], power.imag()+norms.data[inI+1]);
                }
            }
            result.put(0, minL, coherenceProduct(sum, power));

            //// Compute (relatively) sum over rest of data blocks ////
            final int maxI = extent_AZ - 1;
            for (i = 0; i < maxI; i++) {
                final int iwinL = i + winL;
                for (l = minL; l < maxL; l++) {
                    //sum.addi(input.get(iwinL, l).sub(input.get(i, l)));
                    //power.addi(norms.get(iwinL, l).sub(norms.get(i, l)));

                    int inI = 2 * input.index(i, l);
                    int inWinL = 2 * input.index(iwinL, l);
                    sum.set(sum.real()+(input.data[inWinL]-input.data[inI]), sum.imag()+(input.data[inWinL+1]-input.data[inI+1]));
                    power.set(power.real()+(norms.data[inWinL]-norms.data[inI]), power.imag()+(norms.data[inWinL+1]-norms.data[inI+1]));
                }
                result.put(i + 1, j - leadingZeros, coherenceProduct(sum, power));
            }
        }
        return result;
    }

    static double coherenceProduct(final ComplexDouble sum, final ComplexDouble power) {
        final double product = power.real() * power.imag();
//        return (product > 0.0) ? Math.sqrt(Math.pow(sum.abs(),2) / product) : 0.0;
        return (product > 0.0) ? sum.abs() / Math.sqrt(product) : 0.0;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
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
