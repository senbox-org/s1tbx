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
class TiffDouble extends TiffValue {

    public TiffDouble(final double value) {
        setData(ProductData.createInstance(ProductData.TYPE_FLOAT64));
        getData().setElemDouble(value);
    }

    public double getValue() {
        return getData().getElemDouble();
    }
}
