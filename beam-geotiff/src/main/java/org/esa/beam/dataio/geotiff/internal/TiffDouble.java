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
class TiffDouble extends TiffValue {

    public TiffDouble(final double value) {
        setData(ProductData.createInstance(ProductData.TYPE_FLOAT64));
        getData().setElemDouble(value);
    }

    public double getValue() {
        return getData().getElemDouble();
    }
}
