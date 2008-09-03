package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractMultiLevelImage;
import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LevelImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class BandMultiLevelImage {

    public static LevelImage create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform);
    }
    public static LevelImage create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform, int levelCount) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform, levelCount);
    }
    public static LevelImage create(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform) {
        return create(rasterDataNodes, affineTransform,
                ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                        rasterDataNodes[0].getSceneRasterHeight()));
    }
    public static LevelImage create(RasterDataNode[] rasterDataNodes, AffineTransform imageToModelTransform, int levelCount) {
        LRImageFactory lrImageFactory = new Factory(rasterDataNodes, levelCount);
        DeferredMultiLevelImage deferredMultiLevelImage = new DeferredMultiLevelImage(imageToModelTransform, levelCount, lrImageFactory);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        Rectangle2D modelBounds = AbstractMultiLevelImage.getModelBounds(imageToModelTransform, w, h);
        deferredMultiLevelImage.setModelBounds(modelBounds);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, levelCount);
        return deferredMultiLevelImage;
    }
    
    private static class Factory implements LRImageFactory {
        
        private final RasterDataNode[] rasterDataNodes;
        private final int levelCount;

        public Factory(RasterDataNode[] rasterDataNodes, int levelCount) {
            this.rasterDataNodes = rasterDataNodes.clone();
            this.levelCount = levelCount;
        }

        @Override
        public RenderedImage createLRImage(int level) {
            return ImageManager.getInstance().createRgbImage(rasterDataNodes, level, levelCount);
        }
    }

}
