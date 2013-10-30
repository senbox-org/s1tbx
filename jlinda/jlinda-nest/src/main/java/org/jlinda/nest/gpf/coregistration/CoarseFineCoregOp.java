/*
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
package org.jlinda.nest.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.toolviews.placemark.PlacemarkNameFactory;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.*;
import org.esa.nest.util.MemUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.Window;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.nest.dat.coregistration.CoarseFineCoregOpUI;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "CoarseFine-Coregistration",
        category = "InSAR\\InSAR Coregistration",
        authors = "Petar Marinkovic (with contributions of Jun Lu, Luis Veci)",
        copyright = "PPO.labs and European Space Agency",
        description = "Performs coarse and found coregistration with correlation optimization method")
public class CoarseFineCoregOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Number of Correlation Windows", interval = "(10, *)", defaultValue = "200",
            label = "Number of GCPs")
    private int numWindows = 200;

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128", label = "Coarse Registration Window Width")
    private String coarseRegistrationWindowWidth = "128";
    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128", label = "Coarse Registration Window Height")
    private String coarseRegistrationWindowHeight = "128";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64", "128", "256"}, defaultValue = "2", label = "Search Window Accuracy in Azimuth Direction")
    private String coarseRegistrationWindowAccAzimuth = "32";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64", "128", "256"}, defaultValue = "2", label = "Search Window Accuracy in Range Direction")
    private String coarseRegistrationWindowAccRange = "8";

    // ==================== input parameters used for fine co-registration ==================
//    @Parameter(defaultValue="true", label="Apply Fine Registration")
    private boolean applyFineRegistration = true;

    public final static String MAG_FFT = "Frequency Domain";
    public final static String MAG_SPACE = "Space Domain";
    public final static String MAG_OVERSAMPLE = "Frequency Domain with Oversampling";

    @Parameter(valueSet = {MAG_FFT, MAG_SPACE, MAG_OVERSAMPLE}, defaultValue = MAG_FFT, label = "Correlation Optimization Method")
    private String fineMethod = MAG_FFT;

    @Parameter(valueSet = {"4", "8", "16", "32", "64", "128"}, defaultValue = "32", label = "Fine Registration Window Width")
    private String fineRegistrationWindowWidth = "32";
    @Parameter(valueSet = {"4", "8", "16", "32", "64", "128"}, defaultValue = "32", label = "Fine Registration Window Height")
    private String fineRegistrationWindowHeight = "32";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Search Window Accuracy in Azimuth Direction")
    private String fineRegistrationWindowAccAzimuth = "16";
    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Search Window Accuracy in Range Direction")
    private String fineRegistrationWindowAccRange = "16";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Window oversampling factor")
    private String fineRegistrationOversampling = "16";

    @Parameter(description = "The coherence threshold", interval = "(0, *)", defaultValue = "0.4",
            label = "Coherence Threshold")
    private double coherenceThreshold = 0.4;


    private boolean useAllPolarimetricBands = false;

    // =========================================================================================
    @Parameter(defaultValue = "false", label = "Estimate Coarse Offset")
    private boolean computeOffset = false;
    @Parameter(defaultValue = "false", label = "Test GCPs are on land")
    private boolean onlyGCPsOnLand = false;


    private Band masterBand1 = null;
    private Band masterBand2 = null;

    private CorrelationWindow coarseWin;
    private CorrelationWindow fineWin;

    private boolean complexCoregistration = true;

    private ProductNodeGroup<Placemark> masterGcpGroup = null;

    private int sourceImageWidth;
    private int sourceImageHeight;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Boolean> gcpsComputedMap = new HashMap<>(10);
    private Band primarySlaveBand = null;    // the slave band to process
    private boolean gcpsCalculated = false;
    private boolean collocatedStack = false;

    private ElevationModel dem = null;

    // Constants
    private static final int EXTRA_BORDER = 20; // work with slightly smaller search space, used in gcp validation


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CoarseFineCoregOp() {
    }

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

            // parameters: Image
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            // parameters: Coarse
            coarseWin = new CorrelationWindow(
                    Integer.parseInt(coarseRegistrationWindowWidth),
                    Integer.parseInt(coarseRegistrationWindowHeight),
                    Integer.parseInt(coarseRegistrationWindowAccAzimuth),
                    Integer.parseInt(coarseRegistrationWindowAccRange),
                    1);

            // parameters: Fine
            fineWin = new CorrelationWindow(
                    Integer.parseInt(fineRegistrationWindowWidth),
                    Integer.parseInt(fineRegistrationWindowHeight),
                    Integer.parseInt(fineRegistrationWindowAccAzimuth),
                    Integer.parseInt(fineRegistrationWindowAccRange),
                    Integer.parseInt(fineRegistrationOversampling));

            masterBand1 = sourceProduct.getBandAt(0);
            if (masterBand1.getUnit() != null && masterBand1.getUnit().equals(Unit.REAL) && sourceProduct.getNumBands() > 1) {
                masterBand2 = sourceProduct.getBandAt(1);
                complexCoregistration = true;
            }

            getCollocatedStackFlag();
            createTargetProduct();

            masterGcpGroup = sourceProduct.getGcpGroup(masterBand1);
            if (masterGcpGroup.getNodeCount() <= 0) {
                addGCPGrid(sourceImageWidth, sourceImageHeight, numWindows, masterGcpGroup,
                        targetProduct.getGeoCoding());
            }

            OperatorUtils.copyGCPsToTarget(masterGcpGroup, targetProduct.getGcpGroup(targetProduct.getBandAt(0)),
                    targetProduct.getGeoCoding());

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    // Construct gcp grid: revers diagonal distribution
    private void addGCPGrid(final int width, final int height, final int numPins,
                            final ProductNodeGroup<Placemark> group,
                            final GeoCoding targetGeoCoding) {

        // Construct overlap window - work in absolute master geometry, this should take care for borders
        final int l0 = (int) (0 + 0.5 + coarseWin.height + coarseWin.accY + EXTRA_BORDER);
        final int lN = (int) (height - 0.5 + coarseWin.height + coarseWin.accY + EXTRA_BORDER);
        final int p0 = (int) (0 + 0.5 + coarseWin.width + coarseWin.accX + EXTRA_BORDER);
        final int pN = (int) (width - 0.5 + coarseWin.width + coarseWin.accX + EXTRA_BORDER);

        final Window overlapWindow = new Window(l0, lN, p0, pN);

        int[][] pinCenters = MathUtils.distributePoints(numPins, overlapWindow);

        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        group.removeAll();
        int pinNumber = group.getNodeCount() + 1;

        for (int i = 0; i < numPins; i++) {

            final String name = PlacemarkNameFactory.createName(gcpDescriptor, pinNumber);
            final String label = PlacemarkNameFactory.createLabel(gcpDescriptor, pinNumber, true);

            final Placemark newPin = Placemark.createPointPlacemark(gcpDescriptor,
                    name, label, "",
                    new PixelPos(pinCenters[i][1], pinCenters[i][0]), null, targetGeoCoding);
            group.add(newPin);
            ++pinNumber;
        }
    }

    private void getCollocatedStackFlag() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        MetadataAttribute attr = absRoot.getAttribute("collocated_stack");
        if (attr == null) {
            collocatedStack = false;
        } else {
            collocatedStack = true;
            absRoot.removeAttribute(attr);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);

        final int numSrcBands = sourceProduct.getNumBands();
        boolean oneSlaveProcessed = false;          // all other use setSourceImage
        for (int i = 0; i < numSrcBands; ++i) {
            final Band srcBand = sourceProduct.getBandAt(i);
            final Band targetBand = targetProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            sourceRasterMap.put(targetBand, srcBand);
            gcpsComputedMap.put(srcBand, false);

            if (srcBand == masterBand1 || srcBand == masterBand2 || oneSlaveProcessed ||
                    StringUtils.contains(masterBandNames, srcBand.getName())) {
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                final String unit = srcBand.getUnit();
                if (oneSlaveProcessed == false && unit != null && !unit.contains(Unit.IMAGINARY)) {
                    oneSlaveProcessed = true;
                    primarySlaveBand = srcBand;
                    final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
                    AbstractMetadata.addAbstractedAttribute(absRoot, "processed_slave", ProductData.TYPE_ASCII, "", "");
                    absRoot.setAttributeString("processed_slave", primarySlaveBand.getName());
                }
            }

            if (srcBand.getUnit() != null && srcBand.getUnit().equals(Unit.REAL)) {
                if (i + 1 < numSrcBands)
                    complexSrcMap.put(srcBand, sourceProduct.getBandAt(i + 1));
            }
        }
    }

    private synchronized void createDEM() {
        if (dem != null) return;

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor("SRTM 3Sec");
        if (demDescriptor.isInstallingDem()) {
            throw new OperatorException("The DEM is currently being installed.");
        }
        dem = demDescriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.NEAREST_NEIGHBOUR_NAME));
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
            //int x0 = targetRectangle.x;
            //int y0 = targetRectangle.y;
            //int w = targetRectangle.width;
            //int h = targetRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            if (onlyGCPsOnLand && dem == null) {
                createDEM();
            }

            final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);

            // select only one band per slave product
            final Map<String, Band> singleSlvBandMap = new HashMap<>();
            final Map<Band, Band> bandList = new HashMap<>();

            // -----
            // Band Management
            for (Band targetBand : targetProduct.getBands()) {

                // get slave band
                final Band slaveBand = sourceRasterMap.get(targetBand);

                // check if gcp's for this band are calculated
                if (gcpsCalculated && slaveBand == primarySlaveBand) {
                    bandList.put(targetBand, slaveBand);
                    break;
                }

                // check if this is master band
                if (slaveBand == masterBand1 || slaveBand == masterBand2 ||
                        StringUtils.contains(masterBandNames, slaveBand.getName()))
                    continue;

                // clean up in case of polarimetric bands
                if (!useAllPolarimetricBands) {
                    final String mstPol = OperatorUtils.getPolarizationFromBandName(masterBand1.getName());
                    final String slvProductName = StackUtils.getSlaveProductName(targetProduct, targetBand, mstPol);
                    if (slvProductName == null || singleSlvBandMap.get(slvProductName) != null) {
                        continue;
                    }
                    singleSlvBandMap.put(slvProductName, targetBand);
                }
                final String unit = slaveBand.getUnit();
                if (unit != null && (unit.contains(Unit.IMAGINARY) || unit.contains(Unit.BIT)))
                    continue;
                bandList.put(targetBand, slaveBand);
            }

            // -----
            // Offset computation

            int bandCnt = 0;
            Band firstTargetBand = null;
            for (Band targetBand : bandList.keySet()) {
                ++bandCnt;
                final Band slaveBand = bandList.get(targetBand);

                if (collocatedStack || !collocatedStack && bandCnt == 1) {
                    final String bandCountStr = bandCnt + " of " + bandList.size();
                    computeSlaveGCPs(slaveBand, complexSrcMap.get(slaveBand), targetBand, bandCountStr);
                    if (bandCnt == 1) {
                        firstTargetBand = targetBand;
                    }
                } else {
                    copyFirstTargetBandGCPs(firstTargetBand, targetBand);
                }

                // copy slave data to target
                final Tile targetTile = targetTileMap.get(targetBand);
                if (targetTile != null) {
                    targetTile.setRawSamples(getSourceTile(slaveBand, targetRectangle).getRawSamples());
                }
            }
            setGCPsCalculated();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void setGCPsCalculated() {
        gcpsCalculated = true;
    }

    /**
     * Compute slave GCPs for the given tile.
     *
     * @param slaveBand1  the input band
     * @param slaveBand2 for complex
     * @param targetBand the output band
     */
    private synchronized void computeSlaveGCPs(final Band slaveBand1, final Band slaveBand2, final Band targetBand,
                                               final String bandCountStr) throws OperatorException {

        if (gcpsComputedMap.get(slaveBand1))
            return;
        try {

            final ProductNodeGroup<Placemark> targetGCPGroup = targetProduct.getGcpGroup(targetBand);
            final GeoCoding tgtGeoCoding = targetProduct.getGeoCoding();

            final ThreadManager threadManager = new ThreadManager();

            final int numberOfMasterGCPs = masterGcpGroup.getNodeCount();
            final StatusProgressMonitor status = new StatusProgressMonitor(numberOfMasterGCPs,
                    "Cross Correlating " + bandCountStr + ' ' + slaveBand1.getName() + "... ");

            for (int i = 0; i < numberOfMasterGCPs; ++i) {

                checkForCancellation();

                final Placemark mPin = masterGcpGroup.get(i);

                if (checkMasterGCPValidity(mPin)) {

                    final GeoPos mGCPGeoPos = mPin.getGeoPos();
                    final PixelPos mGCPPixelPos = mPin.getPixelPos();
                    final PixelPos sGCPPixelPos = new PixelPos(mPin.getPixelPos().x, mPin.getPixelPos().y);
                    if (!checkSlaveGCPValidity(sGCPPixelPos)) {
                        System.out.println("GCP(" + i + ") is outside slave image.");
                        continue;
                    }

                    final Thread worker = new Thread() {

                        @Override
                        public void run() {

                            System.out.println("Running " + mPin.getName());
                            boolean getSlaveGCP = getCoarseOffsets(slaveBand1, slaveBand2, mGCPPixelPos, sGCPPixelPos);

                            if (getSlaveGCP) {
                                getSlaveGCP = getFineOffsets(slaveBand1, slaveBand2, mGCPPixelPos, sGCPPixelPos);
                            }

                            if (getSlaveGCP) {

                                final Placemark sPin = Placemark.createPointPlacemark(
                                        GcpDescriptor.getInstance(),
                                        mPin.getName(),
                                        mPin.getLabel(),
                                        mPin.getDescription(),
                                        sGCPPixelPos,
                                        mGCPGeoPos,
                                        tgtGeoCoding);

                                addPlacemark(sPin);

                            }
                        }

                        private synchronized void addPlacemark(final Placemark pin) {
                            targetGCPGroup.add(pin);
                        }

                    };

                    threadManager.add(worker);
                }
                status.worked(i);
            }

            threadManager.finish();

            gcpsComputedMap.put(slaveBand1, true);

            MemUtils.tileCacheFreeOldTiles();

            status.done();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " computeSlaveGCPs ", e);
        }
    }


    /**
     * Copy GCPs of the first target band to current target band.
     *
     * @param firstTargetBand First target band.
     * @param targetBand      Current target band.
     */
    private void copyFirstTargetBandGCPs(final Band firstTargetBand, final Band targetBand) {

        final ProductNodeGroup<Placemark> firstTargetBandGcpGroup = targetProduct.getGcpGroup(firstTargetBand);
        final ProductNodeGroup<Placemark> currentTargetBandGCPGroup = targetProduct.getGcpGroup(targetBand);
        final int numberOfGCPs = firstTargetBandGcpGroup.getNodeCount();
        for (int i = 0; i < numberOfGCPs; ++i) {
            currentTargetBandGCPGroup.add(firstTargetBandGcpGroup.get(i));
        }
    }

    /**
     * Check if a given master GCP is within the given tile and the GCP imagette is within the image.
     *
     * @param mPin The GCP position.
     * @return flag Return true if the GCP is within the given tile and the GCP imagette is within the image,
     *         false otherwise.
     */
    private boolean checkMasterGCPValidity(final Placemark mPin) throws Exception {

        // Note: this is redundant check, still doing it

        final PixelPos pixelPos = mPin.getPixelPos();

        if (onlyGCPsOnLand) {
            double alt = dem.getElevation(mPin.getGeoPos());
            if (alt == dem.getDescriptor().getNoDataValue())
                return false;
        }

        return (pixelPos.x - coarseWin.halfWidth + 1 >= 0 && pixelPos.x + coarseWin.halfWidth <= sourceImageWidth - 1) &&
                (pixelPos.y - coarseWin.halfWidth + 1 >= 0 && pixelPos.y + coarseWin.halfHeight <= sourceImageHeight - 1);
    }

    /**
     * Check if a given slave GCP imagette is within the image.
     *
     * @param pixelPos The GCP pixel position.
     * @return flag Return true if the GCP is within the image, false otherwise.
     */
    private boolean checkSlaveGCPValidity(final PixelPos pixelPos) {

        return (pixelPos.x - coarseWin.halfWidth + 1 >= 0 && pixelPos.x + coarseWin.halfWidth <= sourceImageWidth - 1) &&
                (pixelPos.y - coarseWin.halfWidth + 1 >= 0 && pixelPos.y + coarseWin.halfHeight <= sourceImageHeight - 1);
    }

    private boolean getCoarseOffsets(final Band slaveBand1, final Band slaveBand2,
                                     final PixelPos mGCPPixelPos,
                                     final PixelPos sGCPPixelPos) {

        try {

            // get data
            final ComplexDoubleMatrix mI = getComplexDoubleMatrix(masterBand1, masterBand2, mGCPPixelPos, coarseWin);
            final ComplexDoubleMatrix sI = getComplexDoubleMatrix(slaveBand1, slaveBand2, sGCPPixelPos, coarseWin);

            final double[] coarseOffset = {0, 0};

            double coherence = CoregistrationUtils.crossCorrelateFFT(coarseOffset, mI, sI, coarseWin.ovsFactor, coarseWin.accY, coarseWin.accX);

            System.out.println("Coarse sGCP = (" + coarseOffset[0] + ", " + coarseOffset[1] + "), coherence = " + coherence);


            sGCPPixelPos.x += (float) coarseOffset[1];
            sGCPPixelPos.y += (float) coarseOffset[0];

            return true;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getCoarseSlaveGCPPosition ", e);
        }
        return false;
    }

    private boolean getFineOffsets(final Band slaveBand1, final Band slaveBand2,
                                   final PixelPos mGCPPixelPos,
                                   final PixelPos sGCPPixelPos) {
        try {
            System.out.println("mGCP = (" + mGCPPixelPos.x + ", " + mGCPPixelPos.y + ")");
            System.out.println("Initial sGCP = (" + sGCPPixelPos.x + ", " + sGCPPixelPos.y + ")");


            ComplexDoubleMatrix mI = getComplexDoubleMatrix(masterBand1, masterBand2, mGCPPixelPos, fineWin);
            ComplexDoubleMatrix sI = getComplexDoubleMatrix(slaveBand1, slaveBand2, mGCPPixelPos, fineWin);

            final double[] fineOffset = {sGCPPixelPos.x, sGCPPixelPos.y};

            final double coherence = CoregistrationUtils.crossCorrelateFFT(fineOffset, mI, sI, fineWin.ovsFactor, fineWin.accY, fineWin.accX);

            System.out.println("Final sGCP = (" + fineOffset[0] + ", " + fineOffset[1] + "), coherence = " + coherence);

            if (coherence < coherenceThreshold) {
                //System.out.println("Invalid GCP");
                return false;
            } else {
                sGCPPixelPos.x += (float) fineOffset[0];
                sGCPPixelPos.y += (float) fineOffset[1];
                //System.out.println("Valid GCP");
                return true;
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getFineSlaveGCPPosition ", e);
        }
        return false;
    }

    private ComplexDoubleMatrix getComplexDoubleMatrix(Band band1, Band band2, PixelPos pixelPos, CorrelationWindow corrWindow) {

        Rectangle rectangle = corrWindow.defineRectangleMask(pixelPos);
        Tile tileReal = getSourceTile(band1, rectangle);
        Tile tileImag = getSourceTile(band2, rectangle);
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private static class CorrelationWindow {

        final int height;
        final int width;
        final int halfWidth;
        final int halfHeight;
        final int accY;
        final int accX;

        final int ovsFactor;

        private CorrelationWindow(int winWidth, int winHeight, int accX, int accY, int ovsFactor) {
            this.accX = accX;
            this.accY = accY;
            this.width = winWidth;
            this.height = winHeight;

            this.halfWidth = winWidth / 2;
            this.halfHeight = winHeight / 2;

            this.ovsFactor = ovsFactor;
        }

        public Window defineWindowMask(int x, int y) {
            int l0 = y - halfHeight;
            int lN = y + halfHeight - 1;
            int p0 = x - halfWidth;
            int pN = x + halfWidth - 1;

            return new Window(l0, lN, p0, pN);
        }

        public Window defineWindowMask(PixelPos pos) {
            int l0 = (int) (pos.y - halfHeight);
            int lN = (int) (pos.y + halfHeight - 1);
            int p0 = (int) (pos.x - halfWidth);
            int pN = (int) (pos.x + halfWidth - 1);

            return new Window(l0, lN, p0, pN);
        }

        public Rectangle defineRectangleMask(int x, int y) {
            Window temp = defineWindowMask(x, y);
            return new Rectangle((int) temp.linelo, (int) temp.pixlo, (int) temp.lines(), (int) temp.pixels());
        }

        public Rectangle defineRectangleMask(PixelPos pos) {
            Window temp = defineWindowMask(pos);
            return new Rectangle((int) temp.linelo, (int) temp.pixlo, (int) temp.lines(), (int) temp.pixels());
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
            super(CoarseFineCoregOp.class);
            super.setOperatorUI(CoarseFineCoregOpUI.class);
        }
    }
}
