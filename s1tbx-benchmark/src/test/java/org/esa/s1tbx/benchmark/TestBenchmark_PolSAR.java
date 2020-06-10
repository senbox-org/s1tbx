package org.esa.s1tbx.benchmark;

import org.csa.rstb.polarimetric.gpf.PolarimetricDecompositionOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark_PolSAR extends TestBenchmarks {

    @Test
    public void testQP_read_write() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = read(qpFile);
                writeGPF(srcProduct);
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_pauli() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Pauli Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_sinclair() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Sinclair Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_FreemanDurden() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Freeman-Durden Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_GeneralizedFreemanDurden() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Generalized Freeman-Durden Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_Yamaguchi() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Yamaguchi Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_vanZyl() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("van Zyl Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_Cloude() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Cloude Decomposition");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_Touzi() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("Touzi Decomposition", "outputTouziParamSet0");
            }
        };
        b.run();
    }

    @Test
    public void testQP_decomposition_HAAlphaQuadPol() throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition("H-A-Alpha Quad Pol Decomposition", "outputHAAlpha");
            }
        };
        b.run();
    }

    private void decomposition(final String name) throws Exception {
        Benchmark b = new Benchmark() {
            @Override
            protected void execute() throws Exception {
                decomposition(name, null);
            }
        };
        b.run();
    }

    private void decomposition(final String name, final String param) throws IOException {
        final Product srcProduct = read(qpFile);

        PolarimetricDecompositionOp op = new PolarimetricDecompositionOp();
        op.setSourceProduct(srcProduct);
        op.setParameter("decomposition", name);
        if(param != null) {
            op.setParameter(param, true);
        }
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }
}
