package org.esa.beam.glevel;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.ImageLayerModel;
import com.bc.ceres.glevel.support.AbstractLevelImageSource;
import com.bc.ceres.glevel.support.DefaultImageLayerModel;


public class RoiLayerImageFactory {

    public static ImageLayerModel create(RasterDataNode rasterDataNode, Color color, AffineTransform imageToModelTransform) {
        Assert.notNull(rasterDataNode);
        Assert.notNull(color);
        final int rasterWidth = rasterDataNode.getSceneRasterWidth();
        final int rasterHeight = rasterDataNode.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        final LIS levelImageSource = new LIS(rasterDataNode, color, levelCount);
        Rectangle2D modelBounds = DefaultImageLayerModel.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        return new DefaultImageLayerModel(levelImageSource, imageToModelTransform, modelBounds);
    }

    private static class LIS extends AbstractLevelImageSource{
        private final RasterDataNode rasterDataNode;
        private final Color color;

        public LIS(RasterDataNode rasterDataNode, Color color, int levelCount) {
            super(levelCount);
            this.rasterDataNode = rasterDataNode;
            this.color = color;
        }

        @Override
        public RenderedImage createLevelImage(int level) {
            return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
        }
    }
}