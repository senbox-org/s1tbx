package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 1.1 $ $Date: 2006/09/14 13:19:21 $
 */
class TiffShort extends TiffValue {

    public TiffShort(final int value) {
        TiffValueRangeChecker.checkValueTiffShort(value, "value");
        setData(ProductData.createInstance(ProductData.TYPE_UINT16));
        getData().setElemUInt(value);
    }

    public int getValue() {
        return getData().getElemInt();
    }
}
