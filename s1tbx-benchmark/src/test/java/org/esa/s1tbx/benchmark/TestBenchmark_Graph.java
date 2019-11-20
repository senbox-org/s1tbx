package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

public class TestBenchmark_Graph extends TestBenchmarks {

    @Test
    public void testGRD_read() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        writeGPF(srcProduct);
    }

    @Test
    public void testGRD_calibrate() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        CalibrationOp op = new CalibrationOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();

    }
}
