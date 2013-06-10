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

package org.esa.beam.timeseries.export.text;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.visat.VisatApp;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
class TimeCsvExporter extends CsvExporter {

    private final List<List<Band>> variablesList;
    private final List<Placemark> pins;
    private final int level;
    private final boolean exportImageCoords = true;
    private final boolean exportLonLat = true;
    private final boolean exportUnit = true;

    TimeCsvExporter(List<List<Band>> rasterList, List<Placemark> pins, PrintWriter writer) {
        super(writer);
        this.variablesList = new ArrayList<List<Band>>(rasterList);
        this.pins = new ArrayList<Placemark>(pins);
        this.level = 0;
    }

    @Override
    void setUpHeader() {
        if (!variablesList.isEmpty()) {

        }
        header.add("Time Series Tool pin time series export table");
        header.add("");
        header.add("Product:\t" + resolveProductName());
        header.add("Created on:\t" + new Date());
    }

    private String resolveProductName() {
        for (List<Band> bandList : variablesList) {
            if (!bandList.isEmpty()) {
                for (Band band : bandList) {
                    if (band != null) {
                        return band.getProduct().getName();
                    }
                }
            }
        }
        return "Time Series Product";
    }

    @Override
    void setUpColumns() {
        columns.add("Name");
        if (exportImageCoords) {
            columns.add("X");
            columns.add("Y");
        }
        if (exportLonLat) {
            columns.add("Lon");
            columns.add("Lat");
        }
        columns.add("Variable");
        if (exportUnit) {
            columns.add("Unit");
        }
        // we assume all bandlists to contain the same time information, so the columns are built on the first
        // non-empty bandlist.
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

        for (List<Band> bandList : variablesList) {
            if (!bandList.isEmpty()) {
                for (Band band : bandList) {
                    TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
                    if (timeCoding != null) {
                        final Date date = timeCoding.getStartTime().getAsDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        columns.add(sdf.format(date));
                    }
                }
                break;
            }
        }
    }

    @Override
    void setUpRows(ProgressMonitor pm) {
        pm.beginTask("Exporting pin data as csv-file...", pins.size());
        for (Placemark pin : pins) {
            for (List<Band> bandList : variablesList) {
                if (!bandList.isEmpty()) {
                    rows.add(setUpRow(pin, bandList));
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private String setUpRow(Placemark pin, List<Band> bandList) {
        Band refBand = bandList.get(0);
        final StringBuilder row = new StringBuilder();
        row.append(pin.getLabel());
        row.append(getSeparator());
        PixelPos pixelPos = pin.getPixelPos();
        if (exportImageCoords) {
            exportImageCoords(row, pixelPos);
        }
        if (exportLonLat) {
            exportLatLon(refBand, row, pixelPos);
        }
        row.append(AbstractTimeSeries.rasterToVariableName(refBand.getName()));
        row.append(getSeparator());
        if (exportUnit) {
            row.append(refBand.getUnit());
            row.append(getSeparator());
        }
        for (int i = 0; i < bandList.size(); i++) {
            Band band = bandList.get(i);
            row.append(getValue(band, (int) pixelPos.x, (int) pixelPos.y, level));
            if (i < bandList.size() - 1) {
                row.append(getSeparator());
            }
        }
        return row.toString();
    }

    private void exportLatLon(Band refBand, StringBuilder row, PixelPos pixelPos) {
        final GeoPos geoPos = new GeoPos();
        refBand.getGeoCoding().getGeoPos(pixelPos, geoPos);
        row.append(geoPos.getLon());
        row.append(getSeparator());
        row.append(geoPos.getLat());
        row.append(getSeparator());
    }

    private void exportImageCoords(StringBuilder row, PixelPos pixelPos) {
        DecimalFormat formatter = new DecimalFormat("0.000");
        row.append(formatter.format(pixelPos.getX()));
        row.append(getSeparator());
        row.append(formatter.format(pixelPos.getY()));
        row.append(getSeparator());
    }

    @Override
    String getSeparator() {
        return "\t";
    }
}
