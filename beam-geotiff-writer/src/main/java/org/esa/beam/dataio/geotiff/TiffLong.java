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
class TiffLong extends TiffValue {

    public TiffLong(final long value) {
        TiffValueRangeChecker.checkValueTiffLong(value, "value");
        setData(ProductData.createInstance(ProductData.TYPE_UINT32));
        getData().setElemUInt(value);
    }

    public long getValue() {
        return getData().getElemUInt();
    }

}
