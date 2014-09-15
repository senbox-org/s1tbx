/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.datamodel;

import Jama.LUDecomposition;
import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.BitRaster;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Vector;


/**
 * The <code>PixelGeoCoding</code> is an implementation of a {@link GeoCoding} which uses
 * dedicated latitude and longitude bands in order to provide geographical positions
 * for <i>each</i> pixel. Unlike the {@link TiePointGeoCoding}</p>, which uses sub-sampled {@link TiePointGrid tie-point grids},
 * the  <code>PixelGeoCoding</code> class uses {@link Band bands}.</p>
 * <p/>
 * <p>This class is especially useful for high accuracy geo-coding, e.g. if geographical positions are computed for each pixel
 * by an upstream orthorectification.</p>
 * <p/>
 * <p>While the implementation of the {@link #getGeoPos(PixelPos, GeoPos)} is straight forward,
 * the {@link #getPixelPos(GeoPos, PixelPos)} uses two different search algorithms in order to
 * find the corresponding geo-position for a given pixel:
 * <ol>
 * <li>Search an N x N window around an estimated pixel position using the geo-coding of the source product (if any) or</li>
 * <li>perform a quad-tree search if the source product has no geo-coding.</li>
 * </ol></p>
 * <p/>
 * <p><i>Use instances of this class with care: The constructor fully loads the data given by the latitudes and longitudes bands and
 * the valid mask (if any) into memory.</i></p>
 * <p/>
 * <em>Note (rq-20110526):</em>
 * A better implementation of the find pixel method could be something like:
 * <ol>
 * <li>Create a coverage of the source region by means of largely overlapping
 * image tiles (e.g. tile size of 100 pixels squared with an overlap of 25 pixels)</li>
 * <li>For each tile create a rational function model of the (lon, lat) to (x, y)
 * transformation, rotating to the (lon, lat) of the tile center</li>
 * <li>Refine the accuracy of the selected rational function model until the
 * accuracy goal (i.e. a certain RMSE) is reached</li>
 * <li>Find all tiles {T1, T2, ...} that may include the (x, y) pixel coordinate
 * of interest</li>
 * <li>Select the tile T in {T1, T2, ...} where the the (x, y) result is nearest
 * to (0, 0)</li>
 * <li>Use the three closest pixels to compute the final (x, y)</li>
 * <li>Keep all rational function approximations in a map and reuse them for
 * subsequent calls.</li>
 * </ol>
 * <p/>
 * The advantage of this algorithm is that it obviously avoids problems related
 * to the antimeridian and poles included in the source region.
 */
public class PixelGeoCoding extends AbstractGeoCoding implements BasicPixelGeoCoding {

    /**
     * @since BEAM 4.9
     */
    private static final String SYSPROP_PIXEL_GEO_CODING_USE_TILING = "snap.pixelGeoCoding.useTiling";
    /**
     * @since BEAM 4.9
     */
    private static final String SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY = "snap.pixelGeoCoding.fractionAccuracy";

    private static final int MAX_SEARCH_CYCLES = 10;

    // TODO - (nf) make EPS for quad-tree search dependent on current scene
    private static final float EPS = 0.04F; // used by quad-tree search
    private static final boolean TRACE = false;
    private static final float D2R = (float) (Math.PI / 180.0);

    private Boolean crossingMeridianAt180;
    private final Band latBand;
    private final Band lonBand;
    private final String validMaskExpression;
    private final int searchRadius; // used by direct search only
    private final int rasterWidth;
    private final int rasterHeight;
    private final boolean useTiling;
    private final boolean fractionAccuracy;
    private GeoCoding pixelPosEstimator;
    private final boolean estimatorCreatedInternally;
    private PixelGrid latGrid;
    private PixelGrid lonGrid;
    private boolean initialized;
    private LatLonImage latLonImage;
    private double deltaThreshold;

