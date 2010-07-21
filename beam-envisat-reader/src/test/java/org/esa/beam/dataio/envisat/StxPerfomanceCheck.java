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

package org.esa.beam.dataio.envisat;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Stx;

import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;

public class StxPerfomanceCheck {

    public static void main(String[] args) throws Exception {
        System.out.println("Stx computation performance test:");
        System.out.println("=================================");
        for (int i = 0; i < args.length; i++) {
            String filePath = args[i];
            performeBandTest(filePath);
        }
    }

    private static void performeBandTest(String filePath) throws IOException {
        Product product = ProductIO.readProduct(filePath);
        Band band0 = product.getBandAt(0);
        double[] times = computeStx(band0);
        
        System.out.println(product.getProductType());
        System.out.println("band width : "+band0.getSceneRasterWidth());
        System.out.println("band height: "+band0.getSceneRasterHeight());
        System.out.println();
        for (int i = 0; i < times.length; i++) {
            System.out.println("stx"+i+" : "+times[i]);
        }
        System.out.println();
    }
    
    private static double[] computeStx(Band band) {
        double[] times = new double[3];
        for (int i = 0; i < times.length; i++) {
            times[i] = computeStxOnce(band);
        }
        return times;
    }
    private static double computeStxOnce(Band band) {
        final long t0 = System.nanoTime();
        Stx stx1 = Stx.create(band, 0, ProgressMonitor.NULL);
        final long t1 = System.nanoTime();
        return (t1-t0)/1e6;
        
    }
}
