/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

class TimeRangeProductFilter extends BinningProductFilter {

    private final ProductData.UTC startTime;
    private final ProductData.UTC endTime;

    TimeRangeProductFilter(BinningProductFilter parent, ProductData.UTC startTime, ProductData.UTC endTime) {
        setParent(parent);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    protected boolean acceptForBinning(Product product) {
        final ProductData.UTC productStartTime = product.getStartTime();
        final ProductData.UTC productEndTime = product.getEndTime();
        final boolean hasStartTime = productStartTime != null;
        final boolean hasEndTime = productEndTime != null;
        if (startTime != null && hasStartTime && productStartTime.getAsDate().after(startTime.getAsDate())
            && endTime != null && hasEndTime && productEndTime.getAsDate().before(endTime.getAsDate())) {
            return true;
        } else if (!hasStartTime && !hasEndTime) {
            return true;
        } else if (startTime != null && hasStartTime && productStartTime.getAsDate().after(startTime.getAsDate()) && !hasEndTime) {
            return true;
        } else if (!hasStartTime && endTime != null && productEndTime.getAsDate().before(endTime.getAsDate())) {
            return true;
        }
        setReason("Does not match the time range.");
        return false;
    }
}
