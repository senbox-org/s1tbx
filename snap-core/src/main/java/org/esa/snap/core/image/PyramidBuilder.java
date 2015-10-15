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

package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PyramidBuilder {

    public PyramidBuilder() {
    }

    public static void main(String[] args) throws IOException {
        final File imageFile = new File(args[0]);
        final File outputDir = new File(args[1]);
        final String tileFormat = args[2];
        final int levelCount = Integer.parseInt(args[3]);
        int tileWidth0 = Integer.parseInt(args[4]);
        int tileHeight0 = Integer.parseInt(args[5]);
        new PyramidBuilder().doit(imageFile,
                                  outputDir,
                                  tileFormat,
                                  levelCount,
                                  tileWidth0,
                                  tileHeight0);
    }

    private void doit(File imageFile, File outputDir, String tileFormat, int levelCount, int tileWidth0, int tileHeight0) throws IOException {

        outputDir.mkdir();

        int dataType;
        int tileWidth = tileWidth0;
        int tileHeight = tileHeight0;
        Interpolation interpolation;
        RenderedImage image0;

        boolean rawZip = tileFormat.equalsIgnoreCase("raw.zip");
        boolean raw = tileFormat.equalsIgnoreCase("raw") || rawZip;
        if (raw) {
            // Raw data images
            interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            image0 = TiledFileOpImage.create(imageFile.toPath(), new Properties());
        } else {
            // Visual RGB images
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            image0 = FileLoadDescriptor.create(imageFile.getPath(), null, true, null);
        }

        dataType = image0.getSampleModel().getDataType();
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(levelCount, new AffineTransform(), new Rectangle(image0.getWidth(), image0.getHeight()));
        MultiLevelSource multiLevelSource = new DefaultMultiLevelSource(image0, model, interpolation);

        for (int level = 0; level < levelCount; level++) {

            final PlanarImage image = PlanarImage.wrapRenderedImage(multiLevelSource.getImage(level));

            final int width = image.getWidth();
            final int height = image.getHeight();

            System.out.println("width = " + width + ", height = " + height);

            int numXTiles;
            int numYTiles;
            while (true) {
                numXTiles = width / tileWidth;
                numYTiles = height / tileHeight;
                System.out.println("tileWidth = " + tileWidth + ", tileHeight = " + tileHeight);
                System.out.println("numXTiles = " + numXTiles + ", numYTiles = " + numYTiles);
                if (numXTiles * tileWidth == width && numYTiles * tileHeight == image.getHeight()) {
                    break;
                }
                if (numXTiles * tileWidth < width) {
                    tileWidth /= 2;
                }
                if (numYTiles * tileHeight < height) {
                    tileHeight /= 2;
                }
            }
            if (numXTiles == 0 || numYTiles == 0) {
                throw new IllegalStateException("numXTiles == 0 || numYTiles == 0");
            }
            if (tileWidth < tileWidth0 && tileHeight < tileHeight0) {
                tileWidth = width;
                tileHeight = height;
                numXTiles = numYTiles = 1;
            }

            final File outputLevelDir = new File(outputDir, "" + level);
            outputLevelDir.mkdir();
            final File imagePropertiesFile = new File(outputLevelDir, "image.properties");
            System.out.println("Writing " + imagePropertiesFile + "...");
            final PrintWriter printWriter = new PrintWriter(new FileWriter(imagePropertiesFile));
            writeImageProperties(level,
                                 dataType,
                                 width,
                                 height,
                                 tileWidth,
                                 tileHeight,
                                 numXTiles,
                                 numYTiles,
                                 new PrintWriter(System.out));
            writeImageProperties(level,
                                 dataType,
                                 width,
                                 height,
                                 tileWidth,
                                 tileHeight,
                                 numXTiles,
                                 numYTiles,
                                 printWriter);
            System.out.flush();
            printWriter.close();
            if (raw) {
                writeRawTiles(outputLevelDir, image, tileWidth, tileHeight, numXTiles, numYTiles, rawZip);
            } else {
                writeTiles(outputLevelDir, tileFormat, image, tileWidth, tileHeight, numXTiles, numYTiles);
            }
        }
    }

    private void writeTiles(File outputLevelDir, String tileFormat, PlanarImage image, int tileWidth, int tileHeight, int numXTiles, int numYTiles) {
        for (int tileY = 0; tileY < numYTiles; tileY++) {
            for (int tileX = 0; tileX < numXTiles; tileX++) {
                final int x = tileX * tileWidth;
                final int y = tileY * tileHeight;
                Rectangle region = new Rectangle(x, y, tileWidth, tileHeight);
                BufferedImage bufferedImage = image.getAsBufferedImage(region, null);
                final String baseName = tileX + "-" + tileY + "." + tileFormat;
                FileStoreDescriptor.create(bufferedImage, new File(outputLevelDir, baseName).getPath(), tileFormat, null, false, null);
            }
        }
    }

    private void writeRawTiles(File levelDir, PlanarImage image, int tileWidth, int tileHeight, int numXTiles, int numYTiles, boolean rawZip) throws IOException {
        for (int tileY = 0; tileY < numYTiles; tileY++) {
            for (int tileX = 0; tileX < numXTiles; tileX++) {
                final int x = tileX * tileWidth;
                final int y = tileY * tileHeight;
                final Raster raster = image.getData(new Rectangle(x, y, tileWidth, tileHeight));
                // todo - only "int" currently supported! check for other types!!!
                int[] data = ((DataBufferInt) raster.getDataBuffer()).getData();
                if (data.length != tileWidth * tileHeight) {
                    data = new int[tileWidth * tileHeight];
                    raster.getDataElements(x, y, tileWidth, tileHeight, data);
                }
                writeRawData(levelDir, tileX, tileY, data, rawZip);
            }
        }
    }

    private void writeRawData(File levelDir, int tileX, int tileY, int[] data, boolean rawZip) throws IOException {
        final String baseName = tileX + "-" + tileY + ".raw";
        if (rawZip) {
            final File file = new File(levelDir, baseName + ".zip");
            final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
            zipOutputStream.putNextEntry(new ZipEntry(baseName));
            final ImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(zipOutputStream);
            imageOutputStream.writeInts(data, 0, data.length);
            imageOutputStream.flush();
            zipOutputStream.closeEntry();
            zipOutputStream.close();
        } else {
            final File file = new File(levelDir, baseName);
            FileImageOutputStream outputStream = new FileImageOutputStream(file);
            outputStream.writeInts(data, 0, data.length);
            outputStream.close();
        }
    }

    private void writeImageProperties(int level, int dataType, int width, int height, int tileWidth, int tileHeight, int numXTiles, int numYTiles, PrintWriter printWriter) {
        printWriter.println("level      = " + level);
        printWriter.println("dataType   = " + dataType);
        printWriter.println("width      = " + width);
        printWriter.println("height     = " + height);
        printWriter.println("tileWidth  = " + tileWidth);
        printWriter.println("tileHeight = " + tileHeight);
        printWriter.println("numXTiles  = " + numXTiles);
        printWriter.println("numYTiles  = " + numYTiles);
    }

}
