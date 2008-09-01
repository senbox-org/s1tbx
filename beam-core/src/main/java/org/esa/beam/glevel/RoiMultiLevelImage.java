package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;


public class RoiMultiLevelImage  extends DeferredMultiLevelImage implements LRImageFactory {

    private final RasterDataNode rasterDataNode;
    private final Color color;

    public RoiMultiLevelImage(RasterDataNode rasterDataNode, Color color, AffineTransform imageToModelTransform) {
        super(imageToModelTransform, ImageManager.computeMaxLevelCount(rasterDataNode.getSceneRasterWidth(), rasterDataNode.getSceneRasterHeight()));
        setLRImageFactory(this);
        setModelBounds(getModelBounds(imageToModelTransform, rasterDataNode.getSceneRasterWidth(), rasterDataNode.getSceneRasterHeight()));
        this.rasterDataNode = rasterDataNode;
        this.color = color;
    }

    @Override
    public RenderedImage createLRImage(int level) {
        return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
    }

}