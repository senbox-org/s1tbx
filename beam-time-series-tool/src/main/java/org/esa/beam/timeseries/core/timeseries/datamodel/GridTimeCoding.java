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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 * <p/>
 * Grid implementation of {@link TimeCoding}. It simply returns the central value of start and end time.
 *
 * @author Thomas Storm
 */
public class GridTimeCoding extends TimeCoding {

    /**
     * Constructor for a GridTimeCoding with only a single time point given.
     * This means startTime == endTime == timePoint.
     *
     * @param timePoint the time point
     */
    private GridTimeCoding(ProductData.UTC timePoint) {
        super(timePoint, timePoint);
    }

    /**
     * Constructor for a GridTimeCoding.
     *
     * @param startTime the start time
     * @param endTime   the end time
     */
    public GridTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        super(startTime, endTime);
    }

    /**
     * Returns the central time of start and end time.
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the  time at the given pixel position
     */
    @Override
    public ProductData.UTC getTime(PixelPos pos) {
        final ProductData.UTC startTime = getStartTime();
        final ProductData.UTC endTime = getEndTime();

        final double dStart = startTime.getMJD();
        final double dEnd = endTime.getMJD();
        final double dCentral = (dEnd - dStart) / 2;
        return new ProductData.UTC(dCentral);
    }

    /**
     * Factory method for creating a GridTimeCoding-instance from a product.
     *
     * @param product the product to create the time coding from. Its start time must not be <code>null</code>.
     *
     * @return a TimeCoding instance
     */
    public static TimeCoding create(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime != null) {
            return new GridTimeCoding(startTime, endTime);
        } else {
            return new GridTimeCoding(startTime);
        }
    }

}

