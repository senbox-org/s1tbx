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

package org.esa.snap.watermask.util;

import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.math.Histogram;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.watermask.operator.WatermaskUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


class RasterImageOutputter {

    private static final int TILE_WIDTH = WatermaskUtils.computeSideLength(50);

    public static void main(String[] args) throws IOException {

        final File file = new File(args[0]);
        if (file.isDirectory()) {
            final File[] imgFiles = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".img");
                }
            });
            final ExecutorService executorService = Executors.newFixedThreadPool(6);
            for (File imgFile : imgFiles) {
                final File outputFile = FileUtils.exchangeExtension(imgFile, ".png");
                if (!outputFile.exists()) {
                    executorService.submit(new ImageWriterRunnable(imgFile, outputFile));
                }
            }

            executorService.shutdown();
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            final InputStream inputStream;
            if (file.getName().toLowerCase().endsWith(".zip")) {
                ZipFile zipFile = new ZipFile(file);
                String shapefile = args[1];
                final ZipEntry entry = zipFile.getEntry(shapefile);
                inputStream = zipFile.getInputStream(entry);
            } else {
                inputStream = new FileInputStream(file);
            }
            writeImage(inputStream, new File(args[args.length - 1]));
        }

    }

    private static boolean writeImage(InputStream inputStream, File outputFile) throws IOException {
        WritableRaster targetRaster = Raster.createPackedRaster(0, TILE_WIDTH, TILE_WIDTH, 1, 1,
                                                                new Point(0, 0));

        final byte[] data = ((DataBufferByte) targetRaster.getDataBuffer()).getData();
        inputStream.read(data);
        final BufferedImage image = new BufferedImage(TILE_WIDTH, TILE_WIDTH, BufferedImage.TYPE_BYTE_BINARY);
        image.setData(targetRaster);
        boolean valid = validateImage(image);
        ImageIO.write(image, "png", outputFile);
        return valid;
    }

    private static boolean validateImage(BufferedImage image) {
        final Histogram histogram = Histogram.computeHistogram(image, null, 3, new Range(0, 3));
        final int[] binCounts = histogram.getBinCounts();
        // In both bins must be values
        for (int binCount : binCounts) {
            if (binCount == 0) {
                return false;
            }
        }
        return true;
    }

    private static class ImageWriterRunnable implements Runnable {

        private File inputFile;
        private File outputFile;

        private ImageWriterRunnable(File inputFile, File outputFile) {

            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        @Override
        public void run() {
            InputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(inputFile);
                final boolean valid = writeImage(fileInputStream, outputFile);
                if(!valid) {
                    System.out.printf("Not valid: %s%n", outputFile);
                }else {
                    System.out.printf("Written: %s%n", outputFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}