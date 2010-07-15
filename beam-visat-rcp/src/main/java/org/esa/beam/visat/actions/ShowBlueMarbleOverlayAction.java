package org.esa.beam.visat.actions;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;


public class ShowBlueMarbleOverlayAction extends AbstractShowOverlayAction {

    private static final String BLUE_MARBLE_LAYER_TYPE_NAME = "BlueMarbleLayerType";
    private volatile LayerType blueMarbleLayerType;

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            Layer rootLayer = view.getRootLayer();
            Layer blueMarbleLayer = findBlueMarbleLayer(view);
            if (blueMarbleLayer == null) {
                blueMarbleLayer = createBlueMarbleLayer();
                rootLayer.getChildren().add(blueMarbleLayer);
            }
            blueMarbleLayer.setVisible(isSelected());
        }
    }

    private Layer createBlueMarbleLayer() {
        final LayerType layerType = getBlueMarbleLayerType();
        final PropertySet template = layerType.createLayerConfig(null);
        return layerType.createLayer(null, template);
    }


    @Override
    protected void updateEnableState(ProductSceneView view) {
        RasterDataNode raster = view.getRaster();
        GeoCoding geoCoding = raster.getGeoCoding();
        boolean isGeographic = false;
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                isGeographic = true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            isGeographic = CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }
        LayerType blueMarbleLayerType = getBlueMarbleLayerType();
        setEnabled(isGeographic && blueMarbleLayerType != null);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        Layer blueMarbleLayer = findBlueMarbleLayer(view);
        setSelected(blueMarbleLayer != null && blueMarbleLayer.isVisible());
    }

    private LayerType getBlueMarbleLayerType() {
        if (blueMarbleLayerType == null) {
            synchronized (this) {
                if (blueMarbleLayerType == null) {
                    blueMarbleLayerType =  LayerTypeRegistry.getLayerType(BLUE_MARBLE_LAYER_TYPE_NAME);
                }
            }
        }
        return blueMarbleLayerType;
    }

    private Layer findBlueMarbleLayer(ProductSceneView view) {
        final LayerType layerType = getBlueMarbleLayerType();
        return LayerUtils.getChildLayer(view.getRootLayer(), LayerUtils.SearchMode.DEEP, new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layerType == layer.getLayerType();
            }
        });
    }

}
