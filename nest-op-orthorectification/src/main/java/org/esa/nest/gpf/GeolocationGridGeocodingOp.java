/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.resamp.Resampling;
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
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.CRSGeoCodingHandler;
import org.esa.nest.util.Constants;
import org.esa.nest.util.MathUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Raw SAR images usually contain significant geometric distortions. One of the factors that cause the
 * distortions is the ground elevation of the targets. This operator corrects the topographic distortion
 * in the raw image caused by this factor. The operator implements the Geolocation-Grid (GG) geocoding method.
 *
 * The method consis of the following major steps:
 * (1) Get coner latitudes and longitudes for the source image;
 * (2) Compute [LatMin, LatMax] and [LonMin, LonMax];
 * (3) Get the range and azimuth spacings for the source image;
 * (4) Compute DEM traversal sample intervals (delLat, delLon) based on source image pixel spacing;
 * (5) Compute target geocoded image dimension;
 * (6) Get latitude, longitude and slant range time tie points from geolocation LADS;
 * (7) Repeat the following steps for each point in the target raster [LatMax:-delLat:LatMin]x[LonMin:delLon:LonMax]:
 * (7.1) Get local latitude lat(i,j) and longitude lon(i,j) for current point;
 * (7.2) Determine the 4 cells in the source image that are immidiately adjacent and enclose the point;
 * (7.3) Compute slant range r(i,j) for the point using biquadratic interpolation;
 * (7.4) Compute azimuth time t(i,j) for the point using biquadratic interpolation;
 * (7.5) Compute bias-corrected zero Doppler time tc(i,j) = t(i,j) + r(i,j)*2/c, where c is the light speed;
 * (7.6) Compute azimuth image index Ia using zero Doppler time tc(i,j);
 * (7.8) Compute range image index Ir using slant range r(i,j) or groung range;
 * (7.9) Compute pixel value x(Ia,Ir) using interpolation and save it for current sample.
 *
 * Reference: Guide to ASAR Geocoding, Issue 1.0, 19.03.2008
 */

@OperatorMetadata(alias="Ellipsoid-Correction-GG",
        category = "Geometry\\Ellipsoid Correction",
        description="GG method for orthorectification")
public final class GeolocationGridGeocodingOp extends Operator {

