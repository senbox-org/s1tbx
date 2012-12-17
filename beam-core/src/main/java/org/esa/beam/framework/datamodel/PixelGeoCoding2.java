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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.DistanceCalculator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.SinusoidalDistanceCalculator;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

public class PixelGeoCoding2 extends AbstractGeoCoding {

    private static final String SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY = "beam.pixelGeoCoding.fractionAccuracy";
    private static final int MAX_SEARCH_CYCLES = 30;

    private Band latBand;
    private Band lonBand;
    private final String maskExpression;

    private final int rasterW;
    private final int rasterH;
    private final boolean fractionAccuracy = Boolean.getBoolean(SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY);

    private PixelPosEstimator pixelPosEstimator;
    private PlanarImage lonImage;
    private PlanarImage latImage;
    private PlanarImage maskImage;
    private final double pixelDiagonalSquared;

    /**
     * Constructs a new pixel-based geo-coding.
     * <p/>
     * <i>Use with care: In contrast to the other constructor this one loads the data not until first access to
     * {@link #getPixelPos(org.esa.beam.framework.datamodel.GeoPos, org.esa.beam.framework.datamodel.PixelPos)} or {@link #getGeoPos(org.esa.beam.framework.datamodel.PixelPos, org.esa.beam.framework.datamodel.GeoPos)}. </i>
     *
     * @param latBand        the band providing the latitudes
     * @param lonBand        the band providing the longitudes
     * @param maskExpression the expression defining a valid-pixel mask, may be {@code null}
     */
    public PixelGeoCoding2(final Band latBand, final Band lonBand, String maskExpression) {
        Guardian.assertNotNull("latBand", latBand);
        Guardian.assertNotNull("lonBand", lonBand);
        final Product product = latBand.getProduct();

        if (product == null) {
            throw new IllegalArgumentException("latBand.getProduct() == null");
        }
        if (lonBand.getProduct() == null) {
            throw new IllegalArgumentException("lonBand.getProduct() == null");
        }
        // Note that if two bands are of the same product, they also have the same raster size
        if (product != lonBand.getProduct()) {
            throw new IllegalArgumentException("latBand.getProduct() != lonBand.getProduct()");
        }
        if (product.getSceneRasterWidth() < 2 || product.getSceneRasterHeight() < 2) {
            throw new IllegalArgumentException(
                    "latBand.getProduct().getSceneRasterWidth() < 2 || latBand.getProduct().getSceneRasterHeight() < 2");
        }

        this.latBand = latBand;
        this.lonBand = lonBand;
        final String validLatExpression = latBand.getValidMaskExpression();
        final String validLonExpression = lonBand.getValidMaskExpression();

        final StringBuilder expressionBuilder;
        if (maskExpression == null) {
            expressionBuilder = new StringBuilder();
        } else {
            expressionBuilder = new StringBuilder(maskExpression.trim());
        }
        if (validLatExpression != null) {
            if (expressionBuilder.length() > 0) {
                expressionBuilder.append(" && ");
            }
            expressionBuilder.append("(").append(validLatExpression).append(")");
        }
        if (validLonExpression != null && !validLonExpression.equals(validLatExpression)) {
            if (expressionBuilder.length() > 0) {
                expressionBuilder.append(" && ");
            }
            expressionBuilder.append("(").append(validLonExpression).append(")");
        }
        this.maskExpression = expressionBuilder.toString();

        this.rasterW = latBand.getSceneRasterWidth();
        this.rasterH = latBand.getSceneRasterHeight();

        lonImage = lonBand.getGeophysicalImage();
        latImage = latBand.getGeophysicalImage();

        if (maskExpression != null && maskExpression.trim().length() > 0) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            for (int i = 0; i < maskGroup.getNodeCount(); i++) {
                final Mask mask = maskGroup.get(i);
                if (mask.getImageType() == Mask.BandMathsType.INSTANCE) {
                    if (Mask.BandMathsType.getExpression(mask).equals(maskExpression)) {
                        maskImage = mask.getSourceImage();
                        break;
                    }
                }
            }
            if (maskImage == null) {
                maskImage = ImageManager.getInstance().getMaskImage(maskExpression, lonBand.getProduct());
            }
        } else {
            maskImage = ConstantDescriptor.create((float) lonImage.getWidth(),
                                                  (float) lonImage.getHeight(),
                                                  new Byte[]{1}, null);
        }

