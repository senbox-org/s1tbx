package org.esa.beam.glevel;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;

public class BandImageMultiLevelSource extends AbstractMultiLevelSource {

    private final RasterDataNode[] rasterDataNodes;

    public static MultiLevelSource create(RasterDataNode rasterDataNode,
                                          AffineTransform i2mTransform) {
        return create(new RasterDataNode[] { rasterDataNode }, i2mTransform);
    }

    public static MultiLevelSource create(RasterDataNode rasterDataNode,
                                          AffineTransform i2mTransform, int levelCount) {
        return create(new RasterDataNode[] { rasterDataNode }, i2mTransform, levelCount);
    }

    public static MultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                          AffineTransform i2mTransform) {
        return create(rasterDataNodes, i2mTransform,
                      ImageManager.computeMaxLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                                                        rasterDataNodes[0].getSceneRasterHeight()));
    }

    private static MultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                           AffineTransform i2mTransform, int levelCount) {
        Assert.notNull(rasterDataNodes);
        Assert.argument(rasterDataNodes.length > 0);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        MultiLevelModel model = new DefaultMultiLevelModel(levelCount, i2mTransform, w, h);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, levelCount);
        return new BandImageMultiLevelSource(model, rasterDataNodes);
    }

    public BandImageMultiLevelSource(MultiLevelModel model, RasterDataNode[] rasterDataNodes) {
        super(model);
        this.rasterDataNodes = rasterDataNodes.clone();
    }

    @Override
    public RenderedImage createImage(int level) {
        return ImageManager.getInstance().createRgbImage(rasterDataNodes, level,
                                                         getModel().getLevelCount());
    }
}
