package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class EnvisatProductReaderTest {

    private EnvisatProductReaderPlugIn readerPlugIn;

    @Before
    public void setUp() {
        readerPlugIn = new EnvisatProductReaderPlugIn();
    }

    @Test
    public void testAatsrGeoLocationRefersToLowerLeftCornerOfPixel() throws IOException, URISyntaxException {
        final EnvisatProductReader reader = (EnvisatProductReader) readerPlugIn.createReaderInstance();

        try {
            final Product product = reader.readProductNodes(
                    new File(getClass().getResource(
                            "ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.N1").toURI()), null);
            final TiePointGrid latGrid = product.getTiePointGrid("latitude");
            final TiePointGrid lonGrid = product.getTiePointGrid("longitude");
            assertNotNull(latGrid);
            assertNotNull(lonGrid);

            final ProductFile productFile = reader.getProductFile();

            assertTrue(productFile.storesPixelsInChronologicalOrder());

            float offsetX = latGrid.getOffsetX();

            assertEquals(-18.0f, offsetX, 0.0f);

            float offsetY = latGrid.getOffsetY();

            assertEquals(1.0f, offsetY, 0.0f);

            float subSamplingX = latGrid.getSubSamplingX();

            assertEquals(25.0f, subSamplingX, 0.0f);

            float subSamplingY = latGrid.getSubSamplingY();

            assertEquals(32.0f, subSamplingY, 0.0f);

            final GeoPos geoPos = product.getGeoCoding().getGeoPos(new PixelPos(-18.0f + 25.0f, 1.0f), null);

            assertEquals(latGrid.getTiePoints()[1], geoPos.getLat(), 0.0f);
            assertEquals(lonGrid.getTiePoints()[1], geoPos.getLon(), 0.0f);
        } finally {
            reader.close();
        }
    }
}
