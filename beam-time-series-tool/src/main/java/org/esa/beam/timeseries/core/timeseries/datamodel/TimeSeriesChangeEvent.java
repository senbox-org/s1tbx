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

/**
 * Event that gets fired when a time series has been changed.
 *
 * @author Thomas Storm
 */
public class TimeSeriesChangeEvent {

    public static final int BAND_TO_BE_REMOVED = 1;
    public static final int START_TIME_PROPERTY_NAME = BAND_TO_BE_REMOVED << 1;
    public static final int END_TIME_PROPERTY_NAME = START_TIME_PROPERTY_NAME << 1;
    public static final int PROPERTY_PRODUCT_LOCATIONS = END_TIME_PROPERTY_NAME << 1;
    public static final int PROPERTY_EO_VARIABLE_SELECTION = PROPERTY_PRODUCT_LOCATIONS << 1;
    public static final int INSITU_SOURCE_CHANGED = PROPERTY_EO_VARIABLE_SELECTION << 1;
    public static final int PROPERTY_INSITU_VARIABLE_SELECTION = INSITU_SOURCE_CHANGED << 1;
    public static final int PROPERTY_AXIS_MAPPING_CHANGED = PROPERTY_INSITU_VARIABLE_SELECTION << 1;

    private final int type;
    private final Object value;
    private final AbstractTimeSeries timeSeries;

    public TimeSeriesChangeEvent(int type, Object value, AbstractTimeSeries timeSeries) {
        this.type = type;
        this.value = value;
        this.timeSeries = timeSeries;
    }

    public int getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public AbstractTimeSeries getTimeSeries() {
        return timeSeries;
    }
}
