package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ReplaceValueOpImageTest {

    @Test
    public void testReplacingFloatValuesWithNaN() throws Exception {
        int width = 4;
        int height = 4;
        Product product = new Product("n", "t", width, height);
        Band band = product.addBand("b", ProductData.TYPE_FLOAT32);
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        float[] sourceData = new float[width * height];
        Arrays.fill(sourceData, 1.7f);
        sourceData[5] = 0.3f;
        sourceData[6] = 0.3f;
        sourceData[9] = 0.3f;
        sourceData[10] = 0.3f;
        sourceData[15] = 2.5f;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferFloat(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(1.7);
        band.setNoDataValueUsed(true);
        ReplaceValueOpImage nanImage = new ReplaceValueOpImage(sourceImage, 1.7, Double.NaN, DataBuffer.TYPE_DOUBLE);
        assertEquals(DataBuffer.TYPE_DOUBLE, nanImage.getSampleModel().getDataType());
        double[] result = nanImage.getData().getPixels(0, 0, width, height, new double[width * height]);

        assertEquals(0.3, result[5], 1.0e-6);
        assertEquals(0.3, result[6], 1.0e-6);
        assertEquals(0.3, result[9], 1.0e-6);
        assertEquals(0.3, result[10], 1.0e-6);
        assertEquals(2.5, result[15], 1.0e-6);

        assertEquals(Double.NaN, result[0], 1.0e-6);
        assertEquals(Double.NaN, result[1], 1.0e-6);
        assertEquals(Double.NaN, result[2], 1.0e-6);
        assertEquals(Double.NaN, result[3], 1.0e-6);
        assertEquals(Double.NaN, result[4], 1.0e-6);
        assertEquals(Double.NaN, result[7], 1.0e-6);
        assertEquals(Double.NaN, result[8], 1.0e-6);
        assertEquals(Double.NaN, result[11], 1.0e-6);
        assertEquals(Double.NaN, result[12], 1.0e-6);
        assertEquals(Double.NaN, result[13], 1.0e-6);
        assertEquals(Double.NaN, result[14], 1.0e-6);

    }

    @Test
    public void testReplacingIntegerValuesWithNan() throws Exception {
        int width = 4;
        int height = 4;
        Product product = new Product("n", "t", width, height);
        Band band = product.addBand("b", ProductData.TYPE_INT32);
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_INT, width, height, 1, width, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        int[] sourceData = new int[width * height];
        Arrays.fill(sourceData, -1);
        sourceData[5] = 0;
        sourceData[6] = 1;
        sourceData[9] = 1;
        sourceData[10] = 12;
        sourceData[15] = -3;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferInt(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(-1);
        band.setNoDataValueUsed(true);
        ReplaceValueOpImage nanImage = new ReplaceValueOpImage(sourceImage, -1, Double.NaN, DataBuffer.TYPE_DOUBLE);
        assertEquals(DataBuffer.TYPE_DOUBLE, nanImage.getSampleModel().getDataType());
        double[] result = nanImage.getData().getPixels(0, 0, width, height, new double[width * height]);

        assertEquals(0, result[5], 1.0e-6);
        assertEquals(1, result[6], 1.0e-6);
        assertEquals(1, result[9], 1.0e-6);
        assertEquals(12, result[10], 1.0e-6);
        assertEquals(-3, result[15], 1.0e-6);

        assertEquals(Double.NaN, result[0], 1.0e-6);
        assertEquals(Double.NaN, result[1], 1.0e-6);
        assertEquals(Double.NaN, result[2], 1.0e-6);
        assertEquals(Double.NaN, result[3], 1.0e-6);
        assertEquals(Double.NaN, result[4], 1.0e-6);
        assertEquals(Double.NaN, result[7], 1.0e-6);
        assertEquals(Double.NaN, result[8], 1.0e-6);
        assertEquals(Double.NaN, result[11], 1.0e-6);
        assertEquals(Double.NaN, result[12], 1.0e-6);
        assertEquals(Double.NaN, result[13], 1.0e-6);
        assertEquals(Double.NaN, result[14], 1.0e-6);

    }

    @Test
    public void testReplacingShortValuesWithFloatNaN() throws Exception {
        int width = 4;
        int height = 4;
        Product product = new Product("n", "t", width, height);
        Band band = product.addBand("b", ProductData.TYPE_INT16);
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_SHORT, width, height, 1, width, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        short[] sourceData = new short[width * height];
        Arrays.fill(sourceData, (short) -1);
        sourceData[5] = 0;
        sourceData[6] = 1;
        sourceData[9] = 1;
        sourceData[10] = 12;
        sourceData[15] = -3;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferShort(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(-1);
        band.setNoDataValueUsed(true);
        ReplaceValueOpImage nanImage = new ReplaceValueOpImage(sourceImage, -1, Float.NaN, DataBuffer.TYPE_FLOAT);
        assertEquals(DataBuffer.TYPE_FLOAT, nanImage.getSampleModel().getDataType());
        float[] result = nanImage.getData().getPixels(0, 0, width, height, new float[width * height]);

        assertEquals(0, result[5], 1.0e-6);
        assertEquals(1, result[6], 1.0e-6);
        assertEquals(1, result[9], 1.0e-6);
        assertEquals(12, result[10], 1.0e-6);
        assertEquals(-3, result[15], 1.0e-6);

        assertEquals(Float.NaN, result[0], 1.0e-6);
        assertEquals(Float.NaN, result[1], 1.0e-6);
        assertEquals(Float.NaN, result[2], 1.0e-6);
        assertEquals(Float.NaN, result[3], 1.0e-6);
        assertEquals(Float.NaN, result[4], 1.0e-6);
        assertEquals(Float.NaN, result[7], 1.0e-6);
        assertEquals(Float.NaN, result[8], 1.0e-6);
        assertEquals(Float.NaN, result[11], 1.0e-6);
        assertEquals(Float.NaN, result[12], 1.0e-6);
        assertEquals(Float.NaN, result[13], 1.0e-6);
        assertEquals(Float.NaN, result[14], 1.0e-6);

    }

    @Test
    public void testReplacingShortValuesWithShort() throws Exception {
        int width = 4;
        int height = 4;
        Product product = new Product("n", "t", width, height);
        Band band = product.addBand("b", ProductData.TYPE_INT16);
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_SHORT, width, height, 1, width, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        short[] sourceData = new short[width * height];
        Arrays.fill(sourceData, (short) -1);
        sourceData[5] = 0;
        sourceData[6] = 1;
        sourceData[9] = 1;
        sourceData[10] = 12;
        sourceData[15] = -3;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferShort(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(-1);
        band.setNoDataValueUsed(true);
        ReplaceValueOpImage nanImage = new ReplaceValueOpImage(sourceImage, (short)-1, (short)-99, DataBuffer.TYPE_SHORT);
        assertEquals(DataBuffer.TYPE_SHORT, nanImage.getSampleModel().getDataType());
        float[] result = nanImage.getData().getPixels(0, 0, width, height, new float[width * height]);

        assertEquals(0, result[5], 1.0e-6);
        assertEquals(1, result[6], 1.0e-6);
        assertEquals(1, result[9], 1.0e-6);
        assertEquals(12, result[10], 1.0e-6);
        assertEquals(-3, result[15], 1.0e-6);

        assertEquals(-99, result[0], 1.0e-6);
        assertEquals(-99, result[1], 1.0e-6);
        assertEquals(-99, result[2], 1.0e-6);
        assertEquals(-99, result[3], 1.0e-6);
        assertEquals(-99, result[4], 1.0e-6);
        assertEquals(-99, result[7], 1.0e-6);
        assertEquals(-99, result[8], 1.0e-6);
        assertEquals(-99, result[11], 1.0e-6);
        assertEquals(-99, result[12], 1.0e-6);
        assertEquals(-99, result[13], 1.0e-6);
        assertEquals(-99, result[14], 1.0e-6);

    }


}
