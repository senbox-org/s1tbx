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


import com.sun.media.jai.opimage.RIFUtil;

import javax.media.jai.BorderExtender;
import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.DFTDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.IDFTDescriptor;
import javax.media.jai.operator.MultiplyComplexDescriptor;
import javax.media.jai.operator.RescaleDescriptor;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;


public class DFTConvolveRIF implements RenderedImageFactory {

    boolean trace = false;

    public DFTConvolveRIF() {
    }
        

    /**
     * Create a new instance of ConvolveOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock The source image and the convolution kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        KernelJAI kernel = (KernelJAI) paramBlock.getObjectParameter(0);
        RenderedImage kernelFT = (RenderedImage) paramBlock.getObjectParameter(1);

        RenderedImage sourceImage = (RenderedImage) paramBlock.getSource(0);
        printImageInfo(sourceImage, "sourceImage");

        int iw = sourceImage.getWidth();
        int ih = sourceImage.getHeight();

        int kw = kernel.getWidth();
        int kh = kernel.getHeight();
        int iw2 = getNextBase2Size(iw + 2 * kw);
        int ih2 = getNextBase2Size(ih + 2 * kh);

        int leftPad = (iw2 - iw) / 2;
        int rightPad = (iw2 - iw) - leftPad;
        int topPad = (ih2 - ih) / 2;
        int bottomPad = (ih2 - ih) - topPad;

        RenderedImage extendedImage = BorderDescriptor.create(toFloat(sourceImage, renderHints),
                                                              leftPad,
                                                              rightPad,
                                                              topPad,
                                                              bottomPad,
                                                              extender,
                                                              null);
        printImageInfo(extendedImage, "extendedImage");

        if (kernelFT == null) {
            TiledImage kernelImage = createKernelImage(extendedImage,
                                                       extendedImage.getSampleModel().getDataType(),
                                                       kernel);
            printImageInfo(kernelImage, "kernelImage");

            kernelFT = DFTDescriptor.create(kernelImage,
                                            DFTDescriptor.SCALING_NONE,
                                            DFTDescriptor.REAL_TO_COMPLEX,
                                            null);
        }
        printImageInfo(kernelFT, "kernelFT");

        RenderedOp sourceFT = DFTDescriptor.create(extendedImage,
                                                   DFTDescriptor.SCALING_NONE,
                                                   DFTDescriptor.REAL_TO_COMPLEX,
                                                   null);
        printImageInfo(sourceFT, "sourceFT");

        RenderedImage productFT = MultiplyComplexDescriptor.create(sourceFT, kernelFT, null);
        printImageInfo(productFT, "productFT");

        RenderedImage convolvedImage = IDFTDescriptor.create(productFT,
                                                             DFTDescriptor.SCALING_DIMENSIONS,
                                                             DFTDescriptor.COMPLEX_TO_REAL,
                                                             null);
        printImageInfo(convolvedImage, "convolvedImage");

        RenderedOp croppedImage = CropDescriptor.create(convolvedImage,
                                                        0.0f,
                                                        0.0f,
                                                        (float) iw,
                                                        (float) ih,
                                                        null);
        croppedImage.setProperty("kernelFT", kernelFT);
        printImageInfo(croppedImage, "croppedImage");
        return croppedImage;

    }

    public static TiledImage createKernelImage(RenderedImage sourceImage,
                                               int dataType,
                                               KernelJAI kernel) {
        float[] kernelData = kernel.getKernelData();

        if (dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE) {
            throw new IllegalArgumentException("dataType");
        }

        ComponentSampleModelJAI sm = new ComponentSampleModelJAI(dataType,
                                                                 sourceImage.getTileWidth(),
                                                                 sourceImage.getTileHeight(),
                                                                 1,
                                                                 sourceImage.getTileWidth(),
                                                                 new int[]{0});
        int iw = sourceImage.getWidth();
        int ih = sourceImage.getHeight();
        int ix0 = sourceImage.getMinX();
        int iy0 = sourceImage.getMinY();

        TiledImage kernelImage = new TiledImage(ix0,
                                                iy0,
                                                iw,
                                                ih,
                                                sourceImage.getTileGridXOffset(),
                                                sourceImage.getTileGridYOffset(),
                                                sm,
                                                PlanarImage.createColorModel(sm));

        kernelData = normalizeKernelData(kernelData);

        int kw = kernel.getWidth();
        int kx0 = kernel.getXOrigin();
        int ky0 = kernel.getYOrigin();

        // Upper
        for (int y = 0; y <= ky0; y++) {
            int ky = y + ky0;
            // Left
            for (int x = 0; x <= kx0; x++) {
                int kx = x + kx0;
                kernelImage.setSample(ix0 + x, iy0 + y, 0, kernelData[ky * kw + kx]);
            }
            // Right
            for (int x = iw - kx0; x < iw; x++) {
                int kx = x - (iw - kx0);
                kernelImage.setSample(ix0 + x, iy0 + y, 0, kernelData[ky * kw + kx]);
            }
        }

        // Lower
        for (int y = ih - ky0; y < ih; y++) {
            int ky = y - (ih - ky0);
            // Left
            for (int x = 0; x <= kx0; x++) {
                int kx = x + kx0;
                kernelImage.setSample(ix0 + x, iy0 + y, 0, kernelData[ky * kw + kx]);
            }
            // Right
            for (int x = iw - kx0; x < iw; x++) {
                int kx = x - (iw - kx0);
                kernelImage.setSample(ix0 + x, iy0 + y, 0, kernelData[ky * kw + kx]);
            }
        }
        return kernelImage;
    }

    public static float[] normalizeKernelData(float[] kernelData) {
        float[] clone = kernelData.clone();
        float sum = 0;
        for (int i = 0; i < kernelData.length; i++) {
            sum += clone[i];
        }
        for (int i = 0; i < kernelData.length; i++) {
            clone[i] /= sum;
        }
        return clone;
    }

    public static RenderedImage toFloat(RenderedImage sourceImage, RenderingHints hints) {
        int type = sourceImage.getSampleModel().getDataType();
        if (type == DataBuffer.TYPE_BYTE || type == DataBuffer.TYPE_INT) {
            return toFloat(sourceImage, 0.0, 255.0, 0.0, 1.0, hints);
        } else if (type == DataBuffer.TYPE_SHORT) {
            return toFloat(sourceImage, Short.MIN_VALUE, Short.MAX_VALUE, 0.0, 1.0, hints);
        } else if (type == DataBuffer.TYPE_USHORT) {
            return toFloat(sourceImage, 0, 2 * Short.MAX_VALUE + 1, 0.0, 1.0, hints);
        } else {
            return sourceImage;
        }
    }

    public static RenderedImage toFloat(RenderedImage sourceImage,
                                        double x1, double x2,
                                        double y1, double y2,
                                        RenderingHints hints) {
        double a = (y2 - y1) / (x2 - x1);
        double b = y1 - a * x1;
        return RescaleDescriptor.create(FormatDescriptor.create(sourceImage,
                                                                DataBuffer.TYPE_FLOAT,
                                                                hints),
                                        new double[]{a},
                                        new double[]{b},
                                        hints);
    }

    public static int getNextBase2Size(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n <= 0");
        }
        if (Integer.bitCount(n) == 1) {
            return n;
        }

        return (int) Math.pow(2.0, 1.0 + Math.floor(Math.log(n) / Math.log(2.0)));
    }

    private void printImageInfo(RenderedImage sourceImage, String name) {
        if (trace) {
            System.out.println(name + ":");
            System.out.println("  minX   = " + sourceImage.getMinX());
            System.out.println("  minY   = " + sourceImage.getMinY());
            System.out.println("  width  = " + sourceImage.getWidth());
            System.out.println("  height = " + sourceImage.getHeight());
            System.out.println("  tileWidth  = " + sourceImage.getTileWidth());
            System.out.println("  tileHeight = " + sourceImage.getTileHeight());
        }
    }
}