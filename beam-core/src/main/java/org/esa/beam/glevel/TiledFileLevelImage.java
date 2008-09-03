package org.esa.beam.glevel;

import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.glevel.support.AbstractMultiLevelImage;
import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import org.esa.beam.jai.TiledFileOpImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class TiledFileLevelImage {

    public static LevelImage create(File imageDir, boolean visualDebug) throws IOException {
        final Properties imageProperties = new Properties();
        imageProperties.load(new FileReader(new File(imageDir, "image.properties")));
        int levelCount = Integer.parseInt(imageProperties.getProperty("numLevels"));
        int sourceWidth = Integer.parseInt(imageProperties.getProperty("width"));
        int sourceHeight = Integer.parseInt(imageProperties.getProperty("height"));
        // todo - read 6 parameters from properties1
        final AffineTransform transform = AffineTransform.getScaleInstance(360.0 / sourceWidth, 180.0 / sourceHeight);
        LRImageFactory lrImageFactory = new Factory(imageDir, imageProperties, visualDebug);
        DeferredMultiLevelImage deferredMultiLevelImage = new DeferredMultiLevelImage(transform, levelCount, lrImageFactory);
        Rectangle2D modelBounds = AbstractMultiLevelImage.getModelBounds(transform, sourceWidth, sourceHeight);
        deferredMultiLevelImage.setModelBounds(modelBounds);
        return deferredMultiLevelImage;
    }

    private static class Factory implements LRImageFactory {
        private final File imageDir;
        private final Properties imageProperties;
        boolean visualDebug;
        
        public Factory(File imageDir, Properties imageProperties, boolean visualDebug) {
            this.imageDir = imageDir;
            this.imageProperties = imageProperties;
            this.visualDebug = visualDebug;
        }

    @Override
        public RenderedImage createLRImage(int level) {
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