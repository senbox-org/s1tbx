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

package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.data.xy.XYDataset;


public class XYPlotToolTipGenerator extends CustomXYToolTipGenerator {

    @Override
    public String generateToolTip(XYDataset data, int series, int item) {
        final Comparable key = data.getSeriesKey(series);
        final double valueX = data.getXValue(series, item);
        final double valueY = data.getYValue(series, item);
        return String.format("%s: X = %6.2f, Y = %6.2f", key, valueX, valueY);
    }

}
