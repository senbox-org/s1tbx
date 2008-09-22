package org.esa.beam.dataio.geotiff.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.jdom.Document;
import org.jdom.Element;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class BeamMetadataTest_0_1 {

    private Product product;
    private BeamMetadata.ProductMetadata productMetadata;
    private BeamMetadata.DomMetadata_0_1 domMetadata;

    @Before
    public void setup() {
        product = new Product("ProductName", "ProductType", 3, 5);
        init();
    }

    private void init() {
        productMetadata = (BeamMetadata.ProductMetadata) BeamMetadata.createMetadata(product);
        final Document dom = productMetadata.getDocument();
        domMetadata = (BeamMetadata.DomMetadata_0_1) BeamMetadata.createMetadata(dom);
    }

    @Test
    public void testIsBeamMetadata() {
        assertEquals(true, BeamMetadata.isBeamMetadata(productMetadata.getDocument()));

        Document otherDom = null;
        assertEquals(false, BeamMetadata.isBeamMetadata(otherDom));
        otherDom = new Document();
        otherDom.setRootElement(new Element("blahblah"));
        assertEquals(false, BeamMetadata.isBeamMetadata(otherDom));
        otherDom.getRootElement().setName("beam_metadata");
        assertEquals(false, BeamMetadata.isBeamMetadata(otherDom));
        otherDom.getRootElement().setAttribute("lsmf", "erg");
        assertEquals(false, BeamMetadata.isBeamMetadata(otherDom));
        otherDom.getRootElement().setAttribute("version", "1.0");
        assertEquals(true, BeamMetadata.isBeamMetadata(otherDom));
    }

    @Test
    public void testConstructorWithBadDom() {
        final Document badDom = new Document(new Element("blahblah"));
        try {
            BeamMetadata.createMetadata(badDom);
            fail("Exception expected");
        } catch (IllegalArgumentException ignore) {
            // ignore
        }
    }

    @Test
    public void testProductProperties() {

        String name = BeamMetadata.NODE_NAME;
        assertEquals(product.getName(), domMetadata.getProductProperty(name));
        assertEquals(product.getName(), productMetadata.getProductProperty(name));

        name = BeamMetadata.NODE_PRODUCTTYPE;
        assertEquals(product.getProductType(), domMetadata.getProductProperty(name));
        assertEquals(product.getProductType(), productMetadata.getProductProperty(name));
    }

    @Test
    public void testBandProperties() {
        final Band band = product.addBand("BandUInt16", ProductData.TYPE_UINT16);
        band.setScalingFactor(3.21);
        band.setScalingOffset(1.23);
        init();

        final int bandindex = 0;

        String name = BeamMetadata.NODE_NAME;
        assertEquals(band.getName(), domMetadata.getBandProperty(bandindex, name));
        assertEquals(band.getName(), productMetadata.getBandProperty(bandindex, name));

        name = BeamMetadata.NODE_DATATYPE;
        assertEquals("" + band.getDataType(), domMetadata.getBandProperty(bandindex, name));
        assertEquals("" + band.getDataType(), productMetadata.getBandProperty(bandindex, name));

        name = BeamMetadata.NODE_SCALING_FACTOR;
        assertEquals("" + band.getScalingFactor(), domMetadata.getBandProperty(bandindex, name));
        assertEquals("" + band.getScalingFactor(), productMetadata.getBandProperty(bandindex, name));

        name = BeamMetadata.NODE_SCALING_OFFSET;
        assertEquals("" + band.getScalingOffset(), domMetadata.getBandProperty(bandindex, name));
        assertEquals("" + band.getScalingOffset(), productMetadata.getBandProperty(bandindex, name));
    }  
}
