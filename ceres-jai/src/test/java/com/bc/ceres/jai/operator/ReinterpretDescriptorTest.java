package com.bc.ceres.jai.operator;

import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.RenderedImage;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static org.junit.Assert.*;

public class ReinterpretDescriptorTest {

    private RenderedOp sourceImage;

    @Test
    public void testLinearRescaleUShort() {
        final RenderedImage targetImage = ReinterpretDescriptor.create(sourceImage, 17.0, 11.0, LINEAR, AWT, null);
        assertNotNull(targetImage);
        assertEquals(5, targetImage.getWidth());
        assertEquals(5, targetImage.getHeight());
        assertEquals(DataBuffer.TYPE_FLOAT, targetImage.getSampleModel().getDataType());

        final float[] targetData = ((DataBufferFloat) targetImage.getData().getDataBuffer()).getData();
        for (int i = 0; i < targetData.length; i++) {
            assertEquals("i = " + i, 130.0, targetData[i], 0.0);
        }
    }

    @Test
    public void testTargetImageRenderingIsSameAsSourceImageRendering() {
        final RenderedOp targetImage = ReinterpretDescriptor.create(sourceImage, 1.0, 0.0, LINEAR, AWT, null);
        assertSame(sourceImage.getRendering(), targetImage.getRendering());
    }

    @Before
    public void setup() {
        sourceImage = ConstantDescriptor.create(5f, 5f, new Short[]{7}, null);
    }
}
