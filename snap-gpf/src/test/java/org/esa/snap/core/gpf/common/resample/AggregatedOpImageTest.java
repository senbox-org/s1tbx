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
public class AggregatedOpImageTest {

    private Band referenceBand;

    @Before
    public void setUp() {
        int referenceWidth = 2;
        int referenceHeight = 2;
        int referenceScaleX = 4;
        int referenceScaleY = 4;
        int referenceTranslateX = 4;
        int referenceTranslateY = 4;
        int referenceLevelCount = 2;
        final AffineTransform imageToModelTransform = new AffineTransform(referenceScaleX, 0, 0, referenceScaleY,
                                                                          referenceTranslateX, referenceTranslateY);
        referenceBand = new Band("referenceBand", ProductData.TYPE_INT8, referenceWidth, referenceHeight);
        final DefaultMultiLevelModel referenceModel = new DefaultMultiLevelModel(referenceLevelCount, imageToModelTransform,
                                                                                 referenceWidth, referenceHeight);
        referenceBand.setSourceImage(new DefaultMultiLevelImage(new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new BufferedImage(referenceWidth / (1 + level), referenceHeight / (1 + level), ProductData.TYPE_INT8);
            }
        }));
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Double_Mean() throws Exception {
        test_mean(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Double_Median() throws Exception {
        test_median(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Double_Min() throws Exception {
        test_min(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Double_Max() throws Exception {
        test_max(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Double_First() throws Exception {
        test_first(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Float_Mean() throws Exception {
        test_mean(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Float_Median() throws Exception {
        test_median(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Float_Min() throws Exception {
        test_min(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Float_Max() throws Exception {
        test_max(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Float_First() throws Exception {
        test_first(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Mean_Byte() throws Exception {
        test_mean(ProductData.TYPE_INT8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Byte_Median() throws Exception {
        test_median(ProductData.TYPE_INT8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Byte_Min() throws Exception {
        test_min(ProductData.TYPE_INT8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Byte_Max() throws Exception {
        test_max(ProductData.TYPE_INT8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Byte_First() throws Exception {
        test_first(ProductData.TYPE_INT8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Mean_Short() throws Exception {
        test_mean(ProductData.TYPE_INT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Short_Median() throws Exception {
        test_median(ProductData.TYPE_INT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Short_Min() throws Exception {
        test_min(ProductData.TYPE_INT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Short_Max() throws Exception {
        test_max(ProductData.TYPE_INT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Short_First() throws Exception {
        test_first(ProductData.TYPE_INT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Int_Mean() throws Exception {
        test_mean(ProductData.TYPE_INT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Int_Median() throws Exception {
        test_median(ProductData.TYPE_INT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Int_Min() throws Exception {
        test_min(ProductData.TYPE_INT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Int_Max() throws Exception {
        test_max(ProductData.TYPE_INT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Int_First() throws Exception {
        test_first(ProductData.TYPE_INT32);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_UShort_Mean() throws Exception {
        test_mean(ProductData.TYPE_UINT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_UShort_Median() throws Exception {
        test_median(ProductData.TYPE_UINT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_UShort_Min() throws Exception {
        test_min(ProductData.TYPE_UINT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_UShort_Max() throws Exception {
        test_max(ProductData.TYPE_UINT16);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_UShort_First() throws Exception {
        test_first(ProductData.TYPE_UINT16);
    }

    private void test_mean(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = ImageManager.getDataBufferType(dataType);
        final ImageLayout imageLayout = createImageLayout(sourceBand);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                              sourceBand.getNoDataValue(),
                                                              AggregationType.Mean, dataBufferType,
                                                              sourceBand.getImageToModelTransform(),
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster imageData = image.getData();
        if (dataType != ProductData.TYPE_FLOAT32 && dataType != ProductData.TYPE_FLOAT64) {
            assertEquals(7.0, imageData.getSample(0, 0, 0), 1e-8);
            assertEquals(9.0, imageData.getSample(0, 1, 0), 1e-8);
            assertEquals(13.0, imageData.getSample(1, 0, 0), 1e-8);
            assertEquals(15.0, imageData.getSample(1, 1, 0), 1e-8);
        } else {
            assertEquals(7.5, imageData.getSampleDouble(0, 0, 0), 1e-8);
            assertEquals(9.0, imageData.getSampleDouble(0, 1, 0), 1e-8);
            assertEquals(13.5, imageData.getSampleDouble(1, 0, 0), 1e-8);
            assertEquals(15.0, imageData.getSampleDouble(1, 1, 0), 1e-8);
        }
    }

    private void test_median(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand_Median(dataType);
        final int dataBufferType = ImageManager.getDataBufferType(dataType);
        final ImageLayout imageLayout = createImageLayout(sourceBand);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                              sourceBand.getNoDataValue(), AggregationType.Median,
                                                              dataBufferType,
                                                              sourceBand.getImageToModelTransform(),
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster imageData = image.getData();
        assertEquals(6.0, imageData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(10.0, imageData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(10.0, imageData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(16.0, imageData.getSampleDouble(1, 1, 0), 1e-8);
    }

    private void test_min(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = ImageManager.getDataBufferType(dataType);
        final ImageLayout imageLayout = createImageLayout(sourceBand);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                              sourceBand.getNoDataValue(), AggregationType.Min,
                                                              dataBufferType,
                                                              sourceBand.getImageToModelTransform(),
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster imageData = image.getData();
        assertEquals(5.0, imageData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(7.0, imageData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(13.0, imageData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData.getSampleDouble(1, 1, 0), 1e-8);
    }

    private void test_max(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = ImageManager.getDataBufferType(dataType);
        final ImageLayout imageLayout = createImageLayout(sourceBand);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                              sourceBand.getNoDataValue(), AggregationType.Max,
                                                              dataBufferType,
                                                              sourceBand.getImageToModelTransform(),
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster imageData = image.getData();
        assertEquals(10.0, imageData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(11.0, imageData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(14.0, imageData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData.getSampleDouble(1, 1, 0), 1e-8);
    }

    private void test_first(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = ImageManager.getDataBufferType(dataType);
        final ImageLayout imageLayout = createImageLayout(sourceBand);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand.getSourceImage(), imageLayout,
                                                              sourceBand.getNoDataValue(), AggregationType.First,
                                                              dataBufferType,
                                                              sourceBand.getImageToModelTransform(),
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster imageData = image.getData();
        assertEquals(5.0, imageData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(7.0, imageData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(13.0, imageData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData.getSampleDouble(1, 1, 0), 1e-8);
    }

    private ImageLayout createImageLayout(Band sourceBand) {
        final int dataType = sourceBand.getSourceImage().getSampleModel().getDataType();
        return ImageManager.createSingleBandedImageLayout(dataType, referenceBand.getRasterWidth(),
                                                          referenceBand.getRasterHeight(),
                                                          referenceBand.getRasterWidth(),
                                                          referenceBand.getRasterHeight());
    }

    private Band createSourceBand(int dataType) {
        final Product sourceProduct = new Product("dummy", "dummy", 4, 4);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "((X - 0.5) * 4) + (Y - 0.5)", dataType);
        sourceBand.setNoDataValue(117);
        sourceBand.setImageToModelTransform(new AffineTransform(2, 0, 0, 2, 2, 2));
        return sourceBand;
    }

    private Band createSourceBand_Median(int dataType) {
        final Product sourceProduct = new Product("dummy", "dummy", 4, 4);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "(X + 0.5) * (Y + 0.5)", dataType);
        sourceBand.setNoDataValue(117);
        sourceBand.setImageToModelTransform(new AffineTransform(2, 0, 0, 2, 2, 2));
        return sourceBand;
    }

}