package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.s1tbx.sar.gpf.geometric.EllipsoidCorrectionRDOp;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.s1tbx.sar.gpf.geometric.TerrainFlatteningOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.raster.gpf.texture.GLCMOp;
import org.junit.Ignore;
import org.junit.Test;

public class TestBenchmark_SAR extends TestBenchmarks {

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

        writeGPF(trgProduct);
    }

    @Test
    public void testGRD_multilook() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        MultilookOp op = new MultilookOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }

    @Test
    public void testGRD_terraincorrect() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        RangeDopplerGeocodingOp op = new RangeDopplerGeocodingOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }

    @Test
    public void testGRD_ellipsoidcorrect() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        EllipsoidCorrectionRDOp op = new EllipsoidCorrectionRDOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }

    @Test
    public void testGRD_terrainflatten() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        CalibrationOp cal = new CalibrationOp();
        cal.setSourceProduct(srcProduct);
        cal.setParameter("outputBetaBand", true);
        Product calProduct = cal.getTargetProduct();

        TerrainFlatteningOp op = new TerrainFlatteningOp();
        op.setSourceProduct(calProduct);
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }

    @Test
    @Ignore
    public void testGRD_glcm() throws Exception {
        final Product srcProduct = subset(grdFile, rect);

        GLCMOp op = new GLCMOp();
        op.setSourceProduct(srcProduct);
        op.setParameter("quantizationLevelsStr", "16");
        Product trgProduct = op.getTargetProduct();

        writeGPF(trgProduct);
    }
}
