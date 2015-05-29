package org.esa.s1tbx.dat.actions;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.product.ProductSceneImage;

/**

 */
public class ShowImageViewNestAction  {

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
     /*   final ProductSceneImage image = super.createProductSceneImage(raster, pm);

        final Layer rootLayer = image.getRootLayer();
        Layer layer = MapToolsLayer.findMapToolsLayer(rootLayer);
        if (layer == null) {
            final MapToolsOptions options = new MapToolsOptions();
            layer = MapToolsLayerType.createLayer(raster, options);
            rootLayer.getChildren().add(0, layer);
        }
        layer.setVisible(true);

        return image;*/
        return null;
    }
}
