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

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;


public class WmsLayerSource implements LayerSource {

    static final String PROPERTY_WMS = "WmsLayerSource.wms";
    static final String PROPERTY_WMS_CAPABILITIES = "WmsLayerSource.wmsCapabilities";
    static final String PROPERTY_SELECTED_LAYER = "WmsLayerSource.selectedLayer";
    static final String PROPERTY_SELECTED_STYLE = "WmsLayerSource.selectedStyle";
    static final String PROPERTY_CRS_ENVELOPE = "WmsLayerSource.crsEnvelope";

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return true;
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

        WmsLayerWorker layerWorker = new WmsLayerWorker(view.getRootLayer(),
                                                        raster,
                                                        pageContext);
        layerWorker.execute();   // todo - don't close dialog before image is downloaded! (nf)
    }
}