    /**
     * Constructs a new pixel-based geo-coding.
     * <p/>
     * <i>Use with care: In contrast to the other constructor this one loads the data not until first access to
     * {@link #getPixelPos(GeoPos, PixelPos)} or {@link #getGeoPos(PixelPos, GeoPos)}. </i>
     *
     * @param latBand      the band providing the latitudes
     * @param lonBand      the band providing the longitudes
     * @param validMask    the valid mask expression used to identify valid lat/lon pairs, e.g. "NOT l1_flags.DUPLICATED".
     *                     Can be <code>null</code> if a valid mask is not used.
     * @param searchRadius the search radius in pixels, shall depend on the actual spatial scene resolution,
     *                     e.g. for 300 meter pixels a search radius of 5 is a good choice. This parameter is ignored
     *                     if the source product is not geo-coded.
     */
    public PixelGeoCoding(final Band latBand, final Band lonBand, final String validMask, final int searchRadius) {
        Guardian.assertNotNull("latBand", latBand);
        Guardian.assertNotNull("lonBand", lonBand);
        if (latBand.getProduct() == null) {
            throw new IllegalArgumentException("latBand.getProduct() == null");
        }
        if (lonBand.getProduct() == null) {
            throw new IllegalArgumentException("lonBand.getProduct() == null");
        }
        // Note that if two bands are of the same product, they also have the same raster size
        if (latBand.getProduct() != lonBand.getProduct()) {
            throw new IllegalArgumentException("latBand.getProduct() != lonBand.getProduct()");
        }
        if (latBand.getProduct().getSceneRasterWidth() < 2 || latBand.getProduct().getSceneRasterHeight() < 2) {
            throw new IllegalArgumentException(
                    "latBand.getProduct().getSceneRasterWidth() < 2 || latBand.getProduct().getSceneRasterHeight() < 2");
        }
        this.latBand = latBand;
        this.lonBand = lonBand;
        validMaskExpression = validMask;
        this.searchRadius = searchRadius;

        rasterWidth = latBand.getSceneRasterWidth();
        rasterHeight = latBand.getSceneRasterHeight();

        boolean disableTiling = "false".equalsIgnoreCase(System.getProperty(SYSPROP_PIXEL_GEO_CODING_USE_TILING));
        useTiling = !disableTiling; // the default since BEAM 4.10.3 is 'useTiling=true'

        // fraction accuracy is only implemented in tiling mode (because tiling mode will be the default soon)
        fractionAccuracy = useTiling && Boolean.getBoolean(SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY);

        pixelPosEstimator = latBand.getProduct().getGeoCoding();

        final int subSampling = 30;
        if (pixelPosEstimator == null && useTiling && rasterWidth / subSampling > 1 && rasterHeight / subSampling > 1) {

            final int tpGridWidth = rasterWidth / subSampling;
            final int tpGridHeight = rasterHeight / subSampling;
            final float tpOffsetX = rasterWidth % subSampling / 2 + 0.5f;
            final float tpOffsetY = rasterHeight % subSampling / 2 + 0.5f;
            final float unscaledImageOffsetX = -tpOffsetX + subSampling / 2.0f;
            final float unscaledImageOffsetY = -tpOffsetY + subSampling / 2.0f;
            final MultiLevelImage latImage = latBand.getGeophysicalImage();
            final MultiLevelImage lonImage = lonBand.getGeophysicalImage();

            float scale = 1.0f / subSampling;

            Interpolation nearestInterpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            final RenderedOp tempLatOffsetImg = ScaleDescriptor.create(latImage, 1.0f, 1.0f,
                                                                       unscaledImageOffsetX, unscaledImageOffsetY,
                                                                       nearestInterpolation, null);
            final RenderedOp tempLatImg = ScaleDescriptor.create(tempLatOffsetImg, scale, scale, 0f, 0f,
                                                                 nearestInterpolation, null);

            final RenderedOp tempLonOffsetImg = ScaleDescriptor.create(lonImage, 1.0f, 1.0f,
                                                                       unscaledImageOffsetX, unscaledImageOffsetY,
                                                                       nearestInterpolation, null);
            final RenderedOp tempLonImg = ScaleDescriptor.create(tempLonOffsetImg, scale, scale, 0f, 0f,
                                                                 nearestInterpolation, null);

            final int minX = tempLatImg.getMinX();
            final int minY = tempLatImg.getMinY();
            int numTiePoints = tpGridWidth * tpGridHeight;
            final boolean containsAngles = true;

            final float[] latTiePoints = tempLatImg.getAsBufferedImage().getRaster().getPixels(minX, minY, tpGridWidth,
                                                                                               tpGridHeight,
                                                                                               new float[numTiePoints]);
            final float[] lonTiePoints = tempLonImg.getAsBufferedImage().getRaster().getPixels(minX, minY, tpGridWidth,
                                                                                               tpGridHeight,
                                                                                               new float[numTiePoints]);

            final TiePointGrid tpLatGrid = new TiePointGrid("lat", tpGridWidth, tpGridHeight, tpOffsetX, tpOffsetY,
                                                            subSampling, subSampling, latTiePoints, containsAngles);
            final TiePointGrid tpLonGrid = new TiePointGrid("lon", tpGridWidth, tpGridHeight, tpOffsetX, tpOffsetY,
                                                            subSampling, subSampling, lonTiePoints, containsAngles);
            pixelPosEstimator = new TiePointGeoCoding(tpLatGrid, tpLonGrid);
            estimatorCreatedInternally = true;
        } else {
            estimatorCreatedInternally = false;
        }

        if (pixelPosEstimator != null) {
            if (searchRadius < 2) {
                throw new IllegalArgumentException("searchRadius < 2");
            }
            crossingMeridianAt180 = pixelPosEstimator.isCrossingMeridianAt180();
            GeoPos p0 = pixelPosEstimator.getGeoPos(new PixelPos(0.5f, 0.5f), null);
            GeoPos p1 = pixelPosEstimator.getGeoPos(new PixelPos(1.5f, 0.5f), null);

            float r = (float) Math.cos(Math.toRadians(p1.lat));
            float dlat = Math.abs(p0.lat - p1.lat);
            float dlon = r * lonDiff(p0.lon, p1.lon);
            float delta = dlat * dlat + dlon * dlon;
            deltaThreshold = Math.sqrt(delta) * 2;

        }
        initialized = false;
    }

    /**
     * Constructs a new pixel-based geo-coding.
     * <p/>
     * <p><i>Use with care: This constructor fully loads the data given by the latitudes and longitudes bands and
     * the valid mask (if any) into memory.</i></p>
     *
     * @param latBand      the band providing the latitudes
     * @param lonBand      the band providing the longitudes
     * @param validMask    the valid mask expression used to identify valid lat/lon pairs, e.g. "NOT l1_flags.DUPLICATED".
     *                     Can be <code>null</code> if a valid mask is not used.
     * @param searchRadius the search radius in pixels, shall depend on the actual spatial scene resolution,
     *                     e.g. for 300 meter pixels a search radius of 5 is a good choice. This parameter is ignored
     *                     if the source product is not geo-coded.
     * @param pm           a monitor to inform the user about progress
     *
     * @throws IOException if an I/O error occurs while additional data is loaded from the source product
     */
    public PixelGeoCoding(final Band latBand, final Band lonBand, final String validMask, final int searchRadius,
                          ProgressMonitor pm) throws IOException {
        this(latBand, lonBand, validMask, searchRadius);
        initData(latBand, lonBand, validMask, pm);
        initialized = true;
    }

