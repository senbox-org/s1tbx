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
class TiffAscii extends TiffValue {

    public TiffAscii(final String... values) {
        final StringBuffer buffer = new StringBuffer();
        for (String value : values) {
            buffer.append(value).append('\u0000');
        }
        setData(ProductData.createInstance(buffer.toString()));
    }

    public String getValue() {
        return getData().getElemString();
    }
}