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
package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.wms.WebMapServer;

import java.awt.Dimension;
import java.net.URL;
import java.util.List;

abstract class WmsWorker extends ProgressMonitorSwingWorker<com.bc.ceres.glayer.Layer, Object> {

    private final LayerSourcePageContext context;
    private final Dimension mapImageSize;

    protected WmsWorker(LayerSourcePageContext context, Dimension mapImageSize) {
        super(context.getWindow(), "WMS Access");
        this.context = context;
        this.mapImageSize = mapImageSize;
    }

    public LayerSourcePageContext getContext() {
        return context;
    }

    @Override
    protected com.bc.ceres.glayer.Layer doInBackground(ProgressMonitor pm) throws Exception {

        try {
            pm.beginTask("Loading layer from WMS", ProgressMonitor.UNKNOWN);

            final LayerType wmsType = LayerTypeRegistry.getLayerType(WmsLayerType.class.getName());
            final PropertySet template = wmsType.createLayerConfig(getContext().getLayerContext());

            final RasterDataNode raster = getContext().getAppContext().getSelectedProductSceneView().getRaster();
            template.setValue(WmsLayerType.PROPERTY_NAME_RASTER, raster);
            template.setValue(WmsLayerType.PROPERTY_NAME_IMAGE_SIZE, mapImageSize);
            URL wmsUrl = (URL) context.getPropertyValue(WmsLayerSource.PROPERTY_NAME_WMS_URL);
            template.setValue(WmsLayerType.PROPERTY_NAME_URL, wmsUrl);
            StyleImpl selectedStyle = (StyleImpl) context.getPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_STYLE);
            String styleName = null;
            if (selectedStyle != null) {
                styleName = selectedStyle.getName();
            }
            template.setValue(WmsLayerType.PROPERTY_NAME_STYLE_NAME, styleName);
            WebMapServer wms = (WebMapServer) context.getPropertyValue(WmsLayerSource.PROPERTY_NAME_WMS);
            final List<Layer> layerList = wms.getCapabilities().getLayerList();
            Layer selectedLayer = (Layer) context.getPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_LAYER);
            template.setValue(WmsLayerType.PROPERTY_NAME_LAYER_INDEX, layerList.indexOf(selectedLayer));
            CRSEnvelope crsEnvelope = (CRSEnvelope) context.getPropertyValue(WmsLayerSource.PROPERTY_NAME_CRS_ENVELOPE);
            template.setValue(WmsLayerType.PROPERTY_NAME_CRS_ENVELOPE, crsEnvelope);
            final com.bc.ceres.glayer.Layer layer = wmsType.createLayer(getContext().getLayerContext(), template);
            layer.setName(selectedLayer.getName());
            return layer;
        } finally {
            pm.done();
        }
    }
}