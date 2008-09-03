package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractLayerImage;
import com.bc.ceres.glevel.support.DeferredLayerImage;
import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.LayerImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class BandLayerImageFactory {

    public static LayerImage create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform);
    }
    public static LayerImage create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform, int levelCount) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform, levelCount);
    }
    public static LayerImage create(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform) {
        return create(rasterDataNodes, affineTransform,
                ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                        rasterDataNodes[0].getSceneRasterHeight()));
    }
    public static LayerImage create(RasterDataNode[] rasterDataNodes, AffineTransform imageToModelTransform, int levelCount) {
        LevelImageFactory levelImageFactory = new LIF(rasterDataNodes, levelCount);
        DeferredLayerImage deferredLayerImage = new DeferredLayerImage(imageToModelTransform, levelCount, levelImageFactory);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        Rectangle2D modelBounds = AbstractLayerImage.getModelBounds(imageToModelTransform, w, h);
        deferredLayerImage.setModelBounds(modelBounds);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, levelCount);
        return deferredLayerImage;
    }
    
    private static class LIF implements LevelImageFactory {
        
        private final RasterDataNode[] rasterDataNodes;
        private final int levelCount;

        public LIF(RasterDataNode[] rasterDataNodes, int levelCount) {
            this.rasterDataNodes = rasterDataNodes.clone();
            this.levelCount = levelCount;
        }

        @Override
        public RenderedImage createLevelImage(int level) {
            return ImageManager.getInstance().createRgbImage(rasterDataNodes, level, levelCount);
        }
    }

}
