package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;

/**
 * @author Tonio Fincke
 */
public class DoubleDataInterpolatorTest {

    private DoubleDataInterpolator.CubicConvolution cubicConvolutionInterpolator;
    private int srcOffset;
    private int srcScanlineStride;
    private Raster vbData;

    @Before
    public void setup() {
        cubicConvolutionInterpolator = new DoubleDataInterpolator.CubicConvolution();
        final Product product = new Product("product", "type", 4, 4);
        final VirtualBand vb = new VirtualBand("vb", ProductData.TYPE_FLOAT64, 4, 4, "((Y - 0.5) * 4) + (X - 0.5)");
        product.addBand(vb);
        final RasterFormatTag[] compatibleTags =
                RasterAccessor.findCompatibleTags(new RenderedImage[]{vb.getSourceImage()}, vb.getSourceImage());
        vbData = vb.getSourceImage().getData();
        final RasterAccessor accessor = new RasterAccessor(vbData, new Rectangle(4, 4), compatibleTags[0], null);
        cubicConvolutionInterpolator.init(vb, accessor, accessor, Double.NaN);
        srcScanlineStride = accessor.getScanlineStride();
        srcOffset = accessor.getBandOffset(0);
    }

    @Test
    public void testCubicConvolution_GetValidRectangle_1_1() {
        int srcIndexY = srcOffset + srcScanlineStride;
        final double[][] validRectangle = cubicConvolutionInterpolator.getValidRectangle(1, 1, srcIndexY, 4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(vbData.getSampleDouble(i, j, 0), validRectangle[i][j], 1e-8);
            }
        }
    }

    @Test
    public void testCubicConvolution_GetValidRectangle_0_0() {
        int srcIndexY = srcOffset;
        final double[][] validRectangle = cubicConvolutionInterpolator.getValidRectangle(0, 0, srcIndexY, 4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(vbData.getSampleDouble(Math.max(0, i - 1), Math.max(0, j - 1), 0), validRectangle[i][j], 1e-8);
            }
        }
    }

    @Test
    public void testCubicConvolution_GetValidRectangle_3_0() {
        int srcIndexY = srcOffset;
        final double[][] validRectangle = cubicConvolutionInterpolator.getValidRectangle(3, 0, srcIndexY, 4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(vbData.getSampleDouble(Math.min(3, i + 2), Math.max(0, j - 1), 0), validRectangle[i][j], 1e-8);
            }
        }
    }

    @Test
    public void testCubicConvolution_GetValidRectangle_0_3() {
        int srcIndexY = srcOffset + (3 * srcScanlineStride);
        final double[][] validRectangle = cubicConvolutionInterpolator.getValidRectangle(0, 3, srcIndexY, 4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(vbData.getSampleDouble(Math.max(0, i - 1), Math.min(3, j + 2), 0), validRectangle[i][j], 1e-8);
            }
        }
    }

    @Test
    public void testCubicConvolution_GetValidRectangle_3_3() {
        int srcIndexY = srcOffset + (3 * srcScanlineStride);
        final double[][] validRectangle = cubicConvolutionInterpolator.getValidRectangle(3, 3, srcIndexY, 4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(vbData.getSampleDouble(Math.min(3, i + 2), Math.min(3, j + 2), 0), validRectangle[i][j], 1e-8);
            }
        }
    }

}