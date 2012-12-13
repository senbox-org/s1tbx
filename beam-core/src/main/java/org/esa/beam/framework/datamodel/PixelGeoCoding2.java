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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;


/**
 * The <code>PixelGeoCoding</code> is an implementation of a {@link org.esa.beam.framework.datamodel.GeoCoding} which uses
 * dedicated latitude and longitude bands in order to provide geographical positions
 * for <i>each</i> pixel. Unlike the {@link org.esa.beam.framework.datamodel.TiePointGeoCoding}</p>, which uses sub-sampled {@link org.esa.beam.framework.datamodel.TiePointGrid tie-point grids},
 * the  <code>PixelGeoCoding</code> class uses {@link org.esa.beam.framework.datamodel.Band bands}.</p>
 * <p/>
 * <p>This class is especially useful for high accuracy geo-coding, e.g. if geographical positions are computed for each pixel
 * by an upstream orthorectification.</p>
 * <p/>
 * <p>While the implementation of the {@link #getGeoPos(org.esa.beam.framework.datamodel.PixelPos, org.esa.beam.framework.datamodel.GeoPos)} is straight forward,
 * the {@link #getPixelPos(org.esa.beam.framework.datamodel.GeoPos, org.esa.beam.framework.datamodel.PixelPos)} uses two different search algorithms in order to
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
class PixelGeoCoding2 extends AbstractGeoCoding {

    /**
     * @since BEAM 4.9
     */
    private static final String SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY = "beam.pixelGeoCoding.fractionAccuracy";
    private static final int MAX_SEARCH_CYCLES = 10;
    private static final double D2R = Math.PI / 180.0;

    private Band latBand;
    private Band lonBand;
    private PixelPosEstimator pixelPosEstimator;
    private PlanarImage lonImage;
    private PlanarImage latImage;

    private final int rasterWidth;
    private final int rasterHeight;
    private final boolean fractionAccuracy;
    private final double pixelSize;

    /**
     * Constructs a new pixel-based geo-coding.
     * <p/>
     * <i>Use with care: In contrast to the other constructor this one loads the data not until first access to
     * {@link #getPixelPos(org.esa.beam.framework.datamodel.GeoPos, org.esa.beam.framework.datamodel.PixelPos)} or {@link #getGeoPos(org.esa.beam.framework.datamodel.PixelPos, org.esa.beam.framework.datamodel.GeoPos)}. </i>
     *
     * @param latBand the band providing the latitudes
     * @param lonBand the band providing the longitudes
     */
    PixelGeoCoding2(final Band latBand, final Band lonBand) {
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

        rasterWidth = latBand.getSceneRasterWidth();
        rasterHeight = latBand.getSceneRasterHeight();

        // fraction accuracy is only implemented in tiling mode (because tiling mode will be the default soon)
        fractionAccuracy = Boolean.getBoolean(SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY);

        lonImage = lonBand.getGeophysicalImage();
        latImage = latBand.getGeophysicalImage();
        pixelPosEstimator = new PixelPosEstimator(lonImage,
                                                  latImage,
                                                  null,
                                                  0.5, 10.0, new PixelPosEstimator.PixelSteppingFactory());
        final Dimension2D pixelDimension = pixelPosEstimator.getPixelDimension();
        final double pixelSizeX = pixelDimension.getWidth();
        final double pixelSizeY = pixelDimension.getHeight();
        pixelSize = pixelSizeX * pixelSizeX + pixelSizeY * pixelSizeY;
    }

    public Band getLatBand() {
        return latBand;
    }

    public Band getLonBand() {
        return lonBand;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
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
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            getPixelPosUsingEstimator(geoPos, pixelPos);
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
    void getPixelPosUsingEstimator(final GeoPos geoPos, PixelPos pixelPos) {
        final PixelPosEstimator.Approximation approximation = pixelPosEstimator.getPixelPos(geoPos, pixelPos);

        if (pixelPos.isValid()) {
            final int stepX = approximation.getStepping().getStepX();
            final int stepY = approximation.getStepping().getStepY();

            final int x0 = (int) Math.floor(pixelPos.x);
            final int y0 = (int) Math.floor(pixelPos.y);

            if (x0 >= 0 && x0 < rasterWidth && y0 >= 0 && y0 < rasterHeight) {
                final float lat0 = geoPos.lat;
                final float lon0 = geoPos.lon;

                pixelPos.setLocation(x0, y0);
                int y1;
                int x1;
                double minDelta;
                int cycles = 0;


                x1 = x0 - MAX_SEARCH_CYCLES * stepX;
                y1 = y0 - MAX_SEARCH_CYCLES * stepY;
                int x2 = x0 + MAX_SEARCH_CYCLES * stepX;
                int y2 = y0 + MAX_SEARCH_CYCLES * stepY;
                x1 = Math.max(x1, 0);
                y1 = Math.max(y1, 0);
                x2 = Math.min(x2, rasterWidth - 1);
                y2 = Math.min(y2, rasterHeight - 1);

                final Rectangle rectangle = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
                final Raster latData = latImage.getData(rectangle);
                final Raster lonData = lonImage.getData(rectangle);

                do {
                    x1 = (int) Math.floor(pixelPos.x);
                    y1 = (int) Math.floor(pixelPos.y);
                    minDelta = findNearestPixel(lat0, lon0, x1, y1, stepX, stepY, pixelPos, latData, lonData);
                }
                while (++cycles < MAX_SEARCH_CYCLES && (x1 != (int) pixelPos.x || y1 != (int) pixelPos.y) && minDelta < 0.0);

                if (minDelta * minDelta < pixelSize) {
                    pixelPos.setLocation(pixelPos.x + 0.5f, pixelPos.y + 0.5f);
                } else {
                    pixelPos.setInvalid();
                }
            }
        }
    }

    private double findNearestPixel(double lat0, double lon0, int x0, int y0, int radiusX, int radiusY,
                                    PixelPos pixelPos, Raster latData, Raster lonData) {
        int x1 = x0 - radiusX;
        int y1 = y0 - radiusY;
        int x2 = x0 + radiusX;
        int y2 = y0 + radiusY;
        x1 = Math.max(x1, 0);
        y1 = Math.max(y1, 0);
        x2 = Math.min(x2, rasterWidth - 1);
        y2 = Math.min(y2, rasterHeight - 1);

        final double r = Math.cos(lat0 * D2R);

        double minDelta = Double.POSITIVE_INFINITY;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                final double lat = latData.getSampleDouble(x, y, 0);
                if (lat >= -90.0 && lat <= 90.0) {
                    final double lon = lonData.getSampleDouble(x, y, 0);
                    if (lon >= -180.0 && lon <= 180.0) {
                        final double dlat = Math.abs(lat - lat0);
                        final double dlon = r * lonDiff(lon, lon0);
                        // TODO - use these values to compute average pixel size for the rectangle and use this instead of 'pixelSize' field
                        // TODO - use arc distance
                        final double delta = dlat * dlat + dlon * dlon;
                        if (delta < minDelta) {
                            minDelta = delta;
                            x0 = x;
                            y0 = y;
                            pixelPos.setLocation(x, y);
                        } else if (delta == minDelta && Math.abs(x - x0) + Math.abs(y - y0) > Math.abs(
                                // TODO - what this is good for?
                                pixelPos.x - x0) + Math.abs(pixelPos.y - y0)) {
                            x0 = x;
                            y0 = y;
                            pixelPos.setLocation(x, y);
                        }
                    }
                }
            }
        }

        return (x0 == x1 || x0 == x2) && (y0 == y1 || y0 == y2) ? -minDelta : minDelta;
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
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (pixelPos.isValid()) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());
            if (x0 >= 0 && x0 < rasterWidth && y0 >= 0 && y0 < rasterHeight) {
                if (fractionAccuracy) {
                    if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == rasterWidth - 1) {
                        x0 -= 1;
                    }
                    if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == rasterHeight - 1) {
                        y0 -= 1;
                    }
                    final float wx = pixelPos.x - (x0 + 0.5f);
                    final float wy = pixelPos.y - (y0 + 0.5f);
                    final Raster latData = latImage.getData(new Rectangle(x0, y0, 2, 2));
                    final Raster lonData = lonImage.getData(new Rectangle(x0, y0, 2, 2));
                    final float lat = interpolate(wx, wy, latData, 0);
                    final float lon = interpolate(wx, wy, lonData, 0);
                    geoPos.setLocation(lat, lon);
                } else {
                    getGeoPosInternal(x0, y0, geoPos);
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
        // TODO - check if values are in [-90, 90] and [-180, 180]

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

        PixelGeoCoding2 that = (PixelGeoCoding2) o;

        if (!latBand.equals(that.latBand)) {
            return false;
        }
        if (!lonBand.equals(that.lonBand)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latBand.hashCode();
        result = 31 * result + lonBand.hashCode();
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
        pixelPosEstimator = null;
        latBand = null;
        lonBand = null;
        lonImage = null;
        latImage = null;
    }

    private void getGeoPosInternal(int pixelX, int pixelY, GeoPos geoPos) {
        final int x = lonImage.getMinX() + pixelX;
        final int y = lonImage.getMinY() + pixelY;
        final Raster lonData = lonImage.getData(new Rectangle(x, y, 1, 1));
        final Raster latData = latImage.getData(new Rectangle(x, y, 1, 1));
        final float lat = latData.getSampleFloat(x, y, 0);
        final float lon = lonData.getSampleFloat(x, y, 1);

        geoPos.setLocation(lat, lon);
    }

    /*
     * Computes the absolute and smaller difference for two angles.
     * @param a1 the first angle in the degrees (-180 <= a1 <= 180)
     * @param a2 the second angle in degrees (-180 <= a2 <= 180)
     * @return the difference between 0 and 180 degrees
     */

    private static double lonDiff(double a1, double a2) {
        double d = a1 - a2;
        if (d < 0.0) {
            d = -d;
        }
        if (d > 180.0) {
            d = 360.0 - d;
        }
        return d;
    }

    /**
     * Transfers the geo-coding of the {@link org.esa.beam.framework.datamodel.Scene srcScene} to the {@link org.esa.beam.framework.datamodel.Scene destScene} with respect to the given
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
            latBand = createSubset(srcLatBand, destScene, subsetDef);
            destProduct.addBand(latBand);
        }
        final Band srcLonBand = getLonBand();
        Band lonBand = destProduct.getBand(srcLonBand.getName());
        if (lonBand == null) {
            lonBand = createSubset(srcLonBand, destScene, subsetDef);
            destProduct.addBand(lonBand);
        }
        destScene.setGeoCoding(new PixelGeoCoding2(latBand, lonBand));
        return true;
    }

    private Band createSubset(Band srcBand, Scene destScene, ProductSubsetDef subsetDef) {
        Band band = new Band(srcBand.getName(),
                             srcBand.getDataType(),
                             destScene.getRasterWidth(),
                             destScene.getRasterHeight());
        ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        band.setSourceImage(getSourceImage(subsetDef, srcBand));
        return band;
    }

    private RenderedImage getSourceImage(ProductSubsetDef subsetDef, Band band) {
        RenderedImage image = band.getSourceImage();
        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            if (region != null) {
                float x = region.x;
                float y = region.y;
                float width = region.width;
                float height = region.height;
                image = CropDescriptor.create(image, x, y, width, height, null);
            }
            final int subSamplingX = subsetDef.getSubSamplingX();
            final int subSamplingY = subsetDef.getSubSamplingY();
            if (subSamplingX != 1 || subSamplingY != 1) {
                float scaleX = 1.0f / subSamplingX;
                float scaleY = 1.0f / subSamplingY;
                float transX = 0.0f;
                float transY = 0.0f;
                Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                image = ScaleDescriptor.create(image, scaleX, scaleY, transX, transY, interpolation, null);
            }
        }
        return image;
    }

    /**
     * Gets the datum, the reference point or surface against which {@link org.esa.beam.framework.datamodel.GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }
}
