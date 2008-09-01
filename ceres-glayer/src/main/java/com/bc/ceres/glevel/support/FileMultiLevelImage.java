package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LRImageFactory;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;

public class FileMultiLevelImage extends DeferredMultiLevelImage {

    public FileMultiLevelImage(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount, new IF(location, location.getName(), extension));
        setModelBounds(getModelBounds(imageToModelTransform, getLRImage(0)));
    }

    private static class IF implements LRImageFactory {
        private final File location;
        private final String basename;
        private final String extension;

        public IF(File location, String basename, String extension) {
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