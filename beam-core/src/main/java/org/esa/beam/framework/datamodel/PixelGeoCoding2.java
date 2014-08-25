/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * This is an experimental pixel geo-coding, which solves some problems of {@link PixelGeoCoding},
 * but may bring-up others.
 *
 * @author Ralf Quast
 */
class PixelGeoCoding2 extends AbstractGeoCoding implements BasicPixelGeoCoding {

    private static final String SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY = "snap.pixelGeoCoding.fractionAccuracy";
    private static final String SYSPROP_PIXEL_GEO_CODING_USE_TILING = "snap.pixelGeoCoding.useTiling";

    private final Band latBand;
    private final Band lonBand;
    private final String maskExpression;

    private final int rasterW;
    private final int rasterH;
    private final boolean fractionAccuracy = Boolean.getBoolean(SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY);
    private final DataProvider dataProvider;
    private final GeoCoding formerGeocoding;

    //    private final PixelPosEstimatorFactory pixelPosEstimatorFactory;
    private transient PixelPosEstimator pixelPosEstimator;
    private transient DefaultPixelFinder pixelFinder;

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
        formerGeocoding = product.getGeoCoding();

        this.rasterW = latBand.getSceneRasterWidth();
        this.rasterH = latBand.getSceneRasterHeight();

        PlanarImage lonImage;
        try {
            lonImage = (PlanarImage) lonBand.getGeophysicalImage().getImage(0);
        } catch (ClassCastException e) {
            lonImage = lonBand.getGeophysicalImage();
        }
        PlanarImage latImage;
        try {
            latImage = (PlanarImage) latBand.getGeophysicalImage().getImage(0);
        } catch (ClassCastException e) {
            latImage = latBand.getGeophysicalImage();
        }

        PlanarImage maskImage = null;
        if (maskExpression != null) {
            maskExpression = maskExpression.trim();
            if (maskExpression.length() > 0) {
                final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                for (int i = 0; i < maskGroup.getNodeCount(); i++) {
                    final Mask mask = maskGroup.get(i);
                    if (mask.getImageType() == Mask.BandMathsType.INSTANCE) {
                        if (maskExpression.equals(Mask.BandMathsType.getExpression(mask))) {
                            maskImage = mask.getSourceImage();
                            break;
                        }
                    }
                }
                if (maskImage == null) {
                    // TODO - ensure that tile layout of lat and lon images is used
                    maskImage = (PlanarImage) ImageManager.getInstance().getMaskImage(maskExpression,
                                                                                      lonBand.getProduct()).getImage(0);
                }
            } else {
                maskExpression = null;
                maskImage = null;
            }
        } else {
            maskExpression = null;
            maskImage = null;
        }
        this.maskExpression = maskExpression;

        final PixelDimensionEstimator pixelDimensionEstimator = new SimplePixelDimensionEstimator();
        final Dimension2D pixelDimension = pixelDimensionEstimator.getPixelDimension(lonImage,
                                                                                     latImage,
                                                                                     maskImage);
        final double pixelSizeX = pixelDimension.getWidth();
        final double pixelSizeY = pixelDimension.getHeight();
        final double pixelDiagonalSquared = pixelSizeX * pixelSizeX + pixelSizeY * pixelSizeY;

//        pixelPosEstimatorFactory = new PixelPosEstimatorFactory(lonImage, latImage, maskImage, 0.5);
        pixelPosEstimator = new PixelPosEstimator(lonImage, latImage, maskImage, 0.5);
        pixelFinder = new DefaultPixelFinder(lonImage, latImage, maskImage, pixelDiagonalSquared);

