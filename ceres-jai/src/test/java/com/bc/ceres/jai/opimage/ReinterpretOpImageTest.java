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

package com.bc.ceres.jai.opimage;

import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.operator.ScalingType;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.EXPONENTIAL;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.INTERPRET_BYTE_SIGNED;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.INTERPRET_INT_UNSIGNED;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LOGARITHMIC;
import static org.junit.Assert.assertEquals;

public class ReinterpretOpImageTest {

    private static final int W = 10;
    private static final int H = 10;

    @Test
    public void testRescaleByte() {
        testRescale(new Byte[]{-1}, 11.0, 1.0, LINEAR, AWT,
                    DataBuffer.TYPE_FLOAT, 255 * 11.0 + 1.0);
    }

    @Test
    public void testRescaleSByte() {
        testRescale(new Byte[]{-1}, 11.0, 1.0, LINEAR, INTERPRET_BYTE_SIGNED,
                    DataBuffer.TYPE_FLOAT, -1 * 11.0 + 1.0);
    }

    @Test
    public void testRescaleInt() {
        testRescale(new Integer[]{-1}, 11.0, 1.0, LINEAR, AWT,
                    DataBuffer.TYPE_DOUBLE, -1 * 11.0 + 1.0);
    }

    @Test
    public void testRescaleUInt() {
        testRescale(new Integer[]{-1}, 11.0, 1.0, LINEAR, INTERPRET_INT_UNSIGNED,
                    DataBuffer.TYPE_DOUBLE, (((long) 1 << 32) - 1) * 11.0 + 1.0);
    }

    @Test
    public void testRescaleFloatLin() {
        testRescale(new Float[]{0.1f}, 11.0, 1.0, LINEAR, AWT,
                    DataBuffer.TYPE_FLOAT, 0.1f * 11.0 + 1.0);
    }

    @Test
    public void testRescaleFloatExp10() {
        testRescale(new Float[]{0.1f}, 11.0, 1.0, EXPONENTIAL, AWT,
                    DataBuffer.TYPE_FLOAT, Math.pow(10, 0.1f * 11.0 + 1.0));
    }

    @Test
    public void testRescaleFloatLog10() {
        testRescale(new Float[]{0.1f}, 11.0, 1.0, LOGARITHMIC, AWT,
                    DataBuffer.TYPE_FLOAT, Math.log10(0.1 * 11.0 + 1.0));
    }

    @Test
    public void testRescaleByteExp10() {
        testRescale(new Byte[]{-1}, 1.0 / 255.0, 1.0, EXPONENTIAL, AWT,
                    DataBuffer.TYPE_FLOAT, Math.pow(10, 1.0 + 1.0));
        testRescale(new Byte[]{-1}, 1.0 / 255.0, 1.0, EXPONENTIAL, INTERPRET_BYTE_SIGNED,
                    DataBuffer.TYPE_FLOAT, Math.pow(10, -1.0 / 255.0 + 1.0));
    }

    @Test
    public void testRescaleByteLog10() {
        testRescale(new Byte[]{-1}, 1.0 / 255.0, 1.0, LOGARITHMIC, AWT,
                    DataBuffer.TYPE_FLOAT, Math.log10(1.0 + 1.0));
        testRescale(new Byte[]{-1}, 1.0 / 255.0, 1.0, LOGARITHMIC, INTERPRET_BYTE_SIGNED,
                    DataBuffer.TYPE_FLOAT, Math.log10(-1.0 / 255.0 + 1.0));
    }

    private void testRescale(Number[] sourcePixelValue, double factor, double offset, ScalingType scalingType, InterpretationType interpretationType, int expectedDataType, double expectedPixelValue) {
        final RenderedImage target = ReinterpretOpImage.create(createSourceImage(sourcePixelValue), factor, offset, scalingType, interpretationType, null);
        assertEquals(expectedDataType, target.getSampleModel().getDataType());
        final Raster targetData = target.getData();
        assertEquals(expectedPixelValue, targetData.getSampleDouble(0, 0, 0), expectedDataType == DataBuffer.TYPE_DOUBLE ? 1e-10 : 1e-5);
    }

    @Test
    @Ignore
    public void testScalingTypePerformance() {
        testScalingTypePerformance(LINEAR);
        testScalingTypePerformance(EXPONENTIAL);
        testScalingTypePerformance(LOGARITHMIC);
    }

    private void testScalingTypePerformance(ScalingType scalingType) {
        int size = 2 * 1024; // = 4 mega-pixels
        RenderedOp sourceImage = ConstantDescriptor.create((float) size, (float) size, new Byte[]{1}, null);
        RenderedImage renderedImage = ReinterpretOpImage.create(sourceImage, 1.1, 0.1, scalingType, AWT, null);
        int n = 10;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            long t0 = System.currentTimeMillis();
            renderedImage.getData();
            long t1 = System.currentTimeMillis();
            sum += t1 - t0;
        }
        System.out.println(scalingType + ": " + sum/n + " ms for " + size*size + " pixels");
    }

    @Test
    public void testImageLayout() {
        int w = 1121;
        int h = 2000;
        final RenderedImage source = createSourceImage(w, h, w, 64, new Integer[]{0});

        final SampleModel sampleModel = source.getSampleModel().createCompatibleSampleModel(w, h);
        final ImageLayout imageLayout = ReinterpretDescriptor.createTargetImageLayout(source, sampleModel);
        final RenderedImage target = ReinterpretDescriptor.create(source, 11.0, 1.0, LINEAR, AWT, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        assertEquals(w, target.getWidth());
        assertEquals(h, target.getHeight());
        assertEquals(w, target.getTileWidth());
        assertEquals(64, target.getTileHeight());
    }

    static RenderedImage createSourceImage(Number[] v) {
        return ConstantDescriptor.create((float) W, (float) H, v, null);
    }

    static RenderedImage createSourceImage(int w, int h, int tw, int th, Number[] v) {
        ImageLayout imageLayout = new ImageLayout(0, 0, w, h);
        imageLayout.setTileWidth(tw);
        imageLayout.setTileHeight(th);
        return ConstantDescriptor.create((float) w, (float) h, v, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
    }
}
