package org.esa.beam.glevel;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;

public class RoiImageMultiLevelSource extends AbstractMultiLevelSource {

    private final RasterDataNode rasterDataNode;
    private final Color color;

    public static MultiLevelSource create(RasterDataNode rasterDataNode, Color color,
                                          AffineTransform imageToModelTransform) {
        Assert.notNull(rasterDataNode);
        Assert.notNull(color);
        final int width = rasterDataNode.getSceneRasterWidth();
        final int height = rasterDataNode.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(width, height);
        MultiLevelModel model = new DefaultMultiLevelModel(levelCount, imageToModelTransform,
                                                           width, height);
        return new RoiImageMultiLevelSource(model, rasterDataNode, color);
    }

    public RoiImageMultiLevelSource(MultiLevelModel model, RasterDataNode rasterDataNode,
                                     Color color) {
        super(model);
        this.rasterDataNode = rasterDataNode;
        this.color = color;
    }

    @Override
    public RenderedImage createLevelImage(int level) {
        return ImageManager.getInstance().createColoredRoiImage(rasterDataNode, color, level);
    }
}