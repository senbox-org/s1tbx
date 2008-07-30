package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.BandMultiLevelImage;

import java.io.IOException;
import java.awt.geom.AffineTransform;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
class ProductSceneImage45 extends ProductSceneImage {

    private Layer rootLayer;
    private BandMultiLevelImage levelImage;

    ProductSceneImage45(RasterDataNode raster, ProductSceneView45 view) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
        levelImage = view.getSceneImage45().getLevelImage();
        rootLayer = view.getRootLayer();
    }

    ProductSceneImage45(RasterDataNode raster) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo());
        levelImage = new BandMultiLevelImage(raster, new AffineTransform());
        initRootLayer();
    }

    ProductSceneImage45(RasterDataNode[] rasters) throws IOException {
        super("RGB", rasters, null);
        levelImage = new BandMultiLevelImage(rasters, new AffineTransform());
        initRootLayer();
    }

    private void initRootLayer() {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(levelImage);
        imageLayer.setName(getName());
        imageLayer.setConcurrent(true);
        imageLayer.setVisible(true);
        rootLayer.getChildLayers().add(imageLayer);
    }

    public Layer getRootLayer() {
        return rootLayer;
    }


    public BandMultiLevelImage getLevelImage() {
        return levelImage;
    }
}