        boolean disableTiling = "false".equalsIgnoreCase(System.getProperty(SYSPROP_PIXEL_GEO_CODING_USE_TILING));
        if (disableTiling) {
            dataProvider = new ArrayDataProvider(lonBand, latBand, maskImage);
        } else {
            dataProvider = new ImageDataProvider(lonImage, maskImage, latImage, maskImage);
        }
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
        return maskExpression;
    }

    @Override
    public GeoCoding getPixelPosEstimator() {
        return formerGeocoding;
    }

    @Override
    public int getSearchRadius() {
        return 0;
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
//        if (pixelPosEstimator != null) {
        return pixelPosEstimator.canGetPixelPos();
//        }
//        return true;
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
//            ensurePixelPosEstimatorExist();
//            if (pixelPosEstimator.canGetPixelPos()) {
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);
            if (pixelPos.isValid()) {
                pixelFinder.findPixelPos(geoPos, pixelPos);
            }
//            } else {
//                pixelPos.setInvalid();
//            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
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
        if (pixelPos.isValid() && pixelPosIsInsideRasterWH(pixelPos)) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());

            if (fractionAccuracy) {
                if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == rasterW - 1) {
                    x0 -= 1;
                }
                if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == rasterH - 1) {
                    y0 -= 1;
                }

                final float wx = pixelPos.x - (x0 + 0.5f);
                final float wy = pixelPos.y - (y0 + 0.5f);

                dataProvider.getGeoPosFloat(x0, y0, wx, wy, geoPos);
            } else {
                dataProvider.getGeoPosInteger(x0, y0, geoPos);
            }
            if (!geoPos.isValid()) {
                if (formerGeocoding != null && formerGeocoding.canGetGeoPos()) {
                    formerGeocoding.getGeoPos(pixelPos, geoPos);
                } else {
//                    ensurePixelPosEstimatorExist();
                    pixelPosEstimator.getGeoPos(pixelPos, geoPos);
                }
            }
        }
        return geoPos;
    }

    private boolean pixelPosIsInsideRasterWH(PixelPos pixelPos) {
        final float x = pixelPos.x;
        final float y = pixelPos.y;
        return x >= 0 && x < rasterW && y >= 0 && y < rasterH;
    }

    public int getRasterWidth() {
        return rasterW;
    }

    public int getRasterHeight() {
        return rasterH;
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
        if (maskExpression != null) {
            if (!maskExpression.equals(that.maskExpression)) {
                return false;
            }
        } else {
            if (that.maskExpression != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latBand.hashCode();
        result = 31 * result + lonBand.hashCode();
        if (maskExpression != null) {
            result = 31 * result + maskExpression.hashCode();
        }
        return result;
    }

    @Override
    public synchronized void dispose() {
        pixelPosEstimator = null;
        pixelFinder = null;
    }

    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene,
                                     final ProductSubsetDef subsetDef) {
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
        String validMaskExpression = getValidMask();
        try {
            if (validMaskExpression != null) {
                GeoCodingFactory.copyReferencedRasters(validMaskExpression, srcScene, destScene, subsetDef);
            }
        } catch (ParseException ignored) {
            validMaskExpression = null;
        }
        destScene.setGeoCoding(new PixelGeoCoding2(latBand, lonBand, validMaskExpression));

        return true;
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

//    private void ensurePixelPosEstimatorExist() {
//        synchronized (pixelPosEstimatorFactory) {
//            if (pixelPosEstimator == null) {
//                pixelPosEstimator = pixelPosEstimatorFactory.create();
//            }
//        }
//    }

    private interface DataProvider {

        void getGeoPosInteger(int x0, int y0, GeoPos geoPos);

        void getGeoPosFloat(int x0, int y0, float wx, float wy, GeoPos geoPos);
    }

    private static class ImageDataProvider implements DataProvider {

        private final RenderedImage lonImage;
        private final RenderedImage lonMaskImage;
        private final RenderedImage latImage;
        private final RenderedImage latMaskImage;

        ImageDataProvider(RenderedImage lonImage, RenderedImage lonMaskImage, RenderedImage latImage,
                          RenderedImage latMaskImage) {
            this.lonImage = lonImage;
            this.lonMaskImage = lonMaskImage;
            this.latImage = latImage;
            this.latMaskImage = latMaskImage;
        }

        @Override
        public void getGeoPosInteger(int x0, int y0, GeoPos geoPos) {
            final float lon0 = getSampleFloat(x0, y0, lonImage, lonMaskImage);
            final float lat0 = getSampleFloat(x0, y0, latImage, latMaskImage);
            if (lat0 >= -90.0f && lat0 <= 90.0f && lon0 >= -180.0f && lon0 <= 180.0f) {
                geoPos.setLocation(lat0, lon0);
            }
        }

        @Override
        public void getGeoPosFloat(int x0, int y0, float wx, float wy, GeoPos geoPos) {
            Rectangle region = new Rectangle(x0, y0, 2, 2);
            if (lonMaskImage == null || allValid(lonMaskImage.getData(region))) {
                final Raster lonData = lonImage.getData(region);
                geoPos.lon = interpolate(wx, wy, lonData, -180.0f, 180.0f);
            } else {
                geoPos.lon = getSampleFloat(x0, y0, lonImage, lonMaskImage);
            }

            if (latMaskImage == null || allValid(latMaskImage.getData(region))) {
                final Raster latData = latImage.getData(region);
                geoPos.lat = interpolate(wx, wy, latData, -90.0f, 90.0f);
            } else {
                geoPos.lat = getSampleFloat(x0, y0, latImage, latMaskImage);
            }
        }

        private boolean allValid(Raster raster) {
            final int x0 = raster.getMinX() - raster.getSampleModelTranslateX();
            final int x1 = x0 + 1;
            final int y0 = raster.getMinY() - raster.getSampleModelTranslateY();
            final int y1 = y0 + 1;
            DataBuffer dataBuffer = raster.getDataBuffer();
            SampleModel sampleModel = raster.getSampleModel();

            if (sampleModel.getSample(x0, y0, 0, dataBuffer) == 0) {
                return false;
            }
            if (sampleModel.getSample(x1, y0, 0, dataBuffer) == 0) {
                return false;
            }
            if (sampleModel.getSample(x0, y1, 0, dataBuffer) == 0) {
                return false;
            }
            if (sampleModel.getSample(x1, y1, 0, dataBuffer) == 0) {
                return false;
            }
            return true;
        }

        private float interpolate(float wx, float wy, Raster raster, float min, float max) {
            final int x0 = raster.getMinX() - raster.getSampleModelTranslateX();
            final int x1 = x0 + 1;
            final int y0 = raster.getMinY() - raster.getSampleModelTranslateY();
            final int y1 = y0 + 1;
            DataBuffer dataBuffer = raster.getDataBuffer();
            SampleModel sampleModel = raster.getSampleModel();

            final float d00 = sampleModel.getSampleFloat(x0, y0, 0, dataBuffer);
            if (d00 >= min && d00 <= max) {
                final float d10 = sampleModel.getSampleFloat(x1, y0, 0, dataBuffer);
                if (d10 >= min && d10 <= max) {
                    final float d01 = sampleModel.getSampleFloat(x0, y1, 0, dataBuffer);
                    if (d01 >= min && d01 <= max) {
                        final float d11 = sampleModel.getSampleFloat(x1, y1, 0, dataBuffer);
                        if (d11 >= min && d11 <= max) {
                            return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
                        }
                    }
                }
            }
            return d00;
        }

        private float getSampleFloat(int pixelX, int pixelY, RenderedImage dataImage, RenderedImage maskImage) {
            if (maskImage != null) {
                // tile coordinates of images and mask "shall" be equal, but they are not - TODO check why (mz 2013-11-08)
                final int x = maskImage.getMinX() + pixelX;
                final int y = maskImage.getMinY() + pixelY;
                final int maskTileX = PlanarImage.XToTileX(x, maskImage.getTileGridXOffset(), maskImage.getTileWidth());
                final int maskTileY = PlanarImage.YToTileY(y, maskImage.getTileGridYOffset(),
                                                           maskImage.getTileHeight());
                final int maskValue = maskImage.getTile(maskTileX, maskTileY).getSample(x, y, 0);
                if (maskValue == 0) {
                    return Float.NaN;
                }
            }
            final int x = dataImage.getMinX() + pixelX;
            final int y = dataImage.getMinY() + pixelY;
            final int tileX = PlanarImage.XToTileX(x, dataImage.getTileGridXOffset(), dataImage.getTileWidth());
            final int tileY = PlanarImage.YToTileY(y, dataImage.getTileGridYOffset(), dataImage.getTileHeight());
            final Raster data = dataImage.getTile(tileX, tileY);
            return data.getSampleFloat(x, y, 0);
        }
    }

    private static class ArrayDataProvider implements DataProvider {

        private final float[] lonData;
        private final float[] latData;
        private final int width;

        ArrayDataProvider(RasterDataNode lonBand, RasterDataNode latBand, PlanarImage maskImage) {
            width = lonBand.getSceneRasterWidth();
            int height = lonBand.getSceneRasterHeight();
            MultiLevelImage lonImage = ImageManager.createMaskedGeophysicalImage(lonBand, Float.NaN);
            lonData = lonImage.getData().getSamples(0, 0, width, height, 0, (float[]) null);
            MultiLevelImage latImage = ImageManager.createMaskedGeophysicalImage(latBand, Float.NaN);
            latData = latImage.getData().getSamples(0, 0, width, height, 0, (float[]) null);
            if (maskImage != null) {
                final int[] maskValues = maskImage.getData().getSamples(0, 0, width, height, 0, (int[]) null);
                for (int i = 0; i < maskValues.length; i++) {
                    if (maskValues[i] == 0) {
                        lonData[i] = Float.NaN;
                        latData[i] = Float.NaN;
                    }
                }
            }
        }

        @Override
        public void getGeoPosInteger(int x0, int y0, GeoPos geoPos) {
            final int i = width * y0 + x0;
            final float lon0 = lonData[i];
            final float lat0 = latData[i];
            if (lat0 >= -90.0f && lat0 <= 90.0f && lon0 >= -180.0f && lon0 <= 180.0f) {
                geoPos.setLocation(lat0, lon0);
            }
        }

        @Override
        public void getGeoPosFloat(int x0, int y0, float wx, float wy, GeoPos geoPos) {
            geoPos.lon = interpolate(x0, y0, wx, wy, lonData, -180.0f, 180.0f);
            geoPos.lat = interpolate(x0, y0, wx, wy, latData, -90.0f, 90.0f);
        }

        private float interpolate(int x0, int y0, float wx, float wy, float[] data, float min, float max) {
            final int x1 = x0 + 1;
            final int y1 = y0 + 1;

            final float d00 = data[width * y0 + x0];
            if (d00 >= min && d00 <= max) {
                final float d10 = data[width * y0 + x1];
                if (d10 >= min && d10 <= max) {
                    final float d01 = data[width * y1 + x0];
                    if (d01 >= min && d01 <= max) {
                        final float d11 = data[width * y1 + x1];
                        if (d11 >= min && d11 <= max) {
                            return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
                        }
                    }
                }
            }
            return d00;
        }

    }

    private static class PixelPosEstimatorFactory {

        private final PlanarImage lonImage;
        private final PlanarImage latImage;
        private final PlanarImage maskImage;
        private final double accuracy;

        private PixelPosEstimatorFactory(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage,
                                         double accuracy) {
            this.lonImage = lonImage;
            this.latImage = latImage;
            this.maskImage = maskImage;
            this.accuracy = accuracy;
        }

        private PixelPosEstimator create() {
            return new PixelPosEstimator(lonImage, latImage, maskImage, accuracy);
        }
    }
}
