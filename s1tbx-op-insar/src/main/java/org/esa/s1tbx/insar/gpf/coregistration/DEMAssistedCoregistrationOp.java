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
import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.s1tbx.insar.gpf.support.SARPosition;
import org.esa.s1tbx.commons.SARUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.ProductInformation;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.delaunay.TriangleInterpolator;

import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * Co-registering non-burst products based on orbits and DEM.
 */
@OperatorMetadata(alias = "DEM-Assisted-Coregistration",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Orbit and DEM based co-registration")
public final class DEMAssistedCoregistrationOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME;

    @Parameter(label = "Tile Extension [%]", description = "Define tile extension percentage.", interval = "[0, *)",
            defaultValue = "50")
    private int tileExtensionPercent = 50;

    @Parameter(defaultValue = "true", label = "Mask out areas with no elevation")
    private boolean maskOutAreaWithoutElevation = true;

    @Parameter(defaultValue = "false", label = "Output Range and Azimuth Offset")
    private boolean outputRangeAzimuthOffset = false;

    private Resampling selectedResampling;
    private Product masterProduct;
    private Product[] slaveProducts;
    private Metadata mstMetadata = new Metadata();
    private Metadata[] slvMetadatas;
    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0; // no data value for DEM
    private double noDataValue = 0.0;
    private GeoCoding targetGeoCoding = null;
    private final HashMap<Band, Band> slaveBandToTargetBandMap = new HashMap<>(2);
    private static final double invalidIndex = -9999.0;

    private static final String PRODUCT_SUFFIX = "_Stack";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DEMAssistedCoregistrationOp() {
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
            if (sourceProduct == null) {
                return;
            }

            if (sourceProduct.length < 2) {
                throw new OperatorException("Please select two or more source products");
            }

            if (checkIfS1SLCProduct()) {
                throw new OperatorException("For coregistration of S-1 TOPS SLC products, please use S-1 Back Geocoding");
            }

            masterProduct = sourceProduct[0];
            slaveProducts = new Product[sourceProduct.length-1];
            System.arraycopy(sourceProduct, 1, slaveProducts, 0, slaveProducts.length);

            getProductMetadata(masterProduct, mstMetadata);
            slvMetadatas = new Metadata[slaveProducts.length];
            int i=0;
            for(Product slaveProduct : slaveProducts) {
                slvMetadatas[i] = new Metadata();
                getProductMetadata(slaveProduct, slvMetadatas[i]);
                ++i;
            }

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);
            if(selectedResampling == null) {
                throw new OperatorException("Resampling method "+ resamplingType + " is invalid");
            }

            createTargetProduct();

            updateTargetProductMetadata();

            noDataValue = masterProduct.getBandAt(0).getNoDataValue();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private boolean checkIfS1SLCProduct() throws OperatorException {
        final MetadataElement mAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        if (mAbsRoot == null) {
            throw new OperatorException("Cannot find the abstracted metadata for product "+ sourceProduct[0].getName());
        }
        String mission = mAbsRoot.getAttributeString(AbstractMetadata.MISSION);
        String productType = mAbsRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE, "");
        return mission != null && productType != null && mission.startsWith("SENTINEL-1") && productType.equals("SLC");
    }

    private static void getProductMetadata(final Product sourceProduct, final Metadata metadata) throws Exception {

        metadata.product = sourceProduct;

        metadata.sourceImageWidth = sourceProduct.getSceneRasterWidth();
        metadata.sourceImageHeight = sourceProduct.getSceneRasterHeight();

        metadata.absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        metadata.srgrFlag = AbstractMetadata.getAttributeBoolean(metadata.absRoot, AbstractMetadata.srgr_flag);

        metadata.wavelength = SARUtils.getRadarWavelength(metadata.absRoot);

        metadata.rangeSpacing = AbstractMetadata.getAttributeDouble(
                metadata.absRoot, AbstractMetadata.range_spacing);

        metadata.azimuthSpacing = AbstractMetadata.getAttributeDouble(
                metadata.absRoot, AbstractMetadata.azimuth_spacing);

        metadata.firstLineTime = AbstractMetadata.parseUTC(
                metadata.absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        metadata.lastLineTime = AbstractMetadata.parseUTC(
                metadata.absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days

        metadata.lineTimeInterval = metadata.absRoot.getAttributeDouble(
                AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day

        OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(
                metadata.absRoot);

        metadata.orbit = new OrbitStateVectors(
                orbitStateVectors, metadata.firstLineTime, metadata.lineTimeInterval, metadata.sourceImageHeight);


        if (metadata.srgrFlag) {
            metadata.srgrConvParams = AbstractMetadata.getSRGRCoefficients(metadata.absRoot);
        } else {
            metadata.nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(
                    metadata.absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        metadata.nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, metadata.sourceImageWidth);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(
                OperatorUtils.createProductName(masterProduct.getName(), PRODUCT_SUFFIX),
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(masterProduct, targetProduct);

        final java.util.List<String> masterProductBands = new ArrayList<>(masterProduct.getNumBands());
        final String[] masterBandNames = masterProduct.getBandNames();
        final String mstSuffix = StackUtils.MST + StackUtils.createBandTimeStamp(masterProduct);
        for (String bandName : masterBandNames) {
            if (masterProduct.getBand(bandName) instanceof VirtualBand) {
                continue;
            }

            if(targetProduct.getBand(bandName + mstSuffix) != null) {
                continue;
            }

            final Band targetBand = ProductUtils.copyBand(
                    bandName, masterProduct, bandName + mstSuffix, targetProduct, true);

            masterProductBands.add(targetBand.getName());

            if(targetBand != null && Unit.IMAGINARY.equals(targetBand.getUnit())) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx - 1), targetBand, mstSuffix);
            }
        }

        final Band masterBand = masterProduct.getBand(masterBandNames[0]);
        final int masterBandWidth = masterBand.getRasterWidth();
        final int masterBandHeight = masterBand.getRasterHeight();

        final HashMap<Band, Band> targetBandToSlaveBandMap = new HashMap<>(2);
        int k = 0;
        for(Product slaveProduct : slaveProducts) {
            k++;
            final String[] slaveBandNames = slaveProduct.getBandNames();
            final String slvSuffix = StackUtils.SLV + k + StackUtils.createBandTimeStamp(slaveProduct);
            for (String bandName : slaveBandNames) {
                final Band srcBand = slaveProduct.getBand(bandName);
                if (srcBand instanceof VirtualBand) {
                    continue;
                }

                if (targetProduct.getBand(bandName + slvSuffix) != null) {
                    continue;
                }

                final Band targetBand = new Band(
                        bandName + slvSuffix,
                        ProductData.TYPE_FLOAT32,
                        masterBandWidth,
                        masterBandHeight);

                targetBand.setUnit(srcBand.getUnit());
                targetBand.setDescription(srcBand.getDescription());
                targetProduct.addBand(targetBand);
                targetBandToSlaveBandMap.put(targetBand, srcBand);
                slaveBandToTargetBandMap.put(srcBand, targetBand);

                if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                    int idx = targetProduct.getBandIndex(targetBand.getName());
                    ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx - 1), targetBand, slvSuffix);
                }
            }

            copySlaveMetadata(slaveProduct);
        }

        StackUtils.saveMasterProductBandNames(
                targetProduct, masterProductBands.toArray(new String[masterProductBands.size()]));

        StackUtils.saveSlaveProductNames(sourceProduct, targetProduct,
                masterProduct, targetBandToSlaveBandMap);

        // set non-elevation areas to no data value for the master bands using the slave bands
        setMasterValidPixelExpression(targetProduct, maskOutAreaWithoutElevation);

        if (outputRangeAzimuthOffset) {
            final Band azOffsetBand = new Band(
                    "azOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            azOffsetBand.setUnit("Index");
            targetProduct.addBand(azOffsetBand);

            final Band rgOffsetBand = new Band(
                    "rgOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            rgOffsetBand.setUnit("Index");
            targetProduct.addBand(rgOffsetBand);
        }

        targetGeoCoding = targetProduct.getSceneGeoCoding();
    }

    public static void setMasterValidPixelExpression(final Product targetProduct,
                                                     final boolean maskOutAreaWithoutElevation) {
        if(maskOutAreaWithoutElevation) {
            Band slvBand = null;
            for(Band tgtBand : targetProduct.getBands()) {
                if(StackUtils.isSlaveBand(tgtBand.getName(), targetProduct)) {
                    slvBand = tgtBand;
                    break;
                }
            }

            if(slvBand != null) {
                for (Band tgtBand : targetProduct.getBands()) {
                    if (StackUtils.isMasterBand(tgtBand.getName(), targetProduct)) {
                        tgtBand.setValidPixelExpression(slvBand.getName());
                    }
                }
            }
        }
    }

    private void copySlaveMetadata(final Product slaveProduct) {

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(
                targetProduct.getMetadataRoot());
        final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(slaveProduct);
        if (slvAbsMetadata != null) {
            final String timeStamp = StackUtils.createBandTimeStamp(slaveProduct);
            final MetadataElement targetSlaveMetadata = new MetadataElement(slaveProduct.getName() + timeStamp);
            targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
            ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
        }
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement abstractedMetadata = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(abstractedMetadata != null) {
            abstractedMetadata.setAttributeInt("collocated_stack", 1);
        }

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);
        for(Product slaveProduct : slaveProducts) {
            final MetadataElement slvInputElem = ProductInformation.getInputProducts(slaveProduct);
            final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
            for (MetadataAttribute attrib : slvInputProductAttrbList) {
                final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(
                        inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
                inputAttrb.getData().setElems(attrib.getData().getElemString());
            }
        }

        CreateStackOp.getBaselines(sourceProduct, targetProduct);
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
     public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
             throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            for(Metadata slaveMetadata : slvMetadatas) {
                final PixelPos[][] slavePixPos = computeSlavePixPos(tx0, ty0, tw, th, slaveMetadata);

                if (slavePixPos == null) {
                    return;
                }

                if (outputRangeAzimuthOffset) {
                    outputRangeAzimuthOffsets(tx0, ty0, tw, th, targetTileMap, slavePixPos);
                }

                final int margin = selectedResampling.getKernelSize();
                final Rectangle sourceRectangle = getBoundingBox(slaveMetadata, slavePixPos, margin);

                if (sourceRectangle == null) {
                    return;
                }

                performInterpolation(tx0, ty0, tw, th, sourceRectangle, targetTileMap, slavePixPos, slaveMetadata);
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get elevation model.
     *
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) return;
        try {
            if (externalDEMFile != null) { // if external DEM file is specified by user
                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                demNoDataValue = externalDEMNoDataValue;
                demName = externalDEMFile.getPath();
            } else {
                dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
                demNoDataValue = dem.getDescriptor().getNoDataValue();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        isElevationModelAvailable = true;
    }

    private PixelPos[][] computeSlavePixPos(final int x0, final int y0, final int w, final int h, Metadata slvMetadata)
            throws Exception {

        try {
            // Compute lat/lon boundaries for target tile
            final double[] latLonMinMax = new double[4];
            computeImageGeoBoundary(x0, x0 + w, y0, y0 + h, latLonMinMax);

            final double extralat = (latLonMinMax[1] - latLonMinMax[0]) * tileExtensionPercent / 100.0;
            final double extralon = (latLonMinMax[3] - latLonMinMax[2]) * tileExtensionPercent / 100.0;

            final double latMin = latLonMinMax[0] - extralat;
            final double latMax = latLonMinMax[1] + extralat;
            final double lonMin = latLonMinMax[2] - extralon;
            final double lonMax = latLonMinMax[3] + extralon;

            // Compute lat/lon indices in DEM for the boundaries;
            final PixelPos upperLeft = dem.getIndex(new GeoPos(latMax, lonMin));
            final PixelPos lowerRight = dem.getIndex(new GeoPos(latMin, lonMax));
            final int latMaxIdx = (int)Math.floor(upperLeft.getY());
            final int latMinIdx = (int)Math.ceil(lowerRight.getY());
            final int lonMinIdx = (int)Math.floor(upperLeft.getX());
            final int lonMaxIdx = (int)Math.ceil(lowerRight.getX());

            final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

            // Loop through all DEM points bounded by the indices computed above. For each point,
            // get its lat/lon and its azimuth/range indices in target image;
            final int numLines = latMinIdx - latMaxIdx;
            final int numPixels = lonMaxIdx - lonMinIdx;
            final double[][] masterAz = new double[numLines][numPixels];
            final double[][] masterRg = new double[numLines][numPixels];
            final double[][] slaveAz = new double[numLines][numPixels];
            final double[][] slaveRg = new double[numLines][numPixels];
            final double[][] lat = new double[numLines][numPixels];
            final double[][] lon = new double[numLines][numPixels];

            final SARPosition mstSARPosition = new SARPosition(
                    mstMetadata.firstLineTime,
                    mstMetadata.lastLineTime,
                    mstMetadata.lineTimeInterval,
                    mstMetadata.wavelength,
                    mstMetadata.rangeSpacing,
                    mstMetadata.sourceImageWidth,
                    mstMetadata.srgrFlag,
                    mstMetadata.nearEdgeSlantRange,
                    mstMetadata.nearRangeOnLeft,
                    mstMetadata.orbit,
                    mstMetadata.srgrConvParams
            );
            final SARPosition slvSARPosition = new SARPosition(
                    slvMetadata.firstLineTime,
                    slvMetadata.lastLineTime,
                    slvMetadata.lineTimeInterval,
                    slvMetadata.wavelength,
                    slvMetadata.rangeSpacing,
                    slvMetadata.sourceImageWidth,
                    slvMetadata.srgrFlag,
                    slvMetadata.nearEdgeSlantRange,
                    slvMetadata.nearRangeOnLeft,
                    slvMetadata.orbit,
                    slvMetadata.srgrConvParams
            );
            final SARPosition.PositionData posData = new SARPosition.PositionData();
            final PixelPos pix = new PixelPos();

            boolean noValidSlavePixPos = true;
            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    pix.setLocation(lonMinIdx + p, latMaxIdx + l);
                    GeoPos gp = dem.getGeoPos(pix);
                    lat[l][p] = gp.lat;
                    lon[l][p] = gp.lon;
                    Double alt = dem.getElevation(gp);

                    if (alt.equals(demNoDataValue)) { // get corrected elevation for 0
                        alt = (double)egm.getEGM(gp.lat, gp.lon);
                    }

                    GeoUtils.geo2xyzWGS84(gp.lat, gp.lon, alt, posData.earthPoint);
                    if(mstSARPosition.getPosition(posData)) {

                        masterAz[l][p] = posData.azimuthIndex;
                        masterRg[l][p] = posData.rangeIndex;
                        if (slvSARPosition.getPosition(posData)) {

                            slaveAz[l][p] = posData.azimuthIndex;
                            slaveRg[l][p] = posData.rangeIndex;
                            noValidSlavePixPos = false;
                            continue;
                        }
                    }

                    masterAz[l][p] = invalidIndex;
                    masterRg[l][p] = invalidIndex;
                }
            }

            if (noValidSlavePixPos) {
                return null;
            }

            // Compute azimuth/range offsets for pixels in target tile using Delaunay interpolation
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, y0 + h - 1, x0, x0 + w - 1);

            final double rgAzRatio = mstMetadata.rangeSpacing / mstMetadata.azimuthSpacing;

            final double[][] latArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] lonArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] azArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] rgArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            for (double[] data : azArray) {
                Arrays.fill(data, invalidIndex);
            }
            for (double[] data : rgArray) {
                Arrays.fill(data, invalidIndex);
            }

            TriangleInterpolator.ZData[] dataList = new TriangleInterpolator.ZData[] {
                    new TriangleInterpolator.ZData(slaveAz, azArray),
                    new TriangleInterpolator.ZData(slaveRg, rgArray),
                    new TriangleInterpolator.ZData(lat, latArray),
                    new TriangleInterpolator.ZData(lon, lonArray)
            };

            TriangleInterpolator.gridDataLinear(masterAz, masterRg, dataList,
                                                tileWindow, rgAzRatio, 1, 1, invalidIndex, 0);

            boolean allElementsAreNull = true;
            final PixelPos[][] slavePixelPos = new PixelPos[h][w];

            Double alt;
            for(int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if (rgArray[yy][xx] == invalidIndex || azArray[yy][xx] == invalidIndex ||
                            rgArray[yy][xx] < 0 || rgArray[yy][xx] >= slvMetadata.sourceImageWidth ||
                            azArray[yy][xx] < 0 || azArray[yy][xx] >= slvMetadata.sourceImageHeight) {
                        slavePixelPos[yy][xx] = null;
                    } else {
                        if (maskOutAreaWithoutElevation) {
                            alt = dem.getElevation(new GeoPos(latArray[yy][xx], lonArray[yy][xx]));
                            if (!alt.equals(demNoDataValue)) {
                                slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                                allElementsAreNull = false;
                            } else {
                                slavePixelPos[yy][xx] = null;
                            }
                        } else {
                            slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                            allElementsAreNull = false;
                        }
                    }
                }
            }

            if (allElementsAreNull) {
                return null;
            }

            return slavePixelPos;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlavePixPos", e);
        }

        return null;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xMin, final int xMax, final int yMin, final int yMax,
                                         double[] latLonMinMax) throws Exception {

        final GeoPos geoPosFirstNear = targetGeoCoding.getGeoPos(new PixelPos(xMin, yMin), null);
        final GeoPos geoPosFirstFar = targetGeoCoding.getGeoPos(new PixelPos(xMax, yMin), null);
        final GeoPos geoPosLastNear = targetGeoCoding.getGeoPos(new PixelPos(xMin, yMax), null);
        final GeoPos geoPosLastFar = targetGeoCoding.getGeoPos(new PixelPos(xMax, yMax), null);

        final double[] lats = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
        final double[] lons = {geoPosFirstNear.getLon(), geoPosFirstFar.getLon(), geoPosLastNear.getLon(), geoPosLastFar.getLon()};
        double latMin = 90.0;
        double latMax = -90.0;
        for (double lat : lats) {
            if (lat < latMin) {
                latMin = lat;
            }
            if (lat > latMax) {
                latMax = lat;
            }
        }

        double lonMin = 180.0;
        double lonMax = -180.0;
        for (double lon : lons) {
            if (lon < lonMin) {
                lonMin = lon;
            }
            if (lon > lonMax) {
                lonMax = lon;
            }
        }

        latLonMinMax[0] = latMin;
        latLonMinMax[1] = latMax;
        latLonMinMax[2] = lonMin;
        latLonMinMax[3] = lonMax;
    }

    /**
     * Get the source rectangle in slave image that contains all the given pixels.
     */
    private Rectangle getBoundingBox(final Metadata slvMetadata,
            final PixelPos[][] slavePixPos, final int margin) {

        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (PixelPos[] slavePixPo : slavePixPos) {
            for (int j = 0; j < slavePixPos[0].length; j++) {
                if (slavePixPo[j] != null) {
                    final int x = (int) Math.floor(slavePixPo[j].getX());
                    final int y = (int) Math.floor(slavePixPo[j].getY());

                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        minX = Math.max(minX - margin, 0);
        maxX = Math.min(maxX + margin, slvMetadata.sourceImageWidth - 1);
        minY = Math.max(minY - margin, 0);
        maxY = Math.min(maxY + margin, slvMetadata.sourceImageHeight - 1);

        if (minX > maxX || minY > maxY) {
            return null;
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void performInterpolation(final int x0, final int y0, final int w, final int h,
                                      final Rectangle sourceRectangle, final Map<Band, Tile> targetTileMap,
                                      final PixelPos[][] slavePixPos, final Metadata slvMetadata) {

        try {
            final Product slaveProduct = slvMetadata.product;
            final Band[] slaveBands = getNonVirtualBands(slaveProduct);
            final int numSlvBands = slaveBands.length;

            Tile targetTile = null;
            final ProductData[] targetBuffers = new ProductData[numSlvBands];
            final ResamplingRaster[] slvResamplingRasters = new ResamplingRaster[numSlvBands];

            int k = 0;
            for (Band slvBand : slaveBands) {
                final Band targetBand = slaveBandToTargetBandMap.get(slvBand);
                targetTile = targetTileMap.get(targetBand);
                targetBuffers[k] = targetTile.getDataBuffer();

                final Tile slvTile = getSourceTile(slvBand, sourceRectangle);
                final ProductData slvBuffer = slvTile.getDataBuffer();
                slvResamplingRasters[k] = new ResamplingRaster(slvTile, slvBuffer);
                k++;
            }

            final TileIndex tgtIndex = new TileIndex(targetTile);

            final Resampling.Index resamplingIndex = selectedResampling.createIndex();

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final PixelPos slavePixelPos = slavePixPos[yy][x - x0];

                    if (slavePixelPos == null) {
                        continue;
                    }

                    selectedResampling.computeCornerBasedIndex(slavePixelPos.x, slavePixelPos.y,
                            slvMetadata.sourceImageWidth, slvMetadata.sourceImageHeight, resamplingIndex);

                    saveToTargetBand(resamplingIndex, slvResamplingRasters, targetBuffers, tgtIdx);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("performInterpolation", e);
        }
    }

    private Band[] getNonVirtualBands(final Product product) {
        final java.util.List<Band> bandList = new ArrayList<>(product.getNumBands());
        for (Band band : product.getBands()) {
            if (!(band instanceof VirtualBand)) {
                bandList.add(band);
            }
        }
        return bandList.toArray(new Band[0]);
    }

    private void saveToTargetBand(final Resampling.Index resamplingIndex, final ResamplingRaster[] slvResamplingRasters,
                                  final ProductData[] targetBuffers, final int tgtIdx) throws Exception {

        int i=0;
        for (ProductData targetBuffer : targetBuffers) {
            targetBuffer.setElemDoubleAt(tgtIdx, selectedResampling.resample(slvResamplingRasters[i], resamplingIndex));
            ++i;
        }
    }

    private void outputRangeAzimuthOffsets(final int x0, final int y0, final int w, final int h,
                                           final Map<Band, Tile> targetTileMap, final PixelPos[][] slavePixPos) {

        try {
            final Band azOffsetBand = targetProduct.getBand("azOffset");
            final Band rgOffsetBand = targetProduct.getBand("rgOffset");

            if (azOffsetBand == null || rgOffsetBand == null) {
                return;
            }

            final Tile tgtTileAzOffset = targetTileMap.get(azOffsetBand);
            final Tile tgtTileRgOffset = targetTileMap.get(rgOffsetBand);
            final ProductData tgtBufferAzOffset = tgtTileAzOffset.getDataBuffer();
            final ProductData tgtBufferRgOffset = tgtTileRgOffset.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileAzOffset);

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;

                    if (slavePixPos[yy][xx] == null) {
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                    } else {
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].y));
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].x));
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("outputRangeAzimuthOffsets", e);
        }
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final ProductData dataBuffer;

        private ResamplingRaster(final Tile tile, final ProductData dataBuffer) {
            this.tile = tile;
            this.dataBuffer = dataBuffer;
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;

            try {
                final TileIndex index = new TileIndex(tile);
                final int maxX = x.length;
                for (int i = 0; i < y.length; i++) {
                    index.calculateStride(y[i]);
                    for (int j = 0; j < maxX; j++) {
                        double v = dataBuffer.getElemDoubleAt(index.getIndex(x[j]));
                        samples[i][j] = v;
                    }
                }

            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
                allValid = false;
            }

            return allValid;
        }
    }

    private static class Metadata {
        public Product product = null;
        public MetadataElement absRoot = null;
        public OrbitStateVectors orbit = null;
        public double firstLineTime = 0.0;
        public double lastLineTime = 0.0; // in days
        public double lineTimeInterval = 0.0;
        public double wavelength = 0.0;
        public double rangeSpacing = 0.0;
        public double azimuthSpacing = 0.0;
        public double nearEdgeSlantRange = 0.0; // in m
        public boolean srgrFlag = false;
        public boolean nearRangeOnLeft = false;
        public int sourceImageWidth = 0;
        public int sourceImageHeight = 0;
        public AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
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
            super(DEMAssistedCoregistrationOp.class);
        }
    }
}