        pixelPosEstimator = new PixelPosEstimator(lonImage,
                                                  latImage,
                                                  maskImage,
                                                  0.5, 10.0, new PixelPosEstimator.PixelSteppingFactory());
        final Dimension2D pixelDimension = pixelPosEstimator.getPixelDimension();
        final double pixelSizeX = pixelDimension.getWidth();
        final double pixelSizeY = pixelDimension.getHeight();

        this.pixelDiagonalSquared = pixelSizeX * pixelSizeX + pixelSizeY * pixelSizeY;
    }

    public Band getLatBand() {
        return latBand;
    }

    public Band getLonBand() {
        return lonBand;
    }

    /**
     * Returns <code>false</code> always.
     *
     * @return <code>false</code>
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
        return pixelPosEstimator.canGetPixelPos();
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

    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);

            if (pixelPos.isValid()) {
                int x0 = (int) Math.floor(pixelPos.x);
                int y0 = (int) Math.floor(pixelPos.y);

                if (x0 >= 0 && x0 < rasterW && y0 >= 0 && y0 < rasterH) {
                    final int searchRadius = 2 * MAX_SEARCH_CYCLES;

                    int x1 = Math.max(x0 - searchRadius, 0);
                    int y1 = Math.max(y0 - searchRadius, 0);
                    int x2 = Math.min(x0 + searchRadius, rasterW - 1);
                    int y2 = Math.min(y0 + searchRadius, rasterH - 1);

                    final Rectangle rectangle = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
                    final Raster latData = latImage.getData(rectangle);
                    final Raster lonData = lonImage.getData(rectangle);
                    final Raster maskData = maskImage.getData(rectangle);

                    final double lat0 = geoPos.lat;
                    final double lon0 = geoPos.lon;
                    final DistanceCalculator dc = new SinusoidalDistanceCalculator(lon0, lat0);

                    double minDelta;
                    if (maskData.getSampleDouble(x0, y0, 0) != 0) {
                        minDelta = dc.distance(lonData.getSampleDouble(x0, y0, 0), latData.getSampleDouble(x0, y0, 0));
                    } else {
                        minDelta = Double.NaN;
                    }
                    for (int i = 0; i < MAX_SEARCH_CYCLES; i++) {
                        x1 = x0;
                        y1 = y0;
                        minDelta = findNearestPixel(x1, y1, pixelPos, latData, lonData, maskData, dc);
                        x0 = (int) Math.floor(pixelPos.x);
                        y0 = (int) Math.floor(pixelPos.y);
                        if (x1 == x0 && y1 == y0) {
                            break;
                        }
                    }
                    if (minDelta * minDelta < pixelDiagonalSquared) {
                        pixelPos.setLocation(x0 + 0.5f, y0 + 0.5f);
                    } else {
                        pixelPos.setInvalid();
                    }
                } else {
                    pixelPos.setInvalid();
                }
            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    private double findNearestPixel(int x0, int y0, PixelPos pixelPos, Raster latData, Raster lonData, Raster maskData,
                                    DistanceCalculator dc) {
        final int minX = Math.max(x0 - 2, 0);
        final int minY = Math.max(y0 - 2, 0);
        final int maxX = Math.min(x0 + 2, rasterW - 1);
        final int maxY = Math.min(y0 + 2, rasterH - 1);

        int x1 = x0;
        int y1 = y0;

        double minDelta = Double.POSITIVE_INFINITY;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (maskData.getSample(x, y, 0) != 0) {
                    final double lat = latData.getSampleDouble(x, y, 0);
                    if (lat >= -90.0 && lat <= 90.0) {
                        final double lon = lonData.getSampleDouble(x, y, 0);
                        if (lon >= -180.0 && lon <= 180.0) {
                            // TODO - use these values to compute average pixel size for the rectangle and use this instead of 'pixelSize' field
                            final double delta = dc.distance(lon, lat);
                            if (delta < minDelta) {
                                x1 = x;
                                y1 = y;
                                minDelta = delta;
                            } else {
                                if (delta == minDelta) {
                                    if (Math.abs(x - x0) + Math.abs(y - y0) > Math.abs(x1 - x0) + Math.abs(x1 - y0)) {
                                        x1 = x;
                                        y1 = y;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        pixelPos.setLocation(x1, y1);

        return minDelta;
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
            if (x0 >= 0 && x0 < rasterW && y0 >= 0 && y0 < rasterH) {
                final Raster maskData = maskImage.getData(new Rectangle(x0, y0, 2, 2));
                if (maskData.getSample(x0, y0, 0) == 0) {
                    geoPos.setInvalid();
                } else if (fractionAccuracy) {
                    if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == rasterW - 1) {
                        x0 -= 1;
                    }
                    if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == rasterH - 1) {
                        y0 -= 1;
                    }

                    final boolean b00 = maskData.getSample(x0, y0, 0) == 0;
                    final boolean b10 = maskData.getSample(x0 + 1, y0, 0) == 0;
                    final boolean b01 = maskData.getSample(x0, y0 + 1, 0) == 0;
                    final boolean b11 = maskData.getSample(x0 + 1, y0 + 1, 0) == 0;

                    if (b00 || b10 || b01 || b11) {
                        getGeoPosInternal(x0, y0, geoPos);
                    } else {
                        final float wx = pixelPos.x - (x0 + 0.5f);
                        final float wy = pixelPos.y - (y0 + 0.5f);
                        final Raster latData = latImage.getData(new Rectangle(x0, y0, 2, 2));
                        final Raster lonData = lonImage.getData(new Rectangle(x0, y0, 2, 2));
                        final float lat = interpolate(wx, wy, latData);
                        final float lon = interpolate(wx, wy, lonData);
                        geoPos.setLocation(lat, lon);
                    }
                } else {
                    getGeoPosInternal(x0, y0, geoPos);
                }
            }
        }
        return geoPos;
    }

    private float interpolate(float wx, float wy, Raster raster) {
        final int x0 = raster.getMinX();
        final int x1 = x0 + 1;
        final int y0 = raster.getMinY();
        final int y1 = y0 + 1;
        final float d00 = raster.getSampleFloat(x0, y0, 0);
        final float d10 = raster.getSampleFloat(x1, y0, 0);
        final float d01 = raster.getSampleFloat(x0, y1, 0);
        final float d11 = raster.getSampleFloat(x1, y1, 0);
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
        if (!maskExpression.equals(that.maskExpression)) {
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
        maskImage = null;
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
        // TODO - copy referenced rasters or de-serialize pixel position estimator
        destScene.setGeoCoding(new PixelGeoCoding2(latBand, lonBand, maskExpression));

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
                final float x = region.x;
                final float y = region.y;
                final float width = region.width;
                final float height = region.height;
                image = CropDescriptor.create(image, x, y, width, height, null);
            }
            final int subSamplingX = subsetDef.getSubSamplingX();
            final int subSamplingY = subsetDef.getSubSamplingY();
            if (subSamplingX != 1 || subSamplingY != 1) {
                final float scaleX = 1.0f / subSamplingX;
                final float scaleY = 1.0f / subSamplingY;
                final float transX = 0.0f;
                final float transY = 0.0f;
                final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
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
