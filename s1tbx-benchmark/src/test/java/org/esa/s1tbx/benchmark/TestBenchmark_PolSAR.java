package org.esa.s1tbx.benchmark;

import org.csa.rstb.polarimetric.gpf.PolarimetricDecompositionOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark_PolSAR extends TestBenchmarks {

    @Test
    public void testQP_decomposition_pauli() throws Exception {
        decomposition("Pauli Decomposition");
    }

    @Test
    public void testQP_decomposition_sinclair() throws Exception {
        decomposition("Sinclair Decomposition");
    }

    @Test
    public void testQP_decomposition_FreemanDurden() throws Exception {
        decomposition("Freeman-Durden Decomposition");
    }

    @Test
    public void testQP_decomposition_GeneralizedFreemanDurden() throws Exception {
        decomposition("Generalized Freeman-Durden Decomposition");
    }

    @Test
    public void testQP_decomposition_Yamaguchi() throws Exception {
        decomposition("Yamaguchi Decomposition");
    }

    @Test
    public void testQP_decomposition_vanZyl() throws Exception {
        decomposition("van Zyl Decomposition");
    }

    @Test
    public void testQP_decomposition_Cloude() throws Exception {
        decomposition("Cloude Decomposition");
    }

    @Test
    public void testQP_decomposition_Touzi() throws Exception {
        decomposition("Touzi Decomposition", "outputTouziParamSet0");
    }

    @Test
    public void testQP_decomposition_HAAlphaQuadPol() throws Exception {
        decomposition("H-A-Alpha Quad Pol Decomposition", "outputHAAlpha");
    }

    private void decomposition(final String name) throws IOException {
        decomposition(name, null);
    }

    private void decomposition(final String name, final String param) throws IOException {
        final Product srcProduct = subset(qpFile, rect);

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
