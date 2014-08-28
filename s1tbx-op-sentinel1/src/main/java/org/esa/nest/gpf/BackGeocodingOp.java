/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.*;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.ProductInformation;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.nest.gpf.geometric.SARGeocoding;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.StackUtils;
import org.esa.snap.gpf.TileIndex;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;


import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * "Backgeocoding" + "Coregistration" processing blocks in The Sentinel-1 TOPS InSAR processing chain.
 * Burst co-registration is performed using orbits and DEM.
 */
@OperatorMetadata(alias = "Back-Geocoding",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Bursts co-registration using orbit and DEM", internal=true)
public final class BackGeocodingOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_INTERPOLATION_NAME},
            defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(valueSet = {ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    private Resampling selectedResampling = null;

    private Product masterProduct = null;
    private Product slaveProduct = null;

    private Sentinel1Utils mSU = null;
    private Sentinel1Utils sSU = null;
    private Sentinel1Utils.SubSwathInfo[] mSubSwath = null;
    private Sentinel1Utils.SubSwathInfo[] sSubSwath = null;
    private SARGeocoding.Orbit mOrbit = null;
    private SARGeocoding.Orbit sOrbit = null;

    private int numOfSubSwath = 0;
    private String acquisitionMode = null;
    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private float demNoDataValue = 0; // no data value for DEM

    private int polyDegree = 2; // degree of polynomial for orbit fitting
    private double noDataValue = -9999.0;


    private int targetWidth = 0;
    private int targetHeight = 0;

    private double targetFirstLineTime = 0;
    private double targetLastLineTime = 0;
    private double targetLineTimeInterval = 0;
    private double targetSlantRangeTimeToFirstPixel = 0;
    private double targetSlantRangeTimeToLastPixel = 0;
    private double targetDeltaSlantRangeTime = 0;

    private boolean absoluteCalibrationPerformed = false;
    private boolean inputSigmaBand = false;
    private boolean inputBetaBand = false;
    private boolean inputGammaBand = false;
    private boolean inputDNBand = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BackGeocodingOp() {
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
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            checkSourceProductValidity();

            masterProduct = sourceProduct[0];
            slaveProduct = sourceProduct[1];

            mSU = new Sentinel1Utils(masterProduct);
            sSU = new Sentinel1Utils(slaveProduct);

            mSubSwath = mSU.getSubSwath();
            sSubSwath = sSU.getSubSwath();
            numOfSubSwath = mSU.getNumOfSubSwath();

            mOrbit = mSU.getOrbit(polyDegree);
            sOrbit = sSU.getOrbit(polyDegree);

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() {

        if (sourceProduct.length != 2) {
            throw new OperatorException("Please select two source products");
        }

        MetadataElement mAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        MetadataElement sAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[1]);

        final String mMission = mAbsRoot.getAttributeString(AbstractMetadata.MISSION);
        final String sMission = sAbsRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mMission.equals("SENTINEL-1A") || !sMission.equals("SENTINEL-1A")) {
            throw new OperatorException("Source product has invalid mission for Sentinel1 product");
        }

        final String mProductType = mAbsRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        final String sProductType = sAbsRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!mProductType.equals("SLC") || !sProductType.equals("SLC")) {
            throw new OperatorException("Source product should be SLC product");
        }

        final String mAcquisitionMode = mAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        final String sAcquisitionMode = sAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        if (!mAcquisitionMode.equals(sAcquisitionMode)) {
            throw new OperatorException("Source products should have the same acquisition modes");
        }
        acquisitionMode = mAcquisitionMode;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(
                masterProduct.getName(),
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());
/*
        final String[] masterBandNames = masterProduct.getBandNames();
        String suffix = "_mst" + StackUtils.getBandTimeStamp(masterProduct);
        for (String bandName:masterBandNames) {
            ProductUtils.copyBand(bandName, masterProduct, bandName + suffix, targetProduct, true);
        }
*/
        final String[] slaveBandNames = slaveProduct.getBandNames();
//        suffix = "_slv" + StackUtils.getBandTimeStamp(slaveProduct);
        for (String bandName:slaveBandNames) {
            if (slaveProduct.getBand(bandName) instanceof VirtualBand) {
                continue;
            }
            final Band masterBand = masterProduct.getBand(bandName);
            final Band targetBand = new Band(
                    bandName + "_slv", //suffix,
                    ProductData.TYPE_FLOAT32,
                    masterBand.getSceneRasterWidth(),
                    masterBand.getSceneRasterHeight());

            targetBand.setUnit(masterBand.getUnit());
            targetProduct.addBand(targetBand);
        }

        ProductUtils.copyProductNodes(masterProduct, targetProduct);
        copySlaveMetadata();
    }

    private void copySlaveMetadata() {

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(slaveProduct);
        if (slvAbsMetadata != null) {
            final String timeStamp = StackUtils.getBandTimeStamp(slaveProduct);
            final MetadataElement targetSlaveMetadata = new MetadataElement(slaveProduct.getName() + timeStamp);
            targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
            ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
        }
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);
        final MetadataElement slvInputElem = ProductInformation.getInputProducts(slaveProduct);
        final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
        for (MetadataAttribute attrib : slvInputProductAttrbList) {
            final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(
                    inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
            inputAttrb.getData().setElems(attrib.getData().getElemString());
        }
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw = targetTileRectangle.width;
            final int th = targetTileRectangle.height;
            final int txMax = tx0 + tw;
            final int tyMax = ty0 + th;
            System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th +
                    ", targetBand = " + targetBand.getName());
