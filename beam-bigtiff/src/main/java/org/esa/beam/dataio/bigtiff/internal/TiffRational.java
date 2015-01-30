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

package org.esa.beam.dataio.bigtiff.internal;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
class TiffRational extends TiffValue {

    private static final int NUMERATOR_INDEX = 0;
    private static final int DENOMINATOR_INDEX = 1;

    public TiffRational(final long numerator, final long denominator) {
        TiffValueRangeChecker.checkValueTiffRational(numerator, "numerator");
        TiffValueRangeChecker.checkValueTiffRational(denominator, "denominator");
        setData(ProductData.createInstance(ProductData.TYPE_UINT32, 2));
        getData().setElemUIntAt(NUMERATOR_INDEX, numerator);
        getData().setElemUIntAt(DENOMINATOR_INDEX, denominator);
    }

    public long getNumerator() {
        return getData().getElemUIntAt(NUMERATOR_INDEX);
    }

    public long getDenominator() {
        return getData().getElemUIntAt(DENOMINATOR_INDEX);
    }

    public double getValue() {
        return getNumerator() / getDenominator();
    }
}
