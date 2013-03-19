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
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

abstract class CsvExporter {

    final List<String> header;
    final List<String> columns;
    final List<String> rows;

    private final PrintWriter out;

    CsvExporter(PrintWriter writer) {
        this.out = writer;
        this.header = new ArrayList<String>();
        this.columns = new ArrayList<String>();
        this.rows = new ArrayList<String>();
    }

    void exportCsv(ProgressMonitor pm) {
        setUpHeader();
        setUpColumns();
        setUpRows(pm);
        StringBuilder builder = new StringBuilder();
        String sep = getSeparator();
        for (String headerString : header) {
            builder.append("#").append(headerString).append("\n");
        }
        builder.append("\n");
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            builder.append(column);
            if (i < columns.size() - 1) {
                builder.append(sep);
            }
        }
        builder.append("\n");
        try {
            out.print(builder.toString());
            for (String row : rows) {
                out.print(row);
                out.print("\n");
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    abstract void setUpHeader();

    abstract void setUpColumns();

    abstract void setUpRows(ProgressMonitor pm);

    abstract String getSeparator();

    static double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
        final RenderedImage image = raster.getGeophysicalImage().getImage(currentLevel);
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        final Raster data = image.getData(pixelRect);
        final MultiLevelImage validMaskImage = raster.getValidMaskImage();
        Raster validMaskData = null;
        if (validMaskImage != null) {
            final RenderedImage validMask = validMaskImage.getImage(currentLevel);
            validMaskData = validMask.getData(pixelRect);
        }
        final double value;
        if (validMaskData == null || validMaskData.getSample(pixelX, pixelY, 0) > 0) {
            value = data.getSampleDouble(pixelX, pixelY, 0);
        } else {
            value = Double.NaN;
        }
        return value;
    }
}
