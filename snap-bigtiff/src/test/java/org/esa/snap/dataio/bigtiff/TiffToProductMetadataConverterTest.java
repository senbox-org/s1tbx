package org.esa.snap.dataio.bigtiff;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.geotiff.GeoTIFFCodes;
import org.junit.Test;

import static org.junit.Assert.*;

public class TiffToProductMetadataConverterTest {

    @Test
    public void testGetModelTypeValueName() {
        ProductData modelTypeValueName = TiffToProductMetadataConverter.getModelTypeValueName(GeoTIFFCodes.ModelTypeProjected);
        assertNotNull(modelTypeValueName);
        assertEquals("ModelTypeProjected", modelTypeValueName.getElemString());

        modelTypeValueName = TiffToProductMetadataConverter.getModelTypeValueName(GeoTIFFCodes.ModelTypeGeographic);
        assertNotNull(modelTypeValueName);
        assertEquals("ModelTypeGeographic", modelTypeValueName.getElemString());

        modelTypeValueName = TiffToProductMetadataConverter.getModelTypeValueName(GeoTIFFCodes.ModelTypeGeocentric);
        assertNotNull(modelTypeValueName);
        assertEquals("ModelTypeGeocentric", modelTypeValueName.getElemString());

        modelTypeValueName = TiffToProductMetadataConverter.getModelTypeValueName(-11);
        assertNotNull(modelTypeValueName);
        assertEquals("unknown", modelTypeValueName.getElemString());
    }

    @Test
    public void testGetRasterTypeValueName() {
        ProductData rasterTypeValueName = TiffToProductMetadataConverter.getRasterTypeValueName(GeoTIFFCodes.RasterPixelIsArea);
        assertNotNull(rasterTypeValueName);
        assertEquals("RasterPixelIsArea", rasterTypeValueName.getElemString());

        rasterTypeValueName = TiffToProductMetadataConverter.getRasterTypeValueName(GeoTIFFCodes.RasterPixelIsPoint);
        assertNotNull(rasterTypeValueName);
        assertEquals("RasterPixelIsPoint", rasterTypeValueName.getElemString());

        rasterTypeValueName = TiffToProductMetadataConverter.getRasterTypeValueName(-12);
        assertNotNull(rasterTypeValueName);
        assertEquals("unknown", rasterTypeValueName.getElemString());

    }
}
