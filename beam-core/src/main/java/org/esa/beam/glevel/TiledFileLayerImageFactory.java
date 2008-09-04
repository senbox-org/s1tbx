package org.esa.beam.glevel;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.esa.beam.jai.TiledFileOpImage;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.ImageLayerModel;
import com.bc.ceres.glevel.support.AbstractLevelImageSource;
import com.bc.ceres.glevel.support.DefaultImageLayerModel;


public class TiledFileLayerImageFactory {

    public static ImageLayerModel create(File imageDir, boolean visualDebug) throws IOException {
        Assert.notNull(imageDir);
        final Properties imageProperties = new Properties();
        imageProperties.load(new FileReader(new File(imageDir, "image.properties")));
        int levelCount = Integer.parseInt(imageProperties.getProperty("numLevels"));
        int sourceWidth = Integer.parseInt(imageProperties.getProperty("width"));
        int sourceHeight = Integer.parseInt(imageProperties.getProperty("height"));
        // todo - read 6 parameters from properties1
        final AffineTransform imageToModelTransform = AffineTransform.getScaleInstance(360.0 / sourceWidth, 180.0 / sourceHeight);
        final LIS levelImageSource = new LIS(imageDir, imageProperties, visualDebug, levelCount);
        Rectangle2D modelBounds = DefaultImageLayerModel.getModelBounds(imageToModelTransform, sourceWidth, sourceHeight);
        return new DefaultImageLayerModel(levelImageSource, imageToModelTransform, modelBounds);
    }

    private static class LIS extends AbstractLevelImageSource {
        private final File imageDir;
        private final Properties imageProperties;
        boolean visualDebug;
        
        public LIS(File imageDir, Properties imageProperties, boolean visualDebug, int levelCount) {
            super(levelCount);
            this.imageDir = imageDir;
            this.imageProperties = imageProperties;
            this.visualDebug = visualDebug;
        }

    @Override
        public RenderedImage createLevelImage(int level) {
            PlanarImage image;
            try {
                image = TiledFileOpImage.create(new File(imageDir, level + ""),
                        imageProperties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (visualDebug) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                pb.add(new int[] { 0x0F });
                image = JAI.create("andconst", pb, null);

                pb = new ParameterBlock();
                pb.addSource(image);
                pb.add(new double[] { 16 });
                pb.add(new double[] { 0 });
                image = JAI.create("rescale", pb, null);

                pb = new ParameterBlock();
                pb.addSource(image);
                pb.add(DataBuffer.TYPE_BYTE);
                image = JAI.create("format", pb, null);
            }
            return image;
        }
    }
}