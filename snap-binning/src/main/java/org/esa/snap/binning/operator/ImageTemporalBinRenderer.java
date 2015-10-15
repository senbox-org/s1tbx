/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.TemporalBinRenderer;
import org.esa.snap.binning.Vector;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * A renderer that renders temporal bins into JPEG or PNG images.
 *
 * @author Norman Fomferra
 */
public final class ImageTemporalBinRenderer implements TemporalBinRenderer {

    private final int bandCount;
    private final int rasterWidth;
    private final int[] bandIndices;
    private final String[] bandNames;
    private final float[] bandMinValues;
    private final float[] bandMaxValues;
    private final float[][] bandData;
    private final Rectangle outputRegion;
    private final boolean writeRgb;
    private final File outputFile;
    private final String outputFormat;

    public ImageTemporalBinRenderer(String[] featureNames,
                                    File outputFile, String outputFormat,
                                    Rectangle outputRegion,
                                    BinningOp.BandConfiguration[] bandConfigurations,
                                    boolean writeRgb) {

        final int bandCount = bandConfigurations.length;
        if (bandCount == 0) {
            throw new IllegalArgumentException("No output band given.");
        }

        this.outputRegion = outputRegion;
        this.writeRgb = writeRgb;
        this.outputFile = outputFile;
        this.outputFormat = outputFormat;
        int rasterWidth = outputRegion.width;
        int rasterHeight = outputRegion.height;

        this.rasterWidth = rasterWidth;

        this.bandCount = bandCount;
        this.bandData = new float[bandCount][rasterWidth * rasterHeight];
        for (int i = 0; i < this.bandCount; i++) {
            Arrays.fill(bandData[i], Float.NaN);
        }

        bandIndices = new int[bandCount];
        bandNames = new String[bandCount];
        bandMinValues = new float[bandCount];
        bandMaxValues = new float[bandCount];
        for (int i = 0; i < bandCount; i++) {
            BinningOp.BandConfiguration bandConfiguration = bandConfigurations[i];
            String nameStr = bandConfiguration.name;
            bandIndices[i] = Integer.parseInt(bandConfiguration.index);
            bandNames[i] = nameStr != null ? nameStr : featureNames[bandIndices[i]];
            bandMinValues[i] = Float.parseFloat(bandConfiguration.minValue);
            bandMaxValues[i] = Float.parseFloat(bandConfiguration.maxValue);
        }
    }

    @Override
    public Rectangle getRasterRegion() {
        return outputRegion;
    }

    @Override
    public void begin() {
        final File parentFile = outputFile.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
    }

    @Override
    public void end() throws IOException {
        if (writeRgb) {
            writeRgbImage(outputRegion.width, outputRegion.height,
                          bandData,
                          bandMinValues, bandMaxValues,
                          outputFormat, outputFile);
        } else {
            for (int i = 0; i < bandCount; i++) {
                String fileName = String.format("%s_%s%s",
                                                FileUtils.getFilenameWithoutExtension(outputFile),
                                                bandNames[i],
                                                FileUtils.getExtension(outputFile));
                File imageFile = new File(outputFile.getParentFile(), fileName);
                writeGrayScaleImage(outputRegion.width, outputRegion.height,
                                    bandData[i],
                                    bandMinValues[i], bandMaxValues[i],
                                    outputFormat, imageFile);
            }
        }
    }

    @Override
    public void renderBin(int x, int y, TemporalBin temporalBin, Vector outputVector) {
        for (int i = 0; i < bandCount; i++) {
            bandData[i][rasterWidth * y + x] = outputVector.get(bandIndices[i]);
        }
    }

    @Override
    public void renderMissingBin(int x, int y) {
        for (int i = 0; i < bandCount; i++) {
            bandData[i][rasterWidth * y + x] = Float.NaN;
        }
    }

    private static void writeGrayScaleImage(int width, int height,
                                            float[] rawData,
                                            float rawValue1, float rawValue2,
                                            String outputFormat, File outputImageFile) throws IOException {

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final byte[] data = dataBuffer.getData();
        final float a = 255f / (rawValue2 - rawValue1);
        final float b = -255f * rawValue1 / (rawValue2 - rawValue1);
        for (int i = 0; i < rawData.length; i++) {
            data[i] = toByte(rawData[i], a, b);
        }
        ImageIO.write(image, outputFormat, outputImageFile);
    }

    private static void writeRgbImage(int width, int height,
                                      float[][] rawData,
                                      float[] rawValue1, float[] rawValue2,
                                      String outputFormat, File outputImageFile) throws IOException {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final byte[] data = dataBuffer.getData();
        final float[] rawDataR = rawData[0];
        final float[] rawDataG = rawData[1];
        final float[] rawDataB = rawData[2];
        final float aR = 255f / (rawValue2[0] - rawValue1[0]);
        final float bR = -255f * rawValue1[0] / (rawValue2[0] - rawValue1[0]);
        final float aG = 255f / (rawValue2[1] - rawValue1[1]);
        final float bG = -255f * rawValue1[1] / (rawValue2[1] - rawValue1[1]);
        final float aB = 255f / (rawValue2[2] - rawValue1[2]);
        final float bB = -255f * rawValue1[2] / (rawValue2[2] - rawValue1[2]);
        final int n = width * height;
        for (int i = 0, j = 0; i < n; i++, j += 3) {
            data[j + 2] = toByte(rawDataR[i], aR, bR);
            data[j + 1] = toByte(rawDataG[i], aG, bG);
            data[j] = toByte(rawDataB[i], aB, bB);
        }
        ImageIO.write(image, outputFormat, outputImageFile);
    }

    private static byte toByte(float s, float a, float b) {
        int sample = (int) (a * s + b);
        if (sample < 0) {
            sample = 0;
        } else if (sample > 255) {
            sample = 255;
        }
        return (byte) sample;
    }


}
