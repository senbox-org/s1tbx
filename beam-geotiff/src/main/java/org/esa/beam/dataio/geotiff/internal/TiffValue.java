package org.esa.beam.dataio.geotiff.internal;

import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * An abstract TIFF value implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public abstract class TiffValue {

    private ProductData data;

    protected void setData(final ProductData data) {
        this.data = data;
    }

    protected ProductData getData() {
        return data;
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (data == null) {
            throw new IllegalStateException("the value has no data to write");
        }
        data.writeTo(ios);
    }

    public int getSizeInBytes() {
        return data.getElemSize() * data.getNumElems();
    }
}
