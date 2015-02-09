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

package org.esa.beam.examples.data_export;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

/**
 * Shows how to access pixel data of a band.
 */
public class BandEx {

    public static void main(String[] args) {
        try {
            Product product = ProductIO.readProduct("C:/Projects/BEAM/data/MER_RR__1P_A.N1");
            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();
            Band band = product.getBand("radiance_13");
            float[] scan = new float[width];
            for (int y = 0; y < height; y += 100) {
                band.readPixels(0, y, width, 1, scan, ProgressMonitor.NULL);
                for (int x = 0; x < width; x += 100) {
                    float value = scan[x];
                    System.out.println(value + " ");
                }
            }
            System.out.println();
            product.dispose();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }

    }
}
