/*
 * Copyright (c) 2013. Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.pfa.fe;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * Usage: PhaseCorrelation &lt;image&gt; &lt;search-pattern&gt;
 * <p/>
 * See http://en.wikipedia.org/wiki/Phase_correlation
 *
 * @author Norman Fomferra
 */
public class PhaseCorrelation {

    public static final int MiB = (1024 * 1024);

    public static void main(String[] args) {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024 * MiB);

        final String imagePath;
        final String patternPath;
        if (args.length == 0) {
            imagePath = "src/test/images/Algal_bloom_20040615.jpg";
            patternPath = "src/test/images/Algal_bloom_20040615-pattern.jpg";
        } else {
            imagePath = args[0];
            patternPath = args[1];
        }

        RenderedOp im1 = FileLoadDescriptor.create(imagePath, null, null, null);
        printInfo("im1", im1);
        RenderedOp im2 = FileLoadDescriptor.create(patternPath, null, null, null);
        printInfo("im2", im2);

        im2 = BorderDescriptor.create(im2, null, im1.getWidth() - im2.getWidth(), null, im1.getHeight() - im2.getHeight(), BorderExtender.createInstance(BorderExtender.BORDER_ZERO), null);
        printInfo("im2", im2);

        RenderedOp im1F = DFTDescriptor.create(im1, DFTDescriptor.SCALING_UNITARY, DFTDescriptor.REAL_TO_COMPLEX, null);
        printInfo("im1F", im1F);
        RenderedOp im2F = DFTDescriptor.create(im2, DFTDescriptor.SCALING_UNITARY, DFTDescriptor.REAL_TO_COMPLEX, null);
        printInfo("im2F", im2F);


        RenderedOp im2FC = ConjugateDescriptor.create(im2F, null);
        printInfo("im2FC", im2FC);

        RenderedOp im3FMul = MultiplyComplexDescriptor.create(im1F, im2FC, null);
        printInfo("im3FMul", im3FMul);
        RenderedOp im3FMag = MagnitudeDescriptor.create(im3FMul, null);
        printInfo("im3FMag", im3FMag);
        write(im3FMag, "im3FMag");
        im3FMag = BandSelectDescriptor.create(im3FMag, new int[]{0, 0, 1, 1, 2, 2}, null);
        printInfo("im3FMag", im3FMag);

        //RenderedOp im3F = im3FMul;
        RenderedOp im3F = DivideDescriptor.create(im3FMul, im3FMag, null);
        printInfo("im3F", im3F);

        RenderedOp im3 = IDFTDescriptor.create(im3F, DFTDescriptor.SCALING_UNITARY, DFTDescriptor.COMPLEX_TO_REAL, null);
        printInfo("im3", im3);

        im3 = CropDescriptor.create(im3, 0F, 0F, 0F + im1.getWidth(), 0F + im1.getHeight(), null);
        printInfo("im3", im3);

        write(im3, "output");
    }

    private static void write(RenderedOp im3, String filename) {
        FileStoreDescriptor.create(toByteIm(im3), filename + ".png", "PNG", null, null, null);
    }

    static void printInfo(String name, RenderedImage im) {
        System.out.printf("%s: w=%d, h=%d, #b=%d, dt=%d%n",
                          name,
                          im.getWidth(),
                          im.getHeight(),
                          im.getSampleModel().getNumBands(),
                          im.getSampleModel().getDataType()
        );
        double[][] extrema = (double[][]) ExtremaDescriptor.create(im, null, 1, 1, null, 1, null).getProperty("extrema");
        double[] min = extrema[0];
        double[] max = extrema[1];
        for (int i = 0; i < min.length; i++) {
            System.out.println("  band " + i + ": " + min[i] + " ... " + max[i]);
        }
        System.out.println();
    }

    private static RenderedOp toByteIm(RenderedOp im) {
        double[][] extrema = (double[][]) ExtremaDescriptor.create(im, null, 1, 1, null, 1, null).getProperty("extrema");

        double[] min = extrema[0];
        double[] max = extrema[1];
        double[] constants = new double[im.getNumBands()];
        double[] offsets = new double[im.getNumBands()];
        for (int i = 0; i < im.getNumBands(); i++) {
            constants[i] = 255.0 / (max[i] - min[i]);
            offsets[i] = -min[i];

            System.out.println("band " + i + ": " + constants[i] + ", " + offsets[i]);
        }

        im = RescaleDescriptor.create(im, constants, offsets, null);
        im = FormatDescriptor.create(im, DataBuffer.TYPE_BYTE, null);
        return im;
    }

}
