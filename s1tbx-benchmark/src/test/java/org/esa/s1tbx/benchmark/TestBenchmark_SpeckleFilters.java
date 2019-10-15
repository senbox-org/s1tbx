package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark_SpeckleFilters extends TestBenchmarks {

    @Test
    public void testGRD_specklefilter_Boxcar() throws Exception {
        process("Boxcar");
    }

    @Test
    public void testGRD_specklefilter_Median() throws Exception {
        process("Median");
    }

    @Test
    public void testGRD_specklefilter_Frost() throws Exception {
        process("Frost");
    }

    @Test
    public void testGRD_specklefilter_GammaMap() throws Exception {
        process("Gamma Map");
    }

    @Test
    public void testGRD_specklefilter_Lee() throws Exception {
        process("Lee");
    }

    @Test
    public void testGRD_specklefilter_RefinedLee() throws Exception {
        process("Refined Lee");
    }

    @Test
    public void testGRD_specklefilter_LeeSigma() throws Exception {
        process("Lee Sigma");
    }

    @Test
    public void testGRD_specklefilter_IDAN() throws Exception {
        process("IDAN");
    }

    private void process(final String name) throws IOException {
        final Product srcProduct = subset(grdFile, rect);

        SpeckleFilterOp op = new SpeckleFilterOp();
        op.setSourceProduct(srcProduct);
        op.SetFilter(name);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }
}
