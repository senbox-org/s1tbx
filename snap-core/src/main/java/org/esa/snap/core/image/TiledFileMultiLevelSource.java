/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.image;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.util.StringUtils;

import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;


public class TiledFileMultiLevelSource extends AbstractMultiLevelSource {

    private final Path imageDir;
    private final Properties imageProperties;

    public static TiledFileMultiLevelSource create(File imageDir) throws IOException {
        return create(imageDir.toPath());
    }

    public static TiledFileMultiLevelSource create(Path imageDir) throws IOException {
        Assert.notNull(imageDir);
        final Properties imageProperties = new Properties();
        imageProperties.load(Files.newBufferedReader(imageDir.resolve("image.properties")));
        int levelCount = Integer.parseInt(imageProperties.getProperty("numLevels"));
        int sourceWidth = Integer.parseInt(imageProperties.getProperty("width"));
        int sourceHeight = Integer.parseInt(imageProperties.getProperty("height"));
        final String s = imageProperties.getProperty("i2mTransform");
        AffineTransform i2mTransform = new AffineTransform();
        if (s != null) {
            try {
                double[] matrix = StringUtils.toDoubleArray(s, ",");
                if (matrix.length == 6) {
                    i2mTransform = new AffineTransform(matrix);
                }
            } catch (IllegalArgumentException e) {
                // may be thrown by StringUtils.toDoubleArray(), use identity instead
            }
        }
        final MultiLevelModel model = new DefaultMultiLevelModel(levelCount, i2mTransform, sourceWidth, sourceHeight);
        return new TiledFileMultiLevelSource(model, imageDir, imageProperties);
    }

    public TiledFileMultiLevelSource(MultiLevelModel model, Path imageDir, Properties imageProperties) {
        super(model);
        this.imageDir = imageDir;
        this.imageProperties = imageProperties;
    }

    @Override
    public RenderedImage createImage(int level) {
        PlanarImage image;
        try {
            image = TiledFileOpImage.create(imageDir.resolve(String.valueOf(level)), imageProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

}
