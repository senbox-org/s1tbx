package org.esa.s1tbx.dat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import org.esa.s1tbx.dat.layers.maptools.MapToolsLayer;
import org.esa.s1tbx.dat.layers.maptools.MapToolsLayerType;
import org.esa.s1tbx.dat.layers.maptools.MapToolsOptions;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.product.ProductSceneImage;
import org.esa.snap.visat.actions.ShowImageViewAction;

/**

 */
public class ShowImageViewNestAction extends ShowImageViewAction {

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
        final ProductSceneImage image = super.createProductSceneImage(raster, pm);

        final Layer rootLayer = image.getRootLayer();
        Layer layer = MapToolsLayer.findMapToolsLayer(rootLayer);
        if (layer == null) {
            final MapToolsOptions options = new MapToolsOptions();
            layer = MapToolsLayerType.createLayer(raster, options);
            rootLayer.getChildren().add(0, layer);
        }
        layer.setVisible(true);

        return image;
    }
}
