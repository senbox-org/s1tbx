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

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.WarpPolynomial;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;
import javax.media.jai.operator.WarpDescriptor;
import java.awt.RenderingHints;
import java.util.Arrays;

public class WarpTestMain {

    public static void main(String[] args) {
        String sourceFile = args[0];
        String targetFile = args[1];
        String format = targetFile.substring(1 + targetFile.lastIndexOf('.'));

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        double backgroundValue = 255;


        RenderedOp source = FileLoadDescriptor.create(sourceFile, null, true, null);


        double[] background = new double[source.getNumBands()];
        Arrays.fill(background, backgroundValue);

        int w = source.getWidth();
        int h = source.getHeight();
        float[] sourceCoords = new float[2 * w * h];
        int n = 0;
        final int step = 32;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                sourceCoords[2*n] = x;
                sourceCoords[2*n + 1] = y;
                n++;
            }
        }
        float[] targetCoords = new TExp(w, h).t(sourceCoords);

        Warp warp = WarpPolynomial.createWarp(sourceCoords, 0,
                                              targetCoords, 0,
                                              n,
                                              1.0F / w,
                                              1.0F / h,
                                              (float) w,
                                              (float) h, 3);
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setWidth(w);
        imageLayout.setHeight(h);

        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        RenderedOp target = WarpDescriptor.create(source, warp, interpolation, background, renderingHints);
        FileStoreDescriptor.create(target, targetFile, format, null, false, null);
    }

    interface T {
        float[] t(float[] coord);
    }


    static class TExp implements T {
        final int w, h;

        TExp(int w, int h) {
            this.w = w;
            this.h = h;
        }

        public float[] t(float[] a) {

            float[] b = new float[a.length];
            float s = 1 / (float) Math.sqrt((float) w * w + (float) h * h);
            s*=s;
            for (int i = 0; i < a.length; i += 2) {
                float x = a[i];
                float y = a[i + 1];
                float dx = x - 0.5f * w;
                float dy = y - 0.5f * h;
                float v = (float) Math.sqrt(dx * dx + dy * dy);
                v = s * (v*v);
                float xx = 0.5f * w + v * dx;
                float yy = 0.5f * h + v * dy;

                System.out.println("(" + x + "," + y + ") --> (" + yy + "," + yy + ")");

                b[i] = xx;
                b[i + 1] = yy;

            }
            return b;
        }
    }

}
