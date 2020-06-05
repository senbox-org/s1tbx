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
import org.esa.s1tbx.insar.gpf.support.JAIFunctions;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.coregistration.CPM;
import org.jlinda.core.coregistration.PolynomialModel;
import org.jlinda.core.coregistration.SimpleLUT;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationTable;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Image co-registration is fundamental for Interferometry SAR (InSAR) imaging and its applications, such as
 * DEM map generation and analysis. To obtain a high quality InSAR image, the individual complex images need
 * to be co-registered to sub-pixel accuracy. The co-registration is accomplished through an alignment of a
 * master image with a slave image.
 * <p>
 * To achieve the alignment of master and slave images, the first step is to generate a set of uniformly
 * spaced ground control points (GCPs) in the master image, along with the corresponding GCPs in the slave
 * image. Details of the generation of the GCP pairs are given in GCPSelectionOperator. The next step is to
 * construct a warp distortion function from the computed GCP pairs and generate co-registered slave image.
 * <p>
 * This operator computes the warp function from the master-slave GCP pairs for given polynomial order.
 * Basically coefficients of two polynomials are determined from the GCP pairs with each polynomial for
 * one coordinate of the image pixel. With the warp function determined, the co-registered image can be
 * obtained by mapping slave image pixels to master image pixels. In particular, for each pixel position in
 * the master image, warp function produces its corresponding pixel position in the slave image, and the
 * pixel value is computed through interpolation. The following interpolation methods are available:
 * <p>
 * 1. Nearest-neighbour interpolation
 * 2. Bilinear interpolation
 * 3. Bicubic interpolation
 * 4. Bicubic2 interpolation
 */

