package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;


public class BandMultiLevelImage extends DeferredMultiLevelImage implements LRImageFactory {

    private final RasterDataNode[] rasterDataNodes;

    public BandMultiLevelImage(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform) {
        this(new RasterDataNode[]{rasterDataNode}, imageToModelTransform);
    }

    public BandMultiLevelImage(RasterDataNode rasterDataNode, AffineTransform imageToModelTransform, int levelCount) {
        this(new RasterDataNode[]{rasterDataNode}, imageToModelTransform, levelCount);
    }

    public BandMultiLevelImage(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform) {
        this(rasterDataNodes, affineTransform,
             ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                                               rasterDataNodes[0].getSceneRasterHeight()));
    }

    public BandMultiLevelImage(RasterDataNode[] rasterDataNodes, AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount);
        setLRImageFactory(this);
        this.rasterDataNodes = rasterDataNodes.clone();
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        setModelBounds(getModelBounds(imageToModelTransform, w, h));
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, levelCount);
    }

    @Override
    public RenderedImage createLRImage(int level) {
        return ImageManager.getInstance().createRgbImage(rasterDataNodes, level, getLevelCount());
    }

}
