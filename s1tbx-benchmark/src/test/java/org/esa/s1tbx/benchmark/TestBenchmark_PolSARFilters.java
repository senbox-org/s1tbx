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

import org.csa.rstb.polarimetric.gpf.PolarimetricSpeckleFilterOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestBenchmark_PolSARFilters extends BaseBenchmarks {

    @Test
    public void testQP_specklefilter_Boxcar() throws Exception {
        specklefilter("Box Car Filter");
    }

    @Test
    public void testQP_specklefilter_RefinedLee() throws Exception {
        specklefilter("Refined Lee Filter");
    }

    @Test
    public void testQP_specklefilter_IDAN() throws Exception {
        specklefilter("IDAN Filter");
    }

    @Test
    public void testQP_specklefilter_LeeSigma() throws Exception {
        specklefilter("Improved Lee Sigma Filter");
    }

    private void specklefilter(final String name) throws Exception {
        Benchmark b = new Benchmark(name) {
            @Override
            protected void execute() throws Exception {
                process(name, outputFolder);
            }
        };
        b.run();
    }

    private void process(final String name, final File outputFolder) throws IOException {
        final Product srcProduct = read(qpFile);

        PolarimetricSpeckleFilterOp op = new PolarimetricSpeckleFilterOp();
        op.setSourceProduct(srcProduct);
        op.SetFilter(name);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct, outputFolder, DIMAP);

        trgProduct.dispose();
        srcProduct.dispose();
    }
}
