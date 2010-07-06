package org.esa.beam.visat.actions;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
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

    private static final String TYPE_NAME = "BlueMarbleLayerType";
    private static final String LAYER_ID = "org.esa.beam.worldmap.BlueMarble";

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            Layer rootLayer = view.getRootLayer();
            Layer blueMarbleLayer = LayerUtils.getChildLayerById(rootLayer, LAYER_ID);
            if (blueMarbleLayer == null) {
                blueMarbleLayer = createBlueMarbleLayer();
                rootLayer.getChildren().add(blueMarbleLayer);
            }
            blueMarbleLayer.setVisible(isSelected());
        }
    }

    private Layer createBlueMarbleLayer() {
        final LayerType layerType = LayerTypeRegistry.getLayerType(TYPE_NAME);
        final PropertySet template = layerType.createLayerConfig(null);
        final Layer blueMarbleLayer = layerType.createLayer(null, template);
        blueMarbleLayer.setId(LAYER_ID);
        return blueMarbleLayer;
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
        LayerType blueMarbleLayerType = LayerTypeRegistry.getLayerType(TYPE_NAME);
        setEnabled(isGeographic && blueMarbleLayerType != null);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        Layer blueMarbleLayer = LayerUtils.getChildLayerById(view.getRootLayer(), LAYER_ID);
        setSelected(blueMarbleLayer != null && blueMarbleLayer.isVisible());
    }

}
