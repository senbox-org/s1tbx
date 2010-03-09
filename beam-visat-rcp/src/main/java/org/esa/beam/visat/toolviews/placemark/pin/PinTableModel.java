/*
 * $Id: PinTableModel.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package org.esa.beam.visat.toolviews.placemark.pin;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.visat.toolviews.placemark.AbstractPlacemarkTableModel;

public class PinTableModel extends AbstractPlacemarkTableModel {

    public PinTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        super(placemarkDescriptor, product, selectedBands, selectedGrids);
    }

    @Override
    public String[] getStandardColumnNames() {
        return new String[]{"X", "Y", "Lon", "Lat", "Label"};
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (getProduct().getGeoCoding() == null &&
            (columnIndex == 2 || columnIndex == 3)) {
            return false;
        }
        
        return columnIndex < getStandardColumnNames().length;
    }

    @Override
    protected Object getStandardColumnValueAt(int rowIndex, int columnIndex) {
        Assert.notNull(getProduct());
        final Placemark placemark = getPlacemarkDescriptor().getPlacemarkGroup(getProduct()).get(rowIndex);

        float x = Float.NaN;
        float y = Float.NaN;

        final PixelPos pixelPos = placemark.getPixelPos();
        if (pixelPos != null) {
            x = pixelPos.x;
            y = pixelPos.y;
        }

        float lon = Float.NaN;
        float lat = Float.NaN;

        final GeoPos geoPos = placemark.getGeoPos();
        if (geoPos != null) {
            lon = geoPos.lon;
            lat = geoPos.lat;
        }

        switch (columnIndex) {
        case 0:
            return x;
        case 1:
            return y;
        case 2:
            return lon;
        case 3:
            return lat;
        case 4:
            return placemark.getLabel();
        default:
            return "";
        }
    }
}