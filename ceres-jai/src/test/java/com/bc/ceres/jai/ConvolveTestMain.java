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

package com.bc.ceres.jai;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.ClampDescriptor;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.MultiplyDescriptor;
import javax.media.jai.operator.NotDescriptor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import static java.lang.Math.*;

public class ConvolveTestMain {

    public static void main(String[] args) {
        String sourceFile = args[0];
        int m = Integer.parseInt(args[1]);

        String format = sourceFile.substring(1 + sourceFile.lastIndexOf('.'));
        String targetBase = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
        String targetFile0 = targetBase + "_" + m + "_0." + format;
        String targetFile1 = targetBase + "_" + m + "_1." + format;
        String targetFile2 = targetBase + "_" + m + "_2." + format;

        KernelJAI gaussKernel = createGaussianKernel(m);
        dumpKernelData("Gaussian", gaussKernel);
        KernelJAI distKernel = createDistancesKernel(m);
        dumpKernelData("Distances", distKernel);

        RenderedOp source = FileLoadDescriptor.create(sourceFile, null, true, null);
        source = BandSelectDescriptor.create(source, new int[1], null);
        System.out.println("Writing " + targetFile0);
        FileStoreDescriptor.create(source, targetFile0, format, null, false, null);



        long t1 = System.currentTimeMillis();
        System.out.println("Computing " + targetFile1);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderedOp target1 = ConvolveDescriptor.create(source, gaussKernel, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
        RenderedOp mask = ClampDescriptor.create(source, new double[]{0}, new double[]{1}, null);
        mask = MultiplyConstDescriptor.create(mask, new double[]{255}, null);
        mask = NotDescriptor.create(mask, null);
        mask = ClampDescriptor.create(mask, new double[]{0}, new double[]{1}, null);
        target1 = MultiplyDescriptor.create(target1, mask, null);
        target1 = AddDescriptor.create(target1, source, null);
        System.out.println("Writing " + targetFile1);
        FileStoreDescriptor.create(target1, targetFile1, format, null, false, null);
        System.out.println("Done in " + (System.currentTimeMillis() - t1) + " ms");

        long t2 = System.currentTimeMillis();
        System.out.println("Computing " + targetFile2);
        BufferedImage target2 = convolveICOL(source, gaussKernel, distKernel);
        System.out.println("Writing " + targetFile2);
        FileStoreDescriptor.create(target2, targetFile2, format, null, false, null);
        System.out.println("Done in " + (System.currentTimeMillis() - t2) + " ms");
    }

    private static BufferedImage convolveICOL(RenderedOp source, KernelJAI gaussKernel, KernelJAI distKernel) {
        final int m = gaussKernel.getWidth();
        final int n = m / 2 + 1;

        float[] weights = new float[n];
        float wSum = 0;
        for (int k = 0; k < n; k++) {
            weights[k] = gaussKernel.getElement(distKernel.getXOrigin() + k, distKernel.getYOrigin());
            wSum += weights[k];
        }
        for (int k = 0; k < n; k++) {
            weights[k] /= wSum;
        }

        dumpArray("Weights", weights);

        int width = source.getWidth();
        int height = source.getHeight();
        Raster aRaster = source.getData(new Rectangle(0, 0, width, height));
        BufferedImage cImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] aData = ((DataBufferByte) aRaster.getDataBuffer()).getData();
        byte[] cData = ((DataBufferByte) cImage.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float[] sums = new float[n];
                int[] counts = new int[n];
                for (int j = 0; j < m; j++) {
                    for (int i = 0; i < m; i++) {
                        int k = (int) distKernel.getElement(i, j);
                        if (k > 0) {
                            int xx = x + i - distKernel.getXOrigin();
                            int yy = y + j - distKernel.getYOrigin();
                            if (xx >= 0 && xx < width && yy >= 0 && yy < height) {
                                int a = (aData[yy * width + xx] & 0xff);
                                if (a > 0) { // = if (not no-data)
                                    sums[k] += a;
                                    counts[k]++;
                                }
                            }
                        }
                    }
                }

                int a = aData[y * width + x] & 0xff;

                float c = weights[0] * a;
                for (int k = 1; k < n; k++) {
                    if (counts[k] > 0) {
                        c += (weights[k] * sums[k]) / counts[k];
                    }
                }

                if (a == 0) {
                    cData[y * width + x] = (byte) c;
                } else {
                    cData[y * width + x] = aData[y * width + x];
                }
            }
        }
        return cImage;
    }


    private static KernelJAI createGaussianKernel(int m) {
        double sig = 0.46;
        float[] kernelData = new float[m * m];
        float sum = 0;
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < m; i++) {
                double dx = 2.0 * (i / (m - 1.0) - 0.5);
                double dy = 2.0 * (j / (m - 1.0) - 0.5);
                double z = 1.0 / (sqrt(2.0 * PI)) * exp(-0.5 * (dx * dx + dy * dy) / (sig * sig));
                float fz = (float) z;
                kernelData[j * m + i] = fz;
                sum += fz;
            }
        }

        for (int i = 0; i < kernelData.length; i++) {
            kernelData[i] /= sum;
        }

        return new KernelJAI(m, m, kernelData);
    }

    private static KernelJAI createDistancesKernel(int m) {
        float[] kernelData = new float[m * m];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < m; i++) {
                double dx = i - 0.5 * (m - 1);
                double dy = j - 0.5 * (m - 1);
                int z = (int) sqrt(dx * dx + dy * dy);
                if (z <= m / 2) {
                    kernelData[j * m + i] = (int) sqrt(dx * dx + dy * dy);
                }
            }
        }
        return new KernelJAI(m, m, kernelData);
    }

    private static void dumpKernelData(String name, KernelJAI kernel) {
        float[] kernelData1 = kernel.getKernelData();
        int m = kernel.getWidth();
        float max = 0;
        for (int i = 0; i < kernelData1.length; i++) {
            max = max(max, kernelData1[i]);
        }
        System.out.println(name + ":");
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < m; i++) {
                System.out.print((int) (9.999f * kernelData1[j * m + i] / max));
            }
            System.out.println();
        }
    }

    private static void dumpArray(String name, float[] data) {
        int n = data.length;
        float max = 0;
        for (int k = 0; k < n; k++) {
            max = max(max, data[k]);
        }

        System.out.println(name + ":");
        for (int k = 0; k < n; k++) {
            System.out.print((int) (9.999 * data[k] / max));
        }
        System.out.println();
    }


}