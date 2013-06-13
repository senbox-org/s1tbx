package org.esa.beam.jai;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
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
    public void testValueReplacing2() throws Exception {
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
        FillConstantOpImage nanImage = new FillConstantOpImage(sourceImage, validMaskImage, Double.NaN);
        double[] result = nanImage.getData().getPixels(0, 0, width, height, new double[width * height]);

        assertEquals(0.3, result[5], 1.0e-6);
        assertEquals(0.3, result[6], 1.0e-6);
        assertEquals(0.3, result[9], 1.0e-6);
        assertEquals(0.3, result[10], 1.0e-6);

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
}
