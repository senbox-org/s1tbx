/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
 *
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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.support.JAIFunctions;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.*;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.core.utils.TileUtilsDoris;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Image co-registration is fundamental for Interferometry SAR (InSAR) imaging and its applications, such as
 * DEM map generation and analysis. To obtain a high quality InSAR image, the individual complex images need
 * to be co-registered to sub-pixel accuracy. The co-registration is accomplished through an alignment of a
 * master image with a slave image.
 * <p>
 * To achieve the alignment of master and slave images, the first step is to generate a set of uniformly
 * spaced ground control points (GCPs) in the master image, along with the corresponding GCPs in the slave
 * image. These GCP pairs are used in constructing a warp distortion function, which establishes a map
 * between pixels in the slave and master images.
 * <p>
 * This operator computes the slave GCPS for given master GCPs. First the geometric information of the
 * master GCPs is used in determining the initial positions of the slave GCPs. Then a cross-correlation
 * is performed between imagettes surrounding each master GCP and its corresponding slave GCP to obtain
 * accurate slave GCP position. This step is repeated several times until the slave GCP position is
 * accurate enough.
 */

@OperatorMetadata(alias = "Cross-Correlation",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci, Petar Marinkovic",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Automatic Selection of Ground Control Points")
public class CrossCorrelationOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The number of GCPs to use in a grid", interval = "(10, *)", defaultValue = "200",
            label = "Number of GCPs")
    private int numGCPtoGenerate = 200;

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128", label = "Coarse Registration Window Width")
    private String coarseRegistrationWindowWidth = "128";
    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128", label = "Coarse Registration Window Height")
    private String coarseRegistrationWindowHeight = "128";
    @Parameter(valueSet = {"2", "4", "8", "16"}, defaultValue = "2", label = "Row Interpolation Factor")
    private String rowInterpFactor = "2";
    @Parameter(valueSet = {"2", "4", "8", "16"}, defaultValue = "2", label = "Column Interpolation Factor")
    private String columnInterpFactor = "2";
    @Parameter(description = "The maximum number of iterations", interval = "(1, 10]", defaultValue = "10",
            label = "Max Iterations")
    private int maxIteration = 10;
    @Parameter(description = "Tolerance in slave GCP validation check", interval = "(0, *)", defaultValue = "0.5",
            label = "GCP Tolerance")
    private double gcpTolerance = 0.5;

    // ==================== input parameters used for complex co-registration ==================
    @Parameter(defaultValue = "true", label = "Apply Fine Registration")
    private boolean applyFineRegistration = true;
    @Parameter(defaultValue = "true", label = "Optimize for InSAR")
    private boolean inSAROptimized = true;

    @Parameter(valueSet = {"8", "16", "32", "64", "128", "256", "512"}, defaultValue = "32", label = "Fine Registration Window Width")
    private String fineRegistrationWindowWidth = "32";
    @Parameter(valueSet = {"8", "16", "32", "64", "128", "256", "512"}, defaultValue = "32", label = "Fine Registration Window Height")
    private String fineRegistrationWindowHeight = "32";
    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Search Window Accuracy in Azimuth Direction")
    private String fineRegistrationWindowAccAzimuth = "16";
    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Search Window Accuracy in Range Direction")
    private String fineRegistrationWindowAccRange = "16";
    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16", label = "Window oversampling factor")
    private String fineRegistrationOversampling = "16";

    @Parameter(description = "The coherence window size", interval = "(1, 16]", defaultValue = "3",
            label = "Coherence Window Size")
    private int coherenceWindowSize = 3;
    @Parameter(description = "The coherence threshold", interval = "(0, *)", defaultValue = "0.6",
            label = "Coherence Threshold")
    private double coherenceThreshold = 0.6;
    @Parameter(description = "Use sliding window for coherence calculation", defaultValue = "false",
            label = "Use coherence sliding window")
    private Boolean useSlidingWindow = false;

    private boolean useAllPolarimetricBands = false;

    //    @Parameter(description = "The coherence function tolerance", interval = "(0, *)", defaultValue = "1.e-6",
    //                label="Coherence Function Tolerance")
    private static final double coherenceFuncToler = 1.e-5;
    //    @Parameter(description = "The coherence value tolerance", interval = "(0, *)", defaultValue = "1.e-3",
    //                label="Coherence Value Tolerance")
    private static final double coherenceValueToler = 1.e-2;
    // =========================================================================================
    @Parameter(defaultValue = "false", label = "Estimate Coarse Offset")
    private boolean computeOffset = false;
    @Parameter(defaultValue = "false", label = "Test GCPs are on land")
    private boolean onlyGCPsOnLand = false;

    private Band masterBand1;
    private Band masterBand2;
    private boolean complexCoregistration;
    private ProductNodeGroup<Placemark> masterGcpGroup;
    private String[] masterBandNames = null;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int cWindowWidth = 0; // row dimension for master and slave imagette for cross correlation, must be power of 2
    private int cWindowHeight = 0; // column dimension for master and slave imagette for cross correlation, must be power of 2
    private int rowUpSamplingFactor = 0; // cross correlation interpolation factor in row direction, must be power of 2
    private int colUpSamplingFactor = 0; // cross correlation interpolation factor in column direction, must be power of 2
    private int cHalfWindowWidth;
    private int cHalfWindowHeight;

    // parameters used for complex co-registration
    private int fWindowWidth = 0;  // row dimension for master and slave imagette for computing coherence, must be power of 2
    private int fWindowHeight = 0; // column dimension for master and slave imagette for computing coherence, must be power of 2

    private final static double MaxInvalidPixelPercentage = 0.66; // maximum percentage of invalid pixels allowed in xcorrelation

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Boolean> gcpsComputedMap = new HashMap<>(10);
    private Band primarySlaveBand = null;    // the slave band to process
    private boolean collocatedStack = false;

    private ElevationModel dem = null;
    private CorrelationWindow fineWin;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CrossCorrelationOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {
            getCollocatedStackFlag();

            cWindowWidth = Integer.parseInt(coarseRegistrationWindowWidth);
            cWindowHeight = Integer.parseInt(coarseRegistrationWindowHeight);
            cHalfWindowWidth = cWindowWidth / 2;
            cHalfWindowHeight = cWindowHeight / 2;

            rowUpSamplingFactor = Integer.parseInt(rowInterpFactor);
            colUpSamplingFactor = Integer.parseInt(columnInterpFactor);

            getMasterBands();

            // parameters: Fine
            if(applyFineRegistration) {
                if (complexCoregistration) {
                    fWindowWidth = Integer.parseInt(fineRegistrationWindowWidth);
                    fWindowHeight = Integer.parseInt(fineRegistrationWindowHeight);
                }

                if (inSAROptimized) {
                    if (fineRegistrationOversampling == null)
                        fineRegistrationOversampling = "2";

                    fineWin = new CorrelationWindow(
                            Integer.parseInt(fineRegistrationWindowWidth),
                            Integer.parseInt(fineRegistrationWindowHeight),
                            Integer.parseInt(fineRegistrationWindowAccRange),
                            Integer.parseInt(fineRegistrationWindowAccAzimuth),
                            Integer.parseInt(fineRegistrationOversampling));
                }
            }

            final double achievableAccuracy = 1.0 / (double) Math.max(rowUpSamplingFactor, colUpSamplingFactor);
            if (gcpTolerance < achievableAccuracy) {
                throw new OperatorException("GCP Tolerance is below the achievable accuracy with current interpolation factors of " +
                                                    achievableAccuracy + '.');
            }

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

            GCPManager.instance().removeAllGcpGroups(); // need this line, otherwise cached data from previous run is used
            masterGcpGroup = GCPManager.instance().getGcpGroup(masterBand1);
            if (masterGcpGroup.getNodeCount() <= 0) {
                addGCPGrid(sourceImageWidth, sourceImageHeight, numGCPtoGenerate, masterGcpGroup,
                           targetProduct.getSceneGeoCoding());
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterBands() {
        String mstBandName = sourceProduct.getBandAt(0).getName();

        // find co-pol bands
        masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            final String mstPol = OperatorUtils.getPolarizationFromBandName(bandName);
            if (mstPol != null && (mstPol.equals("hh") || mstPol.equals("vv"))) {
                mstBandName = bandName;
                break;
            }
        }
        masterBand1 = sourceProduct.getBand(mstBandName);
        if(masterBand1 == null) {
            mstBandName = sourceProduct.getBandAt(0).getName();
            masterBand1 = sourceProduct.getBand(mstBandName);
        }
        if (masterBand1.getUnit() != null && masterBand1.getUnit().equals(Unit.REAL)) {
            int mstIdx = sourceProduct.getBandIndex(mstBandName);
            if (sourceProduct.getNumBands() > mstIdx + 1) {
                masterBand2 = sourceProduct.getBandAt(mstIdx + 1);
                complexCoregistration = true;
            }
        }
    }

    private static void addGCPGrid(final int width, final int height, final int numPins,
                                   final ProductNodeGroup<Placemark> group,
                                   final GeoCoding targetGeoCoding) {

        final double ratio = width / (double) height;
        final double n = Math.sqrt(numPins / ratio);
        final double m = ratio * n;
        final double spacingX = width / m;
        final double spacingY = height / n;
        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();

        group.removeAll();
        int pinNumber = group.getNodeCount() + 1;

        for (double y = spacingY / 2f; y < height; y += spacingY) {

            for (double x = spacingX / 2f; x < width; x += spacingX) {

                final String name = PlacemarkNameFactory.createName(gcpDescriptor, pinNumber);
                final String label = PlacemarkNameFactory.createLabel(gcpDescriptor, pinNumber, true);

                final Placemark newPin = Placemark.createPointPlacemark(gcpDescriptor,
                                                                        name, label, "",
                                                                        new PixelPos((int) x, (int) y), null,
                                                                        targetGeoCoding);
                group.add(newPin);
                ++pinNumber;
            }
        }
    }

    private void getCollocatedStackFlag() {
        collocatedStack = false;
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (absRoot != null) {
            MetadataAttribute attr = absRoot.getAttribute("collocated_stack");
            if (attr != null) {
                collocatedStack = true;
                absRoot.removeAttribute(attr);
            }
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

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final int numSrcBands = sourceProduct.getNumBands();

        //find slave band matching master pol
        Band slvBand1 = null, slvBand2 = null;
        final String mstPol = OperatorUtils.getPolarizationFromBandName(masterBand1.getName());
        for (Band slvBand : sourceProduct.getBands()) {
            if (!StringUtils.contains(masterBandNames, slvBand.getName()) && slvBand != masterBand1) {
                final String slvPol = OperatorUtils.getPolarizationFromBandName(slvBand.getName());
                if (mstPol == null || slvPol == null || mstPol.equals(slvPol)) {
                    final String unit = slvBand.getUnit();
                    if (unit != null && !unit.contains(Unit.IMAGINARY)) {
                        slvBand1 = slvBand;
                        break;
                    } else if (unit == null) {
                        // Assume that the image is real-valued if no unit is set
                        slvBand1 = slvBand;
                    }
                }
            }
        }

        if (slvBand1 == null) {
            //get any polarization
            for (Band slvBand : sourceProduct.getBands()) {
                if (!StringUtils.contains(masterBandNames, slvBand.getName()) && slvBand != masterBand1) {
                    final String unit = slvBand.getUnit();
                    if (unit != null && !unit.contains(Unit.IMAGINARY)) {
                        slvBand1 = slvBand;
                        break;
                    } else if (unit == null) {
                        // Assume that the image is real-valued if no unit is set
                        slvBand1 = slvBand;
                    }
                }
            }
        }

        boolean oneSlaveProcessed = false;          // all other use setSourceImage
        for (int i = 0; i < numSrcBands; ++i) {
            final Band srcBand = sourceProduct.getBandAt(i);
            final Band targetBand = targetProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            sourceRasterMap.put(targetBand, srcBand);
            gcpsComputedMap.put(srcBand, false);

            if (srcBand == masterBand1 || srcBand == masterBand2 || oneSlaveProcessed ||
                    (srcBand != slvBand1 && slvBand1 != null) ||
                    StringUtils.contains(masterBandNames, srcBand.getName())) {
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                final String unit = srcBand.getUnit();
                if (!oneSlaveProcessed && (unit == null || !unit.contains(Unit.IMAGINARY))) {
                    oneSlaveProcessed = true;
                    primarySlaveBand = srcBand;
                    final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
                    AbstractMetadata.addAbstractedAttribute(absRoot, "processed_slave", ProductData.TYPE_ASCII, "", "");
                    absRoot.setAttributeString("processed_slave", primarySlaveBand.getName());
                }
            }

            if (complexCoregistration) {
                if (srcBand.getUnit() != null && srcBand.getUnit().equals(Unit.REAL)) {
                    if (i + 1 < numSrcBands)
                        complexSrcMap.put(srcBand, sourceProduct.getBandAt(i + 1));
                }
            }
        }
    }

    private synchronized void createDEM() {
        if (dem != null) return;

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor("SRTM 3Sec");
        dem = demDescriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.NEAREST_NEIGHBOUR_NAME));
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
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

            // select only one band per slave product
            final Map<String, Band> singleSlvBandMap = new HashMap<>();

            final Map<Band, Band> bandList = new HashMap<>();
            for (Band targetBand : targetProduct.getBands()) {

                final Band slaveBand = sourceRasterMap.get(targetBand);
                if (gcpsComputedMap.get(slaveBand)) {
                    bandList.put(targetBand, primarySlaveBand);
                    break;
                }

                if (slaveBand == masterBand1 || slaveBand == masterBand2 ||
                        StringUtils.contains(masterBandNames, slaveBand.getName())) {
                    continue;
                }

                if (collocatedStack && !useAllPolarimetricBands) {
                    final String mstPol = OperatorUtils.getPolarizationFromBandName(masterBand1.getName());
                    final String slvProductName = StackUtils.getSlaveProductName(targetProduct, targetBand, mstPol);
                    if (slvProductName == null || singleSlvBandMap.get(slvProductName) != null) {
                        continue;
                    }
                    singleSlvBandMap.put(slvProductName, targetBand);
                }

                final String unit = slaveBand.getUnit();
                if (unit != null && (unit.contains(Unit.IMAGINARY) || unit.contains(Unit.BIT) ||
                        (complexCoregistration && unit.contains(Unit.INTENSITY)))) {
                    continue;
                }
                bandList.put(targetBand, slaveBand);
            }

            int bandCnt = 0;
            Band firstTargetBand = null;
            for (Band targetBand : bandList.keySet()) {
                ++bandCnt;
                final Band slaveBand = bandList.get(targetBand);

                if (collocatedStack || !collocatedStack && bandCnt == 1) {
                    final String bandCountStr = bandCnt + " of " + bandList.size();
                    if (complexCoregistration) {
                        computeSlaveGCPs(slaveBand, complexSrcMap.get(slaveBand), targetBand, bandCountStr);
                    } else {
                        computeSlaveGCPs(slaveBand, null, targetBand, bandCountStr);
                    }

                    if (bandCnt == 1) {
                        firstTargetBand = targetBand;
                    }
                } else {
                    copyFirstTargetBandGCPs(firstTargetBand, targetBand);
                }

                // copy slave data to target
                if (slaveBand == primarySlaveBand) {
                    final Tile targetTile = targetTileMap.get(targetBand);
                    if (targetTile != null) {
                        targetTile.setRawSamples(getSourceTile(slaveBand, targetRectangle).getRawSamples());
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute slave GCPs for the given tile.
     *
     * @param slaveBand1 the input band
     * @param slaveBand2 for complex
     * @param targetBand the output band
     */
    private synchronized void computeSlaveGCPs(final Band slaveBand1, final Band slaveBand2, final Band targetBand,
                                               final String bandCountStr) throws OperatorException {

        if (gcpsComputedMap.get(slaveBand1)) {
            return;
        }

        try {

            final ProductNodeGroup<Placemark> targetGCPGroup = GCPManager.instance().getGcpGroup(targetBand);
            final GeoCoding tgtGeoCoding = targetProduct.getSceneGeoCoding();

            final int[] offset = new int[2]; // 0-x, 1-y
            if (computeOffset) {
                determiningImageOffset(slaveBand1, slaveBand2, offset);
            }

            final ThreadExecutor executor = new ThreadExecutor();

            //final ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();
            //timeMonitor.start();

            final int numberOfMasterGCPs = masterGcpGroup.getNodeCount();
            final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
            status.beginTask("Cross Correlating " + bandCountStr + ' ' + slaveBand1.getName() + "... ", numberOfMasterGCPs);

            for (int i = 0; i < numberOfMasterGCPs; ++i) {
                checkForCancellation();

                final Placemark mPin = masterGcpGroup.get(i);

                if (checkMasterGCPValidity(mPin)) {

                    final GeoPos mGCPGeoPos = mPin.getGeoPos();
                    final PixelPos mGCPPixelPos = mPin.getPixelPos();
                    final PixelPos sGCPPixelPos = new PixelPos(mPin.getPixelPos().x + offset[0],
                                                               mPin.getPixelPos().y + offset[1]);
                    if (!checkSlaveGCPValidity(sGCPPixelPos)) {
                        //System.out.println("GCP(" + i + ") is outside slave image.");
                        continue;
                    }

                    final ThreadRunnable worker = new ThreadRunnable() {

                        @Override
                        public void process() {
                            //System.out.println("Running "+mPin.getName());
                            boolean getSlaveGCP = getCoarseSlaveGCPPosition(slaveBand1, slaveBand2, mGCPPixelPos, sGCPPixelPos);

                            if (getSlaveGCP && complexCoregistration && applyFineRegistration) {
                                if (inSAROptimized) {
                                    getSlaveGCP = getFineOffsets(slaveBand1, slaveBand2, mGCPPixelPos, sGCPPixelPos);
                                } else {
                                    getSlaveGCP = getFineSlaveGCPPosition(slaveBand1, slaveBand2, mGCPPixelPos, sGCPPixelPos);
                                }
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
                                //System.out.println("final "+mPin.getName()+" = " + "(" + sGCPPixelPos.x + "," + sGCPPixelPos.y + ")");
                                //System.out.println();

                            } //else {
                            //System.out.println("GCP(" + mPin.getName() + ") is invalid.");
                            //}
                        }

                        private synchronized void addPlacemark(final Placemark pin) {
                            targetGCPGroup.add(pin);
                        }

                    };

                    executor.execute(worker);
                }
                status.worked(1);
            }

            executor.complete();

            SystemUtils.tileCacheFreeOldTiles();

            //final long duration = timeMonitor.stop();
            //System.out.println("XCorr completed in "+ ProcessTimeMonitor.formatDuration(duration));
            status.done();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " computeSlaveGCPs ", e);
        }

        gcpsComputedMap.put(slaveBand1, true);
    }

    private void determiningImageOffset(final Band slaveBand1, final Band slaveBand2, int[] offset) {

        try {
            // get master and slave imagettes
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            double groundRangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 1);
            final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);
            final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
            if (!srgrFlag) {
                final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
                final double incidenceAngleAtCentreRangePixel =
                        incidenceAngle.getPixelDouble(sourceImageWidth / 2f, sourceImageHeight / 2f);
                groundRangeSpacing /= FastMath.sin(incidenceAngleAtCentreRangePixel * Constants.DTOR);
            }
            final int nRgLooks = Math.max(1, sourceImageWidth / 2048);
            final int nAzLooks = Math.max(1, (int) ((double) nRgLooks * groundRangeSpacing / azimuthSpacing + 0.5));
            final int targetImageWidth = sourceImageWidth / nRgLooks;
            final int targetImageHeight = sourceImageHeight / nAzLooks;
            final int windowWidth = (int) FastMath.pow(2, (int) (Math.log10(targetImageWidth) / Math.log10(2)));
            final int windowHeight = (int) FastMath.pow(2, (int) (Math.log10(targetImageHeight) / Math.log10(2)));
            final double[] mI = new double[windowWidth * windowHeight];
            final double[] sI = new double[windowWidth * windowHeight];

            final int tileCountX = 4;
            final int tileCountY = 4;
            final int tileWidth = windowWidth / tileCountX;
            final int tileHeight = windowHeight / tileCountY;
            final Rectangle[] tileRectangles = new Rectangle[tileCountX * tileCountY];
            int index = 0;
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                final int ypos = tileY * tileHeight;
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    final Rectangle tileRectangle = new Rectangle(tileX * tileWidth, ypos,
                                                                  tileWidth, tileHeight);
                    tileRectangles[index++] = tileRectangle;
                }
            }

            final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
            status.beginTask("Computing offset... ", tileRectangles.length);

            final ThreadExecutor executor = new ThreadExecutor();
            try {
                for (final Rectangle rectangle : tileRectangles) {
                    checkForCancellation();

                    final ThreadRunnable worker = new ThreadRunnable() {

                        @Override
                        public void process() {
                            final int x0 = rectangle.x;
                            final int y0 = rectangle.y;
                            final int w = rectangle.width;
                            final int h = rectangle.height;
                            final int xMax = x0 + w;
                            final int yMax = y0 + h;

                            final int xStart = x0 * nRgLooks;
                            final int yStart = y0 * nAzLooks;
                            final int xEnd = xMax * nRgLooks;
                            final int yEnd = yMax * nAzLooks;

                            final Rectangle srcRect = new Rectangle(xStart, yStart, xEnd - xStart, yEnd - yStart);
                            final Tile mstTile1 = getSourceTile(masterBand1, srcRect);
                            final ProductData mstData1 = mstTile1.getDataBuffer();
                            final TileIndex mstIndex = new TileIndex(mstTile1);
                            final Tile slvTile1 = getSourceTile(slaveBand1, srcRect);
                            final ProductData slvData1 = slvTile1.getDataBuffer();
                            final TileIndex slvIndex = new TileIndex(slvTile1);

                            ProductData mstData2 = null;
                            ProductData slvData2 = null;
                            if (complexCoregistration) {
                                mstData2 = getSourceTile(masterBand2, srcRect).getDataBuffer();
                                slvData2 = getSourceTile(slaveBand2, srcRect).getDataBuffer();
                            }

                            final double rgAzLooks = nRgLooks * nAzLooks;

                            for (int y = y0; y < yMax; y++) {
                                final int yByWidth = y * windowWidth;
                                final int y1 = y * nAzLooks;
                                final int y2 = y1 + nAzLooks;
                                for (int x = x0; x < xMax; x++) {
                                    final int x1 = x * nRgLooks;
                                    final int x2 = x1 + nRgLooks;
                                    mI[yByWidth + x] = getMeanValue(x1, x2, y1, y2, mstData1, mstData2, mstIndex, rgAzLooks);
                                    sI[yByWidth + x] = getMeanValue(x1, x2, y1, y2, slvData1, slvData2, slvIndex, rgAzLooks);
                                }
                            }

                            status.worked(1);
                        }
                    };
                    executor.execute(worker);

                }
                executor.complete();

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException("GCPSelectionOp", e);
            } finally {
                status.done();
            }

            // correlate master and slave imagettes
            final RenderedImage masterImage = createRenderedImage(mI, windowWidth, windowHeight);
            final PlanarImage masterSpectrum = JAIFunctions.dft(masterImage);

            final RenderedImage slaveImage = createRenderedImage(sI, windowWidth, windowHeight);
            final PlanarImage slaveSpectrum = JAIFunctions.dft(slaveImage);
            final PlanarImage conjugateSlaveSpectrum = JAIFunctions.conjugate(slaveSpectrum);

            final PlanarImage crossSpectrum = JAIFunctions.multiplyComplex(masterSpectrum, conjugateSlaveSpectrum);
            final PlanarImage correlatedImage = JAIFunctions.idft(crossSpectrum);
            final PlanarImage crossCorrelatedImage = JAIFunctions.magnitude(correlatedImage);

            // compute offset
            final int w = crossCorrelatedImage.getWidth();
            final int h = crossCorrelatedImage.getHeight();
            final Raster idftData = crossCorrelatedImage.getData();
            final double[] real = idftData.getSamples(0, 0, w, h, 0, (double[]) null);

            int peakRow = 0;
            int peakCol = 0;
            double peak = 0;
            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    if (r >= h / 4 && r <= h * 3 / 4 || c >= w / 4 && c <= w * 3 / 4) {
                        continue;
                    }
                    final int s = r * w + c;
                    if (peak < real[s]) {
                        peak = real[s];
                        peakRow = r;
                        peakCol = c;
                    }
                }
            }

            // System.out.println("peakRow = " + peakRow + ", peakCol = " + peakCol);
            if (peakRow <= h / 2) {
                offset[1] = -peakRow * nAzLooks;
            } else {
                offset[1] = (h - peakRow) * nAzLooks;
            }

            if (peakCol <= w / 2) {
                offset[0] = -peakCol * nRgLooks;
            } else {
                offset[0] = (w - peakCol) * nRgLooks;
            }
            // System.out.println("offsetX = " + offset[0] + ", offsetY = " + offset[1]);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " determiningImageOffset ", e);
        }
    }

    private double getMeanValue(final int xStart, final int xEnd, final int yStart, final int yEnd,
                                final ProductData srcData1, final ProductData srcData2,
                                final TileIndex srcIndex, final double rgAzLooks) {

        double v1, v2;
        double meanValue = 0.0;
        if (complexCoregistration) {

            for (int y = yStart; y < yEnd; y++) {
                srcIndex.calculateStride(y);
                for (int x = xStart; x < xEnd; x++) {
                    final int idx = srcIndex.getIndex(x);
                    v1 = srcData1.getElemDoubleAt(idx);
                    v2 = srcData2.getElemDoubleAt(idx);
                    meanValue += v1 * v1 + v2 * v2;
                }
            }

        } else {

            for (int y = yStart; y < yEnd; y++) {
                srcIndex.calculateStride(y);
                for (int x = xStart; x < xEnd; x++) {
                    meanValue += srcData1.getElemDoubleAt(srcIndex.getIndex(x));
                }
            }
        }

        return meanValue / rgAzLooks;
    }

    /**
     * Copy GCPs of the first target band to current target band.
     *
     * @param firstTargetBand First target band.
     * @param targetBand      Current target band.
     */
    private static void copyFirstTargetBandGCPs(final Band firstTargetBand, final Band targetBand) {

        final ProductNodeGroup<Placemark> firstTargetBandGcpGroup = GCPManager.instance().getGcpGroup(firstTargetBand);
        final ProductNodeGroup<Placemark> currentTargetBandGCPGroup = GCPManager.instance().getGcpGroup(targetBand);
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
     * false otherwise.
     */
    private boolean checkMasterGCPValidity(final Placemark mPin) throws Exception {
        final PixelPos pixelPos = mPin.getPixelPos();
        if (onlyGCPsOnLand) {
            double alt = dem.getElevation(mPin.getGeoPos());
            if (alt == dem.getDescriptor().getNoDataValue())
                return false;
        }
        return (pixelPos.x - cHalfWindowWidth + 1 >= 0 && pixelPos.x + cHalfWindowWidth <= sourceImageWidth - 1) &&
                (pixelPos.y - cHalfWindowHeight + 1 >= 0 && pixelPos.y + cHalfWindowHeight <= sourceImageHeight - 1);
    }

    /**
     * Check if a given slave GCP imagette is within the image.
     *
     * @param pixelPos The GCP pixel position.
     * @return flag Return true if the GCP is within the image, false otherwise.
     */
    private boolean checkSlaveGCPValidity(final PixelPos pixelPos) {

        return (pixelPos.x - cHalfWindowWidth + 1 >= 0 && pixelPos.x + cHalfWindowWidth <= sourceImageWidth - 1) &&
                (pixelPos.y - cHalfWindowHeight + 1 >= 0 && pixelPos.y + cHalfWindowHeight <= sourceImageHeight - 1);
    }

    /*private boolean getCoarseOffsets(final Band slaveBand1, final Band slaveBand2,
                                     final PixelPos mGCPPixelPos,
                                     final PixelPos sGCPPixelPos) {

        try {
            // get data
            final ComplexDoubleMatrix mI = getComplexDoubleMatrix(masterBand1, masterBand2, mGCPPixelPos, coarseWin);
            final ComplexDoubleMatrix sI = getComplexDoubleMatrix(slaveBand1, slaveBand2, sGCPPixelPos, coarseWin);

            final double[] coarseOffset = {0, 0};

            double coherence = CoregistrationUtils.crossCorrelateFFT(
                    coarseOffset, mI, sI, coarseWin.ovsFactor, coarseWin.accY, coarseWin.accX);

            SystemUtils.LOG.info("Coarse sGCP = ({}, {})" + coarseOffset[1] + coarseOffset[0]);
            SystemUtils.LOG.info("Coarse sGCP coherence = {}" + coherence);

            sGCPPixelPos.x += (float) coarseOffset[1];
            sGCPPixelPos.y += (float) coarseOffset[0];

            return true;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getCoarseSlaveGCPPosition ", e);
        }
        return false;
    }*/

    private boolean getFineOffsets(final Band slaveBand1, final Band slaveBand2,
                                   final PixelPos mGCPPixelPos,
                                   final PixelPos sGCPPixelPos) {
        try {
            //SystemUtils.LOG.info("mGCP = ({}, {})" + mGCPPixelPos.x + mGCPPixelPos.y);
            //SystemUtils.LOG.info("Initial sGCP = ({}, {})" + sGCPPixelPos.x + sGCPPixelPos.y);

            ComplexDoubleMatrix mI = getComplexDoubleMatrix(masterBand1, masterBand2, mGCPPixelPos, fineWin);
            ComplexDoubleMatrix sI = getComplexDoubleMatrix(slaveBand1, slaveBand2, sGCPPixelPos, fineWin);

            final double[] fineOffset = {0.0, 0.0};

            final double coherence = CoregistrationUtils.crossCorrelateFFT(fineOffset, mI, sI, fineWin.ovsFactor, fineWin.accY, fineWin.accX);

            //SystemUtils.LOG.info("Final sGCP = ({},{})" + fineOffset[1] + fineOffset[0]);
            //SystemUtils.LOG.info("Final sGCP coherence = {}" + coherence);

            if (coherence < coherenceThreshold) {
                //System.out.println("Invalid GCP");
                return false;
            } else {
                sGCPPixelPos.x = (int)sGCPPixelPos.x + (float)fineOffset[1];
                sGCPPixelPos.y = (int)sGCPPixelPos.y + (float)fineOffset[0];
                //System.out.println("Valid GCP");
                return true;
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getFineSlaveGCPPosition ", e);
        }
        return false;
    }

    private ComplexDoubleMatrix getComplexDoubleMatrix(
            final Band band1, final Band band2, final PixelPos pixelPos, final CorrelationWindow corrWindow) {

        Rectangle rectangle = corrWindow.defineRectangleMask(pixelPos);
        Tile tileReal = getSourceTile(band1, rectangle);

        Tile tileImag = null;
        if (band2 != null) {
            tileImag = getSourceTile(band2, rectangle);
        }
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private boolean getCoarseSlaveGCPPosition(final Band slaveBand, final Band slaveBand2,
                                              final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos) {
        try {
            final double[] mI = new double[cWindowWidth * cWindowHeight];
            final double[] sI = new double[cWindowWidth * cWindowHeight];

            final boolean getMISuccess = getMasterImagette(mGCPPixelPos, mI);
            if (!getMISuccess) {
                return false;
            }
            //System.out.println("Master imagette:");
            //outputRealImage(mI);

            double rowShift = gcpTolerance + 1;
            double colShift = gcpTolerance + 1;
            int numIter = 0;

            while (Math.abs(rowShift) >= gcpTolerance || Math.abs(colShift) >= gcpTolerance) {

                if (numIter >= maxIteration) {
                    return false;
                }

                if (!checkSlaveGCPValidity(sGCPPixelPos)) {
                    return false;
                }

                final boolean getSISuccess = getSlaveImagette(slaveBand, slaveBand2, sGCPPixelPos, sI);
                if (!getSISuccess) {
                    return false;
                }
                //System.out.println("Slave imagette:");
                //outputRealImage(sI);

                final double[] shift = {0, 0};
                if (!getSlaveGCPShift(shift, mI, sI)) {
                    return false;
                }

                rowShift = shift[0];
                colShift = shift[1];
                sGCPPixelPos.x += colShift;
                sGCPPixelPos.y += rowShift;
                numIter++;
            }

            return true;
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getCoarseSlaveGCPPosition ", e);
        }
        return false;
    }

    private boolean getMasterImagette(final PixelPos gcpPixelPos, final double[] mI) throws OperatorException {

        final int x0 = (int) gcpPixelPos.x;
        final int y0 = (int) gcpPixelPos.y;
        final int xul = x0 - cHalfWindowWidth + 1;
        final int yul = y0 - cHalfWindowHeight + 1;
        final Rectangle masterImagetteRectangle = new Rectangle(xul, yul, cWindowWidth, cWindowHeight);

        try {
            final Tile masterImagetteRaster1 = getSourceTile(masterBand1, masterImagetteRectangle);
            final ProductData masterData1 = masterImagetteRaster1.getDataBuffer();
            final Double noDataValue1 = masterBand1.getNoDataValue();

            ProductData masterData2 = null;
            Double noDataValue2 = 0.0;
            if (complexCoregistration) {
                final Tile masterImagetteRaster2 = getSourceTile(masterBand2, masterImagetteRectangle);
                masterData2 = masterImagetteRaster2.getDataBuffer();
                noDataValue2 = masterBand2.getNoDataValue();
            }

            final TileIndex mstIndex = new TileIndex(masterImagetteRaster1);

            int k = 0;
            int numInvalidPixels = 0;
            for (int j = 0; j < cWindowHeight; j++) {
                final int offset = mstIndex.calculateStride(yul + j);
                for (int i = 0; i < cWindowWidth; i++) {
                    final int index = xul + i - offset;
                    if (complexCoregistration) {
                        final double v1 = masterData1.getElemDoubleAt(index);
                        final double v2 = masterData2.getElemDoubleAt(index);
                        if (noDataValue1.equals(v1) && noDataValue2.equals(v2)) {
                            numInvalidPixels++;
                        }
                        mI[k++] = v1 * v1 + v2 * v2;
                    } else {
                        final double v = masterData1.getElemDoubleAt(index);
                        if (noDataValue1.equals(v)) {
                            numInvalidPixels++;
                        }
                        mI[k++] = v;
                    }
                }
            }

            masterData1.dispose();
            if (masterData2 != null)
                masterData2.dispose();

            return numInvalidPixels <= MaxInvalidPixelPercentage * cWindowHeight * cWindowWidth;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("getMasterImagette", e);
        }
        return false;
    }

    private boolean getSlaveImagette(
            final Band slaveBand1, final Band slaveBand2, final PixelPos gcpPixelPos, final double[] sI)
            throws OperatorException {

        final double xx = gcpPixelPos.x;
        final double yy = gcpPixelPos.y;
        final int xul = Math.max(0, (int) xx - cHalfWindowWidth);
        final int yul = Math.max(0, (int) yy - cHalfWindowHeight);
        final Rectangle slaveImagetteRectangle = new Rectangle(xul, yul, cWindowWidth + 3, cWindowHeight + 3);
        int k = 0;

        try {
            final Tile slaveImagetteRaster1 = getSourceTile(slaveBand1, slaveImagetteRectangle);
            final ProductData slaveData1 = slaveImagetteRaster1.getDataBuffer();
            final Double noDataValue1 = slaveBand1.getNoDataValue();

            Tile slaveImagetteRaster2 = null;
            ProductData slaveData2 = null;
            Double noDataValue2 = 0.0;
            if (complexCoregistration) {
                slaveImagetteRaster2 = getSourceTile(slaveBand2, slaveImagetteRectangle);
                slaveData2 = slaveImagetteRaster2.getDataBuffer();
                noDataValue2 = slaveBand2.getNoDataValue();
            }

            final TileIndex index0 = new TileIndex(slaveImagetteRaster1);
            final TileIndex index1 = new TileIndex(slaveImagetteRaster1);

            int numInvalidPixels = 0;
            for (int j = 0; j < cWindowHeight; j++) {
                final double y = yy - cHalfWindowHeight + j + 1;
                final int y0 = (int) y;
                final int y1 = y0 + 1;
                final int offset0 = index0.calculateStride(y0);
                final int offset1 = index1.calculateStride(y1);
                final double wy = y - y0;
                for (int i = 0; i < cWindowWidth; i++) {
                    final double x = xx - cHalfWindowWidth + i + 1;
                    final int x0 = (int) x;
                    final int x1 = x0 + 1;
                    final double wx = x - x0;

                    final int x00 = x0 - offset0;
                    final int x01 = x0 - offset1;
                    final int x10 = x1 - offset0;
                    final int x11 = x1 - offset1;

                    if (complexCoregistration) {

                        final double v1 = MathUtils.interpolate2D(wy, wx, slaveData1.getElemDoubleAt(x00),
                                                                  slaveData1.getElemDoubleAt(x01),
                                                                  slaveData1.getElemDoubleAt(x10),
                                                                  slaveData1.getElemDoubleAt(x11));

                        final double v2 = MathUtils.interpolate2D(wy, wx, slaveData2.getElemDoubleAt(x00),
                                                                  slaveData2.getElemDoubleAt(x01),
                                                                  slaveData2.getElemDoubleAt(x10),
                                                                  slaveData2.getElemDoubleAt(x11));

                        if (noDataValue1.equals(v1) && noDataValue2.equals(v2)) {
                            numInvalidPixels++;
                        }
                        sI[k] = v1 * v1 + v2 * v2;
                    } else {

                        final double v = MathUtils.interpolate2D(wy, wx, slaveData1.getElemDoubleAt(x00),
                                                                 slaveData1.getElemDoubleAt(x01),
                                                                 slaveData1.getElemDoubleAt(x10),
                                                                 slaveData1.getElemDoubleAt(x11));

                        if (noDataValue1.equals(v)) {
                            numInvalidPixels++;
                        }
                        sI[k] = v;
                    }
                    ++k;
                }
            }
            slaveData1.dispose();
            if (slaveData2 != null)
                slaveData2.dispose();

            return numInvalidPixels <= MaxInvalidPixelPercentage * cWindowHeight * cWindowWidth;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("getSlaveImagette", e);
        }
        return false;
    }

    private boolean getSlaveGCPShift(final double[] shift, final double[] mI, final double[] sI) {
        try {
            // perform cross correlation
            final PlanarImage crossCorrelatedImage = computeCrossCorrelatedImage(mI, sI);

            // check peak validity
            /*
            final double mean = getMean(crossCorrelatedImage);
            if (Double.compare(mean, 0.0) == 0) {
                return false;
            }

            double max = getMax(crossCorrelatedImage);
            double qualityParam = max / mean;
            if (qualityParam <= qualityThreshold) {
                return false;
            }
            */

            // get peak shift: row and col
            final int w = crossCorrelatedImage.getWidth();
            final int h = crossCorrelatedImage.getHeight();

            final Raster idftData = crossCorrelatedImage.getData();
            final double[] real = idftData.getSamples(0, 0, w, h, 0, (double[]) null);
            //System.out.println("Cross correlated imagette:");
            //outputRealImage(real);

            int peakRow = 0;
            int peakCol = 0;
            double peak = real[0];
            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    final int k = r * w + c;
                    if (real[k] > peak) {
                        peak = real[k];
                        peakRow = r;
                        peakCol = c;
                    }
                }
            }
            //System.out.println("peak = " + peak + " at (" + peakRow + ", " + peakCol + ")");

            if (peakRow <= h / 2) {
                shift[0] = (double) (-peakRow) / (double) rowUpSamplingFactor;
            } else {
                shift[0] = (double) (h - peakRow) / (double) rowUpSamplingFactor;
            }

            if (peakCol <= w / 2) {
                shift[1] = (double) (-peakCol) / (double) colUpSamplingFactor;
            } else {
                shift[1] = (double) (w - peakCol) / (double) colUpSamplingFactor;
            }

            return true;
        } catch (Throwable t) {
            SystemUtils.LOG.warning("getSlaveGCPShift failed " + t.getMessage());
            return false;
        }
    }

    private PlanarImage computeCrossCorrelatedImage(final double[] mI, final double[] sI) {

        // get master imagette spectrum
        final RenderedImage masterImage = createRenderedImage(mI, cWindowWidth, cWindowHeight);
        final PlanarImage masterSpectrum = JAIFunctions.dft(masterImage);
        //System.out.println("Master spectrum:");
        //outputComplexImage(masterSpectrum);

        // get slave imagette spectrum
        final RenderedImage slaveImage = createRenderedImage(sI, cWindowWidth, cWindowHeight);
        final PlanarImage slaveSpectrum = JAIFunctions.dft(slaveImage);
        //System.out.println("Slave spectrum:");
        //outputComplexImage(slaveSpectrum);

        // get conjugate slave spectrum
        final PlanarImage conjugateSlaveSpectrum = JAIFunctions.conjugate(slaveSpectrum);
        //System.out.println("Conjugate slave spectrum:");
        //outputComplexImage(conjugateSlaveSpectrum);

        // multiply master spectrum and conjugate slave spectrum
        final PlanarImage crossSpectrum = JAIFunctions.multiplyComplex(masterSpectrum, conjugateSlaveSpectrum);
        //System.out.println("Cross spectrum:");
        //outputComplexImage(crossSpectrum);

        // upsampling cross spectrum
        final RenderedImage upsampledCrossSpectrum = JAIFunctions.upsampling(crossSpectrum,
                                                                             rowUpSamplingFactor, colUpSamplingFactor);

        // perform IDF on the cross spectrum
        final PlanarImage correlatedImage = JAIFunctions.idft(upsampledCrossSpectrum);
        //System.out.println("Correlated image:");
        //outputComplexImage(correlatedImage);

        // compute the magnitude of the cross correlated image
        return JAIFunctions.magnitude(correlatedImage);
    }

    private static RenderedImage createRenderedImage(final double[] array, final int w, final int h) {

        // create rendered image with dimension being width by height
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, w, h, 1);
        final ColorModel colourModel = PlanarImage.createColorModel(sampleModel);
        final DataBufferDouble dataBuffer = new DataBufferDouble(array, array.length);
        final WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));

        return new BufferedImage(colourModel, raster, false, null);
    }

    // This function is for debugging only.
    private static void outputRealImage(final double[] I) {

        for (double v : I) {
            System.out.print(v + ",");
        }
        System.out.println();
    }

    // This function is for debugging only.
    private static void outputComplexImage(final PlanarImage image) {

        final int w = image.getWidth();
        final int h = image.getHeight();
        final Raster dftData = image.getData();
        final double[] real = dftData.getSamples(0, 0, w, h, 0, (double[]) null);
        final double[] imag = dftData.getSamples(0, 0, w, h, 1, (double[]) null);
        System.out.println("Real part:");
        for (double v : real) {
            System.out.print(v + ", ");
        }
        System.out.println();
        System.out.println("Imaginary part:");
        for (double v : imag) {
            System.out.print(v + ", ");
        }
        System.out.println();
    }

    /**
     * The function is for unit test only.
     *
     * @param windowWidth         The window width for cross-correlation
     * @param windowHeight        The window height for cross-correlation
     * @param rowUpSamplingFactor The row up sampling rate
     * @param colUpSamplingFactor The column up sampling rate
     * @param maxIter             The maximum number of iterations in computing slave GCP shift
     * @param tolerance           The stopping criterion for slave GCP shift calculation
     */
    public void setTestParameters(final String windowWidth,
                                  final String windowHeight,
                                  final String rowUpSamplingFactor,
                                  final String colUpSamplingFactor,
                                  final int maxIter,
                                  final double tolerance) {

        coarseRegistrationWindowWidth = windowWidth;
        coarseRegistrationWindowHeight = windowHeight;
        rowInterpFactor = rowUpSamplingFactor;
        columnInterpFactor = colUpSamplingFactor;
        maxIteration = maxIter;
        gcpTolerance = tolerance;
    }

    //=========================================== Complex Co-registration ==============================================

    private boolean getFineSlaveGCPPosition(final Band slaveBand1, final Band slaveBand2,
                                            final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos) {
        try {
            //System.out.println("mGCP = (" + mGCPPixelPos.x + ", " + mGCPPixelPos.y + ")");
            //System.out.println("Initial sGCP = (" + sGCPPixelPos.x + ", " + sGCPPixelPos.y + ")");

            final FineRegistration fineRegistration = new FineRegistration();

            final FineRegistration.ComplexCoregData complexData =
                    new FineRegistration.ComplexCoregData(coherenceWindowSize,
                                                          coherenceFuncToler, coherenceValueToler,
                                                          fWindowWidth, fWindowHeight, useSlidingWindow);

            getComplexMasterImagette(complexData, mGCPPixelPos);
            /*
            System.out.println("Real part of master imagette:");
            outputRealImage(complexData.mII);
            System.out.println("Imaginary part of master imagette:");
            outputRealImage(complexData.mIQ);
            */

//            getInitialComplexSlaveImagette(complexData, mGCPPixelPos); // for testing only
//            final double[] p = {mGCPPixelPos.x, mGCPPixelPos.y}; // for testing only

            getInitialComplexSlaveImagette(fineRegistration, complexData, slaveBand1, slaveBand2, sGCPPixelPos);
            /*
            System.out.println("Real part of initial slave imagette:");
            outputRealImage(complexData.sII0);
            System.out.println("Imaginary part of initial slave imagette:");
            outputRealImage(complexData.sIQ0);
            */

            final double[] p = {sGCPPixelPos.x, sGCPPixelPos.y};

            final double coherence = fineRegistration.powell(complexData, p);
            //System.out.println("Final sGCP = (" + p[0] + ", " + p[1] + "), coherence = " + (1-coherence));
            //System.out.println("xShift = " + (p[0] - complexData.point0[0]) + ", yShift = " + (p[1] - complexData.point0[1]));

            complexData.dispose();

            if (1 - coherence < coherenceThreshold) {
                //System.out.println("Invalid GCP");
                return false;
            } else {
                sGCPPixelPos.x = p[0];
                sGCPPixelPos.y = p[1];
                //System.out.println("Valid GCP");
                return true;
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getFineSlaveGCPPosition ", e);
        }
        return false;
    }

    private void getComplexMasterImagette(final FineRegistration.ComplexCoregData complexData,
                                          final PixelPos gcpPixelPos) {

        complexData.mII = new double[complexData.fWindowHeight][complexData.fWindowWidth];
        complexData.mIQ = new double[complexData.fWindowHeight][complexData.fWindowWidth];
        final int x0 = (int) gcpPixelPos.x;
        final int y0 = (int) gcpPixelPos.y;
        final int xul = x0 - complexData.fHalfWindowWidth + 1;
        final int yul = y0 - complexData.fHalfWindowHeight + 1;
        final Rectangle masterImagetteRectangle = new Rectangle(xul, yul, complexData.fWindowWidth, complexData.fWindowHeight);

        final Tile masterImagetteRaster1 = getSourceTile(masterBand1, masterImagetteRectangle);
        final Tile masterImagetteRaster2 = getSourceTile(masterBand2, masterImagetteRectangle);

        final ProductData masterData1 = masterImagetteRaster1.getDataBuffer();
        final ProductData masterData2 = masterImagetteRaster2.getDataBuffer();

        final TileIndex index = new TileIndex(masterImagetteRaster1);

        final double[][] mIIdata = complexData.mII;
        final double[][] mIQdata = complexData.mIQ;
        for (int j = 0; j < complexData.fWindowHeight; j++) {
            index.calculateStride(yul + j);
            for (int i = 0; i < complexData.fWindowWidth; i++) {
                final int idx = index.getIndex(xul + i);
                mIIdata[j][i] = masterData1.getElemDoubleAt(idx);
                mIQdata[j][i] = masterData2.getElemDoubleAt(idx);
            }
        }
        masterData1.dispose();
        masterData2.dispose();
    }

    // This function is for testing only
    private static void getInitialComplexSlaveImagette(final FineRegistration fineRegistration,
                                                final FineRegistration.ComplexCoregData complexData,
                                                final PixelPos mGCPPixelPos) {

        complexData.sII0 = new double[complexData.fWindowHeight][complexData.fWindowWidth];
        complexData.sIQ0 = new double[complexData.fWindowHeight][complexData.fWindowWidth];

        complexData.point0[0] = mGCPPixelPos.x;
        complexData.point0[1] = mGCPPixelPos.y;

        final double[][] mIIdata = complexData.mII;
        final double[][] mIQdata = complexData.mIQ;

        final double[][] sII0data = complexData.sII0;
        final double[][] sIQ0data = complexData.sIQ0;

        final double xShift = 0.3;
        final double yShift = -0.2;
        //System.out.println("xShift = " + xShift);
        //System.out.println("yShift = " + yShift);

        fineRegistration.getShiftedData(complexData, mIIdata, mIQdata, xShift, yShift, sII0data, sIQ0data);
    }

    private void getInitialComplexSlaveImagette(final FineRegistration fineRegistration,
                                                final FineRegistration.ComplexCoregData complexData,
                                                final Band slaveBand1, final Band slaveBand2,
                                                final PixelPos sGCPPixelPos) {

        complexData.sII0 = new double[complexData.fWindowHeight][complexData.fWindowWidth];
        complexData.sIQ0 = new double[complexData.fWindowHeight][complexData.fWindowWidth];

        complexData.point0[0] = sGCPPixelPos.x;
        complexData.point0[1] = sGCPPixelPos.y;

        final double[][] sII0data = complexData.sII0;
        final double[][] sIQ0data = complexData.sIQ0;

        final double[][] tmpI = new double[complexData.fWindowHeight][complexData.fWindowWidth];
        final double[][] tmpQ = new double[complexData.fWindowHeight][complexData.fWindowWidth];

        final int x0 = (int) (sGCPPixelPos.x + 0.5);
        final int y0 = (int) (sGCPPixelPos.y + 0.5);

        final int xul = x0 - complexData.fHalfWindowWidth + 1;
        final int yul = y0 - complexData.fHalfWindowHeight + 1;
        final Rectangle slaveImagetteRectangle = new Rectangle(xul, yul, complexData.fWindowWidth, complexData.fWindowHeight);

        final Tile slaveImagetteRaster1 = getSourceTile(slaveBand1, slaveImagetteRectangle);
        final Tile slaveImagetteRaster2 = getSourceTile(slaveBand2, slaveImagetteRectangle);

        final ProductData slaveData1 = slaveImagetteRaster1.getDataBuffer();
        final ProductData slaveData2 = slaveImagetteRaster2.getDataBuffer();
        final TileIndex index = new TileIndex(slaveImagetteRaster1);
        for (int j = 0; j < complexData.fWindowHeight; j++) {
            index.calculateStride(yul + j);
            for (int i = 0; i < complexData.fWindowWidth; i++) {
                final int idx = index.getIndex(xul + i);
                tmpI[j][i] = slaveData1.getElemDoubleAt(idx);
                tmpQ[j][i] = slaveData2.getElemDoubleAt(idx);
            }
        }
        slaveData1.dispose();
        slaveData2.dispose();

        final double xShift = sGCPPixelPos.x - x0;
        final double yShift = sGCPPixelPos.y - y0;
        fineRegistration.getShiftedData(complexData, tmpI, tmpQ, xShift, yShift, sII0data, sIQ0data);
    }

    public static class CorrelationWindow {

        final public int height;
        final public int width;
        final public int halfWidth;
        final public int halfHeight;
        final public int accY;
        final public int accX;
        final public int ovsFactor;

        public CorrelationWindow(int winWidth, int winHeight, int accX, int accY, int ovsFactor) {
            this.accX = accX;
            this.accY = accY;
            this.width = winWidth;
            this.height = winHeight;
            this.halfWidth = winWidth / 2;
            this.halfHeight = winHeight / 2;
            this.ovsFactor = ovsFactor;
        }

        public org.jlinda.core.Window defineWindowMask(int x, int y) {
            int l0 = y - halfHeight;
            int lN = y + halfHeight - 1;
            int p0 = x - halfWidth;
            int pN = x + halfWidth - 1;

            return new org.jlinda.core.Window(l0, lN, p0, pN);
        }

        public org.jlinda.core.Window defineWindowMask(PixelPos pos) {
            int l0 = (int) (pos.y - halfHeight);
            int lN = (int) (pos.y + halfHeight - 1);
            int p0 = (int) (pos.x - halfWidth);
            int pN = (int) (pos.x + halfWidth - 1);

            return new org.jlinda.core.Window(l0, lN, p0, pN);
        }

        public Rectangle defineRectangleMask(int x, int y) {
            org.jlinda.core.Window temp = defineWindowMask(x, y);
            return new Rectangle((int) temp.pixlo, (int) temp.linelo, (int) temp.pixels(), (int) temp.lines());
        }

        public Rectangle defineRectangleMask(PixelPos pos) {
            org.jlinda.core.Window temp = defineWindowMask(pos);
            return new Rectangle((int) temp.pixlo, (int) temp.linelo, (int) temp.pixels(), (int) temp.lines());
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CrossCorrelationOp.class);
        }
    }
}
