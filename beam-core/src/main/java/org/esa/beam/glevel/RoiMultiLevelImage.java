package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractMultiLevelImage;
import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LevelImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class RoiMultiLevelImage {

    public static LevelImage create(RasterDataNode rasterDataNode, Color color, AffineTransform imageToModelTransform) {
        final int rasterWidth = rasterDataNode.getSceneRasterWidth();
        final int rasterHeight = rasterDataNode.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        LRImageFactory lrImageFactory = new Factory(rasterDataNode, color);
        DeferredMultiLevelImage deferredMultiLevelImage = new DeferredMultiLevelImage(imageToModelTransform, levelCount, lrImageFactory);
        Rectangle2D modelBounds = AbstractMultiLevelImage.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        deferredMultiLevelImage.setModelBounds(modelBounds);
        return deferredMultiLevelImage;
    }

    private static class Factory implements LRImageFactory {
        private final RasterDataNode rasterDataNode;
        private final Color color;
        
        public Factory(RasterDataNode rasterDataNode, Color color) {
            this.rasterDataNode = rasterDataNode;
            this.color = color;
        }

        @Override
        public RenderedImage createLRImage(int level) {
            return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
        }
    }
}