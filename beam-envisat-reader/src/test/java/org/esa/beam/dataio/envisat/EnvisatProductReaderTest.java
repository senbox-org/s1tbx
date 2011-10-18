package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EnvisatProductReaderTest {

    private EnvisatProductReaderPlugIn readerPlugIn;

    @Before
    public void setUp() {
        readerPlugIn = new EnvisatProductReaderPlugIn();
    }

    @Test
    public void testAatsrGeoLocationRefersToLowerLeftCornerOfPixel() throws IOException, URISyntaxException {
        final ProductReader reader = readerPlugIn.createReaderInstance();
        try {
            final Product product = reader.readProductNodes(
                    new File(getClass().getResource(
                            "ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.N1").toURI()), null);
            final TiePointGrid latGrid = product.getTiePointGrid("latitude");
            final TiePointGrid lonGrid = product.getTiePointGrid("longitude");
            assertNotNull(latGrid);
            assertNotNull(lonGrid);

            float offsetX = latGrid.getOffsetX();
            float offsetY = latGrid.getOffsetY();
            float subSamplingX = latGrid.getSubSamplingX();
            float subSamplingY = latGrid.getSubSamplingY();

            assertEquals(-19.0f, offsetX, 0.0f);
            assertEquals(1.0f, offsetY, 0.0f);
            assertEquals(25.0f, subSamplingX, 0.0f);
            assertEquals(32.0f, subSamplingY, 0.0f);

            final GeoPos geoPos = product.getGeoCoding().getGeoPos(new PixelPos(6.0f, 1.0f), null);

            assertEquals(latGrid.getTiePoints()[1], geoPos.getLat(), 0.0f);
            assertEquals(lonGrid.getTiePoints()[1], geoPos.getLon(), 0.0f);

            assertEquals(latGrid.getOffsetY(), 1.0f, 0.0f);
            assertEquals(lonGrid.getOffsetY(), 1.0f, 0.0f);
        } finally {
            reader.close();
        }
    }
}
