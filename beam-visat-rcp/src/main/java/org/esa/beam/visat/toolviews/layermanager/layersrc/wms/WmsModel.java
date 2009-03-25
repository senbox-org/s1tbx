/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.opengis.layer.Style;

class WmsModel {
    private WebMapServer wms;
    private WMSCapabilities wmsCapabilities;
    private Layer selectedLayer;
    private Style selectedStyle;
    private CRSEnvelope crsEnvelope;

    WebMapServer getWms() {
        return wms;
    }

    void setWms(WebMapServer wms) {
        this.wms = wms;
    }

    WMSCapabilities getWmsCapabilities() {
        return wmsCapabilities;
    }

    void setWmsCapabilities(WMSCapabilities wmsCapabilities) {
        this.wmsCapabilities = wmsCapabilities;
    }

    Layer getSelectedLayer() {
        return selectedLayer;
    }

    void setSelectedLayer(Layer selectedLayer) {
        this.selectedLayer = selectedLayer;
    }

    Style getSelectedStyle() {
        return selectedStyle;
    }

    void setSelectedStyle(Style selectedStyle) {
        this.selectedStyle = selectedStyle;
    }

    CRSEnvelope getCrsEnvelope() {
        return crsEnvelope;
    }

    void setCrsEnvelope(CRSEnvelope crsEnvelope) {
        this.crsEnvelope = crsEnvelope;
    }
}
