package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class InterpolatedOpImageTest {

    private Band referenceBand;

    @Before
    public void setUp() {
        int referenceWidth = 3;
        int referenceHeight = 3;
        referenceBand = new Band("referenceBand", ProductData.TYPE_INT8, 3, 3);
        final AffineTransform imageToModelTransform = new AffineTransform(2, 0, 0, 2, 2, 2);
        final DefaultMultiLevelModel referenceModel = new DefaultMultiLevelModel(imageToModelTransform, 3, 3);
        referenceBand.setSourceImage(new DefaultMultiLevelImage(new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new BufferedImage(referenceWidth / (1 + level), referenceHeight / (1 + level), ProductData.TYPE_INT8);
            }
        }));
        final Product product = new Product("product", "type", 9, 9);
        product.addBand(referenceBand);
    }

    @Test
    public void testInterpolate_Double_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testInterpolate_Float_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testInterpolate_Byte_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT8);
    }

    @Test
    public void testInterpolate_Short_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT16);
    }

    @Test
    public void testInterpolate_UShort_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_UINT16);
    }

    @Test
    public void testInterpolate_Int_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT32);
    }

    @Test
    public void testInterpolate_Double_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testInterpolate_Float_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testInterpolate_Byte_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT8);
    }

    @Test
    public void testInterpolate_Short_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT16);
    }

    @Test
    public void testInterpolate_UShort_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_UINT16);
    }

    @Test
    public void testInterpolate_Int_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT32);
    }

    @Test
    public void testBilinear_FirstAndLastPixelValid() throws NoninvertibleTransformException {
        String expression = "(X%2 == 0.5) ^ (Y%2 == 0.5) ? 123 :(X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.11538457, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.88461542, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_MiddlePixelsValid() throws NoninvertibleTransformException {
        String expression = "(X%2 == 0.5) ^ (Y%2 == 0.5) ? (X + 0.5) + ((Y + 0.5) * 2) : 123";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(4.5, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.038461685180664, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.961538314819336, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(123.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_FirstPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 0.5) && (Y == 0.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(4.541666507720947, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.375, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.333333492279053, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(5.041666507720947, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.541666507720947, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(5.66666666, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.16666666, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.83333333, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_SecondPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 1.5) && (Y == 0.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.569444417953491, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.513888835906982, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.847222328186035, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.56944465637207, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.16666666, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.83333333, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_ThirdPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 0.5) && (Y == 1.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.430555582046509, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.152777671813965, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.333333492279053, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.486111164093018, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.430555820465088, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(5.666666507720947, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_FourthPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 1.5) && (Y == 1.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(ProductData.TYPE_FLOAT32, expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.4583332538604736, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(3.9583332538604736, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.625, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(4.458333492279053, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(123.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    @Ignore
    public void testInterpolate_Double_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_FLOAT64);
    }

    @Test
    @Ignore
    public void testInterpolate_Float_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_FLOAT32);
    }

    @Test
    @Ignore
    public void testInterpolate_Byte_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT8);
    }

    @Test
    @Ignore
    public void testInterpolate_Short_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT16);
    }

    @Test
    @Ignore
    public void testInterpolate_UShort_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_UINT16);
    }

    @Test
    @Ignore
    public void testInterpolate_Int_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT32);
    }

    private void testCubicConvolution(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Cubic_Convolution),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(1, 1, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0), 1e-8);
    }

    private void testBilinear(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        if (!(dataType == ProductData.TYPE_FLOAT32) && !(dataType == ProductData.TYPE_FLOAT64)) {
            assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(2, 2, 0), 1e-6);
        } else {
            assertEquals(3.5, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.16666666, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(4.33333333, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.83333333, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.5, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(5.66666666, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(5.16666666, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(5.83333333, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
        }
    }

    private void testNearestNeighbour(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);

        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Nearest),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(1, 1, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0), 1e-8);
    }

    private Band createSourceBand(int dataType) {
        return createSourceBand(dataType, "(X + 0.5) + ((Y + 0.5) * 2)");
    }

    private Band createSourceBand(int dataType, String expression) {
        int sourceWidth = 2;
        int sourceHeight = 2;
        int sourceScaleX = 3;
        int sourceScaleY = 3;
        int sourceTranslateX = 1;
        int sourceTranslateY = 1;
        final AffineTransform imageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Product sourceProduct = new Product("dummy", "dummy", sourceWidth, sourceHeight);
        final Band sourceBand = sourceProduct.addBand("sourceBand", expression, dataType);
        sourceBand.setNoDataValue(123);
        sourceBand.setImageToModelTransform(imageToModelTransform);
        return sourceBand;
    }

}