package com.bc.ceres.jai.operator;

import org.junit.Test;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.RenderedImage;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReinterpretDescriptorTest {

    @Test
    public void testRescaleUShort() {
        final short[] sourceData = new short[]{
                0, 1, 2, 3, 4,
                9, 0, 1, 2, 3,
                8, 9, 0, 1, 2,
                7, 8, 9, 0, 1,
                6, 7, 8, 9, 0,
        };

        final RenderedImage sourceImage = SourceImageFactory.createOneBandedUShortImage(5, 5, sourceData);
        final RenderedImage targetImage = ReinterpretDescriptor.create(sourceImage, 17.0, 11.0, LINEAR, AWT, null);
        assertNotNull(targetImage);
        assertEquals(5, targetImage.getWidth());
        assertEquals(5, targetImage.getHeight());
        assertEquals(DataBuffer.TYPE_FLOAT, targetImage.getSampleModel().getDataType());

        final DataBufferFloat targetBuffer = ((DataBufferFloat) targetImage.getData().getDataBuffer());
        final float[] targetData = targetBuffer.getData();
        for (int i = 0; i < targetData.length; i++) {
            assertEquals("i = " + i, sourceData[i] * 17.0 + 11.0, targetData[i], 0.0);
        }
    }
}
