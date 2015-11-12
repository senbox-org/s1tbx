/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.snap.dataio.netcdf.util;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * Similar to the {@link com.bc.ceres.glevel.support.DefaultMultiLevelImage} this class
 * adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the
 * {@link com.bc.ceres.glevel.MultiLevelSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 * <p>
 * The difference is that this {@code MultiLevelImage} computes its model lazy. when requested the first time.
 *
 * @author Marco Zuehlke
 */
public abstract class AbstractNetcdfMultiLevelImage extends MultiLevelImage {

    private final RasterDataNode rasterDataNode;
    private final int levelCount;
    private final RenderedImage[] levelImages;
    private MultiLevelModel multiLevelModel;

    protected AbstractNetcdfMultiLevelImage(RasterDataNode rasterDataNode) {
        super(ImageManager.createSingleBandedImageLayout(rasterDataNode), null, null);
        this.rasterDataNode = rasterDataNode;
        int width = rasterDataNode.getRasterWidth();
        int height = rasterDataNode.getRasterHeight();
        levelCount = DefaultMultiLevelModel.getLevelCount(width, height);
        this.levelImages = new RenderedImage[levelCount];
    }

    protected RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    @Override
    public synchronized MultiLevelModel getModel() {
        if (multiLevelModel == null) {
            multiLevelModel = createMultiLevelModel(rasterDataNode, levelCount);
        }
        return multiLevelModel;
    }

    @Override
    public synchronized RenderedImage getImage(int level) {
        checkLevel(level);
        RenderedImage levelImage = levelImages[level];
        if (levelImage == null) {
            levelImage = createImage(level);
            levelImages[level] = levelImage;
        }
        return levelImage;
    }

    protected abstract RenderedImage createImage(int level);

    @Override
    public Shape getImageShape(int level) {
        return null;
    }

    @Override
    public void reset() {
        for (int level = 0; level < levelImages.length; level++) {
            RenderedImage levelImage = levelImages[level];
            if (levelImage instanceof PlanarImage) {
                PlanarImage planarImage = (PlanarImage) levelImage;
                planarImage.dispose();
            }
            levelImages[level] = null;
        }
    }

    @Override
    public void dispose() {
        reset();
        super.dispose();
    }

    private void checkLevel(int level) {
        if (level < 0 || level >= levelCount) {
            throw new IllegalArgumentException("level=" + level);
        }
    }

    private static MultiLevelModel createMultiLevelModel(RasterDataNode rasterDataNode, int levelCount) {
        final int w = rasterDataNode.getRasterWidth();
        final int h = rasterDataNode.getRasterHeight();
        final AffineTransform i2mTransform = Product.findImageToModelTransform(rasterDataNode.getGeoCoding());
        return new DefaultMultiLevelModel(levelCount, i2mTransform, w, h);
    }
}
