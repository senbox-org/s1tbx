package org.esa.beam.dataio.geotiff.internal;

/**
 * A TIFFValue implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
class GeoTiffAscii extends TiffAscii {

    public GeoTiffAscii(final String ... values) {
        super(appendTerminator(values));
    }

    private static String appendTerminator(String... values) {
        final StringBuffer buffer = new StringBuffer();
        for (String value : values) {
            buffer.append(value).append("|");
        }
        return buffer.toString();
    }
}
