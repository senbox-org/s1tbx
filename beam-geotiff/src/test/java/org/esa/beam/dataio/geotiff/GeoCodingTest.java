package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import javax.imageio.stream.FileCacheImageInputStream;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class GeoCodingTest {

    @Test
    public void testSmallImageNearGreenwichMeridian() throws Exception {
        final URL resource = getClass().getResource("nearGreenwichMeridian.tif");
        final String filePath = resource.getFile();
        final GeoTiffProductReader reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        final Product product = reader.readGeoTIFFProduct(new FileCacheImageInputStream(resource.openStream(), null), new File(filePath));

        final GeoCoding geoCoding = product.getGeoCoding();
        final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        assertEquals(1.92584, ul.lon, 1.0e-5);
        assertEquals(48.28314, ul.lat, 1.0e-5);
        final GeoPos lr = geoCoding.getGeoPos(new PixelPos(49, 49), null);
        assertEquals(2.03596, lr.lon, 1.0e-5);
        assertEquals(48.17303, lr.lat, 1.0e-5);

    }
}