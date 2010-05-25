package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for class {@link ProductSubsetBuilder}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ProductSubsetBuilderTest {

    private Product product;
    private static final String DUMMY_BAND1 = "dummyBand1";
    private static final String DUMMY_BAND2 = "dummyBand2";
    private static final int PRODUCT_WIDTH = 11;
    private static final int PRODUCT_HEIGHT = 11;

    @Before
    public void setUp() throws Exception {

        product = new Product("p", "t", PRODUCT_WIDTH, PRODUCT_HEIGHT);
        TiePointGrid t1 = new TiePointGrid("t1", 3, 3, 0, 0, 5, 5,
                                           new float[]{0.6f, 0.3f, 0.4f, 0.8f, 0.9f, 0.4f, 0.3f, 0.2f, 0.4f});
        product.addTiePointGrid(t1);
        TiePointGrid t2 = new TiePointGrid("t2", 3, 3, 0, 0, 5, 5,
                                           new float[]{0.9f, 0.2f, 0.3f, 0.6f, 0.1f, 0.4f, 0.2f, 0.9f, 0.5f});
        product.addTiePointGrid(t2);
        product.setGeoCoding(new TiePointGeoCoding(t1, t2, Datum.WGS_84));
        attachIndexCodedBand();
        attachColoredBand();
    }

    @Test
    public void testStxHandling() throws IOException {
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(false, product2.getBand(DUMMY_BAND1).isStxSet() );

        product.getBand(DUMMY_BAND1).getStx( true, ProgressMonitor.NULL );
        final Product product3 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(true, product3.getBand( DUMMY_BAND1 ).isStxSet() );
    }

    @Test
    public void testPreserveImageInfo() throws IOException {
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        final Band band = product2.getBand(DUMMY_BAND1);
        final Band band2 = product2.getBand(DUMMY_BAND2);
        assertNotNull(band);
        assertNotNull(band2);

        final ImageInfo imageInfo = band.getImageInfo();
        final ImageInfo imageInfo2 = band2.getImageInfo();
        assertNotNull(imageInfo);
        assertNotNull(imageInfo2);

        testPalette(imageInfo.getColorPaletteDef(), new Color[]{Color.red, Color.green});
        testPalette(imageInfo2.getColorPaletteDef(), new Color[]{Color.blue, Color.black});
    }

    @Test
    public void testPreserveImageInfoAndSubset() throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(2, 2, 5, 5);
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");
        final Band band = product2.getBand(DUMMY_BAND1);
        final Band band2 = product2.getBand(DUMMY_BAND2);
        assertNotNull(band);
        assertNotNull(band2);

        final ImageInfo imageInfo = band.getImageInfo();
        final ImageInfo imageInfo2 = band2.getImageInfo();
        assertNotNull(imageInfo);
        assertNotNull(imageInfo2);

        testPalette(imageInfo.getColorPaletteDef(), new Color[]{Color.red, Color.green});
        testPalette(imageInfo2.getColorPaletteDef(), new Color[]{Color.blue, Color.black});
    }

    private void testPalette(ColorPaletteDef palette, Color[] colors) {
        assertEquals( colors.length, palette.getPoints().length );
        for( int i = 0; i < colors.length; i++ ) {
            assertEquals( colors[i], palette.getPointAt( i ).getColor() );
        }
    }

    @Test
    public void testCopyPlacemarkGroupsOnlyForRegionSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.INSTANCE;
        final Placemark pin1 = new Placemark("P1", "", "", new PixelPos(1.5f, 1.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark pin2 = new Placemark("P2", "", "", new PixelPos(3.5f, 3.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark pin3 = new Placemark("P3", "", "", new PixelPos(9.5f, 9.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp1 = new Placemark("G1", "", "", new PixelPos(2.5f, 2.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp2 = new Placemark("G2", "", "", new PixelPos(4.5f, 4.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp3 = new Placemark("G3", "", "", new PixelPos(10.5f, 10.5f), null, pinDescriptor,
                                             product.getGeoCoding());

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

    @Test
    public void testCopyPlacemarkGroupsOnlyForNullSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.INSTANCE;
        final Placemark pin1 = new Placemark("P1", "", "", new PixelPos(1.5f, 1.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark pin2 = new Placemark("P2", "", "", new PixelPos(3.5f, 3.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark pin3 = new Placemark("P3", "", "", new PixelPos(9.5f, 9.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp1 = new Placemark("G1", "", "", new PixelPos(2.5f, 2.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp2 = new Placemark("G2", "", "", new PixelPos(4.5f, 4.5f), null, pinDescriptor,
                                             product.getGeoCoding());
        final Placemark gcp3 = new Placemark("G3", "", "", new PixelPos(10.5f, 10.5f), null, pinDescriptor,
                                             product.getGeoCoding());

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

    private void attachIndexCodedBand() {
        final Band band = createDataBand(0,1, DUMMY_BAND1);
        final IndexCoding indexCoding = new IndexCoding("ic1");
        indexCoding.addIndex("i0", 0, "i0");
        indexCoding.addIndex("i1", 1, "i1");
        band.setSampleCoding(indexCoding);
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[2];
        points[0] = new ColorPaletteDef.Point(0, Color.RED);
        points[1] = new ColorPaletteDef.Point(1, Color.GREEN);
        ColorPaletteDef colors = new ColorPaletteDef(points);
        band.setImageInfo(new ImageInfo(colors));
        product.getIndexCodingGroup().add(indexCoding);
        product.addBand(band);
    }

    private void attachColoredBand() {
        final Band band = createDataBand(0, 255, DUMMY_BAND2);
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[2];
        points[0] = new ColorPaletteDef.Point(128, Color.BLUE);
        points[1] = new ColorPaletteDef.Point(255, Color.BLACK);
        ColorPaletteDef colors = new ColorPaletteDef(points);
        band.setImageInfo(new ImageInfo(colors));
        product.addBand(band);
    }

    private Band createDataBand(int min, int max, String bandName) {
        final Band band = new Band(bandName, ProductData.TYPE_INT8, PRODUCT_WIDTH, PRODUCT_HEIGHT);
        final byte[] array = new byte[PRODUCT_WIDTH * PRODUCT_HEIGHT];
        fillArray(array, max, min);
        band.setData(new ProductData.Byte(array));
        return band;
    }

    private void fillArray(byte[] array, int max, int min) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) (Math.random() > 0.5 ? max : min);
        }
    }

}
