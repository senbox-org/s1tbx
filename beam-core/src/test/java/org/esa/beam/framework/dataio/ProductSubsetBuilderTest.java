package org.esa.beam.framework.dataio;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.io.IOException;

/**
 * Tests for class {@link ProductSubsetBuilder}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ProductSubsetBuilderTest extends TestCase {

    Product product;

    @Override
    protected void setUp() throws Exception {

        product = new Product("p", "t", 11, 11);
        TiePointGrid t1 = new TiePointGrid("t1", 3, 3, 0, 0, 5, 5,
                                           new float[]{0.6f, 0.3f, 0.4f, 0.8f, 0.9f, 0.4f, 0.3f, 0.2f, 0.4f});
        product.addTiePointGrid(t1);
        TiePointGrid t2 = new TiePointGrid("t2", 3, 3, 0, 0, 5, 5,
                                           new float[]{0.9f, 0.2f, 0.3f, 0.6f, 0.1f, 0.4f, 0.2f, 0.9f, 0.5f});
        product.addTiePointGrid(t2);
        product.setGeoCoding(new TiePointGeoCoding(t1, t2, Datum.WGS_84));
    }

    public void testCopyPlacemarkGroupsOnlyForRegionSubset() throws IOException {
        final PlacemarkSymbol defaultPinSymbol = PlacemarkSymbol.createDefaultPinSymbol();
        final Placemark pin1 = new Placemark("P1", "", "", new PixelPos(1.5f, 1.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark pin2 = new Placemark("P2", "", "", new PixelPos(3.5f, 3.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark pin3 = new Placemark("P3", "", "", new PixelPos(9.5f, 9.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp1 = new Placemark("G1", "", "", new PixelPos(2.5f, 2.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp2 = new Placemark("G2", "", "", new PixelPos(4.5f, 4.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp3 = new Placemark("G3", "", "", new PixelPos(10.5f, 10.5f), null, defaultPinSymbol, product.getGeoCoding());

        product.getPinGroup().add(pin1);
        product.getPinGroup().add(pin2);
        product.getPinGroup().add(pin3);
        product.getGcpGroup().add(gcp1);
        product.getGcpGroup().add(gcp2);
        product.getGcpGroup().add(gcp3);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(2, 2, 5, 5);
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");

        assertEquals(1, product2.getPinGroup().getNodeCount());
        assertEquals(3, product2.getGcpGroup().getNodeCount());

        assertEquals("P2", product2.getPinGroup().get(0).getName());
        assertEquals("G1", product2.getGcpGroup().get(0).getName());
        assertEquals("G2", product2.getGcpGroup().get(1).getName());
        assertEquals("G3", product2.getGcpGroup().get(2).getName());
    }

    public void testCopyPlacemarkGroupsOnlyForNullSubset() throws IOException {
        final PlacemarkSymbol defaultPinSymbol = PlacemarkSymbol.createDefaultPinSymbol();
        final Placemark pin1 = new Placemark("P1", "", "", new PixelPos(1.5f, 1.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark pin2 = new Placemark("P2", "", "", new PixelPos(3.5f, 3.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark pin3 = new Placemark("P3", "", "", new PixelPos(9.5f, 9.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp1 = new Placemark("G1", "", "", new PixelPos(2.5f, 2.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp2 = new Placemark("G2", "", "", new PixelPos(4.5f, 4.5f), null, defaultPinSymbol, product.getGeoCoding());
        final Placemark gcp3 = new Placemark("G3", "", "", new PixelPos(10.5f, 10.5f), null, defaultPinSymbol, product.getGeoCoding());

        product.getPinGroup().add(pin1);
        product.getPinGroup().add(pin2);
        product.getPinGroup().add(pin3);
        product.getGcpGroup().add(gcp1);
        product.getGcpGroup().add(gcp2);
        product.getGcpGroup().add(gcp3);

        final ProductSubsetDef subsetDef = null;
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");

        assertEquals(3, product2.getPinGroup().getNodeCount());
        assertEquals(3, product2.getGcpGroup().getNodeCount());

        assertEquals("P1", product2.getPinGroup().get(0).getName());
        assertEquals("P2", product2.getPinGroup().get(1).getName());
        assertEquals("P3", product2.getPinGroup().get(2).getName());
        assertEquals("G1", product2.getGcpGroup().get(0).getName());
        assertEquals("G2", product2.getGcpGroup().get(1).getName());
        assertEquals("G3", product2.getGcpGroup().get(2).getName());
    }

}
