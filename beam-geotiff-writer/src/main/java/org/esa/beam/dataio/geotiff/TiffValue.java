package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * An abstract TIFF value implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
abstract class TiffValue {

    private ProductData _data;

    protected void setData(final ProductData data) {
        _data = data;
    }

    protected ProductData getData() {
        return _data;
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (_data == null) {
            throw new IllegalStateException("the value has no data to write");
        }
        _data.writeTo(ios);
    }

    public int getSizeInBytes() {
        return _data.getElemSize() * _data.getNumElems();
    }
}
