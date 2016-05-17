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

import org.esa.snap.core.image.ImageHeader;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * OpImage to read from GlobCover-based water mask images.
 *
 * @author Thomas Storm
 */
public class PNGSourceImage extends SourcelessOpImage {

    private final ZipFile zipFile;

    static PNGSourceImage create(Properties properties, File zipFile) throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(properties, null);
        return new PNGSourceImage(imageHeader, zipFile);
    }

    private PNGSourceImage(ImageHeader imageHeader, File zipFile) throws IOException {
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
        // this image uses its own tile cache in order not to disturb the GPF tile cache.
        setTileCache(JAI.createTileCache(50L * 1024 * 1024));
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        Raster raster;
        try {
            raster = computeRawRaster(tileX, tileY);
        } catch (IOException e) {
            throw new RuntimeException(MessageFormat.format("Failed to read image tile ''{0} | {1}''.", tileX, tileY), e);
        }
        return raster;

    }

    private Raster computeRawRaster(int tileX, int tileY) throws IOException {
        final String fileName = getFileName(tileX, tileY);
        final WritableRaster targetRaster = createWritableRaster(tileX, tileY);
        final ZipEntry zipEntry = zipFile.getEntry(fileName);

        InputStream inputStream = null;
        try {
            inputStream = zipFile.getInputStream(zipEntry);
            BufferedImage image = ImageIO.read(inputStream);
            Raster imageData = image.getData();
            for (int x = 0; x < imageData.getWidth(); x++) {
                int xPos = tileXToX(tileX) + x;
                for (int y = 0; y < imageData.getHeight(); y++) {
                    byte sample = (byte) imageData.getSample(x, y, 0);
                    sample = (byte) Math.abs(sample - 1);
                    int yPos = tileYToY(tileY) + y;
                    targetRaster.setSample(xPos, yPos, 0, sample);
                }
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return targetRaster;
    }

    private WritableRaster createWritableRaster(int tileX, int tileY) {
        final Point location = new Point(tileXToX(tileX), tileYToY(tileY));
        final SampleModel sampleModel = new SingleBandedSampleModel(DataBuffer.TYPE_BYTE, getTileWidth(), getTileHeight());
        return createWritableRaster(sampleModel, location);
    }

    private String getFileName(int tileX, int tileY) {
        return String.format("%d-%d.png", tileX, tileY);
    }
}
