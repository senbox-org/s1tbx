package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LevelImage;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;

public class FileMultiLevelImage {

    public static LevelImage create(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        DeferredMultiLevelImage deferredMultiLevelImage = new DeferredMultiLevelImage(
                imageToModelTransform, levelCount, new Factory(location, location.getName(), extension));
        Rectangle2D modelBounds2 = AbstractMultiLevelImage.getModelBounds(imageToModelTransform, deferredMultiLevelImage.getLRImage(0));
        deferredMultiLevelImage.setModelBounds(modelBounds2);
        return deferredMultiLevelImage;
    }

    private static class Factory implements LRImageFactory {
        private final File location;
        private final String basename;
        private final String extension;

        public Factory(File location, String basename, String extension) {
            this.location = location;
            this.basename = basename;
            this.extension = extension;
        }

        public RenderedImage createLRImage(int level) {
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