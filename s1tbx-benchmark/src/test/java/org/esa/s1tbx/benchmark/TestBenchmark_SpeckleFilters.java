package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark_SpeckleFilters extends TestBenchmarks {

    @Test
    public void testGRD_specklefilter_Boxcar() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Boxcar");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_Median() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Median");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_Frost() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Frost");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_GammaMap() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Gamma Map");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_Lee() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Lee");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_RefinedLee() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Refined Lee");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_LeeSigma() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("Lee Sigma");
            }
        };
        b.run();
    }

    @Test
    public void testGRD_specklefilter_IDAN() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                process("IDAN");
            }
        };
        b.run();
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
