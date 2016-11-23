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

import com.bc.ceres.core.Assert;
import org.esa.snap.core.image.ImageHeader;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Responsible for tiled access on the data below 60° northern latitude.
 *
 * @author Thomas Storm
 */
public class SRTMOpImage extends SourcelessOpImage {

    private ZipFile zipFile;
    private Properties missingTiles;
    private SampleModel rawImgSampleModel;
    private WritableRaster landRaster;
    private WritableRaster waterRaster;
    private WritableRaster invalidRaster;

    public static SRTMOpImage create(Properties defaultImageProperties, File zipFile) throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(defaultImageProperties, null);
        return new SRTMOpImage(imageHeader, zipFile);
    }

    private SRTMOpImage(ImageHeader imageHeader, File zipFile) throws IOException {
        super(imageHeader.getImageLayout(),
              null,
              ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE,
                                                       imageHeader.getImageLayout().getSampleModel(null).getWidth(),
                                                       imageHeader.getImageLayout().getSampleModel(null).getHeight()),
              imageHeader.getImageLayout().getMinX(null),
              imageHeader.getImageLayout().getMinY(null),
              imageHeader.getImageLayout().getWidth(null),
              imageHeader.getImageLayout().getHeight(null));
        this.zipFile = new ZipFile(zipFile);
        missingTiles = new Properties();
        missingTiles.load(getClass().getResourceAsStream("MissingTiles.properties"));
        // this image uses its own tile cache in order not to disturb the GPF tile cache.
        setTileCache(JAI.createTileCache(50L * 1024 * 1024));
        rawImgSampleModel = imageHeader.getImageLayout().getSampleModel(null);
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        try {
            return readRawDataTile(tileX, tileY);
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed to read image tile ''{0} | {1}''.", tileX, tileY);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public synchronized void dispose() {
        try {
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Raster readRawDataTile(int tileX, int tileY) throws IOException {
        final Point location = new Point(tileXToX(tileX), tileYToY(tileY));

        // 89 not 90, because tile coordinates are given for lower left corner
        final String imgFileName = WatermaskUtils.createImgFileName(89 - tileY, tileX - 180);
        final String missingTileValue = missingTiles.getProperty(imgFileName.substring(0, imgFileName.indexOf('.')));
        final boolean tileIsMissing = missingTileValue != null;
        if (tileIsMissing) {
            final byte tileValue = Byte.parseByte(missingTileValue);
            switch (tileValue) {
                case 0:
                    return getLandRaster(location, tileValue);
                case 1:
                    return getWaterRaster(location, tileValue);
                default:
                    return getInvalidRaster(location, tileValue);
            }
        }

        final WritableRaster targetRaster = createWritableRaster(rawImgSampleModel, location);
        final byte[] data = ((DataBufferByte) targetRaster.getDataBuffer()).getData();
        try (InputStream inputStream = createInputStream(imgFileName)) {
            int count = 0;
            int amount = data.length;
            while (count < data.length) {
                if (count + amount > data.length) {
                    amount = data.length - count;
                }
                count += inputStream.read(data, count, amount);
            }
            Assert.state(count == data.length, "Not all data have been read.");
        } catch (NullPointerException e) {
            throw new RuntimeException(imgFileName, e);
        }
        return targetRaster;
    }

    private Raster getLandRaster(Point location, byte tileValue) {
        if (landRaster == null) {
            landRaster = createRaster(tileValue);
        }
        return landRaster.createTranslatedChild(location.x, location.y);
    }

    private Raster getWaterRaster(Point location, byte tileValue) {
        if (waterRaster == null) {
            waterRaster = createRaster(tileValue);
        }
        return waterRaster.createTranslatedChild(location.x, location.y);
    }

    private Raster getInvalidRaster(Point location, byte tileValue) {
        if (invalidRaster == null) {
            invalidRaster = createRaster(tileValue);
        }
        return invalidRaster.createTranslatedChild(location.x, location.y);
    }

    private WritableRaster createRaster(byte tileValue) {
        WritableRaster raster = createWritableRaster(sampleModel, new Point(0, 0));
        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();
        Arrays.fill(data, tileValue);
        return raster;
    }

    private InputStream createInputStream(String imgFileName) throws IOException {
        final ZipEntry entry = zipFile.getEntry(imgFileName);
        if (entry != null) {
            return zipFile.getInputStream(entry);
        }else {
            throw new IllegalArgumentException(String.format("Can not find '%s' entry in zip file '%s'", imgFileName, zipFile.getName()));
        }
    }
}
