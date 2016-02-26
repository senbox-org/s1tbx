package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.Interpolation;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class ResampleTest_Interpolate {
    private Interpolation interpolation;

    @Before
    public void setUp() {
        interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
    }

    @Test
    public void testCreateScaledImage_AffineProvidedForSource_OnlyScale() throws Exception {
        int sourceWidth = 2;
        int sourceHeight = 2;
        int referenceWidth = 4;
        int referenceHeight = 4;
        int sourceScaleX = 2;
        int sourceScaleY = 2;
        int sourceTranslateX = 0;
        int sourceTranslateY = 0;
        int levelCount = 2;
        final AffineTransform imageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Band sourceBand = createSourceBand(imageToModelTransform, sourceWidth, sourceHeight);
        final Band referenceBand = createReferenceBand(referenceWidth, referenceHeight, levelCount, new AffineTransform());

//        final MultiLevelImage scaledMultiLevelImage = Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);
        final MultiLevelImage scaledMultiLevelImage = Resample.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);

        assertNotNull(scaledMultiLevelImage);
        assertEquals(levelCount, scaledMultiLevelImage.getModel().getLevelCount());
        for (int i = 0; i < levelCount; i++) {
            final RenderedImage image = scaledMultiLevelImage.getImage(i);
            int expectedWidth = referenceWidth / (i + 1);
            int expectedHeight = referenceHeight / (i + 1);
            assertEquals(expectedWidth, image.getWidth());
            assertEquals(expectedHeight, image.getHeight());
            final Raster targetData = image.getData();
            final Raster sourceData = sourceBand.getSourceImage().getData();
            for (int x = 0; x < expectedWidth; x++) {
                for (int y = 0; y < expectedHeight; y++) {
                    assertEquals(sourceData.getSampleDouble(x / (2 - i), y / (2 - i), 0),
                                 targetData.getSampleDouble(x, y, 0), 1e-8);
                }
            }
        }
    }

    @Test
    public void testCreateScaledImage_AffineProvidedForSource_ScaleAndTransform() throws Exception {
        int sourceWidth = 2;
        int sourceHeight = 2;
        int referenceWidth = 8;
        int referenceHeight = 8;
        int scaleX = 2;
        int scaleY = 2;
        int translateX = 2;
        int translateY = 2;
        int levelCount = 2;
        final AffineTransform imageToModelTransform = new AffineTransform(scaleX, 0, 0, scaleY, translateX, translateY);
        final Band sourceBand = createSourceBand(imageToModelTransform, sourceWidth, sourceHeight);
        final Band referenceBand = createReferenceBand(referenceWidth, referenceHeight, levelCount, new AffineTransform());

//        final MultiLevelImage scaledMultiLevelImage = Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);
        final MultiLevelImage scaledMultiLevelImage = Resample.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);

        assertNotNull(scaledMultiLevelImage);
        assertEquals(levelCount, scaledMultiLevelImage.getModel().getLevelCount());
        for (int i = 0; i < levelCount; i++) {
            final RenderedImage image = scaledMultiLevelImage.getImage(i);
            int expectedWidth = referenceWidth / (i + 1);
            int expectedHeight = referenceHeight / (i + 1);
            assertEquals(expectedWidth, image.getWidth());
            assertEquals(expectedHeight, image.getHeight());
            final Raster targetData = image.getData();
            final Raster sourceData = sourceBand.getSourceImage().getData();
            for (int x = 0; x < expectedWidth; x++) {
                for (int y = 0; y < expectedHeight; y++) {
                    if (x < (2 - i) || x >= expectedWidth - (2 - i) || y < (2 - i) || y >= expectedHeight - (2 - i)) {
                        assertEquals(sourceBand.getNoDataValue(), targetData.getSampleDouble(x, y, 0), 1e-8);
                    } else {
                        assertEquals(sourceData.getSampleDouble((x - (2 - i)) / (2 - i), (y - (2 - i)) / (2 - i), 0),
                                     targetData.getSampleDouble(x, y, 0), 1e-8);
                    }
                }
            }
        }
    }

    @Test
    public void testCreateScaledImage_AffineProvidedForSourceAndReference_OnlyScale() throws Exception {
        int sourceWidth = 4;
        int sourceHeight = 4;
        int referenceWidth = 2;
        int referenceHeight = 2;
        int sourceScaleX = 4;
        int sourceScaleY = 4;
        int sourceTranslateX = 0;
        int sourceTranslateY = 0;
        int referenceScaleX = 2;
        int referenceScaleY = 2;
        int referenceTranslateX = 0;
        int referenceTranslateY = 0;
        int levelCount = 2;
        final AffineTransform sourceImageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Band sourceBand = createSourceBand(sourceImageToModelTransform, sourceWidth, sourceHeight);
        final AffineTransform referenceImageToModelTransform = new AffineTransform(referenceScaleX, 0, 0, referenceScaleY, referenceTranslateX, referenceTranslateY);
        final Band referenceBand = createReferenceBand(referenceWidth, referenceHeight, levelCount, referenceImageToModelTransform);

//        final MultiLevelImage scaledMultiLevelImage = Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);
        final MultiLevelImage scaledMultiLevelImage = Resample.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);

        assertNotNull(scaledMultiLevelImage);
        assertEquals(levelCount, scaledMultiLevelImage.getModel().getLevelCount());
        for (int i = 0; i < levelCount; i++) {
            final RenderedImage image = scaledMultiLevelImage.getImage(i);
            int expectedWidth = referenceWidth / (i + 1);
            int expectedHeight = referenceHeight / (i + 1);
            assertEquals(expectedWidth, image.getWidth());
            assertEquals(expectedHeight, image.getHeight());
            final Raster targetData = image.getData();
            final Raster sourceData = sourceBand.getSourceImage().getData();
            for (int x = 0; x < expectedWidth; x++) {
                for (int y = 0; y < expectedHeight; y++) {
                    assertEquals(sourceData.getSampleDouble(x / (2 - i), y / (2 - i), 0),
                                 targetData.getSampleDouble(x, y, 0), 1e-8);
                }
            }
        }
    }

    @Test
    @Ignore     //todo fix test
    public void testCreateScaledImage_AffineProvidedForSourceAndReference_ScaleAndTransform() throws Exception {
        int sourceWidth = 2;
        int sourceHeight = 2;
        int sourceScaleX = 3;
        int sourceScaleY = 3;
        int sourceTranslateX = 1;
        int sourceTranslateY = 1;
        int referenceWidth = 3;
        int referenceHeight = 3;
        int referenceScaleX = 2;
        int referenceScaleY = 2;
        int referenceTranslateX = 1;
        int referenceTranslateY = 1;
        int referenceLevelCount = 1;
        final AffineTransform sourceImageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Band sourceBand = createSourceBand(sourceImageToModelTransform, sourceWidth, sourceHeight);
        final AffineTransform referenceImageToModelTransform = new AffineTransform(referenceScaleX, 0, 0, referenceScaleY, referenceTranslateX, referenceTranslateY);
        final Band referenceBand = createReferenceBand(referenceWidth, referenceHeight, referenceLevelCount, referenceImageToModelTransform);

//        final MultiLevelImage scaledMultiLevelImage = Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);
        final MultiLevelImage scaledMultiLevelImage = Resample.createInterpolatedMultiLevelImage(sourceBand, referenceBand, interpolation);

        assertNotNull(scaledMultiLevelImage);
        assertEquals(referenceLevelCount, scaledMultiLevelImage.getModel().getLevelCount());
        for (int i = 0; i < referenceLevelCount; i++) {
            final RenderedImage image = scaledMultiLevelImage.getImage(i);
            int expectedWidth = referenceWidth / (i + 1);
            int expectedHeight = referenceHeight / (i + 1);
            assertEquals(expectedWidth, image.getWidth());
            assertEquals(expectedHeight, image.getHeight());
            final Raster targetData = image.getData();
            final Raster sourceData = sourceBand.getSourceImage().getData();
            for (int x = 0; x < expectedWidth; x++) {
                for (int y = 0; y < expectedHeight; y++) {
                    if (x < 0 || x >= expectedWidth - 1 - i || y < 0 || y >= expectedHeight - 1 - i) {
                        assertEquals(sourceBand.getNoDataValue(), targetData.getSampleDouble(x, y, 0), 1e-8);
                    } else {
                        assertEquals(sourceData.getSampleDouble(x * (3 - i) / (2 - i), y * (3 - i) / (2 - i), 0),
                                     targetData.getSampleDouble(x, y, 0), 1e-8);
                    }
                }
            }
        }
    }

    private Band createSourceBand(AffineTransform transform, int width, int height) {
        final Product sourceProduct = new Product("dummy", "dummy", width, height);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "cos(X) + sin(Y)");
        sourceBand.setNoDataValue(-23);
        sourceBand.setImageToModelTransform(transform);
        return sourceBand;
    }

    private Band createReferenceBand(int width, int height, int levelcount, AffineTransform imageToModelTransform) {
        final Band referenceBand = new Band("referenceBand", ProductData.TYPE_INT8, width, height);
        final DefaultMultiLevelModel referenceModel = new DefaultMultiLevelModel(levelcount, imageToModelTransform, width, height);
        referenceBand.setSourceImage(new DefaultMultiLevelImage(new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new BufferedImage(width / (1 + level), height / (1 + level), ProductData.TYPE_INT8);
            }
        }));
        return referenceBand;
    }
}