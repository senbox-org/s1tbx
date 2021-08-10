/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.s1tbx.sar.gpf.geometric.EllipsoidCorrectionRDOp;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.s1tbx.sar.gpf.geometric.TerrainFlatteningOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.raster.gpf.texture.GLCMOp;
import org.junit.Ignore;
import org.junit.Test;

public class TestBenchmark_SAR extends BaseBenchmarks {

    @Test
    public void testGRD_multilook() throws Exception {
        Benchmark b = new Benchmark("GRD_multilook") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = subset(grdFile, rect);

                MultilookOp op = new MultilookOp();
                op.setSourceProduct(srcProduct);
                Product trgProduct = op.getTargetProduct();

                writeGPF(trgProduct, outputFolder, DIMAP);

                trgProduct.dispose();
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testGRD_terraincorrect() throws Exception {
        Benchmark b = new Benchmark("GRD_terraincorrect") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = subset(grdFile, rect);

                RangeDopplerGeocodingOp op = new RangeDopplerGeocodingOp();
                op.setSourceProduct(srcProduct);
                Product trgProduct = op.getTargetProduct();

                writeGPF(trgProduct, outputFolder, DIMAP);

                trgProduct.dispose();
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testGRD_ellipsoidcorrect() throws Exception {
        Benchmark b = new Benchmark("GRD_ellipsoidcorrect") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = subset(grdFile, rect);

                EllipsoidCorrectionRDOp op = new EllipsoidCorrectionRDOp();
                op.setSourceProduct(srcProduct);
                Product trgProduct = op.getTargetProduct();

                writeGPF(trgProduct, outputFolder, DIMAP);

                trgProduct.dispose();
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testGRD_terrainflatten() throws Exception {
        Benchmark b = new Benchmark("GRD_terrainflatten") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = subset(grdFile, rect);

                CalibrationOp cal = new CalibrationOp();
                cal.setSourceProduct(srcProduct);
                cal.setParameter("outputBetaBand", true);
                Product calProduct = cal.getTargetProduct();

                TerrainFlatteningOp op = new TerrainFlatteningOp();
                op.setSourceProduct(calProduct);
                Product trgProduct = op.getTargetProduct();

                writeGPF(trgProduct, outputFolder, DIMAP);

                trgProduct.dispose();
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    @Ignore
    public void testGRD_glcm() throws Exception {
        Benchmark b = new Benchmark("GRD_glcm") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = subset(grdFile, rect);

                GLCMOp op = new GLCMOp();
                op.setSourceProduct(srcProduct);
                op.setParameter("quantizationLevelsStr", "16");
                Product trgProduct = op.getTargetProduct();

                writeGPF(trgProduct, outputFolder, DIMAP);

                trgProduct.dispose();
                srcProduct.dispose();
            }
        };
        b.run();
    }
}
