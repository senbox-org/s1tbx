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

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.DFTDescriptor;
import javax.media.jai.operator.ExtremaDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.IDFTDescriptor;
import javax.media.jai.operator.MagnitudeDescriptor;
import javax.media.jai.operator.PhaseDescriptor;
import javax.media.jai.operator.RescaleDescriptor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import static java.lang.Math.*;

public class DFTTestMain {
    private static int location;

    public static void main(String[] args) {

        RenderedImage src;
        if (args.length == 0) {
            src = createTestImage(512, 512);
        } else {
            src = FileLoadDescriptor.create(args[0], null, false, null);
        }

        PlanarImage dft = DFTDescriptor.create(src,
                                               DFTDescriptor.SCALING_NONE,
                                               DFTDescriptor.REAL_TO_COMPLEX,
                                               null);

        PlanarImage idft = IDFTDescriptor.create(dft,
                                                 DFTDescriptor.SCALING_DIMENSIONS,
                                                 DFTDescriptor.COMPLEX_TO_REAL,
                                                 null);

        RenderedOp dftMagnitude = MagnitudeDescriptor.create(dft, null);
        RenderedOp dftPhase = PhaseDescriptor.create(dft, null);

        showImage(src, "Source");
        showImage(rescale(dftMagnitude, 0, 1000), "FFT Magnitude");
        showImage(rescale(dftPhase, -3, 3), "FFT Phase");
        showImage(rescale(idft, 0.0, 255.0), "Inverse FFT");
    }

    public static RenderedOp rescale(RenderedImage idft) {
        double[][] extrema = (double[][]) ExtremaDescriptor.create(idft, null, 1, 1, true, 1000, null).getProperty("extrema");

        double x1 = extrema[0][0];
        double x2 = extrema[1][0];

        System.out.println("extrema min = " + x1);
        System.out.println("extrema max = " + x2);
        return rescale(idft, x1, x2);
    }

    public static RenderedOp rescale(RenderedImage idft, double x1, double x2) {
        return FormatDescriptor.create(rescale(idft, x1, x2, 0.0, 255.0),
                                       DataBuffer.TYPE_BYTE,
                                       null);
    }

    public static RenderedOp rescale(RenderedImage src, double x1, double x2, double y1, double y2) {
        double a = (y2 - y1) / (x2 - x1);
        double b = y1 - a * x1;
        return RescaleDescriptor.create(FormatDescriptor.create(src,
                                                                DataBuffer.TYPE_DOUBLE,
                                                                null),
                                        new double[]{a},
                                        new double[]{b},
                                        null);
    }

    public static void showImage(RenderedImage image, String name) {
        int width = image.getWidth();
        int height = image.getHeight();
        int numBands = image.getSampleModel().getNumBands();
        int dataType = image.getSampleModel().getDataType();

        System.out.println("============= Image " + name);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("numBands = " + numBands);
        System.out.println("dataType = " + dataType);

        BufferedImage bufferedImage;
        if (image instanceof PlanarImage) {
            PlanarImage planarImage = (PlanarImage) image;
            long t0 = System.nanoTime();
            bufferedImage = planarImage.getAsBufferedImage();
            long t1 = System.nanoTime();
            System.out.println("BufferedImage created in " + (t1-t0)/(1000.0*1000.0) + " ms");
        } else if (image instanceof BufferedImage) {
            bufferedImage = (BufferedImage) image;
        } else {
            throw new IllegalArgumentException("image");
        }
        JScrollPane scrollPane = new JScrollPane(new JLabel(new ImageIcon(bufferedImage)));
        scrollPane.setBorder(null);
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(scrollPane);
        frame.pack();
        frame.setLocation(location += 24, location += 24);
        frame.setVisible(true);
    }

    public static BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        double r = 2 * PI;
        int n0 = 5;
        int n = 3;
        double fu = 1;
        double fv = 1;
        double s = 1.0 / n;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = r * x / (width - 1.0);
                double v = r * y / (height - 1.0);
                double w = 0;
                for (int k = n0; k < n0 + n; k++) {
                    w += 0.5*s * (sin(fu * k * u) + sin(fv * k * v));
                }
                w = 0.5 * (1.0 + w);
                data[y * width + x] = (byte) floor(255.0 * w);
            }
        }
        return image;
    }
}
