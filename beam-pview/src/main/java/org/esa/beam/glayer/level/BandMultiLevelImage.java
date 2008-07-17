package org.esa.beam.glayer.level;

import com.bc.layer.level.AbstractMultiLevelImage;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class BandMultiLevelImage extends AbstractMultiLevelImage {

    private final RasterDataNode[] rasterDataNodes;

    private final Rectangle2D boundingBox;


    public BandMultiLevelImage(RasterDataNode rasterDataNode, AffineTransform affineTransform) {
        this(new RasterDataNode[]{rasterDataNode}, affineTransform);
    }

    public BandMultiLevelImage(RasterDataNode rasterDataNode, AffineTransform affineTransform, int levelCount) {
        this(new RasterDataNode[]{rasterDataNode}, affineTransform, levelCount);
    }

    public BandMultiLevelImage(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform) {
        this(rasterDataNodes, affineTransform,
             ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                                               rasterDataNodes[0].getSceneRasterHeight()));
    }

    public BandMultiLevelImage(RasterDataNode[] rasterDataNodes, AffineTransform affineTransform, int levelCount) {
        super(affineTransform, levelCount);
        this.rasterDataNodes = rasterDataNodes.clone();
        this.boundingBox = getImageToModelTransform(0).createTransformedShape(new Rectangle(0, 0, rasterDataNodes[0].getSceneRasterWidth(), rasterDataNodes[0].getSceneRasterHeight())).getBounds2D();
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        return ImageManager.getInstance().createRgbImage(rasterDataNodes, level, getLevelCount());
    }

    @Override
    public Rectangle2D getBoundingBox(int level) {
        return boundingBox;
    }
}
