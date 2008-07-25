package com.bc.ceres.glevel.support;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

public class FileMultiLevelImage extends AbstractMultiLevelImage {

    private final File location;
    private final String basename;
    private final String extension;

    public FileMultiLevelImage(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount);
        this.location = location;
        this.basename = location.getName();
        this.extension = extension;
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        final StringBuilder sb = new StringBuilder(basename);
        sb.append('.');
        sb.append(level);
        sb.append('.');
        sb.append(extension);
        final String fileName = sb.toString();
        final File file = new File(location, fileName);
        return FileLoadDescriptor.create(file.getPath(), null, true, null).getRendering();
    }

    @Override
    public Rectangle2D getBoundingBox(int level) {
        checkLevel(level);
        final PlanarImage image = getPlanarImage(0);
        return getImageToModelTransform(0).createTransformedShape(new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight())).getBounds2D();
    }
}