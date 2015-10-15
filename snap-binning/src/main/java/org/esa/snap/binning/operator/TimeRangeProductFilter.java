/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;/*
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

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.util.Date;

class TimeRangeProductFilter extends BinningProductFilter {

    private final Date rangeStart;
    private final Date rangeEnd;

    TimeRangeProductFilter(BinningProductFilter parent, ProductData.UTC rangeStart, ProductData.UTC rangeEnd) {
        setParent(parent);
        Assert.notNull(rangeStart, "rangeStart");
        Assert.notNull(rangeEnd, "rangeEnd");
        this.rangeStart = rangeStart.getAsDate();
        this.rangeEnd = rangeEnd.getAsDate();
    }

    @Override
    protected boolean acceptForBinning(Product product) {
        final ProductData.UTC productStart = product.getStartTime();
        final ProductData.UTC productEnd = product.getEndTime();

        if (productStart == null && productEnd == null) {
            // no product data at all
            return true;
        } else if (productStart == null || productEnd == null) {
            // only one product date given
            Date  productDate = productStart != null ? productStart.getAsDate() : productEnd.getAsDate();
            boolean isDateInRange = productDate.after(rangeStart) && productDate.before(rangeEnd);
            if (!isDateInRange) {
                setReason("Does not match the time range.");
            }
            return isDateInRange;
        } else if (productEnd.getAsDate().after(rangeStart) && productStart.getAsDate().before(rangeEnd)) {
            return true;
//        } else if (productStart != null && productStart.getAsDate().after(rangeStart)) {
//            setReason("Does not match the time range.");
//            return false;
//
//        } else if (productStart != null && productStart.getAsDate().before(rangeStart)
//            && productEnd != null && productEnd.getAsDate().before(rangeEnd)) {
//            return true;
//
//        } else if (productStart == null && productEnd.getAsDate().before(rangeEnd)) {
//            return true;
        }
        setReason("Does not match the time range.");
        return false;
    }
}
