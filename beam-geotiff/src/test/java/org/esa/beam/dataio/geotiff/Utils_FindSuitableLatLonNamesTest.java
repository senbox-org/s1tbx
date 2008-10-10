package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class Utils_FindSuitableLatLonNamesTest {

    private Product product;

    @Before
    public void setup() {
        product = new Product("p", "t", 1, 1);
    }

    @Test
    public void testDefaultLatitudeNamesAreOccupied() {
        String[] tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude", tpNames[0]);
        assertEquals("longitude", tpNames[1]);

        product.addBand("latitude", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_tpg", tpNames[0]);
        assertEquals("longitude_tpg", tpNames[1]);

        product.addBand("latitude_tpg", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("lat", tpNames[0]);
        assertEquals("lon", tpNames[1]);

        product.addBand("lat", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("lat_tpg", tpNames[0]);
        assertEquals("lon_tpg", tpNames[1]);

        product.addBand("lat_tpg", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_1", tpNames[0]);
        assertEquals("longitude_1", tpNames[1]);

        product.addBand("latitude_1", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_2", tpNames[0]);
        assertEquals("longitude_2", tpNames[1]);
    }

    @Test
    public void testDefaultLongitudesNamesAreOccupied() {
        String[] tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude", tpNames[0]);
        assertEquals("longitude", tpNames[1]);

        product.addBand("longitude", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_tpg", tpNames[0]);
        assertEquals("longitude_tpg", tpNames[1]);

        product.addBand("longitude_tpg", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("lat", tpNames[0]);
        assertEquals("lon", tpNames[1]);

        product.addBand("lon", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("lat_tpg", tpNames[0]);
        assertEquals("lon_tpg", tpNames[1]);

        product.addBand("lon_tpg", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_1", tpNames[0]);
        assertEquals("longitude_1", tpNames[1]);

        product.addBand("longitude_1", ProductData.TYPE_INT8);

        tpNames = Utils.findSuitableLatLonNames(product);
        assertEquals("latitude_2", tpNames[0]);
        assertEquals("longitude_2", tpNames[1]);
    }
}
