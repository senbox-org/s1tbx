package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractLayerImage;
import com.bc.ceres.glevel.support.DeferredLayerImage;
import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LayerImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class RoiMultiLevelImage {

    public static LayerImage create(RasterDataNode rasterDataNode, Color color, AffineTransform imageToModelTransform) {
        final int rasterWidth = rasterDataNode.getSceneRasterWidth();
        final int rasterHeight = rasterDataNode.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        LRImageFactory lrImageFactory = new Factory(rasterDataNode, color);
        DeferredLayerImage deferredLayerImage = new DeferredLayerImage(imageToModelTransform, levelCount, lrImageFactory);
        Rectangle2D modelBounds = AbstractLayerImage.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        deferredLayerImage.setModelBounds(modelBounds);
        return deferredLayerImage;
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