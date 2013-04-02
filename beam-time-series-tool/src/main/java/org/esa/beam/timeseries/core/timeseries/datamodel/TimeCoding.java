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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;

/**
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 * <p/>
 * Abstract class representing a time-coding. A time-coding is defined by a start and an end time and thus represents
 * a time span. It maps time information to pixel-positions.
 */
public abstract class TimeCoding {

    private final ProductData.UTC startTime;
    private final ProductData.UTC endTime;

    /**
     * Constructor creates a new TimeCoding-instance with a given start and end time.
     *
     * @param startTime the start time of the time span represented by the time-coding
     * @param endTime   the end time of the time span represented by the time-coding
     */
    protected TimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        Guardian.assertNotNull("startTime", startTime);
        Guardian.assertNotNull("endTime", endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Allows to retrieve time information for a given pixel.
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the time at the given pixel position, can be {@code null} if time can not be determined.
     */
    public abstract ProductData.UTC getTime(final PixelPos pos);

    /**
     * Getter for the start time
     *
     * @return the start time, may be {@code null}
     */
    public ProductData.UTC getStartTime() {
        return startTime;
    }

    /**
     * Getter for the end time
     *
     * @return the end time, may be {@code null}
     */
    public ProductData.UTC getEndTime() {
        return endTime;
    }

    /**
     * Checks if the given {@code timeCoding} is within the start and end time of this {@link org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding}.
     *
     * @param timeCoding the time coding to check if it is within this {@code TimeCoding}
     *
     * @return whether this {@code TimeCoding} contains the given time coding
     */
    public boolean contains(TimeCoding timeCoding) {
        if (getStartTime().getAsDate().after(timeCoding.getStartTime().getAsDate())) {
            return false;
        }
        if (getEndTime().getAsDate().before(timeCoding.getEndTime().getAsDate())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeCoding that = (TimeCoding) o;

        boolean startEqual = areEqual(startTime, that.startTime);
        boolean endEqual = areEqual(endTime, that.endTime);
        return startEqual && endEqual;
    }

    private boolean areEqual(ProductData.UTC time1, ProductData.UTC time2) {
        return time1.getAsDate().getTime() == time2.getAsDate().getTime();

    }

    @Override
    public int hashCode() {
        return 31 * startTime.hashCode() + endTime.hashCode();
    }
}