/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.layers.topsbursts;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.snap.framework.ui.layer.LayerSource;
import org.esa.snap.framework.ui.layer.LayerSourcePageContext;
import org.esa.snap.rcp.SnapApp;

/**

 */
public class TOPSBurstLayerSource implements LayerSource {

    public boolean isApplicable(LayerSourcePageContext pageContext) {
        final Product product = SnapApp.getDefault().getSelectedProductSceneView().getProduct();
        final Band band = product.getBand(SnapApp.getDefault().getSelectedProductSceneView().getRaster().getName());

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null && band != null) {
            final MetadataElement BurstBoundary = absRoot.getElement("BurstBoundary");
            if (BurstBoundary != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFirstPage() {
        return false;
    }

    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return null;
    }

    public boolean canFinish(LayerSourcePageContext pageContext) {
        return true;
    }

    public boolean performFinish(LayerSourcePageContext pageContext) {
        createLayer(pageContext);
        return true;
    }

    public void cancel(LayerSourcePageContext pageContext) {
    }

    public static void createLayer(final LayerSourcePageContext pageContext) {
        final RasterDataNode raster = SnapApp.getDefault().getSelectedProductSceneView().getRaster();
        final TOPSBurstsLayer geoLayer = TOPSBurstLayerType.createLayer(raster);
        pageContext.getLayerContext().getRootLayer().getChildren().add(0, geoLayer);
    }
}
