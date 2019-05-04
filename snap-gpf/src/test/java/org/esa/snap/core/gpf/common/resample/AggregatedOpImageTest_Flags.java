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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class AggregatedOpImageTest_Flags {

    private Band referenceBand;
    private AffineTransform sourceTransform;
    private double noDataValue;

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
        sourceTransform = new AffineTransform(2, 0, 0, 2, 2, 2);
        noDataValue = 117;
    }

    @Test
    public void testFlagAnd() throws NoninvertibleTransformException {
        short[] sourceData = new short[]{
                5, 1, 2, 3,
                9, 6, 3, 2,
                7, 5, 7, 1,
                7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                0, 0,
                1, 5
        };
        final RenderedImage sourceImage = createSourceImage(sourceData);
        final ImageLayout imageLayout = createImageLayout(sourceImage);

        final Band sourceBand = new Band("band", ProductData.TYPE_UINT16, 4, 4);
        sourceBand.setSourceImage(sourceImage);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand, sourceImage, imageLayout, noDataValue,
                                                              ResampleUtils.getDownsamplingFromAggregatorType(AggregationType.FlagAnd),
                                                              ProductData.TYPE_UINT16, sourceTransform,
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, image.getSampleModel().getDataType());
        DataBufferUShort destBuffer = ((DataBufferUShort) image.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

    @Test
    public void testFlagOr() throws NoninvertibleTransformException {
        short[] sourceData = new short[]{
                5, 1, 2, 3,
                9, 6, 3, 2,
                7, 5, 7, 1,
                7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                7, 3,
                13, 5
        };
        final RenderedImage sourceImage = createSourceImage(sourceData);
        final ImageLayout imageLayout = createImageLayout(sourceImage);

        final Band sourceBand = new Band("band", ProductData.TYPE_UINT16, 4, 4);
        sourceBand.setSourceImage(sourceImage);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand, sourceImage, imageLayout, noDataValue,
                                                              ResampleUtils.getDownsamplingFromAggregatorType(AggregationType.FlagOr),
                                                              ProductData.TYPE_UINT16, sourceTransform,
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, image.getSampleModel().getDataType());
        DataBufferUShort destBuffer = ((DataBufferUShort) image.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

    @Test
    public void testFlagMedianAnd() throws NoninvertibleTransformException {
        short[] sourceData = new short[]{
                5, 1, 2, 3,
                9, 6, 3, 2,
                7, 5, 7, 1,
                7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                7, 0,
                1, 5
        };
        final RenderedImage sourceImage = createSourceImage(sourceData);
        final ImageLayout imageLayout = createImageLayout(sourceImage);

        final Band sourceBand = new Band("band", ProductData.TYPE_UINT16, 4, 4);
        sourceBand.setSourceImage(sourceImage);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand, sourceImage, imageLayout, noDataValue,
                                                              ResampleUtils.getDownsamplingFromAggregatorType(AggregationType.FlagMedianAnd),
                                                              ProductData.TYPE_UINT16, sourceTransform,
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, image.getSampleModel().getDataType());
        DataBufferUShort destBuffer = ((DataBufferUShort) image.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

    @Test
    public void testFlagMedianOr() throws NoninvertibleTransformException {
        short[] sourceData = new short[]{
                5, 1, 2, 3,
                9, 6, 3, 2,
                7, 5, 7, 1,
                7, 5, 9, 5,
        };
        short[] expectedData = new short[]{
                7, 3,
                13, 5
        };
        final RenderedImage sourceImage = createSourceImage(sourceData);
        final ImageLayout imageLayout = createImageLayout(sourceImage);

        final Band sourceBand = new Band("band", ProductData.TYPE_UINT16, 4, 4);
        sourceBand.setSourceImage(sourceImage);

        final AggregatedOpImage image = new AggregatedOpImage(sourceBand, sourceImage, imageLayout, noDataValue,
                                                              ResampleUtils.getDownsamplingFromAggregatorType(AggregationType.FlagMedianOr),
                                                              ProductData.TYPE_UINT16, sourceTransform,
                                                              referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, image.getSampleModel().getDataType());
        DataBufferUShort destBuffer = ((DataBufferUShort) image.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

    private ImageLayout createImageLayout(RenderedImage sourceImage) {
        final int dataType = sourceImage.getSampleModel().getDataType();
        return ImageManager.createSingleBandedImageLayout(dataType, referenceBand.getRasterWidth(),
                                                          referenceBand.getRasterHeight(),
                                                          referenceBand.getRasterWidth(),
                                                          referenceBand.getRasterHeight());
    }

    static RenderedImage createSourceImage(short[] data) {
        final int widthAndHeight = (int) Math.sqrt(data.length);
        BufferedImage image = new BufferedImage(widthAndHeight, widthAndHeight, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, widthAndHeight * widthAndHeight);
        return image;
    }

}