/*
            final String targetBandName = targetBand.getName();
            final boolean isIBand = targetBandName.contains("i_");
            final boolean isHHBand = targetBandName.contains("HH");
            final int subswathIndex = getSubswathIndex(targetBandName);

            double value = 0.0;
            if (isIBand) {
                value += 100.0;
            } else {
                value += 800.0;
            }

            if (isHHBand) {
                value += 0.0;
            } else {
                value += 1.0;
            }

            value += subswathIndex*10.0;

            final ProductData dataBuffer = targetTile.getDataBuffer();
            final TileIndex trgIndex = new TileIndex(targetTile);

            for (int y = ty0; y < tyMax; ++y) {
                trgIndex.calculateStride(y);
                for (int x = tx0; x < txMax; ++x) {
                    dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)value);
                }
            }
*/
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            final String targetBandName = targetBand.getName();
            final int subswathIndex = getSubswathIndex(targetBandName);

            for (int b = 0; b < mSubSwath[subswathIndex - 1].numOfBursts; b++) {
                if (tx0 == 0 && ty0 == 0) {
                    System.out.println("Sub-Swath: " + (b+1));
                }
                final int firstLineIdx = b*mSubSwath[subswathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + mSubSwath[subswathIndex - 1].linesPerBurst - 1;
                if (tyMax < firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

                final int yMin = Math.max(ty0, firstLineIdx);
                final int yMax = Math.min(tyMax, lastLineIdx);
                final int h = yMax - yMin + 1;
                computePartialTile(subswathIndex, b, tx0, yMin, tw, h, targetBand, targetTile, pm);
            }
            System.out.println("Done: tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private int getSubswathIndex(final String targetBandName) {
        for (int i = 0; i < 5; i++) {
            if (targetBandName.contains(String.valueOf(i+1))){
                return (i+1);
            }
        }
        return -1;
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
                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, (float) externalDEMNoDataValue);
                demNoDataValue = (float) externalDEMNoDataValue;
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

    private void computePartialTile(final int subswathIndex, final int burstIndex, final int x0, final int y0,
                                    final int w, final int h, final Band targetBand, final Tile targetTile,
                                    ProgressMonitor pm) throws Exception {

        if (x0 == 0 && y0 == 0) {
            System.out.println("0: computePartialTile");
        }
        PixelPos[][] slavePixPos = null;
        try {
            slavePixPos = computeSlavePixPos(x0, y0, w, h, pm);
//        final PixelPos[][] slavePixPos = computeSlavePixPos(x0, y0, w, h, pm);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
        }
        if (x0 == 0 && y0 == 0) {
            System.out.println("1: computeSlavePixPos");
        }

        final int sourceRasterWidth = sSubSwath[subswathIndex - 1].numOfSamples;
        final int sourceRasterHeight = sSubSwath[subswathIndex - 1].numOfLines;
        final Rectangle sourceRectangle = getBoundingBox(slavePixPos, sourceRasterWidth, sourceRasterHeight);
        if (x0 == 0 && y0 == 0) {
            System.out.println("2: getBoundingBox");
        }

        if (sourceRectangle == null) {
            return;
        }

        double[][] derampDemodPhase = null;
        try {
            derampDemodPhase = computeDerampDemodPhase(subswathIndex, burstIndex, sourceRectangle);
//        final double[][] derampDemodPhase = computeDerampDemodPhase(subswathIndex, burstIndex, sourceRectangle);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
        }
        if (x0 == 0 && y0 == 0) {
            System.out.println("3: computeDerampDemodPhase");
        }

        final String targetBandName = targetBand.getName();
        final String pol = getPolarization(targetBandName);
        final Band slaveBandI = getSlaveBand("i_", subswathIndex, pol);
        final Band slaveBandQ = getSlaveBand("q_", subswathIndex, pol);
        final Tile slaveTileI = getSourceTile(slaveBandI, sourceRectangle);
        final Tile slaveTileQ = getSourceTile(slaveBandQ, sourceRectangle);
        if (slaveTileI == null || slaveTileQ == null) {
            return;
        }
        final ProductData slaveDataI = slaveTileI.getDataBuffer();
        final ProductData slaveDataQ = slaveTileQ.getDataBuffer();
        final double[][] derampDemodI = new double[sourceRectangle.height][sourceRectangle.width];
        final double[][] derampDemodQ = new double[sourceRectangle.height][sourceRectangle.width];

        try {
        performDerampDemod(
                slaveTileI, sourceRectangle, slaveDataI, slaveDataQ, derampDemodPhase, derampDemodI, derampDemodQ);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
        }
        if (x0 == 0 && y0 == 0) {
            System.out.println("4: performDerampDemod");
        }

        final Resampling.Index resamplingIndex = selectedResampling.createIndex();
        final ResamplingRaster resamplingRasterI = new ResamplingRaster(slaveTileI, sourceRectangle, derampDemodI);
        final ResamplingRaster resamplingRasterQ = new ResamplingRaster(slaveTileQ, sourceRectangle, derampDemodQ);
        final ResamplingRaster resamplingRasterPhase = new ResamplingRaster(slaveTileI, sourceRectangle, derampDemodPhase);

        final ProductData tgtBuffer = targetTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(targetTile);
        final boolean isIBand = targetBandName.contains("i_");

        try {
        for (int y = y0; y < y0 + h; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < x0 + w; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);

                final PixelPos slavePixelPos = slavePixPos[yy][x - x0];
                if (isSlavePixPosValid(slavePixelPos, subswathIndex)) {
                    try {
                        selectedResampling.computeIndex(slavePixelPos.x, slavePixelPos.y,
                                sourceRasterWidth, sourceRasterHeight, resamplingIndex);

                        final double sampleI = selectedResampling.resample(resamplingRasterI, resamplingIndex);
                        final double sampleQ = selectedResampling.resample(resamplingRasterQ, resamplingIndex);
                        final double samplePhase = selectedResampling.resample(resamplingRasterPhase, resamplingIndex);
                        final double cosPhase = Math.cos(samplePhase);
                        final double sinPhase = Math.sin(samplePhase);
                        final double rerampRemodI = sampleI*cosPhase + sampleQ*sinPhase;
                        final double rerampRemodQ = -sampleI*sinPhase + sampleQ*cosPhase;

                        double sample;
                        if (isIBand) {
                            sample = rerampRemodI;
                        } else {
                            sample = rerampRemodQ;
                        }

                        if (Double.isNaN(sample)) {
                            sample = noDataValue;
                        }

                        tgtBuffer.setElemDoubleAt(tgtIdx, sample);

                    } catch (Exception e) {
                        throw new OperatorException(e.getMessage());
                    }
                } else {
                    tgtBuffer.setElemDoubleAt(tgtIdx, noDataValue);
                }
            }
        }
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
            // todo got exception 303920 = 524*580 here for the first tile
        }
        if (x0 == 0 && y0 == 0) {
            System.out.println("5: Resampling");
        }

    }

    private PixelPos[][] computeSlavePixPos(final int x0, final int y0, final int w, final int h, ProgressMonitor pm)
            throws Exception {

        try {
            if (x0 == 0 && y0 == 0) {
                System.out.println("00: computeSlavePixPos");
            }
            // Compute lat/lon boundaries (with extensions) for target tile;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            final double[] latLonMinMax = new double[4];
            computeImageGeoBoundary(x0, xMax, y0, yMax, masterProduct, latLonMinMax);
            if (x0 == 0 && y0 == 0) {
                System.out.println("01: computeSlavePixPos");
            }

            final double delta = (double)dem.getDescriptor().getDegreeRes() / (double)dem.getDescriptor().getPixelRes();
            final double extralat = 1.5*delta + 4.0/25.0;
            final double extralon = 1.5*delta + 4.0/25.0;
            final double latMin = latLonMinMax[0] - extralat;
            final double latMax = latLonMinMax[1] + extralat;
            final double lonMin = latLonMinMax[2] - extralon;
            final double lonMax = latLonMinMax[3] + extralon;

            // Compute lat/lon indices in DEM for the boundaries;
            final PixelPos upperLeft = dem.getIndex(new GeoPos((float)latMax, (float)lonMin));
            final PixelPos lowerRight = dem.getIndex(new GeoPos((float)latMin, (float)lonMax));
            final int latMaxIdx = (int)Math.floor(upperLeft.getY());
            final int latMinIdx = (int)Math.ceil(lowerRight.getY());
            final int lonMinIdx = (int)Math.floor(upperLeft.getX());
            final int lonMaxIdx = (int)Math.ceil(lowerRight.getX());

            // Loop through all DEM points bounded by the indices computed above. For each point,
            // get its lat/lon and its azimuth/range indices in target image;
            final int numLines = latMinIdx - latMaxIdx + 1;
            final int numPixels = lonMaxIdx - lonMinIdx;
            double[][] azIn = new double[numLines][numPixels];
            double[][] rgIn = new double[numLines][numPixels];
            double[][] slaveAz = new double[numLines][numPixels];
            double[][] slaveRg = new double[numLines][numPixels];
            final PositionData masterPosData = new PositionData();
            final PositionData slavePosData = new PositionData();

            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    GeoPos gp = dem.getGeoPos(new PixelPos(lonMinIdx + p, latMaxIdx + l));
                    final double lat = gp.lat;
                    final double lon = gp.lon;
                    final double alt = dem.getElevation(gp);

                    if (alt == demNoDataValue || !getPosition(lat, lon, alt, mSU, masterPosData) ||
                            !getPosition(lat, lon, alt, sSU, slavePosData)) {
                        azIn[l][p] = noDataValue;
                        rgIn[l][p] = noDataValue;
                        continue;
                    }

                    azIn[l][p] = masterPosData.azimuthIndex;
                    rgIn[l][p] = masterPosData.rangeIndex;
                    slaveAz[l][p] = slavePosData.azimuthIndex;
                    slaveRg[l][p] = slavePosData.rangeIndex;
                }
            }
            if (x0 == 0 && y0 == 0) {
                System.out.println("02: computeSlavePixPos");
            }

            // Compute azimuth/range offsets for pixels in target tile using Delaunay interpolation;
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, yMax-1, x0, xMax-1);

            //final double rgAzRatio = computeRangeAzimuthSpacingRatio(w, h, latLonMinMax);
            final double rgAzRatio = mSU.rangeSpacing / mSU.azimuthSpacing;

            final double[][] azArray = TriangleUtils.gridDataLinear(
                    azIn, rgIn, slaveAz, tileWindow, rgAzRatio, 1, 1, noDataValue, 0);

            final double[][] rgArray = TriangleUtils.gridDataLinear(
                    azIn, rgIn, slaveRg, tileWindow, rgAzRatio, 1, 1, noDataValue, 0);
            if (x0 == 0 && y0 == 0) {
                System.out.println("03: computeSlavePixPos");
            }

            final PixelPos[][] slavePixelPos = new PixelPos[h][w];
            for(int l = 0; l < h; l++) {
                for (int p = 0; p < w; p++) {
                    slavePixelPos[l][p] = new PixelPos((float)rgArray[l][p], (float)azArray[l][p]);
                }
            }
            if (x0 == 0 && y0 == 0) {
                System.out.println("04: computeSlavePixPos");
            }

            return slavePixelPos;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlaveCoord", e);
        }

        return null;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xmin, final int xmax, final int ymin, final int ymax,
                                         final Product sourceProduct, double[] latLonMinMax) throws Exception {

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("Product does not contain a geocoding");
        }
        final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(xmin, ymin), null);
        final GeoPos geoPosFirstFar = geoCoding.getGeoPos(new PixelPos(xmax, ymin), null);
        final GeoPos geoPosLastNear = geoCoding.getGeoPos(new PixelPos(xmin, ymax), null);
        final GeoPos geoPosLastFar = geoCoding.getGeoPos(new PixelPos(xmax, ymax), null);

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

    private boolean getPosition(final double lat, final double lon, final double alt,
                                final Sentinel1Utils su, final PositionData data) {

        SARGeocoding.Orbit orbit = su.getOrbit(polyDegree);

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

        final double zeroDopplerTime = SARGeocoding.getZeroDopplerTime(su.firstLineUTC,
                su.lineTimeInterval, su.wavelength, data.earthPoint, orbit);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.azimuthTime = zeroDopplerTime * Constants.secondsInDay; // day to s
        data.azimuthIndex = (zeroDopplerTime - su.firstLineUTC) / su.lineTimeInterval;

        final double slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime - su.firstLineUTC,
                orbit.xPosCoeff, orbit.yPosCoeff, orbit.zPosCoeff, data.earthPoint, data.sensorPos);

        data.slantRangeTime = slantRange / Constants.lightSpeed; // in s

        if (!su.srgrFlag) {
            data.rangeIndex = (slantRange - su.nearEdgeSlantRange) / su.rangeSpacing;
        } else {
            data.rangeIndex = SARGeocoding.computeRangeIndex(
                    su.srgrFlag, su.sourceImageWidth, su.firstLineUTC, su.lastLineUTC,
                    su.rangeSpacing, zeroDopplerTime, slantRange, su.nearEdgeSlantRange, su.srgrConvParams);
        }

        if (!su.nearRangeOnLeft) {
            data.rangeIndex = su.sourceImageWidth - 1 - data.rangeIndex;
        }
        return true;
    }

    private static Rectangle getBoundingBox(final PixelPos[][] slavePixPos, final int maxWidth, final int maxHeight) {

        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (int i = 0; i < slavePixPos.length; i++) {
            for (int j = 0; j < slavePixPos[0].length; j++) {
                final PixelPos pixelsPos = slavePixPos[i][j];
                if (pixelsPos != null) {
                    final int x = (int) Math.floor(pixelsPos.getX());
                    final int y = (int) Math.floor(pixelsPos.getY());

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

        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 4, 0);
        maxX = Math.min(maxX + 4, maxWidth - 1);
        minY = Math.max(minY - 4, 0);
        maxY = Math.min(maxY + 4, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private double[][] computeDerampDemodPhase(
            final int subswathIndex, final int burstIndex, final Rectangle rectangle) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        final int s = subswathIndex - 1;

        final double[][] phase = new double[h][w];
        for (int y = y0; y < yMax; y++) {
            final int yy = y - y0;
            final double ta = y * sSubSwath[s].azimuthTimeInterval;
            for (int x = x0; x < xMax; x++) {
                final int xx = x - x0;
                final double kt = sSubSwath[s].dopplerRate[burstIndex][x];
                final double deramp = -Math.PI * kt * (ta - sSubSwath[s].referenceTime[burstIndex][x]);
                final double demod = -2 * Math.PI * sSubSwath[s].dopplerCentroid[burstIndex][x] * ta;
                phase[yy][xx] = deramp + demod;
            }
        }

        return phase;
    }

    private void performDerampDemod(final Tile slaveTile, final Rectangle slaveRectangle,
                                    final ProductData slaveDataI, final ProductData slaveDataQ,
                                    final double[][] derampDemodPhase, final double[][] derampDemodI,
                                    final double[][] derampDemodQ) {

        final int x0 = slaveRectangle.x;
        final int y0 = slaveRectangle.y;
        final int xMax = x0 + slaveRectangle.width;
        final int yMax = y0 + slaveRectangle.height;
        final TileIndex slvIndex = new TileIndex(slaveTile);

        for (int y = y0; y < yMax; y++) {
            slvIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int idx = slvIndex.getIndex(x);
                final int xx = x - x0;
                final double valueI = slaveDataI.getElemDoubleAt(idx);
                final double valueQ = slaveDataQ.getElemDoubleAt(idx);
                final double cosPhase = Math.cos(derampDemodPhase[yy][xx]);
                final double sinPhase = Math.sin(derampDemodPhase[yy][xx]);
                derampDemodI[yy][xx] = valueI*cosPhase - valueQ*sinPhase;
                derampDemodQ[yy][xx] = valueI*sinPhase + valueQ*cosPhase;
            }
        }
    }

    private String getPolarization(final String bandName) {
        if (bandName.contains("HH")) {
            return "HH";
        } else if (bandName.contains("HV")) {
            return "HV";
        } else if (bandName.contains("VV")) {
            return "VV";
        } else if (bandName.contains("VH")) {
            return "VH";
        } else {
            throw new OperatorException("Unknown polarization in target band " + bandName);
        }
    }

    private Band getSlaveBand(final String prefix, final int subswathIndex, final String polarization) {

        final String[] slaveBandNames = slaveProduct.getBandNames();
        for (String bandName:slaveBandNames) {
            if (bandName.contains(prefix) &&
                    bandName.contains(String.valueOf(subswathIndex)) &&
                    bandName.contains(polarization)) {
                return slaveProduct.getBand(bandName);
            }
        }
        return null;
    }

    private boolean isSlavePixPosValid(final PixelPos slavePixPos, final int subswathIndex) {
        return ((slavePixPos != null &&
                slavePixPos.x >= 0 && slavePixPos.x < sSubSwath[subswathIndex - 1].numOfSamples &&
                slavePixPos.y >= 0 && slavePixPos.y < sSubSwath[subswathIndex - 1].numOfLines));
    }


    private static class PositionData {
        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];
        double azimuthIndex;
        double rangeIndex;
        double azimuthTime;
        double slantRangeTime;
    }

    private static class Index {
        public int i0;
        public int i1;
        public int j0;
        public int j1;
        public double muX;
        public double muY;

        public Index() {
        }
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final int x0;
        private final int y0;
        private final Tile tile;
        private final double[][] data;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;

        public ResamplingRaster(final Tile tile, final Rectangle rectangle, final double[][] data) {
            this.x0 = rectangle.x;
            this.y0 = rectangle.y;
            this.tile = tile;
            this.data = data;
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;
            for (int i = 0; i < y.length; i++) {
                final int yy = y[i] - y0;
                for (int j = 0; j < x.length; j++) {

                    samples[i][j] = data[yy][x[j] - x0];

                    if (usesNoData) {
                        if (scalingApplied && geophysicalNoDataValue == samples[i][j] || noDataValue == samples[i][j]) {
                            samples[i][j] = Double.NaN;
                            allValid = false;
                        }
                    }
                }
            }
            return allValid;
        }
    }

    private static class TriangleUtils {

        public static double[][] gridDataLinear(final double[][] x_in, final double[][] y_in, final double[][] z_in,
                                                final org.jlinda.core.Window window, final double xyRatio, final int xScale,
                                                final int yScale, final double nodata, final int offset) throws Exception {

            FastDelaunayTriangulator FDT = null;
            try {
//                final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, z_in, xyRatio, nodata);
                FDT = triangulate(x_in, y_in, z_in, xyRatio, nodata);
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException("FastDelaunayTriangulator", e);
            }
            double[][] tmp = null;
            try {
                tmp = interpolate(xyRatio, window, xScale, yScale, offset, nodata, FDT);
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException("interpolate", e);
            }
            return tmp;
            //return interpolate(xyRatio, window, xScale, yScale, offset, nodata, FDT);

        }

        private static FastDelaunayTriangulator triangulate(
                final double[][] x_in, final double[][] y_in, final double[][] z_in, final double xyRatio,
                final double nodata) throws Exception {

            java.util.List<Geometry> list = new ArrayList<Geometry>();
            GeometryFactory gf = new GeometryFactory();
            for (int i = 0; i < x_in.length; i++) {
                for (int j = 0; j < x_in[0].length; j++) {
                    if (x_in[i][j] == nodata || y_in[i][j] == nodata) {
                        continue;
                    }
                    list.add(gf.createPoint(new Coordinate(x_in[i][j], y_in[i][j] * xyRatio, z_in[i][j])));
                }
            }

            FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
            try {
                FDT.triangulate(list.iterator());
            } catch (TriangulationException te) {
                te.printStackTrace();
            }

            return FDT;
        }

        private static double[][] interpolate(double xyRatio, final org.jlinda.core.Window tileWindow,
                                              final double xScale, final double yScale,
                                              final double offset, final double nodata,
                                              FastDelaunayTriangulator FDT) {

            final int zLoops = 1;
            final double x_min = tileWindow.linelo;
            final double y_min = tileWindow.pixlo;

            int i, j; // counters
            long i_min, i_max, j_min, j_max; // minimas/maximas
            double xp, yp;
            double xkj, ykj, xlj, ylj;
            double f; // function
            double zj, zk, zl, zkj, zlj;

            // containers
            int zLoop; // z-level - hardcoded!
            double[] a = new double[zLoops];
            double[] b = new double[zLoops];
            double[] c = new double[zLoops];
            // containers for xy coordinates of Triangles: p1-p2-p3-p1
            double[] vx = new double[4];
            double[] vy = new double[4];

            // declare demRadarCode_phase
            double[][] griddedData = new double[(int) tileWindow.lines()][(int) tileWindow.pixels()];
            final int nx = griddedData.length / zLoops;
            final int ny = griddedData[0].length;

            for (int r = 0; r < griddedData.length; r++) {
                Arrays.fill(griddedData[r], nodata);
            }

            //// interpolate: loop over triangles
            for (Triangle triangle : FDT.triangles) {

                // store triangle coordinates in local variables
                vx[0] = vx[3] = triangle.getA().x;
                vy[0] = vy[3] = triangle.getA().y / xyRatio;

                vx[1] = triangle.getB().x;
                vy[1] = triangle.getB().y / xyRatio;

                vx[2] = triangle.getC().x;
                vy[2] = triangle.getC().y / xyRatio;

                // check whether something is no-data
                if (vx[0] == nodata || vx[1] == nodata || vx[2] == nodata) {
                    continue;
                }
                if (vy[0] == nodata || vy[1] == nodata || vy[2] == nodata) {
                    continue;
                }

            /* Compute grid indices the current triangle may cover.*/

                xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
                i_min = coordToIndex(xp, x_min, xScale, offset);

                xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
                i_max = coordToIndex(xp, x_min, xScale, offset);

                yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
                j_min = coordToIndex(yp, y_min, yScale, offset);

                yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
                j_max = coordToIndex(yp, y_min, yScale, offset);

            /* Adjustments for triangles outside -R region. */
            /* Triangle to the left or right. */
                if ((i_max < 0) || (i_min >= nx)) {
                    continue;
                }
            /* Triangle Above or below */
                if ((j_max < 0) || (j_min >= ny)) {
                    continue;
                }
            /* Triangle covers boundary, left or right. */
                if (i_min < 0) {
                    i_min = 0;
                }
                if (i_max >= nx) {
                    i_max = nx - 1;
                }
            /* Triangle covers boundary, top or bottom. */
                if (j_min < 0) {
                    j_min = 0;
                }
                if (j_max >= ny) {
                    j_max = ny - 1;
                }

            /* Find equation for the plane as z = ax + by + c */
                xkj = vx[1] - vx[0];
                ykj = vy[1] - vy[0];
                xlj = vx[2] - vx[0];
                ylj = vy[2] - vy[0];

                f = 1.0 / (xkj * ylj - ykj * xlj);

                for (zLoop = 0; zLoop < zLoops; zLoop++) {
                    zj = triangle.getA().z;
                    zk = triangle.getB().z;
                    zl = triangle.getC().z;
                    zkj = zk - zj;
                    zlj = zl - zj;
                    a[zLoop] = -f * (ykj * zlj - zkj * ylj);
                    b[zLoop] = -f * (zkj * xlj - xkj * zlj);
                    c[zLoop] = -a[zLoop] * vx[1] - b[zLoop] * vy[1] + zk;
                }

                for (i = (int) i_min; i <= i_max; i++) {

                    xp = indexToCoord(i, x_min, xScale, offset);

                    for (j = (int) j_min; j <= j_max; j++) {

                        yp = indexToCoord(j, y_min, yScale, offset);

                        if (!pointInTriangle(vx, vy, xp, yp))
                            continue; /* Outside */

                        for (zLoop = 0; zLoop < zLoops; zLoop++) {
                            griddedData[i][j] = a[zLoop] * xp + b[zLoop] * yp + c[zLoop];
                        }
                    }
                }
            }

            return griddedData;
        }

        private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
            int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
            int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
            int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }

        private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
            return irint((((coord - coord0) / (deltaCoord)) - offset));
        }

        private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
            return (coord0 + idx * deltaCoord + offset);
        }

        private static long irint(final double coord) {
            return ((long) rint(coord));
        }

        private static double rint(final double coord) {
            return Math.floor(coord + 0.5);
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
            super(BackGeocodingOp.class);
        }
    }
}