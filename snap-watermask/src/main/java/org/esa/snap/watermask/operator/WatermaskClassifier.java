/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.watermask.operator;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.watermask.util.ImageDescriptor;
import org.esa.snap.watermask.util.ImageDescriptorBuilder;
import org.geotools.resources.image.ImageUtilities;

import javax.imageio.ImageIO;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Classifies a pixel given by its geo-coordinate as water pixel.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class WatermaskClassifier {

    public static final int WATER_VALUE = 1;
    public static final int INVALID_VALUE = 127;
    public static final int LAND_VALUE = 0;

    static final int GC_TILE_WIDTH = 576;
    static final int GC_TILE_HEIGHT = 491;
    static final int GC_IMAGE_WIDTH = 129600;
    static final int GC_IMAGE_HEIGHT = 10800;

    private final ImageSource imageSource;
    private float[] samplingStepsX;
    private float[] samplingStepsY;
    private final int numSuperSamples;


    public WatermaskClassifier(int resolution) throws IOException {
        this(resolution, 1, 1);
    }

    /**
     * Creates a new classifier instance on the given resolution.
     * The classifier uses a tiled image in background to determine the if a
     * given geo-position is over land or over water.
     * Tiles do not exist if the whole region of the tile would only cover land or water.
     * Where a tile does not exist a so called fill algorithm can be performed.
     * In this case the next existing tile is searched and the nearest classification value
     * for the given geo-position is returned.
     * If the fill algorithm is not performed a value indicating invalid is returned.
     *
     *
     * @param resolution     The resolution specifying on source data is to be queried. Needs to be
     *                       50, 150, or 1000.
     * @param superSamplingX Each pixel of the input is super-sampled in x-direction by using this factor.
     *                       Meaningful values lie between the maximum resolution water mask and
     *                       the - lower resolution - source image in x direction. Only values in [1..M] are
     *                       sensible, with M = (source image resolution in m/pixel) / (50 m/pixel)
     * @param superSamplingY Each pixel of the input is super-sampled in y-direction by using this factor.
     *                       Meaningful values lie between the maximum resolution water mask and
     *                       the - lower resolution - source image in y-direction. Only values in [1..M] are
     *                       sensible, with M = (source image resolution in m/pixel) / (50 m/pixel)
     *
     * @throws java.io.IOException If some IO-error occurs creating the sources.
     */
    public WatermaskClassifier(int resolution, int superSamplingX, int superSamplingY) throws IOException {
        if (!isValidResolution(resolution)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Resolution needs to be {0}, {1}, or {2}.", 50, 150, 1000));
        }

        this.numSuperSamples = superSamplingX * superSamplingY;
        samplingStepsX = getSuperSamplingSteps(superSamplingX);
        samplingStepsY = getSuperSamplingSteps(superSamplingY);

        if (resolution == 50 || resolution == 150) {
            installAuxdata();
            File auxdataDir = WatermaskConstants.LOCAL_AUXDATA_PATH.toFile();
            SRTMOpImage centerImage = createCenterImage(resolution, auxdataDir);
            ImageDescriptor northDescriptor = getNorthDescriptor(auxdataDir);
            PNGSourceImage northImage = createBorderImage(northDescriptor);
            imageSource = new HighResImageSource(centerImage, northImage);
        } else {
            imageSource = new LowResImageSource();
        }
    }

    /**
     * Classifies the given geo-position as water or land.
     *
     * @param lat The latitude value.
     * @param lon The longitude value.
     *
     * @return true, if the geo-position is over water, false otherwise.
     *
     * @throws java.io.IOException If some IO-error occurs reading the source file.
     */
    public boolean isWater(float lat, float lon) throws IOException {
        final int waterMaskSample = getWaterMaskSample(lat, lon);
        return waterMaskSample == WATER_VALUE;
    }

    /**
     * Returns the sample value at the given geo-position, regardless of the source resolution.
     *
     * @param lat The latitude value.
     * @param lon The longitude value.
     * @return {@link WatermaskClassifier#LAND_VALUE} if the given position is over land,
     * {@link WatermaskClassifier#WATER_VALUE} if it is over water, {@link WatermaskClassifier#INVALID_VALUE} if
     * no definite statement can be made about the position.
     */
    public int getWaterMaskSample(float lat, float lon) {
        double normLon = lon + 180.0;
        if (normLon >= 360) {
            normLon %= 360;
        }

        float normLat = Math.abs(lat - 90.0f);

        if (normLon < 0.0 || normLon > 360.0 || normLat < 0.0 || normLat > 180.0) {
            return INVALID_VALUE;
        }

        return getSample(normLat, normLon, imageSource.getLatHeight(normLat), imageSource.getLonWidth(), imageSource.getImage(normLat));
    }

    /**
     * Returns the fraction of water for the given region, considering the super-sampling factors given at
     * construction time.
     *
     * @param geoCoding The geo coding of the product the watermask fraction shall be computed for.
     * @param pixelPosX The pixel X position the watermask fraction shall be computed for.
     * @param pixelPosY The pixel Y position the watermask fraction shall be computed for.
     *
     * @return The fraction of water in the given geographic rectangle, in the range [0..100].
     */
    public byte getWaterMaskFraction(GeoCoding geoCoding, int pixelPosX, int pixelPosY) {
        final GeoPos geoPos = new GeoPos();
        final PixelPos currentPos = new PixelPos();
        float valueSum = 0;
        int invalidCount = 0;
        for (float samplingStepY : samplingStepsY) {
            currentPos.y = pixelPosY + samplingStepY;
            for (float samplingStepX : samplingStepsX) {
                currentPos.x = pixelPosX + samplingStepX;
                geoCoding.getGeoPos(currentPos, geoPos);
                int waterMaskSample = getWaterMaskSample(geoPos);
                if (waterMaskSample != WatermaskClassifier.INVALID_VALUE) {
                    valueSum += waterMaskSample;
                } else {
                    invalidCount++;
                }
            }
        }
        return computeAverage(valueSum, invalidCount, numSuperSamples);
    }

    public static boolean isValidResolution(int resolution) {
        return resolution == 50 || resolution == 150 || resolution == 1000;
    }

    private SRTMOpImage createCenterImage(int resolution, File auxdataDir) throws IOException {
        int tileSize = WatermaskUtils.computeSideLength(resolution);

        int width = tileSize * 360;
        int height = tileSize * 180;
        final Properties properties = new Properties();
        properties.setProperty("width", String.valueOf(width));
        properties.setProperty("height", String.valueOf(height));
        properties.setProperty("tileWidth", String.valueOf(tileSize));
        properties.setProperty("tileHeight", String.valueOf(tileSize));
        final URL imageProperties = getClass().getResource("image.properties");
        properties.load(imageProperties.openStream());

        File zipFile = new File(auxdataDir, resolution + "m.zip");
        return SRTMOpImage.create(properties, zipFile);
    }

    private PNGSourceImage createBorderImage(ImageDescriptor descriptor) throws IOException {
        int width = descriptor.getImageWidth();
        int tileWidth = descriptor.getTileWidth();
        int height = descriptor.getImageHeight();
        int tileHeight = descriptor.getTileHeight();
        final Properties properties = new Properties();
        properties.setProperty("width", String.valueOf(width));
        properties.setProperty("height", String.valueOf(height));
        properties.setProperty("tileWidth", String.valueOf(tileWidth));
        properties.setProperty("tileHeight", String.valueOf(tileHeight));
        final URL imageProperties = getClass().getResource("image.properties");
        properties.load(imageProperties.openStream());

        final File auxdataDir = descriptor.getAuxdataDir();
        final String zipFileName = descriptor.getZipFileName();
        File zipFile = new File(auxdataDir, zipFileName);
        return PNGSourceImage.create(properties, zipFile);
    }

    private void installAuxdata() throws IOException {
        final String remoteHTTPHost = WatermaskConstants.REMOTE_HTTP_HOST;
        final String remoteHTTPPath = WatermaskConstants.REMOTE_HTTP_PATH;
        final String baseURL = remoteHTTPHost + remoteHTTPPath;
        WatermaskUtils.installRemoteHttpFiles(baseURL);
    }

    private static int getSample(double lat, double lon, double latHeight, double lonWidth, OpImage image) {
        if (image == null || latHeight == HighResImageSource.INVALID_LAT_HEIGHT) {
            return INVALID_VALUE;
        }
        final double pixelSizeX = lonWidth / image.getWidth();
        final double pixelSizeY = latHeight / image.getHeight();
        final int x = (int) Math.floor(lon / pixelSizeX);
        final int y = (int) (Math.floor(lat / pixelSizeY));
        final Raster tile = image.getTile(image.XToTileX(x), image.YToTileY(y));
        if (tile == null) {
            return INVALID_VALUE;
        }
        return tile.getSample(x, y, 0);
    }

    private static float[] getSuperSamplingSteps(int superSampling) {
        if (superSampling <= 1) {
            return new float[]{0.5f};
        } else {
            float[] samplingStep = new float[superSampling];
            for (int i = 0; i < samplingStep.length; i++) {
                samplingStep[i] = (i * 2.0F + 1.0F) / (2.0F * superSampling);
            }
            return samplingStep;
        }
    }


    private static byte computeAverage(float valueSum, int invalidCount, int numSuperSamples) {
        final boolean allValuesInvalid = invalidCount == numSuperSamples;
        if (allValuesInvalid) {
            return WatermaskClassifier.INVALID_VALUE;
        } else {
            return (byte) (100 * valueSum / numSuperSamples);
        }
    }

    private int getWaterMaskSample(GeoPos geoPos) {
        final int waterMaskSample;
        if (geoPos.isValid()) {
            waterMaskSample = getWaterMaskSample((float) geoPos.lat, (float) geoPos.lon);
        } else {
            waterMaskSample = WatermaskClassifier.INVALID_VALUE;
        }
        return waterMaskSample;
    }

    private static ImageDescriptor getNorthDescriptor(File auxdataDir) {
        return new ImageDescriptorBuilder()
                        .width(GC_IMAGE_WIDTH)
                        .height(GC_IMAGE_HEIGHT)
                        .tileWidth(GC_TILE_WIDTH)
                        .tileHeight(GC_TILE_HEIGHT)
                        .auxdataDir(auxdataDir)
                        .zipFileName("GC_water_mask.zip")
                        .build();
    }

    private interface ImageSource {

        float getLonWidth();
        float getLatHeight(float lat);
        OpImage getImage(float lat);

    }

    private static class HighResImageSource implements ImageSource {


        private static final float INVALID_LAT_HEIGHT = -1.0F;
        private final OpImage centralImage;
        private final OpImage northImage;

        private HighResImageSource(OpImage centralImage, OpImage northImage) {
            this.centralImage = centralImage;
            this.northImage = northImage;
        }

        @Override
        public float getLonWidth() {
                return 360.0F;
        }

        @Override
        public float getLatHeight(float latitude) {
            if (latitude < 150.0 && latitude > 30.0) {
                return 180.0F;
            } else if (latitude <= 30.0) {
                return 30.0F;
            }
            return INVALID_LAT_HEIGHT;
        }

        @Override
        public OpImage getImage(float latitude) {
            if (latitude < 150.0 && latitude > 30.0) {
                return centralImage;
            } else if (latitude <= 30.0) {
                return northImage;
            }
            return null;
        }
    }

    private static class LowResImageSource implements ImageSource {

        OpImage image;

        @Override
        public float getLonWidth() {
                return 360.0F;
        }

        @Override
        public float getLatHeight(float latitude) {
            return 180.0F;
        }

        @Override
        public OpImage getImage(float latitude) {
            if (image != null) {
                return image;
            }
            try {
                BufferedImage waterImage = ImageIO.read(getClass().getResourceAsStream("water.png"));
                image = new NullOpImage(waterImage, ImageUtilities.getImageLayout(waterImage), null, OpImage.OP_COMPUTE_BOUND);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return image;
        }
    }

}
