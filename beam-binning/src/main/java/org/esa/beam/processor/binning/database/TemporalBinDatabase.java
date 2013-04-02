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

package org.esa.beam.processor.binning.database;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.store.BinStoreFactory;

import java.awt.Point;
import java.io.IOException;
import java.util.Properties;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class TemporalBinDatabase extends AbstractBinDatabase {

    String dbName;

    public TemporalBinDatabase(L3Context context, String dbName) {
        this.context = context;
        this.dbName = dbName;
        this.locator = context.getLocator();
    }

    ////////////////////////////

    public void create() throws IOException {
        store = BinStoreFactory.getInstance().createTemporalStore(context, dbName, sumVarsPerBin());
        initializeMinMax();
    }

    public void open() throws IOException {
        store = BinStoreFactory.getInstance().openTemporalStore(context, dbName);
        readProperties();
    }

    public void close() throws IOException {
        writeProperties();
        store.close();
    }

    @Override
    public void delete() throws IOException {
        super.delete();
        context.deleteProperties(dbName + ".props");
    }

    public void read(Point rowCol, Bin bin) throws IOException {
        if (!locator.isValidPosition(rowCol)) {
            bin.clear();
        } else {
            store.read(rowCol, bin);
        }
    }

    public void write(Point rowCol, Bin bin) throws IOException {
        if (locator.isValidPosition(rowCol)) {
            updateUsedArea(rowCol);
            store.write(rowCol, bin);
        }
    }

    /**
     * Scans the borders of the database finding the corner coordinates in lat/lon.
     *
     * @param ul upper left <code>GeoPos</code> contains the upper left corner coordinate on return
     * @param ur upper right <code>GeoPos</code> contains the upper right corner coordinate on return
     * @param lr lower right <code>GeoPos</code> contains the lower right corner coordinate on return
     * @param ll lower left <code>GeoPos</code> contains the lower left corner coordinate on return
     */
    public void scanBorders(GeoPos ul, GeoPos ur, GeoPos lr, GeoPos ll, ProgressMonitor pm) throws IOException {
        final Bin bin = createBin();
        final Point rowCol = new Point();
        final GeoPos latLon = new GeoPos();
        final GeoPosArea area = new GeoPosArea();

        pm.beginTask("Scanning bin database", rowMax - rowMin);
        try {
            for (rowCol.y = rowMin; rowCol.y <= rowMax; rowCol.y++) {
                rowCol.x = colMin;
                while (rowCol.x <= colMax) {
                    read(rowCol, bin);
                    if (bin.containsData()) {
                        locator.getLatLon(rowCol, latLon);
                        area.update(latLon);
                        break;
                    }
                    ++rowCol.x;
                }

                rowCol.x = colMax;
                while (rowCol.x >= colMin) {
                    read(rowCol, bin);
                    if (bin.containsData()) {
                        locator.getLatLon(rowCol, latLon);
                        area.update(latLon);
                        break;
                    }
                    --rowCol.x;
                }
                pm.worked(1);
                if (pm.isCanceled()) {
                    break;
                }
            }

            ul.lat = area.latMax;
            ul.lon = area.lonMin;

            ur.lat = area.latMax;
            ur.lon = area.lonMax;

            lr.lat = area.latMin;
            lr.lon = area.lonMax;

            ll.lat = area.latMin;
            ll.lon = area.lonMin;
        } finally {
            pm.done();
        }
    }

    /**
     * Updates the row/col minimum and maximum values.
     *
     * @param rowCol the point to be checked
     */
    private void updateUsedArea(Point rowCol) {
        if (rowCol.y > rowMax) {
            rowMax = rowCol.y;
        }
        if (rowCol.y < rowMin) {
            rowMin = rowCol.y;
        }
        if (rowCol.x > colMax) {
            colMax = rowCol.x;
        }
        if (rowCol.x < colMin) {
            colMin = rowCol.x;
        }
        width = colMax - colMin + 1;
    }

    /**
     * Writes the database properties to disk.
     */
    private void writeProperties() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(ROW_MIN_KEY, String.valueOf(rowMin));
        properties.setProperty(ROW_MAX_KEY, String.valueOf(rowMax));
        properties.setProperty(COL_MIN_KEY, String.valueOf(colMin));
        properties.setProperty(COL_MAX_KEY, String.valueOf(colMax));

        context.saveProperties(properties, dbName + ".props");
    }

    /**
     * Read the database properties from disk.
     */
    private void readProperties() throws IOException {
        Properties properties = context.loadProperties(dbName + ".props");

        String value;

        value = properties.getProperty(ROW_MIN_KEY);
        rowMin = Integer.parseInt(value);

        value = properties.getProperty(ROW_MAX_KEY);
        rowMax = Integer.parseInt(value);

        value = properties.getProperty(COL_MIN_KEY);
        colMin = Integer.parseInt(value);

        value = properties.getProperty(COL_MAX_KEY);
        colMax = Integer.parseInt(value);

        width = colMax - colMin + 1;
    }

    static private class GeoPosArea {

        float latMin;
        float latMax;
        float lonMin;
        float lonMax;

        public GeoPosArea() {
            latMin = Float.MAX_VALUE;
            latMax = -Float.MAX_VALUE;
            lonMin = Float.MAX_VALUE;
            lonMax = -Float.MAX_VALUE;
        }

        /**
         * Checks the geopos passes in agains the area and updates the boundary.
         *
         * @param latLon the position to be checked
         */
        public void update(GeoPos latLon) {
            if (latLon.lat > latMax) {
                latMax = latLon.lat;
            }

            if (latLon.lat < latMin) {
                latMin = latLon.lat;
            }

            if (latLon.lon > lonMax) {
                lonMax = latLon.lon;
            }

            if (latLon.lon < lonMin) {
                lonMin = latLon.lon;
            }
        }
    }
}
