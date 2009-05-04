/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.opengis.layer.Style;

import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.net.URL;
import java.util.List;

abstract class WmsWorker extends SwingWorker<com.bc.ceres.glayer.Layer, Object> {

    private final Dimension size;
    private final LayerSourcePageContext context;

    WmsWorker(Dimension size, LayerSourcePageContext context) {
        this.size = size;
        this.context = context;
    }

    public LayerSourcePageContext getContext() {
        return context;
    }

    @Override
    protected com.bc.ceres.glayer.Layer doInBackground() throws Exception {
        final LayerType wmsType = LayerType.getLayerType(WmsLayerType.class.getName());
        final ValueContainer template = wmsType.getConfigurationTemplate();

        final RasterDataNode raster = getContext().getAppContext().getSelectedProductSceneView().getRaster();
        template.setValue(WmsLayerType.PROPERTY_WMS_RASTER, raster);
        template.setValue(WmsLayerType.PROPERTY_WMS_IMAGE_SIZE, size);
        URL wmsUrl = (URL) context.getPropertyValue(WmsLayerSource.PROPERTY_WMS_URL);
        template.setValue(WmsLayerType.PROPERTY_WMS_URL, wmsUrl);
        Style selectedStyle = (Style) context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_STYLE);
        String styleName = null;
        if (selectedStyle != null) {
            styleName = selectedStyle.getName();
        }
        template.setValue(WmsLayerType.PROPERTY_WMS_STYLE_NAME, styleName);
        WebMapServer wms = (WebMapServer) context.getPropertyValue(WmsLayerSource.PROPERTY_WMS);
        final List<Layer> layerList = wms.getCapabilities().getLayerList();
        Layer selectedLayer = (Layer) context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_LAYER);
        template.setValue(WmsLayerType.PROPERTY_WMS_LAYER_INDEX, layerList.indexOf(selectedLayer));
        CRSEnvelope crsEnvelope = (CRSEnvelope) context.getPropertyValue(WmsLayerSource.PROPERTY_CRS_ENVELOPE);
        template.setValue(WmsLayerType.PROPERTY_WMS_CRSENVELOPE, crsEnvelope);
        final com.bc.ceres.glayer.Layer layer = wmsType.createLayer(getContext().getLayerContext(), template);
        layer.setName(selectedLayer.getName());
        return layer;
    }
}