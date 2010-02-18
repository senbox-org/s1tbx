package org.esa.beam.dataio.geotiff.internal;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class TiffShort extends TiffValue {

    public TiffShort(final int value) {
        TiffValueRangeChecker.checkValueTiffShort(value, "value");
        setData(ProductData.createInstance(ProductData.TYPE_UINT16));
        getData().setElemUInt(value);
    }

    public int getValue() {
        return getData().getElemInt();
    }
}
