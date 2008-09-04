package org.esa.beam.glevel;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.ImageLayerModel;
import com.bc.ceres.glevel.support.AbstractLevelImageSource;
import com.bc.ceres.glevel.support.DefaultImageLayerModel;


public class BandLayerImageFactory {

    public static ImageLayerModel create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform);
    }
    public static ImageLayerModel create(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform, int levelCount) {
        return create(new RasterDataNode[]{rasterDataNode}, imageToModelTransform, levelCount);
    }
    public static ImageLayerModel create(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform) {
        return create(rasterDataNodes, affineTransform,
                ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                        rasterDataNodes[0].getSceneRasterHeight()));
    }
    public static ImageLayerModel create(RasterDataNode[] rasterDataNodes, AffineTransform imageToModelTransform, int levelCount) {
        Assert.notNull(rasterDataNodes);
        Assert.argument(rasterDataNodes.length>0);
        final LIS levelImageSource = new LIS(rasterDataNodes, levelCount);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        Rectangle2D modelBounds = DefaultImageLayerModel.getModelBounds(imageToModelTransform, w, h);
        DefaultImageLayerModel defaultLayerImage = new DefaultImageLayerModel(levelImageSource, imageToModelTransform, modelBounds);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, levelCount);
        return defaultLayerImage;
    }
    
    private static class LIS extends AbstractLevelImageSource {
        
        private final RasterDataNode[] rasterDataNodes;

        public LIS(RasterDataNode[] rasterDataNodes, int levelCount) {
            super(levelCount);
            this.rasterDataNodes = rasterDataNodes.clone();
        }

        @Override
        public RenderedImage createLevelImage(int level) {
            return ImageManager.getInstance().createRgbImage(rasterDataNodes, level, getLevelCount());
        }
    }

}
