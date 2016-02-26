package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import org.junit.Test;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;

import static org.junit.Assert.*;

/**
 * @author Tonio Fincke
 */
public class ResampleTest_FlagFilterFunctions {

    @Test
    public void testFlagMinFilterFunction() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 3,
                9, 5, 1, 2, 3,
                9, 9, 6, 3, 2,
                7, 7, 5, 7, 1,
                6, 7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                0, 0, 0, 0, 2,
                0, 0, 0, 0, 2,
                1, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                6, 4, 1, 1, 1,
        };

        BufferedImage image = createOneBandedUShortImage(5, 5, sourceData);
        final GeneralFilterFunction function = new Resample.FlagMinFunction(3, 3, 1, 1, null);
        final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{Double.NaN});

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender);
        final RenderedOp formattedOp = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE,
                                                               hints);
        RenderedOp op = GeneralFilterDescriptor.create(formattedOp, function, hints);
        op = FormatDescriptor.create(op, DataBuffer.TYPE_USHORT, null);

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

    @Test
    public void testFlagMaxFilterFunction() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 3,
                9, 5, 1, 2, 3,
                9, 9, 6, 3, 2,
                7, 7, 5, 7, 1,
                6, 7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                13, 15, 7, 3, 3,
                13, 15, 15, 7, 3,
                15, 15, 15, 7, 7,
                15, 15, 15, 15, 15,
                7, 7, 15, 15, 15,
        };

        BufferedImage image = createOneBandedUShortImage(5, 5, sourceData);
        final GeneralFilterFunction function = new Resample.FlagMaxFunction(3, 3, 1, 1, null);
        final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{Double.NaN});

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender);
        final RenderedOp formattedOp = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE,
                                                               hints);
        RenderedOp op = GeneralFilterDescriptor.create(formattedOp, function, hints);
        op = FormatDescriptor.create(op, DataBuffer.TYPE_USHORT, null);

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

    @Test
    public void testFlagMedianMinFilterFunction() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 3,
                9, 5, 1, 2, 3,
                9, 9, 6, 3, 2,
                7, 7, 5, 7, 1,
                6, 7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                1, 1, 1, 3, 3,
                1, 1, 3, 3, 3,
                1, 5, 7, 3, 3,
                7, 7, 7, 5, 1,
                7, 7, 5, 5, 1,
        };

        BufferedImage image = createOneBandedUShortImage(5, 5, sourceData);
        final GeneralFilterFunction function = new Resample.FlagMedianMinFunction(3, 3, 1, 1, null);
        final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{Double.NaN});

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender);
        final RenderedOp formattedOp = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE,
                                                               hints);
        RenderedOp op = GeneralFilterDescriptor.create(formattedOp, function, hints);
        op = FormatDescriptor.create(op, DataBuffer.TYPE_USHORT, null);

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

    @Test
    public void testFlagMedianMaxFilterFunction() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 3,
                9, 5, 1, 2, 3,
                9, 9, 6, 3, 2,
                7, 7, 5, 7, 1,
                6, 7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                1, 1, 3, 3, 3,
                9, 1, 3, 3, 3,
                13, 5, 7, 3, 3,
                7, 7, 7, 5, 3,
                7, 7, 7, 5, 5,
        };

        BufferedImage image = createOneBandedUShortImage(5, 5, sourceData);
        final GeneralFilterFunction function = new Resample.FlagMedianMaxFunction(3, 3, 1, 1, null);
        final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{Double.NaN});

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender);
        final RenderedOp formattedOp = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE,
                                                               hints);
        RenderedOp op = GeneralFilterDescriptor.create(formattedOp, function, hints);
        op = FormatDescriptor.create(op, DataBuffer.TYPE_USHORT, null);

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

    static BufferedImage createOneBandedUShortImage(int w, int h, short[] data) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, w * h);
        return image;
    }

}
