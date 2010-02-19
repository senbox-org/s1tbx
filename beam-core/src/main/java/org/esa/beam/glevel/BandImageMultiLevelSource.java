package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class BandImageMultiLevelSource extends AbstractMultiLevelSource {

    private final RasterDataNode[] rasterDataNodes;
    private ImageInfo imageInfo;

    public static BandImageMultiLevelSource create(RasterDataNode rasterDataNode, ProgressMonitor pm) {
        return create(new RasterDataNode[]{rasterDataNode}, pm);
    }

    public static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        RasterDataNode rdn = rasterDataNodes[0];
        final ImageManager imageManager = ImageManager.getInstance();
        MultiLevelModel model = ImageManager.getMultiLevelModel(rdn);
        imageManager.prepareImageInfos(rasterDataNodes, pm);
        return new BandImageMultiLevelSource(model, rasterDataNodes);
    }

    public static BandImageMultiLevelSource create(RasterDataNode rasterDataNode,
                                                   AffineTransform i2mTransform, ProgressMonitor pm) {
        return create(new RasterDataNode[]{rasterDataNode}, i2mTransform, pm);
    }

    public static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                                   AffineTransform i2mTransform, ProgressMonitor pm) {
        return create(rasterDataNodes, i2mTransform,
                      DefaultMultiLevelModel.getLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                                                           rasterDataNodes[0].getSceneRasterHeight()), pm);
    }

    private static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                                    AffineTransform i2mTransform,
                                                    int levelCount,
                                                    ProgressMonitor pm) {
        Assert.notNull(rasterDataNodes);
        Assert.argument(rasterDataNodes.length > 0);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        MultiLevelModel model = new DefaultMultiLevelModel(levelCount, i2mTransform, w, h);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, pm);
        return new BandImageMultiLevelSource(model, rasterDataNodes);
    }

    private BandImageMultiLevelSource(MultiLevelModel model, RasterDataNode[] rasterDataNodes) {
        super(model);
        this.rasterDataNodes = rasterDataNodes.clone();
        imageInfo = ImageManager.getInstance().getImageInfo(rasterDataNodes);
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    @Override
    public Shape getImageShape(int level) {
        return rasterDataNodes[0].getSourceImage().getImageShape(level);
    }

    @Override
    public RenderedImage createImage(int level) {
        return ImageManager.getInstance().createColoredBandImage(rasterDataNodes, imageInfo, level);
    }
}
