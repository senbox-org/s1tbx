package com.bc.ceres.jai.operator;

import junit.framework.TestCase;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBuffer;

import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import com.bc.ceres.jai.GeneralFilterFunction;


public class GeneralFilterDescriptorTest extends TestCase {

    public void testMax() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 4,
                9, 0, 1, 2, 3,
                8, 9, 0, 1, 2,
                7, 8, 9, 0, 1,
                6, 7, 8, 9, 0,
        };
        short[] expectedData = new short[]{
                9, 9, 3, 4, 4,
                9, 9, 9, 4, 4,
                9, 9, 9, 9, 3,
                9, 9, 9, 9, 9,
                8, 9, 9, 9, 9,
        };

        BufferedImage image = SourceImageFactory.createOneBandedUShortImage(5, 5, sourceData);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        RenderedOp op = GeneralFilterDescriptor.create(image, GeneralFilterFunction.MAX_3X3, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
        assertNotNull(op);
        assertEquals(5, op.getWidth());
        assertEquals(5, op.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, op.getSampleModel().getDataType());
               
        DataBufferUShort destBuffer = ((DataBufferUShort) op.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

}