@OperatorMetadata(alias = "Warp",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci, Petar Marinkovic",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Create Warp Function And Get Co-registrated Images")
public class WarpOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Confidence level for outlier detection procedure, lower value accepts more outliers",
            valueSet = {"0.001", "0.05", "0.1", "0.5", "1.0"},
            defaultValue = "0.05",
            label = "Significance Level for Outlier Removal")
    private float rmsThreshold = 0.05f;
    private float cpmWtestCriticalValue;

    @Parameter(description = "The order of WARP polynomial function", valueSet = {"1", "2", "3"}, defaultValue = "2",
            label = "Warp Polynomial Order")
    private int warpPolynomialOrder = 2;

    @Parameter(valueSet = {NEAREST_NEIGHBOR, BILINEAR, BICUBIC, BICUBIC2,
            TRI, CC4P, CC6P, TS6P, TS8P, TS16P}, defaultValue = CC6P, label = "Interpolation Method")
    private String interpolationMethod = CC6P;

    //@Parameter(description = "Optimize for Interferometry",
    //        defaultValue = "false", label = "InSAR Optimized")
    private boolean inSAROptimized = true;

    @Parameter(description = "Refine estimated offsets using a-priori DEM",
            defaultValue = "false", label = "Offset Refinement Based on DEM")
    private Boolean demRefinement = false;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(defaultValue = "false")
    private boolean excludeMaster = false;

    private Interpolation interp;
    private InterpolationTable interpTable;

    @Parameter(description = "Show the Residuals file in a text viewer", defaultValue = "false", label = "Show Residuals")
    private Boolean openResidualsFile;

    private Band masterBand;
    private boolean complexCoregistration;
    private boolean warpDataAvailable;

    public static final String NEAREST_NEIGHBOR = "Nearest-neighbor interpolation";
    public static final String BILINEAR = "Bilinear interpolation";
    public static final String BICUBIC = "Bicubic interpolation";
    public static final String BICUBIC2 = "Bicubic2 interpolation";
    public static final String TRI = SimpleLUT.TRI;
    public static final String CC4P = SimpleLUT.CC4P;
    public static final String CC6P = SimpleLUT.CC6P;
    public static final String TS6P = SimpleLUT.TS6P;
    public static final String TS8P = SimpleLUT.TS8P;
    public static final String TS16P = SimpleLUT.TS16P;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, PolynomialModel> warpDataMap = new HashMap<>(10);

    private String processedSlaveBand;
    private String[] masterBandNames;

    // DEM refinement
    private static final int ORBIT_INTERP_DEGREE = 3;
    float demNoDataValue = 0;
    private ElevationModel dem;

    private int maxIterations = 20;

    // demodulation related attributes
    private static final String DEMOD_PHASE_PREFIX = "DemodPhase";
    private Interpolation interpDemodPhase = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
    private final Map<Band, Band> demodPhaseMap = new HashMap<>(10);

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public WarpOp() {
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
            // clear any old residual file
            final File residualsFile = getResidualsFile(sourceProduct);
            if (residualsFile.exists()) {
                residualsFile.delete();
            }

            getMasterBands();

            if (complexCoregistration) {
                if (demRefinement == null)
                    demRefinement = false;

                if (rmsThreshold == 0.001f) {
                    cpmWtestCriticalValue = 3.2905267314919f;
                    inSAROptimized = true;
                } else if (rmsThreshold == 0.05f) {
                    cpmWtestCriticalValue = 1.95996398454005f;
                    inSAROptimized = true;
                } else if (rmsThreshold == 0.1f) {
                    cpmWtestCriticalValue = 1.64485362695147f;
                    inSAROptimized = true;
                } else {
                    cpmWtestCriticalValue = 1.0f;
                }
            } else {
                inSAROptimized = false;
                demRefinement = false;
            }

            switch (interpolationMethod) {
                case NEAREST_NEIGHBOR:
                    interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                    break;
                case BILINEAR:
                    interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                    break;
                case BICUBIC:
                    interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
                    break;
                case BICUBIC2:
                    interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
                    break;
                case CC4P:
                    constructInterpolationTable(CC4P);
                    break;
                case CC6P:
                    constructInterpolationTable(CC6P);
                    break;
                case TS6P:
                    constructInterpolationTable(TS6P);
                    break;
                case TS8P:
                    constructInterpolationTable(TS8P);
                    break;
                case TS16P:
                    constructInterpolationTable(TS16P);
                    break;
                default:
                    interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                    break;
            }

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            if (absRoot != null) {
                processedSlaveBand = absRoot.getAttributeString("processed_slave");
            }

            createTargetProduct();

        } catch (Throwable e) {
            openResidualsFile = true;
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterBands() {
        String mstBandName = sourceProduct.getBandAt(0).getName();

        // find co-pol bands
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            final String mstPol = OperatorUtils.getPolarizationFromBandName(bandName);
            if (mstPol != null && (mstPol.equals("hh") || mstPol.equals("vv"))) {
                mstBandName = bandName;
                break;
            }
        }
        masterBand = sourceProduct.getBand(mstBandName);
        if (masterBand.getUnit() != null && masterBand.getUnit().equals(Unit.REAL)) {
            int mstIdx = sourceProduct.getBandIndex(mstBandName);
            if (sourceProduct.getNumBands() > mstIdx + 1) {
                complexCoregistration = true;
            }
        }
    }

    private void addSlaveGCPs(final PolynomialModel warpData, final String bandName) {

        final GeoCoding targetGeoCoding = targetProduct.getSceneGeoCoding();
        final String newName = excludeMaster ? StackUtils.getBandNameWithoutDate(bandName) : bandName;
        final ProductNodeGroup<Placemark> targetGCPGroup = GCPManager.instance().getGcpGroup(targetProduct.getBand(newName));
        targetGCPGroup.removeAll();

        final List<Placemark> slaveGCPList = warpData.getSlaveGCPList();
        for (final Placemark sPin : slaveGCPList) {
            final Placemark tPin = Placemark.createPointPlacemark(GcpDescriptor.getInstance(),
                                                                  sPin.getName(),
                                                                  sPin.getLabel(),
                                                                  sPin.getDescription(),
                                                                  sPin.getPixelPos(),
                                                                  sPin.getGeoPos(),
                                                                  targetGeoCoding);

            targetGCPGroup.add(tPin);
        }
    }

    private String formatName(final Band srcBand) {
        String name = srcBand.getName();
        if (excludeMaster) {  // multi-output without master
            String newName = StackUtils.getBandNameWithoutDate(name);
            if (name.equals(processedSlaveBand)) {
                processedSlaveBand = newName;
            }
            return newName;
        }
        return name;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        masterBandNames = StackUtils.getMasterBandNames(sourceProduct);

        final Band[] sourceBands = sourceProduct.getBands();
        for (int i = 0; i < sourceBands.length; i++) {
            final Band srcBand = sourceBands[i];
            final String srcBandName = srcBand.getName();

            Band targetBand;
            if (StringUtils.contains(masterBandNames, srcBandName)) {
                if (excludeMaster || targetProduct.getBand(srcBandName) != null) {
                    continue;
                }
                targetBand = ProductUtils.copyBand(srcBandName, sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                final String targetBandName = formatName(srcBand);
                if (targetProduct.getBand(targetBandName) != null) {
                    continue;
                }
                targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
                ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);

                // find demodulation band for slave corresponding to srcBand
                for (Band band : sourceBands) {
                    if (band.getName().equals(DEMOD_PHASE_PREFIX + StackUtils.getBandSuffix(srcBand.getName()))
                            && srcBand.getUnit().equals(Unit.REAL)) {
                        demodPhaseMap.put(band, srcBand);
                        break;
                    }
                }
            }
            sourceRasterMap.put(targetBand, srcBand);

            // continue if band corresponds to demodulation band, as it's not complex valued.
            if (srcBandName.startsWith(DEMOD_PHASE_PREFIX)) {
                continue;
            }

            if (complexCoregistration) {
                final Band srcBandQ = sourceProduct.getBandAt(i + 1);
                Band targetBandQ;
                if (StringUtils.contains(masterBandNames, srcBandName)) {
                    if (targetProduct.getBand(srcBandQ.getName()) != null) {
                        continue;
                    }
                    targetBandQ = ProductUtils.copyBand(srcBandQ.getName(), sourceProduct, targetProduct, false);
                    targetBandQ.setSourceImage(srcBandQ.getSourceImage());
                } else {
                    final String targetBandName = formatName(srcBandQ);
                    if (targetProduct.getBand(targetBandName) != null) {
                        continue;
                    }
                    targetBandQ = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
                    ProductUtils.copyRasterDataNodeProperties(srcBandQ, targetBandQ);
                }
                sourceRasterMap.put(targetBandQ, srcBandQ);

                complexSrcMap.put(srcBandQ, srcBand);
                String suffix = "";
                if (excludeMaster) { // multi-output without master
                    String pol = OperatorUtils.getPolarizationFromBandName(srcBand.getName());
                    if (pol != null && !pol.isEmpty()) {
                        suffix = '_' + pol.toUpperCase();
                    }
                } else {
                    suffix = '_' + OperatorUtils.getSuffixFromBandName(srcBand.getName());
                }
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBand, targetBandQ, suffix);
                i++;
            }
        }

        // co-registered image should have the same geo-coding as the master image
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        updateTargetProductMetadata();
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if (excludeMaster) {
            final String[] slaveNames = StackUtils.getSlaveProductNames(sourceProduct);
            absTgt.setAttributeString(AbstractMetadata.PRODUCT, slaveNames[0]);

            final ProductData.UTC[] times = StackUtils.getProductTimes(sourceProduct);
            targetProduct.setStartTime(times[1]);

            double lineTimeInterval = absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
            int height = sourceProduct.getSceneRasterHeight();
            ProductData.UTC endTime = new ProductData.UTC(times[1].getMJD() + (lineTimeInterval * height) / Constants.secondsInDay);
            targetProduct.setEndTime(endTime);

        } else {
            // only if its a full coregistered stack including master band
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        //System.out.println("WARPOperator: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!warpDataAvailable) {
                if (demRefinement) {
                    createDEM();
                }

                getWarpData(targetRectangle);
            }

            final Band srcBand = sourceRasterMap.get(targetBand);
            if (srcBand == null)
                return;

            Band realSrcBand;
            if (srcBand.getName().startsWith(DEMOD_PHASE_PREFIX)) {
                realSrcBand = demodPhaseMap.get(srcBand);
            } else {
                // get real part, assuming srcBand is imaginary
                realSrcBand = complexSrcMap.get(srcBand);
                // if srcBand was the real part (and hence not found in map)
                if (realSrcBand == null)
                    realSrcBand = srcBand;
            }

            // create source image
            final Tile sourceRaster = getSourceTile(srcBand, targetRectangle);

            if (pm.isCanceled())
                return;

            final PolynomialModel warpData = warpDataMap.get(realSrcBand);
            if (!warpData.isValid())
                return;

            final RenderedImage srcImage = sourceRaster.getRasterDataNode().getSourceImage();

            // get warped image (demodulation bands can be interpolated linearly)
            RenderedOp warpedImage;
            if (srcBand.getName().startsWith(DEMOD_PHASE_PREFIX)) {
                warpedImage = JAIFunctions.createWarpImage(warpData.getJAIWarp(), srcImage,
                                                           interpDemodPhase, null);
            } else {
                warpedImage = JAIFunctions.createWarpImage(warpData.getJAIWarp(), srcImage,
                                                           interp, interpTable);
            }

            // copy warped image data to target
            final float[] dataArray = warpedImage.getData(targetRectangle).getSamples(x0, y0, w, h, 0, (float[]) null);

            targetTile.setRawSamples(ProductData.createInstance(dataArray));

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void createDEM() throws IOException {

        final Resampling resampling = ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME);

        if (dem != null) return;

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);

        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        dem = demDescriptor.createDem(resampling);
        if (dem == null) {
            throw new OperatorException("The DEM '" + demName + "' has not been installed.");
        }
        demNoDataValue = demDescriptor.getNoDataValue();
    }

    private synchronized void getWarpData(final Rectangle targetRectangle) throws Exception {

        if (warpDataAvailable) {
            return;
        }

        // find first real slave band
        final Band targetBand = targetProduct.getBand(processedSlaveBand);
        // force getSourceTile to computeTiles on GCPSelection
        final Tile sourceRaster = getSourceTile(sourceRasterMap.get(targetBand), targetRectangle);

        final ProductNodeGroup<Placemark> masterGCPGroup = GCPManager.instance().getGcpGroup(masterBand);
        final org.jlinda.core.Window masterWindow = new org.jlinda.core.Window(0, sourceProduct.getSceneRasterHeight(), 0, sourceProduct.getSceneRasterWidth());

        // setup master metadata
        SLCImage masterMeta = null;
        Orbit masterOrbit = null;
        if (demRefinement) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
            masterMeta = new SLCImage(absRoot, targetProduct);
            masterOrbit = new Orbit(absRoot, ORBIT_INTERP_DEGREE);
        }

        // for all slave bands or band pairs compute a warp
        boolean appendFlag = false;
        int slaveMetaCnt = 0;
        final Band[] sourceBands = sourceProduct.getBands();
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceProduct.getBandAt(i);
            if (StringUtils.contains(masterBandNames, srcBand.getName()))
                continue;

            if (complexCoregistration && !srcBand.getUnit().equals(Unit.REAL))
                continue;

            ProductNodeGroup<Placemark> slaveGCPGroup = GCPManager.instance().getGcpGroup(srcBand);
            if (slaveGCPGroup.getNodeCount() < 3) {
                // find others for same slave product
                final String slvProductName = StackUtils.getSlaveProductName(sourceProduct, srcBand, null);
                for (Band band : sourceProduct.getBands()) {
                    if (band != srcBand) {
                        final String productName = StackUtils.getSlaveProductName(sourceProduct, band, null);
                        if (slvProductName != null && slvProductName.equals(productName)) {
                            slaveGCPGroup = GCPManager.instance().getGcpGroup(band);
                            if (slaveGCPGroup.getNodeCount() >= 3)
                                break;
                        }
                    }
                }
            }

            if (inSAROptimized) {
                final CPM cpm = new CPM(warpPolynomialOrder, maxIterations, cpmWtestCriticalValue,
                                        masterWindow, masterGCPGroup, slaveGCPGroup);
                warpDataMap.put(srcBand, cpm);

                final int nodeCount = slaveGCPGroup.getNodeCount();
                if (nodeCount < 3) {
                    cpm.noRedundancy = true;
                    continue;
                }

                // setup slave metadata
                if (demRefinement && !cpm.noRedundancy) {

                    // get height for corresponding points
                    double[] heightArray = new double[nodeCount];
                    final List<Placemark> slaveGCPList = new ArrayList<>();

                    for (int j = 0; j < nodeCount; j++) {

                        // work only with windows that survived threshold for this slave
                        slaveGCPList.add(slaveGCPGroup.get(j));
                        final Placemark sPin = slaveGCPList.get(j);
                        final Placemark mPin = masterGCPGroup.get(sPin.getName());
                        final PixelPos mGCPPos = mPin.getPixelPos();

                        double[] phiLamPoint = masterOrbit.lph2ell(mGCPPos.y, mGCPPos.x, 0, masterMeta);
                        PixelPos demIndexPoint = dem.getIndex(new GeoPos((phiLamPoint[0] * org.jlinda.core.Constants.RTOD), (phiLamPoint[1] * org.jlinda.core.Constants.RTOD)));

                        double height = dem.getSample(demIndexPoint.x, demIndexPoint.y);

                        if (Double.isNaN(height)) {
                            height = demNoDataValue;
                        }

                        heightArray[j] = height;
                    }

                    final MetadataElement slaveRoot = targetProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElementAt(slaveMetaCnt);
                    final SLCImage slaveMeta = new SLCImage(slaveRoot, targetProduct);
                    final Orbit slaveOrbit = new Orbit(slaveRoot, ORBIT_INTERP_DEGREE);
                    cpm.setDemNoDataValue(demNoDataValue);
                    cpm.setUpDEMRefinement(masterMeta, masterOrbit, slaveMeta, slaveOrbit, heightArray);
                    cpm.setUpDemOffset();
                }

                cpm.computeCPM();
                cpm.computeEstimationStats();
                cpm.wrapJaiWarpPolynomial();

                if (cpm.noRedundancy) {
                    continue;
                }

                if (!appendFlag) {
                    appendFlag = true;
                }

                //outputCoRegistrationInfo(sourceProduct, warpPolynomialOrder, cpm, appendFlag, srcBand.getName());

                addSlaveGCPs(cpm, srcBand.getName());
            } else {

                final WarpData warpData = new WarpData(slaveGCPGroup);
                warpDataMap.put(srcBand, warpData);

                if (slaveGCPGroup.getNodeCount() < 3) {
                    warpData.setInValid();
                    continue;
                }

                warpData.computeWARPPolynomialFromGCPs(sourceProduct, srcBand, warpPolynomialOrder, masterGCPGroup,
                                                       maxIterations, rmsThreshold, appendFlag);

                if (!warpData.isValid()) {
                    continue;
                }

                if (!appendFlag) {
                    appendFlag = true;
                }

                addSlaveGCPs(warpData, targetBand.getName());
            }
        }

        announceGCPWarning();

        GCPManager.instance().removeAllGcpGroups();

        if (openResidualsFile) {
            final File residualsFile = getResidualsFile(sourceProduct);
            if (Desktop.isDesktopSupported() && residualsFile.exists()) {
                try {
                    Desktop.getDesktop().open(residualsFile);
                } catch (Exception e) {
                    SystemUtils.LOG.warning("Error opening residuals file " + e.getMessage());
                    // do nothing
                }
            }
        }

        // update metadata
        writeWarpDataToMetadata();

        warpDataAvailable = true;
    }

    private void writeWarpDataToMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final Set<Band> bandSet = warpDataMap.keySet();

        for (Band band : bandSet) {
            final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, band.getName(), true);

            MetadataElement warpDataElem = bandElem.getElement("WarpData");
            if (warpDataElem == null) {
                warpDataElem = new MetadataElement("WarpData");
                bandElem.addElement(warpDataElem);
            } else {
                // empty out element
                final MetadataAttribute[] attribList = warpDataElem.getAttributes();
                for (MetadataAttribute attrib : attribList) {
                    warpDataElem.removeAttribute(attrib);
                }
            }

            final PolynomialModel warpData = warpDataMap.get(band);
            if (warpData.getNumObservations() > 0) {
                for (int i = 0; i < warpData.getNumObservations(); i++) {
                    final MetadataElement gcpElem = new MetadataElement("GCP" + i);
                    warpDataElem.addElement(gcpElem);

                    gcpElem.setAttributeDouble("mst_x", warpData.getXMasterCoord(i));
                    gcpElem.setAttributeDouble("mst_y", warpData.getYMasterCoord(i));

                    gcpElem.setAttributeDouble("slv_x", warpData.getXSlaveCoord(i));
                    gcpElem.setAttributeDouble("slv_y", warpData.getYSlaveCoord(i));

                    if (warpData.isValid()) {
                        gcpElem.setAttributeDouble("rms", warpData.getRMS(i));
                    }
                }
            }

            warpDataElem.setAttributeDouble("rmsStd", warpData.getRMSStd());
            warpDataElem.setAttributeDouble("rmsMean", warpData.getRMSMean());
            warpDataElem.setAttributeDouble("rowResidualStd", warpData.getRowResidualStd());
            warpDataElem.setAttributeDouble("rowResidualMean", warpData.getRowResidualMean());
            warpDataElem.setAttributeDouble("colResidualStd", warpData.getColResidualStd());
            warpDataElem.setAttributeDouble("colResidualMean", warpData.getColResidualMean());
        }
    }

    private void constructInterpolationTable(String interpolationMethod) {

        // construct interpolation LUT
        SimpleLUT lut = new SimpleLUT(interpolationMethod);
        lut.constructLUT();

        int kernelLength = lut.getKernelLength();

        // get LUT and cast it to float for JAI
        double[] lutArrayDoubles = lut.getKernelAsArray();
        float lutArrayFloats[] = new float[lutArrayDoubles.length];
        int i = 0;
        for (double lutElement : lutArrayDoubles) {
            lutArrayFloats[i++] = (float) lutElement;
        }

        // construct interpolation table for JAI resampling
        final int subsampleBits = 7;
        final int precisionBits = 32;
        int padding = kernelLength / 2 - 1;

        interpTable = new InterpolationTable(padding, kernelLength, subsampleBits, precisionBits, lutArrayFloats);
    }

    private static File getResidualsFile(final Product sourceProduct) {
        final String fileName = sourceProduct.getName() + "_residual.txt";
        return new File(ResourceUtils.getReportFolder(), fileName);
    }

    private void announceGCPWarning() {
        String msg = "";
        for (Band srcBand : sourceProduct.getBands()) {
            final PolynomialModel warpData = warpDataMap.get(srcBand);
            if (warpData != null && !warpData.isValid()) {
                msg += srcBand.getName() + " does not have enough valid GCPs for the warp\n";
                openResidualsFile = true;
            }
        }
        if (!msg.isEmpty()) {
            SystemUtils.LOG.warning(msg);
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
            super(WarpOp.class);
        }
    }

}
