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
package org.esa.nest.gpf.coregistration;

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
import org.esa.snap.datamodel.*;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.nest.gpf.geometric.SARGeocoding;
import org.esa.nest.gpf.geometric.SARUtils;
import org.esa.snap.gpf.*;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * For each tile of the target product in master frame,
 * 1.	compute its lat/lon boundaries (with extensions) for master image tile;
 * 2.	compute lat/lon indices in DEM for the boundaries;
 * 3.	loop through all DEM points bounded by the indices compute above. For each point,
 *      1)	get its height from DEM;
 *      2)	compute its azimuth/range indices in master image;
 *      3)	compute its azimuth/range indices in slave image;
 *      4)	compute azimuth/range offsets;
 * 4.	compute azimuth/range offsets for pixels in current tile in master image using Delaunay interpolation;
 * 5.	resample slave image into master frame using the azimuth/range offsets computed above.
 */

@OperatorMetadata(alias = "DEM-Based-Coregistration",
        category = "SAR Processing/Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Create DEM Based Co-registrated Images", internal=true)
public class DEMBasedCoregistrationOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @Parameter(description = "The list of master bands.", alias = "masterBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Master Band")
    private String[] masterBandNames = null;

    @Parameter(description = "The list of slave bands.", alias = "slaveBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Slave Bands")
    private String[] slaveBandNames = null;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME},
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
    private ImageInfo masterInfo = null;
    private ImageInfo slaveInfo = null;
    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0; // no data value for DEM
    private int numGCPs = 20;
    private int rowUpSamplingFactor = 4; // cross correlation interpolation factor in row direction, must be power of 2
    private int colUpSamplingFactor = 4; // cross correlation interpolation factor in column direction, must be power of 2
    private int cWindowWidth = 128;
    private int cWindowHeight = 128;
    private int cHalfWindowWidth = 0;
    private int cHalfWindowHeight = 0;
    private int maxIteration = 10;
    private double gcpTolerance = 0.5;
    private boolean isBiasAvailable = false;
    private double azBias = 0.0;
    private double rgBias = 0.0;

    private double noDataValue = -9999.0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DEMBasedCoregistrationOp() {
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

            if (sourceProduct.length != 2) {
                throw new OperatorException("Please select two source products");
            }

            cHalfWindowWidth = cWindowWidth / 2;
            cHalfWindowHeight = cWindowHeight / 2;

            masterProduct = sourceProduct[0];
            masterInfo = new ImageInfo(masterProduct);
            masterBandNames = masterProduct.getBandNames();

            slaveProduct = sourceProduct[1];
            slaveInfo = new ImageInfo(slaveProduct);
            slaveBandNames = slaveProduct.getBandNames();

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        if (dem != null) {
            dem.dispose();
            dem = null;
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(masterProduct.getName(),
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        String suffix = "_mst" + StackUtils.getBandTimeStamp(masterProduct);
        for (String bandName:masterBandNames) {
            ProductUtils.copyBand(bandName, masterProduct, bandName + suffix, targetProduct, true);
        }

        suffix = "_slv1" + StackUtils.getBandTimeStamp(slaveProduct);
        for (String bandName:slaveBandNames) {

            final Band band = slaveProduct.getBand(bandName);
            final Band targetBand = new Band(bandName + suffix,
                    band.getDataType(),
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight());

            targetBand.setUnit(band.getUnit());
            targetProduct.addBand(targetBand);
        }

        ProductUtils.copyProductNodes(masterProduct, targetProduct);

        copySlaveMetadata();

        updateTargetProductMetadata();
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
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);
        final MetadataElement slvInputElem = ProductInformation.getInputProducts(slaveProduct);
        final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
        for (MetadataAttribute attrib : slvInputProductAttrbList) {
            final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
            inputAttrb.getData().setElems(attrib.getData().getElemString());
        }
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

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        //System.out.println("DEMBasedCoregistrationOp: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            if (!isBiasAvailable) {
                estimateBias();
            }

            // Compute first term in azimuth and range shifts
            final double[][] firstTermInAzShift = new double[h][w];
            final double[][] firstTermInRgShift = new double[h][w];
            computeFirstTermInAzRgShifts(x0, xMax, y0, yMax, firstTermInAzShift, firstTermInRgShift);

            // Compute azimuth and range shifts
            final PixelPos[] slavePixPos = new PixelPos[h*w];
            computeAzRgShifts (x0, xMax, y0, yMax, firstTermInAzShift, firstTermInRgShift, slavePixPos);

            // Compute source rectangle
            final Rectangle sourceRectangle = getBoundingBox(
                    slavePixPos, slaveProduct.getSceneRasterWidth(), slaveProduct.getSceneRasterHeight());

            if (sourceRectangle == null) {
                return;
            }

            // Resample slave image to get coregistered slave image in master frame
            final String suffix = "_slv1" + StackUtils.getBandTimeStamp(slaveProduct);
            for (String bandName:slaveBandNames) {
                final Band targetBand = targetProduct.getBand(bandName + suffix);
                final Tile targetTile = targetTileMap.get(targetBand);

                resampleSourceBand(slaveProduct.getBand(bandName), sourceRectangle, slavePixPos,
                        targetTile, selectedResampling);
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

    private synchronized void estimateBias() throws Exception {

        if (isBiasAvailable) {
            return;
        }

        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(masterProduct, tileSize, 0);
        final int numRandomTiles = Math.min(numGCPs, tileRectangles.length);

        final GeoCoding mGeoCoding = masterProduct.getGeoCoding();
        final GeoCoding sGeoCoding = slaveProduct.getGeoCoding();
        final Rectangle[] randomTileArray = getRandomTiles(numRandomTiles, tileRectangles, mGeoCoding, sGeoCoding);

        final Band masterBand = getAmplitudeOrIntensityBand(masterProduct);
        final Band slaveBand = getAmplitudeOrIntensityBand(slaveProduct);

        final StatusProgressMonitor status = new StatusProgressMonitor(numRandomTiles,
                "Estimating bias in azimuth and range shifts... ");
        int tileCnt = 0;

        final ThreadManager threadManager = new ThreadManager();
        try {
            List<Double> azBiasArray = new ArrayList<>(numRandomTiles);
            List<Double> rgBiasArray = new ArrayList<>(numRandomTiles);

            for (final Rectangle tileRectangle:randomTileArray) {
                checkForCancellation();

                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final int x0 = tileRectangle.x;
                            final int y0 = tileRectangle.y;
                            final int w = tileRectangle.width;
                            final int h = tileRectangle.height;
                            final int xMax = x0 + w;
                            final int yMax = y0 + h;
                            final double[][] firstTermInAzShift = new double[h][w];
                            final double[][] firstTermInRgShift = new double[h][w];
                            final PixelPos mGCPPixelPos = new PixelPos(x0 + w/2, y0 + h/2);
                            final PixelPos sGCPPixelPos = new PixelPos();
                            final double[] bias = new double[2]; // az/rg bias

                            getSlaveGCPPixPos(mGeoCoding, sGeoCoding, mGCPPixelPos, sGCPPixelPos);

                            computeFirstTermInAzRgShifts(x0, xMax, y0, yMax, firstTermInAzShift, firstTermInRgShift);

                            if (firstTermInAzShift[h/2][w/2] != noDataValue &&
                                    firstTermInRgShift[h/2][w/2] != noDataValue) {

                                estimateBiasInAzRgShifts(x0, y0, w, h, masterBand, slaveBand, mGCPPixelPos, sGCPPixelPos,
                                        firstTermInAzShift, firstTermInRgShift, bias);

                                if (bias[0] != noDataValue && bias[1] != noDataValue) {
                                    synchronized(azBiasArray) {
                                        azBiasArray.add(bias[0]);
                                        rgBiasArray.add(bias[1]);
                                    }
                                }
                            }

                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException("estimateBias", e);
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(tileCnt++);
            }

            double sumAzBias = 0.0;
            double sumRgBias = 0.0;
            for (int i = 0; i < azBiasArray.size(); i++) {
                sumAzBias += azBiasArray.get(i);
                sumRgBias += rgBiasArray.get(i);
            }
            azBias = sumAzBias / azBiasArray.size();
            rgBias = sumRgBias / azBiasArray.size();

            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateBias", e);
        }

        isBiasAvailable = true;
    }

    private Rectangle[] getRandomTiles(final int numRandomTiles, final Rectangle[] tileRectangles,
                                       final GeoCoding mGeoCoding, final GeoCoding sGeoCoding) {

        final List<Rectangle> validTileList = new ArrayList<>();
        PixelPos sGCPPixelPos = new PixelPos();
        for (Rectangle rectangle:tileRectangles) {
            final int xc = rectangle.x + rectangle.width/2;
            final int yc = rectangle.y + rectangle.height/2;
            final PixelPos mGCPPixelPos = new PixelPos(xc, yc);
            if (getSlaveGCPPixPos(mGeoCoding, sGeoCoding, mGCPPixelPos, sGCPPixelPos)) {
                validTileList.add(rectangle);
            }
        }

        if (validTileList.size() < numRandomTiles) {
            throw new OperatorException("Cannot find " + numRandomTiles + " valid tiles for bis estimation.");
        }

        final List<Rectangle> randomTileList = new ArrayList<>(numRandomTiles);
        Random randomGenerator = new Random();
        int k = 0;
        while (k < numRandomTiles){
            final int randomInt = randomGenerator.nextInt(validTileList.size());
            final Rectangle tileRectangle = validTileList.get(randomInt);
            if (!randomTileList.contains(tileRectangle)) {
                randomTileList.add(tileRectangle);
                k++;
            }
        }

        return randomTileList.toArray(new Rectangle[randomTileList.size()]);
    }

    private boolean getSlaveGCPPixPos(final GeoCoding masterGeoCoding, final GeoCoding slaveGeoCoding,
                                      final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos) {

        final GeoPos mGCPGeoPos = new GeoPos();
        masterGeoCoding.getGeoPos(mGCPPixelPos, mGCPGeoPos);
        slaveGeoCoding.getPixelPos(mGCPGeoPos, sGCPPixelPos);

        return (sGCPPixelPos.x - cHalfWindowWidth + 1 >= 0 &&
                sGCPPixelPos.x + cHalfWindowWidth <= slaveInfo.sourceImageWidth - 1) &&
                (sGCPPixelPos.y - cHalfWindowHeight + 1 >= 0 &&
                        sGCPPixelPos.y + cHalfWindowHeight <= slaveInfo.sourceImageHeight - 1);
    }

    private Band getAmplitudeOrIntensityBand(final Product sourceProduct) {

        final Band[] masterBands = sourceProduct.getBands();
        for (Band band:masterBands) {
            if (band.getUnit().contains(Unit.AMPLITUDE) || band.getUnit().contains(Unit.INTENSITY)) {
                return band;
            }
        }
        return null;
    }

    private void estimateBiasInAzRgShifts(final int x0, final int y0, final int w, final int h,
                                          final Band mBand, final Band sBand,
                                          final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos,
                                          final double[][] firstTermInAzShift, final double[][] firstTermInRgShift,
                                          final double[] bias) {

        if (mBand == null || sBand == null) {
            throw new OperatorException("Cannot find valid master or slave band for bias estimation.");
        }

//        final Rectangle mRectangle = new Rectangle((int)mGCPPixelPos.x - cHalfWindowWidth + 1,
//                (int)mGCPPixelPos.y - cHalfWindowHeight + 1, cWindowWidth, cWindowHeight);

//        final Rectangle sRectangle = new Rectangle((int)sGCPPixelPos.x - cHalfWindowWidth + 1,
//                (int)sGCPPixelPos.y - cHalfWindowHeight + 1, cWindowWidth, cWindowHeight);

        final Rectangle mRectangle = new Rectangle(x0, y0, w, h);

        final Rectangle sRectangle = new Rectangle(Math.max(0, (int) sGCPPixelPos.x - w / 2),
                Math.max(0, (int) sGCPPixelPos.y - h / 2), w, h);

        final Tile mTile = getSourceTile(mBand, mRectangle);
        final Tile sTile = getSourceTile(sBand, sRectangle);

        final ProductData mData = mTile.getDataBuffer();
        final ProductData sData = sTile.getDataBuffer();

        CoarseRegistration coarseRegistration = new CoarseRegistration(cWindowWidth, cWindowHeight,
                rowUpSamplingFactor, colUpSamplingFactor, maxIteration, gcpTolerance, mTile, mData, sTile, sData,
                slaveProduct.getSceneRasterWidth(), slaveProduct.getSceneRasterHeight());

        if (coarseRegistration.getCoarseSlaveGCPPosition(mGCPPixelPos, sGCPPixelPos)) {

            final double estimatedAzShift = sGCPPixelPos.getY() - mGCPPixelPos.getY();
            final double estimatedRgShift = sGCPPixelPos.getX() - mGCPPixelPos.getX();
            System.out.println("estimatedAzShift = " + estimatedAzShift + ", estimatedRgShift = " + estimatedRgShift);

            final int mx = (int)mGCPPixelPos.x - x0;
            final int my = (int)mGCPPixelPos.y - y0;

            bias[0] = estimatedAzShift - firstTermInAzShift[my][mx];
            bias[1] = estimatedRgShift - firstTermInRgShift[my][mx];

        } else {

            bias[0] = noDataValue;
            bias[1] = noDataValue;
        }
        /*
        final double biasAz = masterInfo.prf*masterInfo.firstLineUTC*Constants.secondsInDay
                - slaveInfo.prf*slaveInfo.firstLineUTC*Constants.secondsInDay;
        final double diff = bias[0] - biasAz;
        System.out.println();*/
    }

    private void computeFirstTermInAzRgShifts(final int x0, final int xMax, final int y0, final int yMax,
                                              final double[][] firstTermInAzShift, final double[][] firstTermInRgShift)
            throws Exception {

        try {
            // Compute lat/lon boundaries (with extensions) for target tile;
            final double[] latLonMinMax = new double[4];
            computeImageGeoBoundary(x0, xMax, y0, yMax, masterProduct, latLonMinMax);

            final double delta = (double)dem.getDescriptor().getDegreeRes() / (double)dem.getDescriptor().getPixelRes();
            final double extralat = 1.5*delta + 4.0/25.0;
            final double extralon = 1.5*delta + 4.0/25.0;
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

            // Loop through all DEM points bounded by the indices computed above. For each point,
            // get its lat/lon and its azimuth/range indices in target image;
            final int numLines = latMinIdx - latMaxIdx + 1;
            final int numPixels = lonMaxIdx - lonMinIdx;
            double[][] azIn = new double[numLines][numPixels];
            double[][] rgIn = new double[numLines][numPixels];
            double[][] rgOffset = new double[numLines][numPixels];
            double[][] azOffset = new double[numLines][numPixels];
            final PositionData masterPosData = new PositionData();
            final PositionData slavePosData = new PositionData();

            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    GeoPos gp = dem.getGeoPos(new PixelPos(lonMinIdx + p, latMaxIdx + l));
                    final double lat = gp.lat;
                    final double lon = gp.lon;
                    final double alt = dem.getElevation(gp);

                    if (alt == demNoDataValue || !getPosition(lat, lon, alt, masterInfo, masterPosData) ||
                            !getPosition(lat, lon, alt, slaveInfo, slavePosData)) {
                        azIn[l][p] = noDataValue;
                        rgIn[l][p] = noDataValue;
                        continue;
                    }

                    azIn[l][p] = masterPosData.azimuthIndex;
                    rgIn[l][p] = masterPosData.rangeIndex;
                    azOffset[l][p] = slaveInfo.prf*slavePosData.azimuthTime - masterInfo.prf*masterPosData.azimuthTime;
                    rgOffset[l][p] = slaveInfo.samplingRate*slavePosData.slantRangeTime -
                            masterInfo.samplingRate*masterPosData.slantRangeTime;
                }
            }

            // Compute azimuth/range offsets for pixels in target tile using Delaunay interpolation;
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, yMax-1, x0, xMax-1);

            //final double rgAzRatio = computeRangeAzimuthSpacingRatio(w, h, latLonMinMax);
            final double rgAzRatio = masterInfo.rangeSpacing / masterInfo.azimuthSpacing;

            final double[][] azOffsetArray = TriangleUtils.gridDataLinear(
                    azIn, rgIn, azOffset, tileWindow, rgAzRatio, 1, 1, noDataValue, 0);

            final double[][] rgOffsetArray = TriangleUtils.gridDataLinear(
                    azIn, rgIn, rgOffset, tileWindow, rgAzRatio, 1, 1, noDataValue, 0);

            for(int i = 0; i < azOffsetArray.length; i++) {
                firstTermInAzShift[i] = azOffsetArray[i].clone();
                firstTermInRgShift[i] = rgOffsetArray[i].clone();
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeFirstTermInAzRgShifts", e);
        }
    }

    private void computeAzRgShifts (final int x0, final int xMax, final int y0, final int yMax,
                                    final double[][] firstTermInAzShift, final double[][] firstTermInRgShift,
                                    final PixelPos[] slavePixPos) {

        for (int y = y0, index = 0; y < yMax; y++) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int xx = x - x0;

                if (firstTermInAzShift[yy][xx] == noDataValue || firstTermInRgShift[yy][xx] == noDataValue) {
                    slavePixPos[index++] = null;
                } else {
                    final double azShift = firstTermInAzShift[yy][xx] + azBias;
                    final double rgShift = firstTermInRgShift[yy][xx] + rgBias;
                    slavePixPos[index++] = new PixelPos(x + rgShift, y + azShift);
                }
            }
        }
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
                                 final ImageInfo imageInfo, final PositionData data) {

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

        final double zeroDopplerTime = SARGeocoding.getZeroDopplerTime(imageInfo.firstLineUTC,
                imageInfo.lineTimeInterval, imageInfo.wavelength, data.earthPoint, imageInfo.orbit);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.azimuthTime = zeroDopplerTime * Constants.secondsInDay; // day to s
        data.azimuthIndex = (zeroDopplerTime - imageInfo.firstLineUTC) / imageInfo.lineTimeInterval;

        final double slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime, imageInfo.orbit, data.earthPoint, data.sensorPos);

        data.slantRangeTime = slantRange / Constants.lightSpeed; // in s

        if (!imageInfo.srgrFlag) {
            data.rangeIndex = (slantRange - imageInfo.nearEdgeSlantRange) / imageInfo.rangeSpacing;
        } else {
            data.rangeIndex = SARGeocoding.computeRangeIndex(
                    imageInfo.srgrFlag, imageInfo.sourceImageWidth, imageInfo.firstLineUTC, imageInfo.lastLineUTC,
                    imageInfo.rangeSpacing, zeroDopplerTime, slantRange, imageInfo.nearEdgeSlantRange,
                    imageInfo.srgrConvParams);
        }

        if (!imageInfo.nearRangeOnLeft) {
            data.rangeIndex = imageInfo.sourceImageWidth - 1 - data.rangeIndex;
        }
        return true;
    }

    private double computeRangeAzimuthSpacingRatio(final int w, final int h, final double[] latLonMinMax)
            throws Exception {

        final double latMin = latLonMinMax[0];
        final double latMax = latLonMinMax[1];
        final double lonMin = latLonMinMax[2];
        final double lonMax = latLonMinMax[3];
        final PosVector uL = new PosVector();
        final PosVector uR = new PosVector();
        final PosVector lL = new PosVector();
        final PosVector lR = new PosVector();
        GeoUtils.geo2xyzWGS84(latMax, lonMin, 0.0, uL);
        GeoUtils.geo2xyzWGS84(latMax, lonMax, 0.0, uR);
        GeoUtils.geo2xyzWGS84(latMin, lonMin, 0.0, lL);
        GeoUtils.geo2xyzWGS84(latMin, lonMax, 0.0, lR);

        final double rangeSpacing = (norm(uL, uR) + norm(lL, lR)) / 2 / (w - 1);
        final double aziSpacing = (norm(uL, lL) + norm(uR, lR)) / 2 / (h - 1);

        return rangeSpacing / aziSpacing;
    }

    private double norm(final PosVector p1, final PosVector p2) {
        return Math.sqrt((p1.x - p2.x)*(p1.x - p2.x) +
                (p1.y - p2.y)*(p1.y - p2.y) +
                (p1.z - p2.z)*(p1.z - p2.z));
    }

    private static Rectangle getBoundingBox(final PixelPos[] slavePixPos, final int maxWidth, final int maxHeight) {

        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (final PixelPos pixelsPos : slavePixPos) {
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
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 4, 0);
        maxX = Math.min(maxX + 4, maxWidth - 1);
        minY = Math.max(minY - 4, 0);
        maxY = Math.min(maxY + 4, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public void resampleSourceBand(final RasterDataNode sourceBand, final Rectangle sourceRectangle,
                                   final PixelPos[] slavePixPos, final Tile targetTile, final Resampling resampling)
            throws OperatorException {

        final RasterDataNode targetBand = targetTile.getRasterDataNode();
        final Rectangle targetRectangle = targetTile.getRectangle();
        final ProductData trgBuffer = targetTile.getDataBuffer();

        final double noDataValue = targetBand.getGeophysicalNoDataValue();
        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int txMax = tx0 + targetRectangle.width;
        final int tyMax = ty0 + targetRectangle.height;

        Tile sourceTile = null;
        if (sourceRectangle != null) {
            sourceTile = getSourceTile(sourceBand, sourceRectangle);
        }

        if (sourceTile != null) {
            final Product srcProduct = sourceBand.getProduct();
            final int sourceRasterHeight = srcProduct.getSceneRasterHeight();
            final int sourceRasterWidth = srcProduct.getSceneRasterWidth();

            final Resampling.Index resamplingIndex = resampling.createIndex();
            final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

            for (int y = ty0, index = 0; y < tyMax; ++y) {
                for (int x = tx0; x < txMax; ++x) {

                    final int trgIndex = targetTile.getDataBufferIndex(x, y);

                    final PixelPos slavePixelPos = slavePixPos[index++];

                    if (isSlavePixPosValid(slavePixelPos)) {
                        try {
                            resampling.computeIndex(slavePixelPos.x, slavePixelPos.y,
                                    sourceRasterWidth, sourceRasterHeight, resamplingIndex);

                            double sample = resampling.resample(resamplingRaster, resamplingIndex);

                            if (Double.isNaN(sample)) {
                                sample = noDataValue;
                            }

                            trgBuffer.setElemDoubleAt(trgIndex, sample);

                        } catch (Exception e) {
                            throw new OperatorException(e.getMessage());
                        }
                    } else {
                        trgBuffer.setElemDoubleAt(trgIndex, noDataValue);
                    }
                }
            }
            sourceTile.getDataBuffer().dispose();

        } else {

            final TileIndex trgIndex = new TileIndex(targetTile);
            for (int y = ty0; y < tyMax; ++y) {
                trgIndex.calculateStride(y);
                for (int x = tx0; x < txMax; ++x) {
                    trgBuffer.setElemDoubleAt(trgIndex.getIndex(x), noDataValue);
                }
            }
        }
    }

    private boolean isSlavePixPosValid(final PixelPos slavePixPos) {

        return (slavePixPos != null && slavePixPos.x >= 0 && slavePixPos.x < slaveInfo.sourceImageWidth &&
                slavePixPos.y >= 0 && slavePixPos.y < slaveInfo.sourceImageHeight);
    }


    private static class ImageInfo {

        public SARGeocoding.Orbit orbit = null;
        public double firstLineUTC = 0.0; // in days
        public double lastLineUTC = 0.0; // in days
        public double lineTimeInterval = 0.0; // in days
        public double prf = 0.0; // in Hz
        public double samplingRate = 0.0; // Hz
        public double nearEdgeSlantRange = 0.0; // in m
        public double wavelength = 0.0; // in m
        public double rangeSpacing = 0.0;
        public double azimuthSpacing = 0.0;
        public int sourceImageWidth = 0;
        public int sourceImageHeight = 0;
        public boolean nearRangeOnLeft = true;
        public boolean srgrFlag = false;
        public AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

        public ImageInfo(final Product sourceProduct) throws Exception {

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            this.srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
            this.wavelength = SARUtils.getRadarFrequency(absRoot);
            this.rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
            this.azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
            this.firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
            this.lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
            this.lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) /
                    Constants.secondsInDay; // s to day
            this.prf = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.pulse_repetition_frequency); //Hz
            this.samplingRate = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_sampling_rate)*
                    Constants.oneMillion; // MHz to Hz
            this.sourceImageWidth = sourceProduct.getSceneRasterWidth();
            this.sourceImageHeight = sourceProduct.getSceneRasterHeight();
            OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
            this.orbit = new SARGeocoding.Orbit(
                    orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);

            if (this.srgrFlag) {
                this.srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
            } else {
                this.nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot,
                        AbstractMetadata.slant_range_to_first_pixel);
            }

            final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
            this.nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);
        }
    }

    private static class PositionData {
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
        double azimuthTime;
        double slantRangeTime;
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;
        private final ProductData dataBuffer;

        public ResamplingRaster(final Tile tile) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
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
                for (int j = 0; j < x.length; j++) {

                    samples[i][j] = dataBuffer.getElemDoubleAt(tile.getDataBufferIndex(x[j], y[i]));

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

            final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, z_in, xyRatio, nodata);
            return interpolate(xyRatio, window, xScale, yScale, offset, nodata, FDT);

        }

        private static FastDelaunayTriangulator triangulate(
                final double[][] x_in, final double[][] y_in, final double[][] z_in, final double xyRatio,
                final double nodata) throws Exception {

            List<Geometry> list = new ArrayList<>();
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

            for (double[] aGriddedData : griddedData) {
                Arrays.fill(aGriddedData, nodata);
            }

            //// interpolate: loop over triangles
            long t4 = System.currentTimeMillis();
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
            long t5 = System.currentTimeMillis();

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
            super(DEMBasedCoregistrationOp.class);
        }
    }

}
