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

package org.esa.beam.visat.toolviews.placemark.gcp;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.visat.toolviews.placemark.AbstractPlacemarkTableModel;

public class GcpTableModel extends AbstractPlacemarkTableModel {

    public GcpTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        super(placemarkDescriptor, product, selectedBands, selectedGrids);
    }

    @Override
    public String[] getStandardColumnNames() {
        return new String[]{"X", "Y", "Lon", "Lat", "Delta Lon", "Delta Lat", "Label"};
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < getStandardColumnNames().length && columnIndex != 4 && columnIndex != 5;
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

        float dLon = Float.NaN;
        float dLat = Float.NaN;

        final GeoCoding geoCoding = getProduct().getGeoCoding();

        if (geoCoding instanceof GcpGeoCoding && pixelPos != null) {
            final GeoPos expectedGeoPos = geoCoding.getGeoPos(pixelPos, new GeoPos());
            if (expectedGeoPos != null) {
                dLon = Math.abs(lon - expectedGeoPos.lon);
                dLat = Math.abs(lat - expectedGeoPos.lat);
            }
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
            return dLon;
        case 5:
            return dLat;
        case 6:
            return placemark.getLabel();
        default:
            return "";
        }
    }
}
