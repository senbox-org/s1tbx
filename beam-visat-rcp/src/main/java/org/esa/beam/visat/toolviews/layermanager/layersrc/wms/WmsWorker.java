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

import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

abstract class WmsWorker extends SwingWorker<BufferedImage, Object> {

    private final Dimension size;
    private final WmsModel wmsModel;

    WmsWorker(Dimension size, WmsModel wmsModel) {
        this.size = size;
        this.wmsModel = wmsModel;
    }

    @Override
    protected BufferedImage doInBackground() throws Exception {
        GetMapRequest mapRequest = wmsModel.wms.createGetMapRequest();
        mapRequest.addLayer(wmsModel.selectedLayer, wmsModel.selectedStyle);
        mapRequest.setTransparent(true);
        mapRequest.setDimensions(size.width, size.height);
        mapRequest.setSRS(wmsModel.crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
        mapRequest.setBBox(wmsModel.crsEnvelope); // todo - adjust crsEnvelope to exactly match dimensions w x h (nf)
        mapRequest.setFormat("image/png");
        return downloadWmsImage(mapRequest);
    }
    
    private BufferedImage downloadWmsImage(GetMapRequest mapRequest) throws IOException, ServiceException {
        GetMapResponse mapResponse = wmsModel.wms.issueRequest(mapRequest);
        InputStream inputStream = mapResponse.getInputStream();
        try {
            return ImageIO.read(inputStream);
        } finally {
            inputStream.close();
        }
    }
}