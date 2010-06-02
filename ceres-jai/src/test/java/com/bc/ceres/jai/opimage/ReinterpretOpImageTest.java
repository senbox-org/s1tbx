package com.bc.ceres.jai.opimage;

import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReinterpretOpImageTest {

    private static final int W = 1;
    private static final int H = 1;

    @Test
    public void testRescaleByte() {
        final RenderedImage source = createSourceImage(new Byte[]{-1});
        final RenderedImage target = ReinterpretOpImage.create(source, 11.0, 1.0, LINEAR, AWT, null);
        assertTrue(DataBuffer.TYPE_FLOAT == target.getSampleModel().getDataType());

        final Raster targetData = target.getData();
        assertEquals(2806.0, targetData.getSampleFloat(0, 0, 0), 0.0);
    }

    @Test
    public void testRescaleSByte() {
        final RenderedImage source = createSourceImage(new Byte[]{-1});
        final RenderedImage target = ReinterpretOpImage.create(source, 11.0, 1.0, LINEAR, INTERPRET_BYTE_SIGNED, null);
        assertTrue(DataBuffer.TYPE_FLOAT == target.getSampleModel().getDataType());

        final Raster targetData = target.getData();
        assertEquals(-10.0, targetData.getSampleFloat(0, 0, 0), 0.0);
    }

    @Test
    public void testRescaleInt() {
        final RenderedImage source = createSourceImage(new Integer[]{-1});
        final RenderedImage target = ReinterpretOpImage.create(source, 11.0, 1.0, LINEAR, AWT, null);
        assertTrue(DataBuffer.TYPE_DOUBLE == target.getSampleModel().getDataType());

        final Raster targetData = target.getData();
        assertEquals(-10.0, targetData.getSampleDouble(0, 0, 0), 0.0);
    }

    @Test
    public void testRescaleUInt() {
        final RenderedImage source = createSourceImage(new Integer[]{-1});
        final RenderedImage target = ReinterpretOpImage.create(source, 11.0, 1.0, LINEAR, INTERPRET_INT_UNSIGNED, null);
        assertTrue(DataBuffer.TYPE_DOUBLE == target.getSampleModel().getDataType());

        final Raster targetData = target.getData();
        assertEquals(4.7244640246E10, targetData.getSampleDouble(0, 0, 0), 0.0);
    }

    static RenderedImage createSourceImage(Number[] v) {
        return ConstantDescriptor.create((float) W, (float) H, v, null);
    }
}
