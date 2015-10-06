/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.ui.diagram.DiagramAxis;

import java.text.DateFormat;
import java.util.ArrayList;

/**
 * Shows product band date on axis
 */
public class DateAxis extends DiagramAxis {
    private TimeSeriesTimes times;
    private final DateFormat dateFormat = ProductData.UTC.createDateFormat("ddMMMyy");

    public DateAxis(String name, String unit) {
        super(name, unit);
    }

    public void setTimes(final TimeSeriesTimes times) {
        this.times = times;
    }

  /*  public void setSubDivision(double minValue, double maxValue, int numMajorTicks, int numMinorTicks) {
        setValueRange(minValue, maxValue);
        setNumMajorTicks(tickNames.length);
        setNumMinorTicks(3);
    }   */

    public String[] createTickmarkTexts() {
        final ArrayList<String> tickNames = new ArrayList<>(times.length());
        setName("Acquisition Date");
        int numTicks = getNumMajorTicks();
        double min = getMinValue();
        double dist = getMajorTickMarkDistance();

        for (int i = 0; i < numTicks; ++i) {
            final double mjd = min + (i * dist);
            final ProductData.UTC newTime = new ProductData.UTC(mjd);
            final String tickStr = dateFormat.format(newTime.getAsDate());
            tickNames.add(tickStr);
        }
        return tickNames.toArray(new String[tickNames.size()]);
    }
}
