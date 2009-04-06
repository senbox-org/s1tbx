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

import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.opengis.layer.Style;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

abstract class WmsWorker extends SwingWorker<BufferedImage, Object> {

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
    protected BufferedImage doInBackground() throws Exception {
        WebMapServer wms = (WebMapServer) context.getPropertyValue(WmsLayerSource.PROPERTY_WMS);
        Layer selectedLayer = (Layer) context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_LAYER);
        Style selectedStyle = (Style) context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_STYLE);
        CRSEnvelope crsEnvelope = (CRSEnvelope) context.getPropertyValue(WmsLayerSource.PROPERTY_CRS_ENVELOPE);
        GetMapRequest mapRequest = wms.createGetMapRequest();
        mapRequest.addLayer(selectedLayer, selectedStyle);
        mapRequest.setTransparent(true);
        mapRequest.setDimensions(size.width, size.height);
        mapRequest.setSRS(crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
        mapRequest.setBBox(crsEnvelope);
        mapRequest.setFormat("image/png");
        return downloadWmsImage(mapRequest, wms);
    }

    private BufferedImage downloadWmsImage(GetMapRequest mapRequest, WebMapServer wms) throws IOException,
                                                                                              ServiceException {
        GetMapResponse mapResponse = wms.issueRequest(mapRequest);
        InputStream inputStream = mapResponse.getInputStream();
        try {
            return ImageIO.read(inputStream);
        } finally {
            inputStream.close();
        }
    }
}