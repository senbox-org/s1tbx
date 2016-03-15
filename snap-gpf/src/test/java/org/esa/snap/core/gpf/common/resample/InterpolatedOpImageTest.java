package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.junit.Before;
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

    private void testBilinear(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  InterpolationType.Bilinear,
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        final Raster sourceData = sourceBand.getSourceImage().getData();
        if (!(dataType == ProductData.TYPE_FLOAT32) && !(dataType == ProductData.TYPE_FLOAT64)) {
            assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(2, 2, 0), 1e-6);
        } else {
            double a = 1 / 6.0;
            double b = 1 - a;
            assertEquals(
                    (b * b * sourceData.getSampleDouble(0, 0, 0)) +
                            (b * a * sourceData.getSampleDouble(1, 0, 0)) +
                            (a * b * sourceData.getSampleDouble(0, 1, 0)) +
                            (a * a * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(
                    (b * a * sourceData.getSampleDouble(0, 0, 0)) +
                            (b * b * sourceData.getSampleDouble(1, 0, 0)) +
                            (a * a * sourceData.getSampleDouble(0, 1, 0)) +
                            (a * b * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(
                    (b * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(
                    (a * b * sourceData.getSampleDouble(0, 0, 0)) +
                            (a * a * sourceData.getSampleDouble(1, 0, 0)) +
                            (b * b * sourceData.getSampleDouble(0, 1, 0)) +
                            (b * a * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(
                    (a * a * sourceData.getSampleDouble(0, 0, 0)) +
                            (a * b * sourceData.getSampleDouble(1, 0, 0)) +
                            (b * a * sourceData.getSampleDouble(0, 1, 0)) +
                            (b * b * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(
                    (a * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(1, 0, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(1, 1, 0)), targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(
                    (b * 0.5 * sourceData.getSampleDouble(0, 1, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(1, 1, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(0, 1, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(0, 1, 0)), targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(
                    (a * 0.5 * sourceData.getSampleDouble(0, 1, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(1, 1, 0)) +
                            (a * 0.5 * sourceData.getSampleDouble(0, 1, 0)) +
                            (b * 0.5 * sourceData.getSampleDouble(0, 1, 0)), targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(sourceData.getSampleDouble(1, 1, 0), targetData.getSampleDouble(2, 2, 0));
        }
    }

    private void testNearestNeighbour(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);

        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  InterpolationType.Nearest,
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
        int sourceWidth = 2;
        int sourceHeight = 2;
        int sourceScaleX = 3;
        int sourceScaleY = 3;
        int sourceTranslateX = 1;
        int sourceTranslateY = 1;
        final AffineTransform imageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Product sourceProduct = new Product("dummy", "dummy", sourceWidth, sourceHeight);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "(X + 0.5) + ((Y + 0.5) * 2)", dataType);
        sourceBand.setNoDataValue(-23);
        sourceBand.setImageToModelTransform(imageToModelTransform);
        return sourceBand;
    }

}