package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

public class SourceImageScalerTest {

    private static final int MAX_USHORT = (2 << 15) - 1;
    private Band targetBand;
    private RenderingHints renderingHints;
    private float[] scalings;
    private int levelCount;

    @Before
    public void setup() {
        targetBand = new Band("targetBand", ProductData.TYPE_INT32, 200, 200);
        renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(
                                                                         BorderExtender.BORDER_COPY));
        scalings = new float[]{2f, 2f};
        levelCount = 5;
    }

    @Test
    public void testScaleSourceImage() {
        MultiLevelImage sourceImage = createSourceImage(levelCount, 100, 100);
        MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(targetBand.getSourceImage(),
                                                                             sourceImage, scalings,
                                                                             new float[]{0f, 0f}, renderingHints,
                                                                             Double.NaN, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        final Rectangle targetBounds = targetBand.getSourceImage().getBounds();

        assertEquals(targetBand.getRasterWidth(), scaledImage.getWidth());
        assertEquals(targetBand.getRasterHeight(), scaledImage.getHeight());
        assertEquals(targetBand.getSourceImage().getModel().getLevelCount(), scaledImage.getModel().getLevelCount());
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - 1, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, targetBounds.height - 1, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - 1, targetBounds.height - 1, 0));
    }

    @Test
    public void testScaleMultiLevelImageWithDifferentLevelCounts() {
        MultiLevelImage masterImage = createNewMultiLevelMasterImage(levelCount, 200, 200);
        targetBand.setSourceImage(masterImage);
        int sourceLevelCount = 3;
        MultiLevelImage sourceImage = createSourceImage(sourceLevelCount, 100, 100);
        MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(masterImage, sourceImage, scalings,
                                                                             new float[]{0f, 0f}, renderingHints,
                                                                             Double.NaN, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        final Rectangle targetBounds = targetBand.getSourceImage().getBounds();

        assertEquals(targetBand.getRasterWidth(), scaledImage.getWidth());
        assertEquals(targetBand.getRasterHeight(), scaledImage.getHeight());
        assertEquals(levelCount, scaledImage.getModel().getLevelCount());
        for (int i = 0; i < levelCount; i++) {
            final RenderedImage masterImageAtLevel = ((MultiLevelImage) targetBand.getSourceImage()).getImage(i);
            final RenderedImage scaledImageAtLevel = scaledImage.getImage(i);
            assertEquals(masterImageAtLevel.getWidth(), scaledImageAtLevel.getWidth());
            assertEquals(masterImageAtLevel.getHeight(), scaledImageAtLevel.getHeight());
        }
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - 1, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, targetBounds.height - 1, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - 1, targetBounds.height - 1, 0));
    }

    @Test
    public void testScaleSourceImageWithOffsets() {
        MultiLevelImage sourceImage = createSourceImage(levelCount, 50, 50);
        float[] offsets = new float[]{50f, 50f};
        MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(targetBand.getSourceImage(),
                                                                             sourceImage, scalings,
                                                                             offsets, renderingHints,
                                                                             Double.NaN, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        final Rectangle targetBounds = targetBand.getSourceImage().getBounds();

        assertEquals(targetBand.getRasterWidth(), scaledImage.getWidth());
        assertEquals(targetBand.getRasterHeight(), scaledImage.getHeight());
        assertEquals(targetBand.getSourceImage().getModel().getLevelCount(), scaledImage.getModel().getLevelCount());
        assertEquals(MAX_USHORT, scaledImage.getData().getSample((int)offsets[0], (int)offsets[1], 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - (int)offsets[0] - 1, (int)offsets[1], 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample((int)offsets[0], targetBounds.height - (int)offsets[1]- 1, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(targetBounds.width - (int)offsets[0] - 1, targetBounds.height - (int)offsets[1] - 1, 0));
    }

    @Test
    public void testScaleSourceImageWithNegativeOffsets() {
        MultiLevelImage sourceImage = createSourceImage2(levelCount, 50, 50);
        float[] offsets = new float[]{-50f, -50f};
        MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(targetBand.getSourceImage(),
                                                                             sourceImage, scalings,
                                                                             offsets, renderingHints,
                                                                             Double.NaN, Interpolation.getInstance(Interpolation.INTERP_NEAREST));

        assertEquals(targetBand.getRasterWidth(), scaledImage.getWidth());
        assertEquals(targetBand.getRasterHeight(), scaledImage.getHeight());
        assertEquals(targetBand.getSourceImage().getModel().getLevelCount(), scaledImage.getModel().getLevelCount());
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(49, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 49, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(49, 49, 0));
    }

    @Test
    public void testScaleSourceImageWithNegativeNonIntegerOffsets() {
        MultiLevelImage sourceImage = createSourceImage2(levelCount, 50, 50);
        float[] offsets = new float[]{-50.5f, -50.5f};
        MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(targetBand.getSourceImage(),
                                                                             sourceImage, scalings,
                                                                             offsets, renderingHints,
                                                                             Double.NaN, Interpolation.getInstance(Interpolation.INTERP_NEAREST));

        assertEquals(targetBand.getRasterWidth(), scaledImage.getWidth());
        assertEquals(targetBand.getRasterHeight(), scaledImage.getHeight());
        assertEquals(targetBand.getSourceImage().getModel().getLevelCount(), scaledImage.getModel().getLevelCount());
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(48, 0, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(0, 48, 0));
        assertEquals(MAX_USHORT, scaledImage.getData().getSample(48, 48, 0));
    }

    private static MultiLevelImage createSourceImage(int levelCount, int srcW, int srcH) {
        BufferedImage sourceImage = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                sourceImage.getRaster().setSample(x, y, 0, (int) (MAX_USHORT * Math.random()));
            }
        }
        sourceImage.getRaster().setSample(0, 0, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(srcW - 1, 0, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(0, srcH - 1, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(srcW - 1, srcH - 1, 0, MAX_USHORT);
        final DefaultMultiLevelSource multiLevelSource = new DefaultMultiLevelSource(sourceImage, levelCount);
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    private static MultiLevelImage createSourceImage2(int levelCount, int srcW, int srcH) {
        BufferedImage sourceImage = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                sourceImage.getRaster().setSample(x, y, 0, (int) (MAX_USHORT * Math.random()));
            }
        }
        sourceImage.getRaster().setSample(srcW/2, srcH/2, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(srcW - 1, srcH/2, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(srcW/2, srcH - 1, 0, MAX_USHORT);
        sourceImage.getRaster().setSample(srcW - 1, srcH - 1, 0, MAX_USHORT);
        final DefaultMultiLevelSource multiLevelSource = new DefaultMultiLevelSource(sourceImage, levelCount);
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    private static MultiLevelImage createNewMultiLevelMasterImage(int levelCount, int srcW, int srcH) {
        BufferedImage sourceImage = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        final DifferentlyScalingMultiLevelModel multiLevelModel = new DifferentlyScalingMultiLevelModel(levelCount, sourceImage);
        final DefaultMultiLevelSource source = new DefaultMultiLevelSource(sourceImage, multiLevelModel);
        return new DefaultMultiLevelImage(source);
    }

    private static class DifferentlyScalingMultiLevelModel extends DefaultMultiLevelModel {

        public DifferentlyScalingMultiLevelModel(int levelCount, BufferedImage sourceImage) {
            super(levelCount, new AffineTransform(), sourceImage.getWidth(), sourceImage.getHeight());
        }

        @Override
        public double getScale(int level) {
            checkLevel(level);
            return Math.pow(3, level);
        }

    }

}
