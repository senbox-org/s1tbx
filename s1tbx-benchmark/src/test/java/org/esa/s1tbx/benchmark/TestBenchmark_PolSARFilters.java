package org.esa.s1tbx.benchmark;

import org.csa.rstb.polarimetric.gpf.PolarimetricMatricesOp;
import org.csa.rstb.polarimetric.gpf.PolarimetricSpeckleFilterOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark_PolSARFilters extends TestBenchmarks {

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

    @Test
    @Ignore
    public void testQP_specklefilter_NonLocal() throws Exception {
        String name = "Non Local Filter";

        final Product srcProduct = subset(qpFile, rect);

        PolarimetricMatricesOp mat = new PolarimetricMatricesOp();
        mat.setSourceProduct(srcProduct);
        mat.setParameter("matrix", "C3");
        Product c3Product = mat.getTargetProduct();

        PolarimetricSpeckleFilterOp op = new PolarimetricSpeckleFilterOp();
        op.setSourceProduct(c3Product);
        op.SetFilter(name);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }

    private void specklefilter(final String name) throws IOException {
        final Product srcProduct = subset(qpFile, rect);

        PolarimetricSpeckleFilterOp op = new PolarimetricSpeckleFilterOp();
        op.setSourceProduct(srcProduct);
        op.SetFilter(name);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }
}
