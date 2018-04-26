/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.support;

import javax.media.jai.*;
import javax.media.jai.operator.DFTDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * Created by luis on 11/02/2016.
 */
public class JAIFunctions {

    public static PlanarImage dft(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(DFTDescriptor.SCALING_NONE);
        pb.add(DFTDescriptor.REAL_TO_COMPLEX);
        return JAI.create("dft", pb, null);
    }

    public static PlanarImage idft(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(DFTDescriptor.SCALING_DIMENSIONS);
        pb.add(DFTDescriptor.COMPLEX_TO_COMPLEX);
        return JAI.create("idft", pb, null);
    }

    public static PlanarImage conjugate(final PlanarImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        return JAI.create("conjugate", pb, null);
    }

    public static PlanarImage multiplyComplex(final PlanarImage image1, final PlanarImage image2) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image1);
        pb.addSource(image2);
        return JAI.create("MultiplyComplex", pb, null);
    }

    public static RenderedImage upsampling(final PlanarImage image,
                                           final int rowUpSamplingFactor, final int colUpSamplingFactor) {

        final int w = image.getWidth();  // w is power of 2
        final int h = image.getHeight(); // h is power of 2
        final int newWidth = rowUpSamplingFactor * w; // rowInterpFactor should be power of 2 to avoid zero padding in idft
        final int newHeight = colUpSamplingFactor * h; // colInterpFactor should be power of 2 to avoid zero padding in idft

        // create shifted image
        final ParameterBlock pb1 = new ParameterBlock();
        pb1.addSource(image);
        pb1.add(w / 2);
        pb1.add(h / 2);
        PlanarImage shiftedImage = JAI.create("PeriodicShift", pb1, null);

        // create zero padded image
        final ParameterBlock pb2 = new ParameterBlock();
        final int leftPad = (newWidth - w) / 2;
        final int rightPad = leftPad;
        final int topPad = (newHeight - h) / 2;
        final int bottomPad = topPad;
        pb2.addSource(shiftedImage);
        pb2.add(leftPad);
        pb2.add(rightPad);
        pb2.add(topPad);
        pb2.add(bottomPad);
        pb2.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
        final PlanarImage zeroPaddedImage = JAI.create("border", pb2);

        // reposition zero padded image so the image origin is back at (0,0)
        final ParameterBlock pb3 = new ParameterBlock();
        pb3.addSource(zeroPaddedImage);
        pb3.add(1.0f * leftPad);
        pb3.add(1.0f * topPad);
        final PlanarImage zeroBorderedImage = JAI.create("translate", pb3, null);

        // shift the zero padded image
        final ParameterBlock pb4 = new ParameterBlock();
        pb4.addSource(zeroBorderedImage);
        pb4.add(newWidth / 2);
        pb4.add(newHeight / 2);
        final PlanarImage shiftedZeroPaddedImage = JAI.create("PeriodicShift", pb4, null);

        return shiftedZeroPaddedImage;
    }

    public static PlanarImage magnitude(final PlanarImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        return JAI.create("magnitude", pb, null);
    }

    public static double getMean(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(null); // null ROI means whole image
        pb.add(1); // check every pixel horizontally
        pb.add(1); // check every pixel vertically

        // Perform the mean operation on the source image.
        final RenderedImage meanImage = JAI.create("mean", pb, null);
        // Retrieve and report the mean pixel value.
        final double[] mean = (double[]) meanImage.getProperty("mean");
        return mean[0];
    }

    public static double getMax(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(null); // null ROI means whole image
        pb.add(1); // check every pixel horizontally
        pb.add(1); // check every pixel vertically

        // Perform the extrema operation on the source image
        final RenderedOp op = JAI.create("extrema", pb);
        // Retrieve both the maximum and minimum pixel value
        final double[][] extrema = (double[][]) op.getProperty("extrema");
        return extrema[1][0];
    }

    /**
     * Create warped image.
     *
     * @param warp     The WARP polynomial.
     * @param srcImage The source image.
     * @return The warped image.
     */
    public static RenderedOp createWarpImage(final WarpPolynomial warp, final RenderedImage srcImage,
                                              final Interpolation interp, final InterpolationTable interpTable) {

        // reformat source image by casting pixel values from ushort to float
        final ParameterBlock pb1 = new ParameterBlock();
        pb1.addSource(srcImage);
        pb1.add(DataBuffer.TYPE_FLOAT);
        final RenderedImage srcImageFloat = JAI.create("format", pb1);

        if (warp == null) {
            // no need to warp, images are already perfectly aligned
            return (RenderedOp) srcImageFloat;
        }

        // get warped image
        final ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(srcImageFloat);
        pb2.add(warp);

        if (interp != null) {
            pb2.add(interp);
        } else if (interpTable != null) {
            pb2.add(interpTable);
        }

        return JAI.create("warp", pb2);
    }
}
