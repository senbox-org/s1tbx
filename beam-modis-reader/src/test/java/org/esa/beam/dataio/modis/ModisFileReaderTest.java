package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.productdb.ModisBandDescription;
import org.esa.beam.dataio.modis.productdb.ModisSpectralInfo;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.Range;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ModisFileReaderTest extends TestCase {

    public void testGetTypeString_typeNull() {
        final Product product = new Product("Name", "PROD_TYPE", 5, 5);

        assertEquals("PROD_TYPE", ModisFileReader.getTypeString(null, product));
    }

    public void testGetTypeString_typeSupplied() {
        final Product product = new Product("Name", "PROD_TYPE", 5, 5);

        assertEquals("TYPE_STRING", ModisFileReader.getTypeString("TYPE_STRING", product));
    }

    public void testCreateRangeFromArray_nullArray() {
        final Range range = ModisFileReader.createRangeFromArray(null);
        assertNull(range);
    }

    public void testCreateRangeFromArray_tooShortArray() {
        final Range range = ModisFileReader.createRangeFromArray(new int[]{34});
        assertNull(range);
    }

    public void testCreateRangeFromArray_orderedInts() {
        final Range range = ModisFileReader.createRangeFromArray(new int[]{34, 3809});
        assertNotNull(range);

        assertEquals(34.0, range.getMin(), 1e-8);
        assertEquals(3809.0, range.getMax(), 1e-8);
    }

    public void testCreateRangeFromArray_inverseOrderedInts() {
        final Range range = ModisFileReader.createRangeFromArray(new int[]{9886, 14});
        assertNotNull(range);

        assertEquals(14.0, range.getMin(), 1e-8);
        assertEquals(9886.0, range.getMax(), 1e-8);
    }

    public void testHasInvalidScaleAndOffset_invalidScale() {
        assertTrue(ModisFileReader.hasInvalidScaleAndOffset(new float[2], new float[4], 3));
    }

    public void testHasInvalidScaleAndOffset_invalidOffset() {
        assertTrue(ModisFileReader.hasInvalidScaleAndOffset(new float[4], new float[2], 3));
    }

    public void testHasInvalidScaleAndOffset() {
        assertFalse(ModisFileReader.hasInvalidScaleAndOffset(new float[4], new float[4], 3));
    }

    public void testSetSpectralBandInfo_notSpectral() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "false", "", "", "", "", "", "");

        ModisFileReader.setBandSpectralInformation(description, "", band);

        assertEquals(-1, band.getSpectralBandIndex());
        assertEquals(0.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(0.f, band.getSpectralBandwidth(), 1e-8);
    }

    public void testSetSpectralBandInfo_fromBandIndex() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "true", "", "", "", "", "", "");

        ModisFileReader.setBandSpectralInformation(description, "4", band);

        assertEquals(6, band.getSpectralBandIndex());
        assertEquals(555.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(20.f, band.getSpectralBandwidth(), 1e-8);
    }

    public void testSetSpectralBandInfo_fromSpecInfo() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "true", "", "", "", "", "", "");
        final ModisSpectralInfo spectralInfo = new ModisSpectralInfo("2", "3", "4");
        description.setSpecInfo(spectralInfo);

        ModisFileReader.setBandSpectralInformation(description, "", band);

        assertEquals(4, band.getSpectralBandIndex());
        assertEquals(2.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(3.f, band.getSpectralBandwidth(), 1e-8);
    }

    public void testIsEosGridType() throws IOException {
        final TestGlobalAttributes globalAttributes = new TestGlobalAttributes();

        globalAttributes.setEosType(ModisConstants.EOS_TYPE_GRID);
        assertTrue(ModisFileReader.isEosGridType(globalAttributes));

        globalAttributes.setEosType("EOS_invalid_and_ausgedacht");
        assertFalse(ModisFileReader.isEosGridType(globalAttributes));
    }

    public void testInvert() {
        final float[] scales = new float[] {24.7f, 0.f, -100.f};

        ModisFileReader.invert(scales);

        assertEquals(0.04048583f, scales[0], 1e-8);
        assertEquals(0.f, scales[1], 1e-8);
        assertEquals(-0.01f, scales[2], 1e-8);
    }

    ////////////////////////////////////////////////////////////////////////////////
    /////// INNER CLASS
    ////////////////////////////////////////////////////////////////////////////////


    class TestGlobalAttributes implements ModisGlobalAttributes {
        private String eosType;

        @Override
        public String getProductName() throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public String getProductType() throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public Dimension getProductDimensions(List<ucar.nc2.Dimension> netcdfFileDimensions) {
            throw new NotImplementedException();
        }

        @Override
        public HdfDataField getDatafield(String name) throws ProductIOException {
            throw new NotImplementedException();
        }

        @Override
        public Date getSensingStart() {
            throw new NotImplementedException();
        }

        @Override
        public Date getSensingStop() {
            throw new NotImplementedException();
        }

        @Override
        public int[] getSubsamplingAndOffset(String dimensionName) {
            throw new NotImplementedException();
        }

        @Override
        public boolean isImappFormat() {
            throw new NotImplementedException();
        }

        @Override
        public String getEosType() {
            return eosType;
        }

        public void setEosType(String eosType) {
            this.eosType = eosType;
        }

        @Override
        public GeoCoding createGeocoding() {
            throw new NotImplementedException();
        }
    }
}
