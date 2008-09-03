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
        DeferredLayerImage deferredLayerImage = new DeferredLayerImage(
                imageToModelTransform, levelCount, new Factory(location, location.getName(), extension));
        Rectangle2D modelBounds2 = AbstractLayerImage.getModelBounds(imageToModelTransform, deferredLayerImage.getLevelImage(0));
        deferredLayerImage.setModelBounds(modelBounds2);
        return deferredLayerImage;
    }

    private static class Factory implements LevelImageFactory {
        private final File location;
        private final String basename;
        private final String extension;

        public Factory(File location, String basename, String extension) {
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