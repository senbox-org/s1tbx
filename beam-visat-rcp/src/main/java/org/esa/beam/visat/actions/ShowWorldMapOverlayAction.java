/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
import org.esa.beam.glayer.WorldMapLayerType;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;


public class ShowWorldMapOverlayAction extends AbstractShowOverlayAction {

    private static final String WORLDMAP_TYPE_PROPERTY_NAME = "worldmap.type";
    private static final String GLOB_COVER_LAYER_TYPE = "GlobCoverLayerType";

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            Layer rootLayer = view.getRootLayer();
            Layer worldMapLayer = findWorldMapLayer(view);
            if (isSelected()) {
                if (worldMapLayer == null) {
                    worldMapLayer = createWorldMapLayer();
                    rootLayer.getChildren().add(worldMapLayer);
                }
                worldMapLayer.setVisible(true);
            } else {
                worldMapLayer.getParent().getChildren().remove(worldMapLayer);
            }
        }
    }

    private Layer createWorldMapLayer() {
        final LayerType layerType = getWorldMapLayerType();
        final PropertySet template = layerType.createLayerConfig(null);
        return layerType.createLayer(null, template);
    }


    @Override
    protected void updateEnableState(ProductSceneView view) {
        RasterDataNode raster = view.getRaster();
        GeoCoding geoCoding = raster.getGeoCoding();
        setEnabled(isGeographicLatLon(geoCoding));
    }

    private boolean isGeographicLatLon(GeoCoding geoCoding) {
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                return true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            return CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }
        return false;
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        Layer blueMarbleLayer = findWorldMapLayer(view);
        setSelected(blueMarbleLayer != null && blueMarbleLayer.isVisible());
    }

    private LayerType getWorldMapLayerType() {
        final VisatApp visatApp = VisatApp.getApp();
        String layerTypeClassName = visatApp.getPreferences().getPropertyString(WORLDMAP_TYPE_PROPERTY_NAME,
                                                                                GLOB_COVER_LAYER_TYPE);
        return LayerTypeRegistry.getLayerType(layerTypeClassName);
    }

    private Layer findWorldMapLayer(ProductSceneView view) {
        return LayerUtils.getChildLayer(view.getRootLayer(), LayerUtils.SearchMode.DEEP, new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer.getLayerType() instanceof WorldMapLayerType;
            }
        });
    }

}
