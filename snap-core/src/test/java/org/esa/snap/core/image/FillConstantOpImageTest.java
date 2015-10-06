package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class FillConstantOpImageTest {

    @Test
    public void testFillingFloatImageWithNaN() throws Exception {
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
        band.setValidPixelExpression("b < 2.0");
        MultiLevelImage validMaskImage = band.getValidMaskImage();
        FillConstantOpImage nanImage = new FillConstantOpImage(sourceImage, validMaskImage, Float.NaN);
        assertEquals(DataBuffer.TYPE_FLOAT, nanImage.getSampleModel().getDataType());
        float[] result = nanImage.getData().getPixels(0, 0, width, height, new float[width * height]);

        assertEquals(0.3f, result[5], 1.0e-6);
        assertEquals(0.3f, result[6], 1.0e-6);
        assertEquals(0.3f, result[9], 1.0e-6);
        assertEquals(0.3f, result[10], 1.0e-6);

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

        assertEquals(Float.NaN, result[15], 1.0e-6);
    }

    @Test
    public void testFillingIntegerImageWithNaN() throws Exception {
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
        sourceData[10] = 0;
        sourceData[15] = -3;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferInt(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(-1);
        band.setNoDataValueUsed(true);
        band.setValidPixelExpression("b != -3");
        MultiLevelImage validMaskImage = band.getValidMaskImage();
        FillConstantOpImage nanImage = new FillConstantOpImage(sourceImage, validMaskImage, Double.NaN);
        assertEquals(DataBuffer.TYPE_DOUBLE, nanImage.getSampleModel().getDataType());
        double[] result = nanImage.getData().getPixels(0, 0, width, height, new double[width * height]);

        assertEquals(0, result[5], 1.0e-6);
        assertEquals(1, result[6], 1.0e-6);
        assertEquals(1, result[9], 1.0e-6);
        assertEquals(0, result[10], 1.0e-6);

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
        assertEquals(Double.NaN, result[15], 1.0e-6);

    }

    @Test
    public void testFillingByteImageWithInteger() throws Exception {
        int width = 4;
        int height = 4;
        Product product = new Product("n", "t", width, height);
        Band band = product.addBand("b", ProductData.TYPE_INT8);
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        byte[] sourceData = new byte[width * height];
        Arrays.fill(sourceData, (byte) -1);
        sourceData[5] = 0;
        sourceData[6] = 1;
        sourceData[9] = 1;
        sourceData[10] = 0;
        sourceData[15] = -3;
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferByte(sourceData, width * height), null));
        band.setSourceImage(sourceImage);

        band.setNoDataValue(-1);
        band.setNoDataValueUsed(true);
        band.setValidPixelExpression("b != -3");
        MultiLevelImage validMaskImage = band.getValidMaskImage();
        FillConstantOpImage nanImage = new FillConstantOpImage(sourceImage, validMaskImage, 999);
        assertEquals(DataBuffer.TYPE_INT, nanImage.getSampleModel().getDataType());
        double[] result = nanImage.getData().getPixels(0, 0, width, height, new double[width * height]);

        assertEquals(0, result[5], 1.0e-6);
        assertEquals(1, result[6], 1.0e-6);
        assertEquals(1, result[9], 1.0e-6);
        assertEquals(0, result[10], 1.0e-6);

        assertEquals(999, result[0], 1.0e-6);
        assertEquals(999, result[1], 1.0e-6);
        assertEquals(999, result[2], 1.0e-6);
        assertEquals(999, result[3], 1.0e-6);
        assertEquals(999, result[4], 1.0e-6);
        assertEquals(999, result[7], 1.0e-6);
        assertEquals(999, result[8], 1.0e-6);
        assertEquals(999, result[11], 1.0e-6);
        assertEquals(999, result[12], 1.0e-6);
        assertEquals(999, result[13], 1.0e-6);
        assertEquals(999, result[14], 1.0e-6);
        assertEquals(999, result[15], 1.0e-6);

    }
}
