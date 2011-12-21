/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        TiePointGrid lat = new TiePointGrid("t1", 3, 3, 0, 0, 5, 5,
                                            new float[]{
                                                    2.0f, 2.0f, 2.0f,
                                                    1.0f, 1.0f, 1.0f,
                                                    0.0f, 0.0f, 0.0f});
        product.addTiePointGrid(lat);
        TiePointGrid lon = new TiePointGrid("t2", 3, 3, 0, 0, 5, 5,
                                            new float[]{
                                                    0.0f, 1.0f, 2.0f,
                                                    0.0f, 1.0f, 2.0f,
                                                    0.0f, 1.0f, 2.0f});
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon, Datum.WGS_84));
        attachIndexCodedBand();
        attachColoredBand();
    }

    @Test
    public void testStxHandling() throws IOException {
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(false, product2.getBand(DUMMY_BAND1).isStxSet());

        product.getBand(DUMMY_BAND1).getStx(true, ProgressMonitor.NULL);
        final Product product3 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(true, product3.getBand(DUMMY_BAND1).isStxSet());
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
        assertEquals(colors.length, palette.getPoints().length);
        for (int i = 0; i < colors.length; i++) {
            assertEquals(colors[i], palette.getPointAt(i).getColor());
        }
    }

    @Test
    public void testCopyPlacemarkGroupsOnlyForRegionSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.getInstance();
        final PlacemarkDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        final Placemark pin1 = Placemark.createPointPlacemark(pinDescriptor, "P1", "", "", new PixelPos(1.5f, 1.5f), null,
                                                              product.getGeoCoding());
        final Placemark pin2 = Placemark.createPointPlacemark(pinDescriptor, "P2", "", "", new PixelPos(3.5f, 3.5f), null,
                                                              product.getGeoCoding());
        final Placemark pin3 = Placemark.createPointPlacemark(pinDescriptor, "P3", "", "", new PixelPos(9.5f, 9.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp1 = Placemark.createPointPlacemark(gcpDescriptor, "G1", "", "", new PixelPos(2.5f, 2.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp2 = Placemark.createPointPlacemark(gcpDescriptor, "G2", "", "", new PixelPos(4.5f, 4.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp3 = Placemark.createPointPlacemark(gcpDescriptor, "G3", "", "", new PixelPos(10.5f, 10.5f), null,
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
        assertEquals(2, product2.getGcpGroup().getNodeCount());

        assertEquals("P2", product2.getPinGroup().get(0).getName());
        assertEquals("G1", product2.getGcpGroup().get(0).getName());
        assertEquals("G2", product2.getGcpGroup().get(1).getName());
    }

    @Test
    public void testCopyPlacemarkGroupsOnlyForNullSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.getInstance();
        final PlacemarkDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        final Placemark pin1 = Placemark.createPointPlacemark(pinDescriptor, "P1", "", "", new PixelPos(1.5f, 1.5f), null,
                                                              product.getGeoCoding());
        final Placemark pin2 = Placemark.createPointPlacemark(pinDescriptor, "P2", "", "", new PixelPos(3.5f, 3.5f), null,
                                                              product.getGeoCoding());
        final Placemark pin3 = Placemark.createPointPlacemark(pinDescriptor, "P3", "", "", new PixelPos(9.5f, 9.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp1 = Placemark.createPointPlacemark(gcpDescriptor, "G1", "", "", new PixelPos(2.5f, 2.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp2 = Placemark.createPointPlacemark(gcpDescriptor, "G2", "", "", new PixelPos(4.5f, 4.5f), null,
                                                              product.getGeoCoding());
        final Placemark gcp3 = Placemark.createPointPlacemark(gcpDescriptor, "G3", "", "", new PixelPos(10.5f, 10.5f), null,
                                                              product.getGeoCoding());

        product.getPinGroup().add(pin1);
        product.getPinGroup().add(pin2);
        product.getPinGroup().add(pin3);
        product.getGcpGroup().add(gcp1);
        product.getGcpGroup().add(gcp2);
        product.getGcpGroup().add(gcp3);

        assertEquals(3, product.getPinGroup().getNodeCount());
        assertEquals(3, product.getGcpGroup().getNodeCount());

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
        final Band band = createDataBand(0, 1, DUMMY_BAND1);
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
