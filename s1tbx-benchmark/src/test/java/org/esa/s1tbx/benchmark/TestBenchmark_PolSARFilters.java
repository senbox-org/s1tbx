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
