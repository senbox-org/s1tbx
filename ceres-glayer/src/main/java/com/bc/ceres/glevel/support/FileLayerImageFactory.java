package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.LayerImage;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;

public class FileLayerImageFactory {

    public static LayerImage create(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        final LIS levelImageSource = new LIS(location, location.getName(), extension, levelCount);
        Rectangle2D modelBounds = AbstractLayerImage.getModelBounds(imageToModelTransform, levelImageSource.getLevelImage(0));
        return new DefaultLayerImage(levelImageSource, imageToModelTransform, modelBounds);
    }

    private static class LIS extends AbstractLevelImageSource {
        private final File location;
        private final String basename;
        private final String extension;

        public LIS(File location, String basename, String extension, int levelCount) {
            super(levelCount);
            this.location = location;
            this.basename = basename;
            this.extension = extension;
        }

        public RenderedImage createLevelImage(int level) {
            final StringBuilder sb = new StringBuilder(basename);
            sb.append('.');
            sb.append(level);
            sb.append('.');
            sb.append(extension);
            final String fileName = sb.toString();
            final File file = new File(location, fileName);
            return FileLoadDescriptor.create(file.getPath(), null, true, null).getRendering();
        }
    }
}