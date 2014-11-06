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

import static com.bc.ceres.jai.DFTTestMain.showImage;
import com.bc.ceres.jai.operator.DFTConvolveDescriptor;
import com.bc.ceres.jai.opimage.DFTConvolveRIF;
import com.bc.ceres.jai.tilecache.SwappingTileCache;
import com.bc.ceres.jai.tilecache.DefaultSwapSpace;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class DFTConvolveTestMain {
    private static final long M = 1024L*1024L;

    public static void main(String[] args) {
        final File swapDir = new File("swap");
        swapDir.mkdir();
        final Logger logger = Logger.getAnonymousLogger();
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
        final DefaultSwapSpace swapSpace = new DefaultSwapSpace(swapDir, logger);
        final SwappingTileCache tileCache = new SwappingTileCache(64 * M, swapSpace);
        JAI.getDefaultInstance().setTileCache(tileCache);


        RenderedImage sourceImage;
        if (args.length == 0) {
            sourceImage = DFTTestMain.createTestImage(512, 512);
        } else {
            sourceImage = FileLoadDescriptor.create(args[0], null, false, null);
        }

        sourceImage = DFTConvolveRIF.toFloat(sourceImage, null);

        BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, extender);
        KernelJAI kernel = createBlurrKernel(33);
        RenderedOp convolvedImage = ConvolveDescriptor.create(sourceImage, kernel, hints);
        RenderedOp dftConvolvedImage = DFTConvolveDescriptor.create(sourceImage, kernel, null, hints);
        RenderedOp deltaImage = MultiplyConstDescriptor.create(SubtractDescriptor.create(convolvedImage, dftConvolvedImage, null), new double[]{2}, null);
        showImage(sourceImage, "sourceImage");
        showImage(convolvedImage, "convolvedImage");
        showImage(dftConvolvedImage, "dftConvolvedImage");
        showImage(deltaImage, "deltaImage");

        final Object o = dftConvolvedImage.getProperty("kernelFT");
        System.out.println("o = " + o);

        System.out.println("Kernel\tConvolve\tDFTConvolve\tPerfGain");
        for (int i = 3; i <= 201; i += 2) {
            KernelJAI k = createBlurrKernel(i);
            double t1 = getRenderTime(ConvolveDescriptor.create(sourceImage, k, hints));
            double t2 = getRenderTime(DFTConvolveDescriptor.create(sourceImage, k, null, hints));
            System.out.println(i + "\t" + t1 + "\t" + t2 + "\t" + t1 / t2);
        }
    }

    private static double getRenderTime(RenderedOp op) {
        long t0 = System.nanoTime();
        op.getAsBufferedImage();
        long t1 = System.nanoTime();
        return (t1 - t0) / (1000.0 * 1000.0);
    }

    private static KernelJAI createBlurrKernel(int size) {
        float[] data = new float[size * size];

        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                float dx = i / (size - 1.0f) - 0.5f;
                float dy = j / (size - 1.0f) - 0.5f;
                float v = 1.0f - (float) Math.sqrt(dx * dx + dy * dy);
                data[j * size + i] = v < 0 ? 0 : v;
            }
        }
        float sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] /= sum;
        }
        return new KernelJAI(size, size, data);
    }

    private static KernelJAI createIdentityKernel(int size) {
        float[] data = new float[size * size];
        data[size / 2 * size + size / 2] = 1;
        return new KernelJAI(size, size, data);
    }
}