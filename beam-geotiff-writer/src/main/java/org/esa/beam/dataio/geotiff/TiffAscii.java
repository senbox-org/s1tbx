package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffAscii extends TiffValue {

    public TiffAscii(final String value) {
        Guardian.assertNotNullOrEmpty("value", value);
        setData(ProductData.createInstance(value + "|"));
    }

    public String getValue() {
        final String elemString = getData().getElemString();
        return elemString.substring(0, elemString.length() - 1);
    }
}
