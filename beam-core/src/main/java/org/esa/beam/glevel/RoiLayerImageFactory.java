package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractLayerImage;
import com.bc.ceres.glevel.support.DeferredLayerImage;
import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.LayerImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class RoiLayerImageFactory {

    public static LayerImage create(RasterDataNode rasterDataNode, Color color, AffineTransform imageToModelTransform) {
        final int rasterWidth = rasterDataNode.getSceneRasterWidth();
        final int rasterHeight = rasterDataNode.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        LevelImageFactory levelImageFactory = new LIF(rasterDataNode, color);
        DeferredLayerImage deferredLayerImage = new DeferredLayerImage(imageToModelTransform, levelCount, levelImageFactory);
        Rectangle2D modelBounds = AbstractLayerImage.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        deferredLayerImage.setModelBounds(modelBounds);
        return deferredLayerImage;
    }

    private static class LIF implements LevelImageFactory {
        private final RasterDataNode rasterDataNode;
        private final Color color;
        
        public LIF(RasterDataNode rasterDataNode, Color color) {
            this.rasterDataNode = rasterDataNode;
            this.color = color;
        }

        @Override
        public RenderedImage createLevelImage(int level) {
            return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
        }
    }
}