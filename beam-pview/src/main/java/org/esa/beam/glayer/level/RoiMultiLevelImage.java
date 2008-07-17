package org.esa.beam.glayer.level;

import com.bc.layer.level.AbstractMultiLevelImage;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class RoiMultiLevelImage extends AbstractMultiLevelImage {

    private final RasterDataNode rasterDataNode;
    private final Color color;
    private final Rectangle2D boundingBox;

    public RoiMultiLevelImage(RasterDataNode rasterDataNode, Color color, AffineTransform affineTransform) {
        super(affineTransform, ImageManager.computeMaxLevelCount(rasterDataNode.getSceneRasterWidth(), rasterDataNode.getSceneRasterHeight()));
        this.rasterDataNode = rasterDataNode;
        this.color = color;
        this.boundingBox = getImageToModelTransform(0).createTransformedShape(new Rectangle(0, 0, rasterDataNode.getSceneRasterWidth(), rasterDataNode.getSceneRasterHeight())).getBounds2D();
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
    }

    @Override
    public Rectangle2D getBoundingBox(int level) {
        return boundingBox;
    }
}