    private void initData(final Band latBand, final Band lonBand,
                          final String validMaskExpr, ProgressMonitor pm) throws IOException {

        if (useTiling) {
            RenderedImage validMask = null;
            if (validMaskExpr != null && validMaskExpr.trim().length() > 0 && pixelPosEstimator != null) {
                validMask = ImageManager.getInstance().getMaskImage(validMaskExpr, latBand.getProduct());
            }
            latLonImage = new LatLonImage(this.latBand.getGeophysicalImage(), this.lonBand.getGeophysicalImage(),
                                          validMask, pixelPosEstimator);
        } else {
            try {
                pm.beginTask("Preparing data for pixel based geo-coding...", 4);
                latGrid = PixelGrid.create(latBand, SubProgressMonitor.create(pm, 1));
                lonGrid = PixelGrid.create(lonBand, SubProgressMonitor.create(pm, 1));
                if (validMaskExpr != null && validMaskExpr.trim().length() > 0) {
                    final BitRaster validMask = latBand.getProduct().createValidMask(validMaskExpr,
                                                                                     SubProgressMonitor.create(pm, 1));
                    fillInvalidGaps(new RasterDataNode.ValidMaskValidator(rasterHeight, 0, validMask),
                                    (float[]) latGrid.getDataElems(),
                                    (float[]) lonGrid.getDataElems(), SubProgressMonitor.create(pm, 1));
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * <p>Fills the gaps in the given latitude and longitude data buffers.
     * The method shall fill in reasonable a latitude and longitude value at all positions where
     * {@link IndexValidator#validateIndex(int) validator.validateIndex(pixelIndex)} returns false.</p>
     * <p/>
     * <p>The default implementation uses the underlying {@link #getPixelPosEstimator() estimator} (if any)
     * to find default values for the gaps.</p>
     *
     * @param validator the pixel validator, never null
     * @param latElems  the latitude data buffer in row-major order
     * @param lonElems  the longitude data buffer in row-major order
     * @param pm        a monitor to inform the user about progress
     */
    protected void fillInvalidGaps(final IndexValidator validator,
                                   final float[] latElems,
                                   final float[] lonElems, ProgressMonitor pm) {
        if (pixelPosEstimator != null) {
            try {
                pm.beginTask("Filling invalid pixel gaps", rasterHeight);
                final PixelPos pixelPos = new PixelPos();
                GeoPos geoPos = new GeoPos();
                for (int y = 0; y < rasterHeight; y++) {
                    for (int x = 0; x < rasterWidth; x++) {
                        int i = y * rasterWidth + x;
                        if (!validator.validateIndex(i)) {
                            pixelPos.x = x;
                            pixelPos.y = y;
                            geoPos = pixelPosEstimator.getGeoPos(pixelPos, geoPos);
                            latElems[i] = geoPos.lat;
                            lonElems[i] = geoPos.lon;
                        }
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Computes an estimation of the memory required to create an instance of this class for the given product.
     * The estimation is returned in bytes.
     *
     * @return an estimation of the required memory in bytes
     */
    public static long getRequiredMemory(Product product, boolean usesValidMask) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return 0;
        }
        final long sizeofFloat = 4;
        final long pixelCount = product.getSceneRasterWidth() * product.getSceneRasterHeight();
        // lat + lon band converted to 32-bit float tie-point data
        long size = 2 * sizeofFloat * pixelCount;
        if (geoCoding.isCrossingMeridianAt180()) {
            // additional 32-bit float sine and cosine grids for to lon grid
            size += 2 * sizeofFloat * pixelCount;
        }
        if (usesValidMask) {
            // additional 1-bit data mask
            size += pixelCount / 8;
        }
        return size;
    }

    @Override
    public Band getLatBand() {
        return latBand;
    }

    @Override
    public Band getLonBand() {
        return lonBand;
    }

    @Override
    public String getValidMask() {
        return validMaskExpression;
    }

    /**
     * Gets the underlying geo-coding used as pixel position estimator.
     *
     * @return the underlying delegate geo-coding, can be null
     */
    @Override
    public GeoCoding getPixelPosEstimator() {
        if (estimatorCreatedInternally) {
            return null;
        }
        return pixelPosEstimator;
    }

    /**
     * Gets the search radius used by this geo-coding.
     *
     * @return the search radius in pixels
     */
    @Override
    public int getSearchRadius() {
        return searchRadius;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        if (crossingMeridianAt180 == null) {
            crossingMeridianAt180 = false;
            final PixelPos[] pixelPoses = ProductUtils.createPixelBoundary(lonBand, null, 1);
            try {
                float[] firstLonValue = new float[1];
                lonBand.readPixels(0, 0, 1, 1, firstLonValue);
                float[] secondLonValue = new float[1];
                for (int i = 1; i < pixelPoses.length; i++) {
                    final PixelPos pixelPos = pixelPoses[i];
                    lonBand.readPixels((int) pixelPos.x, (int) pixelPos.y, 1, 1, secondLonValue);
                    if (Math.abs(firstLonValue[0] - secondLonValue[0]) > 180) {
                        crossingMeridianAt180 = true;
                        break;
                    }
                    firstLonValue[0] = secondLonValue[0];
                }
            } catch (IOException e) {
                throw new IllegalStateException("raster data is not readable", e);
            }
        }
        return crossingMeridianAt180;
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        initialize();
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            if (pixelPosEstimator != null) {
                getPixelPosUsingEstimator(geoPos, pixelPos);
            } else {
                getPixelPosUsingQuadTreeSearch(geoPos, pixelPos);
            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos the return value.
     */
    public void getPixelPosUsingEstimator(final GeoPos geoPos, PixelPos pixelPos) {
        initialize();

        pixelPos = pixelPosEstimator.getPixelPos(geoPos, pixelPos);
        if (!pixelPos.isValid()) {
            getPixelPosUsingQuadTreeSearch(geoPos, pixelPos);
            return;
        }
        final int x0 = (int) Math.floor(pixelPos.x);
        final int y0 = (int) Math.floor(pixelPos.y);
        if (x0 >= 0 && x0 < rasterWidth && y0 >= 0 && y0 < rasterHeight) {
            final float lat0 = geoPos.lat;
            final float lon0 = geoPos.lon;

            pixelPos.setLocation(x0, y0);
            int y1;
            int x1;
            float minDelta;
            int cycles = 0;
            do {
                x1 = (int) Math.floor(pixelPos.x);
                y1 = (int) Math.floor(pixelPos.y);
                minDelta = findBestPixel(x1, y1, lat0, lon0, pixelPos);
            }
            while (++cycles < MAX_SEARCH_CYCLES && (x1 != (int) pixelPos.x || y1 != (int) pixelPos.y) && bestPixelIsOnSearchBorder(
                    x1, y1, pixelPos));
            if (Math.sqrt(minDelta) < deltaThreshold) {
                pixelPos.setLocation(pixelPos.x + 0.5f, pixelPos.y + 0.5f);
            } else {
                pixelPos.setInvalid();
            }
        }
    }

    private boolean bestPixelIsOnSearchBorder(int x0, int y0, PixelPos bestPixel) {
        final int diffX = Math.abs((int) bestPixel.x - x0);
        final int diffY = Math.abs((int) bestPixel.y - y0);
        return diffX > (searchRadius - 2) || diffY > (searchRadius - 2);
    }

    private float findBestPixel(int x0, int y0, float lat0, float lon0, PixelPos bestPixel) {
        int x1 = x0 - searchRadius;
        int y1 = y0 - searchRadius;
        int x2 = x0 + searchRadius;
        int y2 = y0 + searchRadius;
        x1 = Math.max(x1, 0);
        y1 = Math.max(y1, 0);
        x2 = Math.min(x2, rasterWidth - 1);
        y2 = Math.min(y2, rasterHeight - 1);
        float r = (float) Math.cos(lat0 * D2R);

        if (useTiling) {
            Rectangle rect = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
            Raster latLonData = latLonImage.getData(rect);
            ComponentSampleModel sampleModel = (ComponentSampleModel) latLonData.getSampleModel();
            DataBufferFloat dataBuffer = (DataBufferFloat) latLonData.getDataBuffer();
            float[][] bankData = dataBuffer.getBankData();
            int sampleModelTranslateX = latLonData.getSampleModelTranslateX();
            int sampleModelTranslateY = latLonData.getSampleModelTranslateY();
            int scanlineStride = sampleModel.getScanlineStride();
            int pixelStride = sampleModel.getPixelStride();

            int bankDataIndex = (y0 - sampleModelTranslateY) * scanlineStride + (x0 - sampleModelTranslateX) * pixelStride;
            float lat = bankData[0][bankDataIndex];
            float lon = bankData[1][bankDataIndex];

            float dlat = Math.abs(lat - lat0);
            float dlon = r * lonDiff(lon, lon0);
            float minDelta = dlat * dlat + dlon * dlon;

            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    if (!(x == x0 && y == y0)) {
                        bankDataIndex = (y - sampleModelTranslateY) * scanlineStride + (x - sampleModelTranslateX) * pixelStride;
                        lat = bankData[0][bankDataIndex];
                        lon = bankData[1][bankDataIndex];

                        dlat = Math.abs(lat - lat0);
                        dlon = r * lonDiff(lon, lon0);
                        float delta = dlat * dlat + dlon * dlon;
                        if (delta < minDelta) {
                            minDelta = delta;
                            bestPixel.setLocation(x, y);
                        } else if (delta == minDelta && Math.abs(x - x0) + Math.abs(y - y0) > Math.abs(
                                bestPixel.x - x0) + Math.abs(bestPixel.y - y0)) {
                            bestPixel.setLocation(x, y);
                        }
                    }
                }
            }
            return minDelta;
        } else {
            final float[] latArray = (float[]) latGrid.getRasterData().getElems();
            final float[] lonArray = (float[]) lonGrid.getRasterData().getElems();

            int i = rasterWidth * y0 + x0;
            float lat = latArray[i];
            float lon = lonArray[i];

            float dlat = Math.abs(lat - lat0);
            float dlon = r * lonDiff(lon, lon0);
            float minDelta = dlat * dlat + dlon * dlon;

            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    if (!(x == x0 && y == y0)) {
                        i = rasterWidth * y + x;
                        lat = latArray[i];
                        lon = lonArray[i];
                        dlat = Math.abs(lat - lat0);
                        dlon = r * lonDiff(lon, lon0);
                        float delta = dlat * dlat + dlon * dlon;
                        if (delta < minDelta) {
                            minDelta = delta;
                            bestPixel.setLocation(x, y);
                        }
                    }
                }
            }
            return minDelta;
        }
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     * This algorithm
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos the return value
     */
    public void getPixelPosUsingQuadTreeSearch(final GeoPos geoPos, PixelPos pixelPos) {
        initialize();

        final Result result = new Result();
        boolean pixelFound = quadTreeSearch(0,
                                            geoPos.lat, geoPos.lon,
                                            0, 0,
                                            rasterWidth,
                                            rasterHeight,
                                            result);

        if (pixelFound) {
            pixelPos.setLocation(result.x + 0.5f, result.y + 0.5f);
        } else {
            pixelPos.setInvalid();
        }
    }

    private synchronized void initialize() {
        if (!initialized) {
            try {
                initData(latBand, lonBand, validMaskExpression, ProgressMonitor.NULL);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to initialse data for pixel geo-coding", e);
            }
            initialized = true;
        }
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the geographical position as lat/lon.
     */
    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        initialize();
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (pixelPos.isValid()) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());
            if (x0 >= 0 && x0 < rasterWidth && y0 >= 0 && y0 < rasterHeight) {
                if (fractionAccuracy) { // implies tiling mode
                    if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == rasterWidth - 1) {
                        x0 -= 1;
                    }
                    if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == rasterHeight - 1) {
                        y0 -= 1;
                    }
                    final float wx = pixelPos.x - (x0 + 0.5f);
                    final float wy = pixelPos.y - (y0 + 0.5f);
                    final Raster latLonData = latLonImage.getData(new Rectangle(x0, y0, 2, 2));
                    final float lat = interpolate(wx, wy, latLonData, 0);
                    final float lon = interpolate(wx, wy, latLonData, 1);
                    geoPos.setLocation(lat, lon);
                } else {
                    getGeoPosInternal(x0, y0, geoPos);
                }
            } else {
                if (pixelPosEstimator != null) {
                    return pixelPosEstimator.getGeoPos(pixelPos, geoPos);
                }
            }
        }
        return geoPos;
    }

    private float interpolate(float wx, float wy, Raster raster, int band) {
        final int x0 = raster.getMinX();
        final int x1 = x0 + 1;
        final int y0 = raster.getMinY();
        final int y1 = y0 + 1;
        final float d00 = raster.getSampleFloat(x0, y0, band);
        final float d10 = raster.getSampleFloat(x1, y0, band);
        final float d01 = raster.getSampleFloat(x0, y1, band);
        final float d11 = raster.getSampleFloat(x1, y1, band);

        return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PixelGeoCoding that = (PixelGeoCoding) o;

        if (searchRadius != that.searchRadius) {
            return false;
        }
        if (!latBand.equals(that.latBand)) {
            return false;
        }
        if (!lonBand.equals(that.lonBand)) {
            return false;
        }
        if (validMaskExpression != null ? !validMaskExpression.equals(
                that.validMaskExpression) : that.validMaskExpression != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latBand.hashCode();
        result = 31 * result + lonBand.hashCode();
        result = 31 * result + (validMaskExpression != null ? validMaskExpression.hashCode() : 0);
        result = 31 * result + searchRadius;
        return result;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public synchronized void dispose() {
        if (latGrid != null) {
            latGrid.dispose();
            latGrid = null;
        }
        if (lonGrid != null) {
            lonGrid.dispose();
            lonGrid = null;
        }
        if (latLonImage != null) {
            latLonImage.dispose();
            latLonImage = null;
        }
        // Don't dispose the estimator, if it is not ours!
        if (estimatorCreatedInternally) {
            pixelPosEstimator.dispose();
        }
        pixelPosEstimator = null;

    }

    private boolean quadTreeSearch(final int depth,
                                   final float lat,
                                   final float lon,
                                   final int x, final int y,
                                   final int w, final int h,
                                   final Result result) {
        if (w < 2 || h < 2) {
            return false;
        }

        final int x1 = x;
        final int x2 = x1 + w - 1;

        final int y1 = y;
        final int y2 = y1 + h - 1;

        GeoPos geoPos = new GeoPos();
        getGeoPosInternal(x1, y1, geoPos);
        final float lat0 = geoPos.lat;
        float lon0 = geoPos.lon;
        getGeoPosInternal(x1, y2, geoPos);
        final float lat1 = geoPos.lat;
        float lon1 = geoPos.lon;
        getGeoPosInternal(x2, y1, geoPos);
        final float lat2 = geoPos.lat;
        float lon2 = geoPos.lon;
        getGeoPosInternal(x2, y2, geoPos);
        final float lat3 = geoPos.lat;
        float lon3 = geoPos.lon;

        final float epsL = EPS;
        final float latMin = min(lat0, min(lat1, min(lat2, lat3))) - epsL;
        final float latMax = max(lat0, max(lat1, max(lat2, lat3))) + epsL;
        float lonMin;
        float lonMax;
        if (isCrossingMeridianInsideQuad(isCrossingMeridianAt180(), lon0, lon1, lon2, lon3)) {
            final float signumLon = Math.signum(lon);
            if (signumLon > 0f) {
                // position is in a region with positive longitudes, so cut negative longitudes from quad area
                lonMax = 180.0f;
                lonMin = getPositiveLonMin(lon0, lon1, lon2, lon3);
            } else {
                // position is in a region with negative longitudes, so cut positive longitudes from quad area
                lonMin = -180.0f;
                lonMax = getNegativeLonMax(lon0, lon1, lon2, lon3);
            }
        } else {
            lonMin = min(lon0, min(lon1, min(lon2, lon3))) - epsL;
            lonMax = max(lon0, max(lon1, max(lon2, lon3))) + epsL;
        }

        boolean pixelFound = false;
        final boolean definitelyOutside = lat < latMin || lat > latMax || lon < lonMin || lon > lonMax;
        if (!definitelyOutside) {
            if (w == 2 && h == 2) {
                final float f = (float) Math.cos(lat * D2R);
                if (result.update(x1, y1, sqr(lat - lat0, f * (lon - lon0)))) {
                    pixelFound = true;
                }
                if (result.update(x1, y2, sqr(lat - lat1, f * (lon - lon1)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y1, sqr(lat - lat2, f * (lon - lon2)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y2, sqr(lat - lat3, f * (lon - lon3)))) {
                    pixelFound = true;
                }
            } else if (w >= 2 && h >= 2) {
                pixelFound = quadTreeRecursion(depth, lat, lon, x1, y1, w, h, result);
            }
        }

        if (TRACE) {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println(
                    depth + ": (" + x + "," + y + ") (" + w + "," + h + ") " + definitelyOutside + "  " + pixelFound);
        }
        return pixelFound;
    }

    static float getNegativeLonMax(float lon0, float lon1, float lon2, float lon3) {
        float lonMax;
        lonMax = -180.0f;
        if (lon0 < 0.0f) {
            lonMax = lon0;
        }
        if (lon1 < 0.0f) {
            lonMax = max(lon1, lonMax);
        }
        if (lon2 < 0.0f) {
            lonMax = max(lon2, lonMax);
        }
        if (lon3 < 0.0f) {
            lonMax = max(lon3, lonMax);
        }
        return lonMax;
    }

    static float getPositiveLonMin(float lon0, float lon1, float lon2, float lon3) {
        float lonMin;
        lonMin = 180.0f;
        if (lon0 >= 0.0f) {
            lonMin = lon0;
        }
        if (lon1 >= 0.0f) {
            lonMin = min(lon1, lonMin);
        }
        if (lon2 >= 0.0f) {
            lonMin = min(lon2, lonMin);
        }
        if (lon3 >= 0.0f) {
            lonMin = min(lon3, lonMin);
        }
        return lonMin;
    }

    static boolean isCrossingMeridianInsideQuad(boolean crossingMeridianInsideProduct, float lon0, float lon1,
                                                float lon2, float lon3) {
        if (!crossingMeridianInsideProduct) {
            return false;
        }
        float lonMin = min(lon0, min(lon1, min(lon2, lon3)));
        float lonMax = max(lon0, max(lon1, max(lon2, lon3)));

        return Math.abs(lonMax - lonMin) > 180.0;
    }

    private void getGeoPosInternal(int pixelX, int pixelY, GeoPos geoPos) {
        if (useTiling) {
            final int x = latLonImage.getMinX() + pixelX;
            final int y = latLonImage.getMinY() + pixelY;
            Raster data = latLonImage.getData(new Rectangle(x, y, 1, 1));
            float lat = data.getSampleFloat(x, y, 0);
            float lon = data.getSampleFloat(x, y, 1);
            geoPos.setLocation(lat, lon);
        } else {
            int i = rasterWidth * pixelY + pixelX;
            final float lat = latGrid.getRasterData().getElemFloatAt(i);
            final float lon = lonGrid.getRasterData().getElemFloatAt(i);
            geoPos.setLocation(lat, lon);
        }
    }

    private boolean quadTreeRecursion(final int depth,
                                      final float lat, final float lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {
        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }

        final boolean b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }


    private static float min(final float a, final float b) {
        return (a <= b) ? a : b;
    }

    private static float max(final float a, final float b) {
        return (a >= b) ? a : b;
    }

    private static float sqr(final float dx, final float dy) {
        return dx * dx + dy * dy;
    }

    /*
     * Computes the absolute and smaller difference for two angles.
     * @param a1 the first angle in the degrees (-180 <= a1 <= 180)
     * @param a2 the second angle in degrees (-180 <= a2 <= 180)
     * @return the difference between 0 and 180 degrees
     */

    private static float lonDiff(float a1, float a2) {
        float d = a1 - a2;
        if (d < 0.0f) {
            d = -d;
        }
        if (d > 180.0f) {
            d = 360.0f - d;
        }
        return d;
    }

    // todo - (nf) do not delete this method, it could be used later, if we want to determine x,y fractions

    private static boolean getPixelPos(final float lat, final float lon,
                                       final float[] lata, final float[] lona,
                                       final int[] xa, final int[] ya,
                                       final PixelPos pixelPos) {
        final Matrix mA = new Matrix(3, 3);
        mA.set(0, 0, 1.0);
        mA.set(1, 0, 1.0);
        mA.set(2, 0, 1.0);
        mA.set(0, 1, lata[0]);
        mA.set(1, 1, lata[1]);
        mA.set(2, 1, lata[2]);
        mA.set(0, 2, lona[0]);
        mA.set(1, 2, lona[1]);
        mA.set(2, 2, lona[2]);
        final LUDecomposition decomp = new LUDecomposition(mA);

        final Matrix mB = new Matrix(3, 1);

        mB.set(0, 0, ya[0] + 0.5);
        mB.set(1, 0, ya[1] + 0.5);
        mB.set(2, 0, ya[2] + 0.5);
        Exception err = null;

        Matrix mY = null;
        try {
            mY = decomp.solve(mB);
        } catch (Exception e) {
            System.out.printf("y1 = %d, y2 = %d, y3 = %d%n", ya[0], ya[1], ya[2]);
            err = e;

        }

        mB.set(0, 0, xa[0] + 0.5);
        mB.set(1, 0, xa[1] + 0.5);
        mB.set(2, 0, xa[2] + 0.5);
        Matrix mX = null;
        try {
            mX = decomp.solve(mB);
        } catch (Exception e) {
            System.out.printf("x1 = %d, x2 = %d, x3 = %d%n", xa[0], xa[1], xa[2]);
            err = e;
        }

        if (err != null) {
            return false;
        }


        final float fx = (float) (mX.get(0, 0) + mX.get(1, 0) * lat + mX.get(2, 0) * lon);
        final float fy = (float) (mY.get(0, 0) + mY.get(1, 0) * lat + mY.get(2, 0) * lon);

        pixelPos.setLocation(fx, fy);
        return true;
    }

    private void trace(final int x0, final int y0, int bestX, int bestY, int bestCount) {
        if (bestCount > 0) {
            int dx = bestX - x0;
            int dy = bestY - y0;
            if (Math.abs(dx) >= searchRadius || Math.abs(dy) >= searchRadius) {
                Debug.trace("WARNING: search radius reached at " +
                            "(x0 = " + x0 + ", y0 = " + y0 + "), " +
                            "(dx = " + dx + ", dy = " + dy + "), " +
                            "#best = " + bestCount);
            }
        } else {
            Debug.trace("WARNING: no better pixel found at " +
                        "(x0 = " + x0 + ", y0 = " + y0 + ")");
        }
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link org.esa.beam.framework.dataio.ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final Band srcLatBand = getLatBand();
        final Product destProduct = destScene.getProduct();
        Band latBand = destProduct.getBand(srcLatBand.getName());
        if (latBand == null) {
            latBand = GeoCodingFactory.createSubset(srcLatBand, destScene, subsetDef);
            destProduct.addBand(latBand);
        }
        final Band srcLonBand = getLonBand();
        Band lonBand = destProduct.getBand(srcLonBand.getName());
        if (lonBand == null) {
            lonBand = GeoCodingFactory.createSubset(srcLonBand, destScene, subsetDef);
            destProduct.addBand(lonBand);
        }
        if (pixelPosEstimator instanceof AbstractGeoCoding && !estimatorCreatedInternally) {
            AbstractGeoCoding origGeoCoding = (AbstractGeoCoding) pixelPosEstimator;
            origGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);
        }
        String validMaskExpression = getValidMask();
        try {
            if (validMaskExpression != null) {
                GeoCodingFactory.copyReferencedRasters(validMaskExpression, srcScene, destScene, subsetDef);
            }
        } catch (ParseException ignored) {
            validMaskExpression = null;
        }
        destScene.setGeoCoding(new PixelGeoCoding(latBand, lonBand,
                                                  validMaskExpression,
                                                  getSearchRadius()));
        return true;
    }

    private static class PixelGrid extends TiePointGrid {

        /**
         * Constructs a new <code>TiePointGrid</code> with the given tie point grid properties.
         *
         * @param p            the product which will become the owner of the created PixelGrid
         * @param name         the name of the new object
         * @param gridWidth    the width of the tie-point grid in pixels
         * @param gridHeight   the height of the tie-point grid in pixels
         * @param offsetX      the X co-ordinate of the first (upper-left) tie-point in pixels
         * @param offsetY      the Y co-ordinate of the first (upper-left) tie-point in pixels
         * @param subSamplingX the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
         *                     this tie-pint grid belongs to. Must not be less than one.
         * @param subSamplingY the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
         *                     this tie-pint grid belongs to. Must not be less than one.
         * @param tiePoints    the tie-point data values, must be an array of the size <code>gridWidth * gridHeight</code>
         */
        private PixelGrid(final Product p,
                          final String name,
                          final int gridWidth, final int gridHeight,
                          final float offsetX, final float offsetY,
                          final float subSamplingX, final float subSamplingY,
                          final float[] tiePoints) {
            super(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints, false);
            // make this grid a component of the product without actually adding it to the product
            setOwner(p);
        }

        private static PixelGrid create(final Band b, ProgressMonitor pm) throws IOException {
            final int w = b.getRasterWidth();
            final int h = b.getRasterHeight();
            final float[] pixels = new float[w * h];
            b.readPixels(0, 0, w, h, pixels, pm);
            return new PixelGrid(b.getProduct(), b.getName(), w, h, 0.5f, 0.5f, 1.0f, 1.0f, pixels);
        }
    }

    private static class Result {

        public static final float INVALID = Float.MAX_VALUE;

        private int x;
        private int y;
        private float delta;

        private Result() {
            delta = INVALID;
        }

        public final boolean update(final int x, final int y, final float delta) {
            final boolean b = delta < this.delta;
            if (b) {
                this.x = x;
                this.y = y;
                this.delta = delta;
            }
            return b;
        }

        @Override
        public String toString() {
            return "Result[" + x + ", " + y + ", " + delta + "]";
        }
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        if (pixelPosEstimator != null) {
            return pixelPosEstimator.getDatum();
        }
        return Datum.WGS_84;
    }


    private static class LatLonImage extends PointOpImage {

        private final GeoCoding estimator;

        private final RasterFormatTag latRasterFormatTag;
        private final RasterFormatTag lonRasterFormatTag;
        private final RasterFormatTag maskRasterFormatTag;
        private final RasterFormatTag targetRasterFormatTag;

        private static ImageLayout layout(RenderedImage source) {
            final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT,
                                                                                  source.getTileWidth(),
                                                                                  source.getTileHeight(),
                                                                                  2);
            final ImageLayout imageLayout = new ImageLayout();
            imageLayout.setSampleModel(sampleModel);
            return imageLayout;

        }

        private static RenderingHints renderingHints(ImageLayout imageLayout) {
            final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
            hints.add(new RenderingHints(JAI.KEY_TILE_CACHE, JAI.getDefaultInstance().getTileCache()));
            return hints;
        }

        private static Vector<RenderedImage> vector(RenderedImage image1, RenderedImage image2, RenderedImage image3) {
            Vector<RenderedImage> v = new Vector<RenderedImage>(3);
            v.addElement(image1);
            v.addElement(image2);
            if (image3 != null) {
                v.addElement(image3);
            }
            return v;
        }

        private LatLonImage(RenderedImage latSrc, RenderedImage lonSrc, RenderedImage validSrc, GeoCoding estimator) {
            this(latSrc, lonSrc, validSrc, layout(latSrc), estimator);
        }

        private LatLonImage(RenderedImage latSrc, RenderedImage lonSrc, RenderedImage maskSrc, ImageLayout imageLayout,
                            GeoCoding estimator) {
            super(vector(latSrc, lonSrc, maskSrc), imageLayout, renderingHints(imageLayout), true);
            this.estimator = estimator;
            latRasterFormatTag = getRasterFormatTag(latSrc.getSampleModel());
            lonRasterFormatTag = getRasterFormatTag(lonSrc.getSampleModel());
            if (maskSrc != null) {
                maskRasterFormatTag = getRasterFormatTag(maskSrc.getSampleModel());
            } else {
                maskRasterFormatTag = null;
            }
            targetRasterFormatTag = getRasterFormatTag(getSampleModel());
        }

        private static RasterFormatTag getRasterFormatTag(SampleModel sampleModel1) {
            int compatibleTag = RasterAccessor.findCompatibleTag(null, sampleModel1);
            return new RasterFormatTag(sampleModel1, compatibleTag);
        }

        @Override
        protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
            RasterAccessor latAcc = new RasterAccessor(sources[0], destRect, latRasterFormatTag,
                                                       getSourceImage(0).getColorModel());
            RasterAccessor lonAcc = new RasterAccessor(sources[1], destRect, lonRasterFormatTag,
                                                       getSourceImage(1).getColorModel());
            RasterAccessor maskAcc = null;
            if (maskRasterFormatTag != null) {
                maskAcc = new RasterAccessor(sources[2], destRect, maskRasterFormatTag,
                                             getSourceImage(2).getColorModel());
            }
            RasterAccessor destAcc = new RasterAccessor(dest, destRect, targetRasterFormatTag, getColorModel());

            if (latAcc.getDataType() == DataBuffer.TYPE_DOUBLE) {
                processDoubleLoop(latAcc, lonAcc, maskAcc, destAcc, destRect);
            } else if (latAcc.getDataType() == DataBuffer.TYPE_FLOAT) {
                processFloatLoop(latAcc, lonAcc, maskAcc, destAcc, destRect);
            } else {
                throw new IllegalStateException("unsupported data type: " + latAcc.getDataType());
            }
            destAcc.copyDataToRaster();
        }

        private void processDoubleLoop(RasterAccessor latAcc, RasterAccessor lonAcc, RasterAccessor maskAcc,
                                       RasterAccessor destAcc, Rectangle destRect) {
            int latLineStride = latAcc.getScanlineStride();
            int latPixelStride = latAcc.getPixelStride();
            int[] sLatBandOffsets = latAcc.getBandOffsets();
            double[][] latData = latAcc.getDoubleDataArrays();

            int lonLineStride = lonAcc.getScanlineStride();
            int lonPixelStride = lonAcc.getPixelStride();
            int[] sLonBandOffsets = lonAcc.getBandOffsets();
            double[][] lonData = lonAcc.getDoubleDataArrays();

            int mLineOffset = 0;
            int mLineStride = 0;
            int mPixelStride = 0;
            byte[] m = null;
            if (maskAcc != null) {
                mLineStride = maskAcc.getScanlineStride();
                mPixelStride = maskAcc.getPixelStride();
                int[] mBandOffsets = maskAcc.getBandOffsets();
                byte[][] mData = maskAcc.getByteDataArrays();
                m = mData[0];
                mLineOffset = mBandOffsets[0];
            }
            int dwidth = destAcc.getWidth();
            int dheight = destAcc.getHeight();
            int dLineStride = destAcc.getScanlineStride();
            int dPixelStride = destAcc.getPixelStride();
            int[] dBandOffsets = destAcc.getBandOffsets();
            float[][] dData = destAcc.getFloatDataArrays();

            double[] lat = latData[0];
            double[] lon = lonData[0];
            @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
            float[] dLat = dData[0];
            @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
            float[] dLon = dData[1];

            int sLatLineOffset = sLatBandOffsets[0];
            int sLonLineOffset = sLonBandOffsets[0];
            int dLatLineOffset = dBandOffsets[0];
            int dLonLineOffset = dBandOffsets[1];

            PixelPos pixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();

            for (int y = 0; y < dheight; y++) {
                int sLatPixelOffset = sLatLineOffset;
                int sLonPixelOffset = sLonLineOffset;
                int mPixelOffset = mLineOffset;
                int dLatPixelOffset = dLatLineOffset;
                int dLonPixelOffset = dLonLineOffset;

                sLatLineOffset += latLineStride;
                sLonLineOffset += lonLineStride;
                mLineOffset += mLineStride;
                dLatLineOffset += dLineStride;
                dLonLineOffset += dLineStride;

                for (int x = 0; x < dwidth; x++) {

                    if (m != null && m[mPixelOffset] == 0) {
                        int x0 = x + destRect.x;
                        int y0 = y + destRect.y;
                        pixelPos.setLocation(x0, y0);
                        estimator.getGeoPos(pixelPos, geoPos);
                        dLat[dLatPixelOffset] = geoPos.lat;
                        dLon[dLonPixelOffset] = geoPos.lon;
                    } else {
                        dLat[dLatPixelOffset] = (float) lat[sLatPixelOffset];
                        dLon[dLonPixelOffset] = (float) lon[sLonPixelOffset];
                    }

                    sLatPixelOffset += latPixelStride;
                    sLonPixelOffset += lonPixelStride;
                    mPixelOffset += mPixelStride;
                    dLatPixelOffset += dPixelStride;
                    dLonPixelOffset += dPixelStride;
                }
            }
        }

        private void processFloatLoop(RasterAccessor latAcc, RasterAccessor lonAcc, RasterAccessor maskAcc,
                                      RasterAccessor destAcc, Rectangle destRect) {
            int latLineStride = latAcc.getScanlineStride();
            int latPixelStride = latAcc.getPixelStride();
            int[] sLatBandOffsets = latAcc.getBandOffsets();
            float[][] latData = latAcc.getFloatDataArrays();

            int lonLineStride = lonAcc.getScanlineStride();
            int lonPixelStride = lonAcc.getPixelStride();
            int[] sLonBandOffsets = lonAcc.getBandOffsets();
            float[][] lonData = lonAcc.getFloatDataArrays();

            int mLineOffset = 0;
            int mLineStride = 0;
            int mPixelStride = 0;
            byte[] m = null;
            if (maskAcc != null) {
                mLineStride = maskAcc.getScanlineStride();
                mPixelStride = maskAcc.getPixelStride();
                int[] mBandOffsets = maskAcc.getBandOffsets();
                byte[][] mData = maskAcc.getByteDataArrays();
                m = mData[0];
                mLineOffset = mBandOffsets[0];
            }
            int dwidth = destAcc.getWidth();
            int dheight = destAcc.getHeight();
            int dLineStride = destAcc.getScanlineStride();
            int dPixelStride = destAcc.getPixelStride();
            int[] dBandOffsets = destAcc.getBandOffsets();
            float[][] dData = destAcc.getFloatDataArrays();

            float[] lat = latData[0];
            float[] lon = lonData[0];
            @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
            float[] dLat = dData[0];
            @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
            float[] dLon = dData[1];

            int sLatLineOffset = sLatBandOffsets[0];
            int sLonLineOffset = sLonBandOffsets[0];
            int dLatLineOffset = dBandOffsets[0];
            int dLonLineOffset = dBandOffsets[1];

            PixelPos pixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();

            for (int y = 0; y < dheight; y++) {
                int sLatPixelOffset = sLatLineOffset;
                int sLonPixelOffset = sLonLineOffset;
                int mPixelOffset = mLineOffset;
                int dLatPixelOffset = dLatLineOffset;
                int dLonPixelOffset = dLonLineOffset;

                sLatLineOffset += latLineStride;
                sLonLineOffset += lonLineStride;
                mLineOffset += mLineStride;
                dLatLineOffset += dLineStride;
                dLonLineOffset += dLineStride;

                for (int x = 0; x < dwidth; x++) {

                    if (m != null && m[mPixelOffset] == 0) {
                        int x0 = x + destRect.x;
                        int y0 = y + destRect.y;
                        pixelPos.setLocation(x0, y0);
                        estimator.getGeoPos(pixelPos, geoPos);
                        dLat[dLatPixelOffset] = geoPos.lat;
                        dLon[dLonPixelOffset] = geoPos.lon;
                    } else {
                        dLat[dLatPixelOffset] = lat[sLatPixelOffset];
                        dLon[dLonPixelOffset] = lon[sLonPixelOffset];
                    }

                    sLatPixelOffset += latPixelStride;
                    sLonPixelOffset += lonPixelStride;
                    mPixelOffset += mPixelStride;
                    dLatPixelOffset += dPixelStride;
                    dLonPixelOffset += dPixelStride;
                }
            }
        }

    }
}
