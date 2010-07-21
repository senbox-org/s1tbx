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

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.layer.LayerSource;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;


public class WmsLayerSource implements LayerSource {

    static final String PROPERTY_NAME_WMS = "wms";
    static final String PROPERTY_NAME_WMS_URL = "wmsUrl";
    static final String PROPERTY_NAME_WMS_CAPABILITIES = "wmsCapabilities";
    static final String PROPERTY_NAME_SELECTED_LAYER = "selectedLayer";
    static final String PROPERTY_NAME_SELECTED_STYLE = "selectedStyle";
    static final String PROPERTY_NAME_CRS_ENVELOPE = "crsEnvelope";

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        ProductSceneView view = pageContext.getAppContext().getSelectedProductSceneView();
        RasterDataNode raster = view.getRaster();
        return raster.getGeoCoding() instanceof MapGeoCoding || raster.getGeoCoding() instanceof CrsGeoCoding;
    }

    @Override
    public boolean hasFirstPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return new WmsAssistantPage1();
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }

    static void insertWmsLayer(LayerSourcePageContext pageContext) {
        ProductSceneView view = pageContext.getAppContext().getSelectedProductSceneView();
        RasterDataNode raster = view.getRaster();
        WmsLayerWorker layerWorker = new WmsLayerWorker(pageContext, raster);
        layerWorker.execute();   // todo - don't close dialog before image is downloaded! (nf)
    }
}
