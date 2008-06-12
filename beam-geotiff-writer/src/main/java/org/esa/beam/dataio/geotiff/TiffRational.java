package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffRational extends TiffValue {

    private static final int _NUMERATOR_INDEX = 0;
    private static final int _DENOMINATOR_INDEX = 1;

    public TiffRational(final long numerator, final long denominator) {
        TiffValueRangeChecker.checkValueTiffRational(numerator, "numerator");
        TiffValueRangeChecker.checkValueTiffRational(denominator, "denominator");
        setData(ProductData.createInstance(ProductData.TYPE_UINT32, 2));
        getData().setElemUIntAt(_NUMERATOR_INDEX, numerator);
        getData().setElemUIntAt(_DENOMINATOR_INDEX, denominator);
    }

    public long getNumerator() {
        return getData().getElemUIntAt(_NUMERATOR_INDEX);
    }

    public long getDenominator() {
        return getData().getElemUIntAt(_DENOMINATOR_INDEX);
    }

    public double getValue() {
        return getNumerator() / getDenominator();
    }
}
