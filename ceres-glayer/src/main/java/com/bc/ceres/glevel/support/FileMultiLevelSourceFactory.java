package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;

public class FileMultiLevelSourceFactory {

    public static MultiLevelSource create(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        final DefaultMultiLevelModel defaultMultiLevelModel = new DefaultMultiLevelModel(levelCount, imageToModelTransform, null);
        final LIS levelImageSource = new LIS(location, location.getName(), extension, defaultMultiLevelModel);
        Rectangle2D modelBounds = DefaultMultiLevelModel.getModelBounds(imageToModelTransform, levelImageSource.getLevelImage(0));
        defaultMultiLevelModel.setModelBounds(modelBounds);
        return levelImageSource;
    }

    private static class LIS extends AbstractMultiLevelSource {
        private final File location;
        private final String basename;
        private final String extension;

        public LIS(File location, String basename, String extension, MultiLevelModel multiLevelModel) {
            super(multiLevelModel);
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