    public static final String PRODUCT_SUFFIX = "_EC";

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label="Image Resampling Method")
    private String imgResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(description = "The coordinate reference system in well known text format")
    private String mapProjection;
    
    private boolean srgrFlag = false;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private TiePointGrid slantRangeTime = null;
    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;
    private GeoCoding targetGeoCoding = null;

    private double rangeSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days

    private CoordinateReferenceSystem targetCRS;
    private double delLat = 0.0;
    private double delLon = 0.0;

    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();

    private Resampling imgResampling = null;

    private String mission = null;
    private boolean nearRangeOnLeft = true;
    private boolean unBiasedZeroDoppler = false;
    private boolean isPolsar = false;

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
            if(OperatorUtils.isMapProjected(sourceProduct)) {
                throw new OperatorException("Source product is already map projected");
            }

            getSourceImageDimension();

            getMetadata();

            imgResampling = ResamplingFactory.createResampling(imgResamplingMethod);

            getTiePointGrids();

            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = RangeDopplerGeocodingOp.getMissionType(absRoot);

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);

        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days

        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        }

        if (mission.contains("CSKS") || mission.contains("TSX") || mission.equals("RS2")) {
            unBiasedZeroDoppler = true;
        }

        TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        nearRangeOnLeft = RangeDopplerGeocodingOp.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);

        isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     * @throws OperatorException The exception.
     */
    private void createTargetProduct() throws OperatorException {

        try {
            double pixelSpacingInMeter = Math.max(RangeDopplerGeocodingOp.getAzimuthPixelSpacing(sourceProduct),
                    RangeDopplerGeocodingOp.getRangePixelSpacing(sourceProduct));
            double pixelSpacingInDegree = RangeDopplerGeocodingOp.getPixelSpacingInDegree(pixelSpacingInMeter);

            delLat = pixelSpacingInDegree;
            delLon = pixelSpacingInDegree;

            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(sourceProduct, mapProjection,
                    pixelSpacingInDegree, pixelSpacingInMeter);

            targetCRS = crsHandler.getTargetCRS();

            targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                    sourceProduct.getProductType(), crsHandler.getTargetWidth(), crsHandler.getTargetHeight());
            targetProduct.setGeoCoding(crsHandler.getCrsGeoCoding());

            OperatorUtils.addSelectedBands(
                    sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true);

            targetGeoCoding = targetProduct.getGeoCoding();

            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
            ProductUtils.copyVectorData(sourceProduct, targetProduct);
            targetProduct.setDescription(sourceProduct.getDescription());

            try {
                OperatorUtils.copyIndexCodings(sourceProduct, targetProduct);
            } catch(Exception e) {
                if(!imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {
                    throw new OperatorException("Use Nearest Neighbour with Classifications: "+e.getMessage());
                }
            }

            updateTargetProductMetadata();
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Update metadata in the target product.
     * @throws OperatorException The exception.
     */
    private void updateTargetProductMetadata() throws OperatorException {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.srgr_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.map_projection, targetCRS.getName().getCode());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetProduct.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetProduct.getSceneRasterWidth());

        final GeoPos geoPosFirstNear = targetGeoCoding.getGeoPos(new PixelPos(0,0), null);
        final GeoPos geoPosFirstFar = targetGeoCoding.getGeoPos(new PixelPos(targetProduct.getSceneRasterWidth()-1, 0), null);
        final GeoPos geoPosLastNear = targetGeoCoding.getGeoPos(new PixelPos(0,targetProduct.getSceneRasterHeight()-1), null);
        final GeoPos geoPosLastFar = targetGeoCoding.getGeoPos(new PixelPos(targetProduct.getSceneRasterWidth()-1,
                                                                            targetProduct.getSceneRasterHeight()-1), null);

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_lat, geoPosFirstNear.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_lat, geoPosFirstFar.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_lat, geoPosLastNear.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_lat, geoPosLastFar.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_long, geoPosFirstNear.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_long, geoPosFirstFar.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_long, geoPosLastNear.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_long, geoPosLastFar.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(targetProduct));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.is_terrain_corrected, 0);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.geo_ref_system, targetCRS.getName().getCode());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lat_pixel_res, delLat);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lon_pixel_res, delLon);

        // save look directions for 5 range lines
        final MetadataElement lookDirectionListElem = new MetadataElement("Look_Direction_List");
        final int numOfDirections = 5;
        for(int i=1; i <= numOfDirections; ++i) {
            RangeDopplerGeocodingOp.addLookDirection("look_direction", lookDirectionListElem, i, numOfDirections,
                    sourceImageWidth, sourceImageHeight, firstLineUTC, lineTimeInterval, nearRangeOnLeft, latitude,
                    longitude);
        }
        absTgt.addElement(lookDirectionListElem);
    }

    /**
     * Get latitude, longitude and slant range time tie point grids.
     */
    private void getTiePointGrids() {
        slantRangeTime = OperatorUtils.getSlantRangeTime(sourceProduct);
        if (slantRangeTime == null) {
            throw new OperatorException("Product without slant range time tie point grid");
        }

        latitude = OperatorUtils.getLatitude(sourceProduct);
        if (latitude == null) {
            throw new OperatorException("Product without latitude tie point grid");
        }

        longitude = OperatorUtils.getLongitude(sourceProduct);
        if (longitude == null) {
            throw new OperatorException("Product without longitude tie point grid");
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

        /*
         * (7.1) Get local latitude lat(i,j) and longitude lon(i,j) for current point;
         * (7.2) Determine the 4 cells in the source image that are immediately adjacent and enclose the point;
         * (7.3) Compute slant range r(i,j) for the point using bi-quadratic interpolation;
         * (7.4) Compute azimuth time t(i,j) for the point using bi-quadratic interpolation;
         * (7.5) Compute bias-corrected zero Doppler time tc(i,j) = t(i,j) + r(i,j)*2/c, where c is the light speed;
         * (7.6) Compute azimuth image index Ia using zero Doppler time tc(i,j);
         * (7.8) Compute range image index Ir using slant range r(i,j) or ground range;
         * (7.9) Compute pixel value x(Ia,Ir) using interpolation and save it for current sample.
         */
        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w  = targetTileRectangle.width;
        final int h  = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        Band sourceBand1 = null;
        Band sourceBand2 = null;
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
        }
        final double srcBandNoDataValue = sourceBand1.getNoDataValue();

        final double oneBillionthHalfSpeedLight = Constants.halfLightSpeed / Constants.oneBillion;

        try {
            final ProductData trgData = targetTile.getDataBuffer();
            final int srcMaxRange = sourceImageWidth - 1;
            final int srcMaxAzimuth = sourceImageHeight - 1;
            GeoPos geoPos = null;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {

                    final int index = targetTile.getDataBufferIndex(x, y);
                    geoPos = targetGeoCoding.getGeoPos(new PixelPos(x,y), null);
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }
                    final PixelPos pixPos = computePixelPosition(lat, lon, sourceBand1);
                    if (Float.isNaN(pixPos.x) || Float.isNaN(pixPos.y) ||
                        pixPos.x < 0.0 || pixPos.x >= srcMaxRange || pixPos.y < 0.0 || pixPos.y >= srcMaxAzimuth) {
                        trgData.setElemDoubleAt(index, srcBandNoDataValue);
                        continue;
                    }

                    final double slantRange = slantRangeTime.getPixelFloat(pixPos.x, pixPos.y) * oneBillionthHalfSpeedLight;

                    final double zeroDopplerTime = computeZeroDopplerTime(pixPos);
                    double azimuthIndex = 0.0;
                    double rangeIndex = 0.0;
                    if (unBiasedZeroDoppler) {
                        azimuthIndex = (zeroDopplerTime - firstLineUTC) / lineTimeInterval;
                        rangeIndex = computeRangeIndex(zeroDopplerTime, slantRange);
                    } else {
                        final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.halfLightSpeed / 86400.0;
                        azimuthIndex = (zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval;
                        rangeIndex = computeRangeIndex(zeroDopplerTimeWithoutBias, slantRange);
                    }

                    if (rangeIndex < 0.0 || rangeIndex >= srcMaxRange ||
                        azimuthIndex < 0.0 || azimuthIndex >= srcMaxAzimuth) {
                            trgData.setElemDoubleAt(index, srcBandNoDataValue);
                    } else {
                        trgData.setElemDoubleAt(index, getPixelValue(azimuthIndex, rangeIndex, sourceBand1, sourceBand2));
                    }
                }
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Compute pixel position in source image for given latitude and longitude.
     * @param lat The latitude in degrees.
     * @param lon The longitude in degrees.
     * @param sourceBand The source band.
     * @return The pixel position.
     */
    private static PixelPos computePixelPosition(final double lat, final double lon, final Band sourceBand) {
        // todo the following method is not accurate, should use point-in-polygon test
        return sourceBand.getGeoCoding().getPixelPos(new GeoPos((float)lat, (float)lon), null);
    }

    /**
     * Compute zero Doppler time for a given pixel using biquadratic interpolation.
     * @param pixPos The pixel position.
     * @return The zero Doppler time in days.
     */
    private double computeZeroDopplerTime(PixelPos pixPos) {
        // todo Guide requires using biquadratic interpolation, is it necessary?
        final int j0 = (int)pixPos.y;
        final double t0 = firstLineUTC + j0*lineTimeInterval;
        final double t1 = firstLineUTC + (j0 + 1)*lineTimeInterval;
        return t0 + (pixPos.y - j0)*(t1 - t0);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange The slant range in meters.
     * @return The range index.
     */
    private double computeRangeIndex(double zeroDopplerTime, double slantRange) throws Exception {

        double rangeIndex = 0.0;

        if (srgrFlag) { // ground detected image

            if(srgrConvParams.length == 0)
                throw new Exception("SRGR coefficients not found in product");

            int idx = 0;
            for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
                idx = i;
            }
            final double groundRange = RangeDopplerGeocodingOp.computeGroundRange(
                    sourceImageWidth, rangeSpacing, slantRange, srgrConvParams[idx].coefficients,
                    srgrConvParams[idx].ground_range_origin);

            if (groundRange < 0.0) {
                return -1.0;
            } else {
                rangeIndex = (groundRange - srgrConvParams[idx].ground_range_origin) / rangeSpacing;
            }

        } else { // slant range image

            final int azimuthIndex = (int)((zeroDopplerTime - firstLineUTC) / lineTimeInterval);
            double r0;
            if (nearRangeOnLeft) {
                r0 = slantRangeTime.getPixelDouble(0, azimuthIndex) / 1000000000.0*Constants.halfLightSpeed;
            } else {
                r0 = slantRangeTime.getPixelDouble(sourceImageWidth-1, azimuthIndex)/1000000000.0*Constants.halfLightSpeed;
            }
            rangeIndex = (slantRange - r0) / rangeSpacing;
        }

        if (!nearRangeOnLeft) {
            rangeIndex = sourceImageWidth - 1 - rangeIndex;
        }

        return rangeIndex;
    }

    /**
     * Compute orthorectified pixel value for given pixel.
     * @param azimuthIndex The azimuth index for pixel in source image.
     * @param rangeIndex The range index for pixel in source image.
     * @return The pixel value.
     */
    private double getPixelValue(final double azimuthIndex, final double rangeIndex,
                                 final Band sourceBand1, final Band sourceBand2) {

        try {
            final int x0 = (int)(rangeIndex + 0.5);
            final int y0 = (int)(azimuthIndex + 0.5);
            Rectangle srcRect = null;
            Tile sourceTileI, sourceTileQ = null;

            if (imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {

                srcRect = new Rectangle(x0, y0, 1, 1);

            } else if (imgResampling.equals(Resampling.BILINEAR_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 1), Math.max(0, y0 - 1), 3, 3);

            } else if (imgResampling.equals(Resampling.CUBIC_CONVOLUTION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else if (imgResampling.equals(Resampling.BISINC_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 3), Math.max(0, y0 - 3), 6, 6);

            } else if (imgResampling.equals(Resampling.BICUBIC_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else {
                throw new OperatorException("Unhandled interpolation method");
            }

            sourceTileI = getSourceTile(sourceBand1, srcRect);
            if (sourceBand2 != null) {
                sourceTileQ = getSourceTile(sourceBand2, srcRect);
            }

            final ResamplingRaster imgResamplingRaster = new ResamplingRaster(sourceTileI, sourceTileQ);
            final Resampling resampling = imgResampling;
            final Resampling.Index imgResamplingIndex = resampling.createIndex();

            resampling.computeIndex(rangeIndex + 0.5, azimuthIndex + 0.5,
                                       sourceImageWidth, sourceImageHeight, imgResamplingIndex);

            return resampling.resample(imgResamplingRaster, imgResamplingIndex);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return 0;
    }

    public static class ResamplingRaster implements Resampling.Raster {

        private final Tile sourceTileI, sourceTileQ;
        private final double noDataValue;
        private final ProductData dataBufferI, dataBufferQ;

        public ResamplingRaster(final Tile sourceTileI, final Tile sourceTileQ) {

            this.sourceTileI = sourceTileI;
            this.sourceTileQ = sourceTileQ;

            this.dataBufferI = sourceTileI.getDataBuffer();
            if (sourceTileQ != null) {
                this.dataBufferQ = sourceTileQ.getDataBuffer();
            } else {
                this.dataBufferQ = null;
            }

            this.noDataValue = sourceTileI.getRasterDataNode().getNoDataValue();
        }

        public final int getWidth() {
            return sourceTileI.getWidth();
        }

        public final int getHeight() {
            return sourceTileI.getHeight();
        }

        public void getSamples(int[] x, int[] y, float[][] samples) {

            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {

                    double v = (float)dataBufferI.getElemDoubleAt(sourceTileI.getDataBufferIndex(x[j], y[i]));
                    if (noDataValue != 0 && (v == noDataValue)) {
                        samples[i][j] = (float)noDataValue;
                        continue;
                    } else {
                        samples[i][j] = (float)v;
                    }

                    if (dataBufferQ != null) {
                        final double vq = dataBufferQ.getElemDoubleAt(sourceTileQ.getDataBufferIndex(x[j], y[i]));
                        if (noDataValue != 0 && vq == noDataValue) {
                            samples[i][j] = (float)noDataValue;
                            continue;
                        }

                        samples[i][j] = (float)(v*v + vq*vq);
                    }
                }
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GeolocationGridGeocodingOp.class);
            setOperatorUI(GeolocationGridGeocodingOpUI.class);            
        }
    }
}