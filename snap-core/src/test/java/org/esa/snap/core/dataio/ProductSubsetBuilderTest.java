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

package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link ProductSubsetBuilder}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ProductSubsetBuilderTest {

    private Product product;
    private static final String INDEX_CODED_BAND_NAME = "indexCodedBand";
    private static final String COLORED_BAND_NAME = "coloredBand";
    private static final String INDEX_CODING_NAME = "indexCoding";
    private static final int PRODUCT_WIDTH = 11;
    private static final int PRODUCT_HEIGHT = 11;

    @Before
    public void setUp() throws Exception {

        product = new Product("p", "t", PRODUCT_WIDTH, PRODUCT_HEIGHT);
        TiePointGrid lat = new TiePointGrid("t1", 3, 3, 0, 0, 5, 5,
                                            new float[]{
                                                    2.0f, 2.0f, 2.0f,
                                                    1.0f, 1.0f, 1.0f,
                                                    0.0f, 0.0f, 0.0f
                                            });
        product.addTiePointGrid(lat);
        TiePointGrid lon = new TiePointGrid("t2", 3, 3, 0, 0, 5, 5,
                                            new float[]{
                                                    0.0f, 1.0f, 2.0f,
                                                    0.0f, 1.0f, 2.0f,
                                                    0.0f, 1.0f, 2.0f
                                            });
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        attachIndexCodedBand();
        attachColoredBand();
    }

    @Test
    public void testStxHandling() throws IOException {
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(false, product2.getBand(INDEX_CODED_BAND_NAME).isStxSet());

        product.getBand(INDEX_CODED_BAND_NAME).getStx(true, ProgressMonitor.NULL);
        final Product product3 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        assertEquals(true, product3.getBand(INDEX_CODED_BAND_NAME).isStxSet());
    }

    @Test
    public void testPreserveImageInfo() throws IOException {
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, null, "subset", "");
        final Band band = product2.getBand(INDEX_CODED_BAND_NAME);
        final Band band2 = product2.getBand(COLORED_BAND_NAME);
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
        final Band band = product2.getBand(INDEX_CODED_BAND_NAME);
        final Band band2 = product2.getBand(COLORED_BAND_NAME);
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
    public void testSampleCodingPreservedInSubset() throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[]{INDEX_CODED_BAND_NAME});
        final Product subset = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");
        assertTrue(subset.getBandGroup().contains(INDEX_CODED_BAND_NAME));
        assertFalse(subset.getBandGroup().contains(COLORED_BAND_NAME));
        assertTrue(subset.getIndexCodingGroup().contains(INDEX_CODING_NAME));
    }
    
    @Test
    public void testSampleCodingRemovedInSubset() throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[]{COLORED_BAND_NAME});
        final Product subset = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");
        assertFalse(subset.getBandGroup().contains(INDEX_CODED_BAND_NAME));
        assertTrue(subset.getBandGroup().contains(COLORED_BAND_NAME));
        assertFalse(subset.getIndexCodingGroup().contains(INDEX_CODING_NAME));
    }

    @Test
    public void testCopyPlacemarkGroupsOnlyForRegionSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.getInstance();
        final PlacemarkDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        GeoCoding geoCoding = product.getSceneGeoCoding();
        final Placemark pin1 = Placemark.createPointPlacemark(pinDescriptor, "P1", "", "", new PixelPos(1.5, 1.5), null, geoCoding);
        final Placemark pin2 = Placemark.createPointPlacemark(pinDescriptor, "P2", "", "", new PixelPos(3.5, 3.5), null, geoCoding);
        final Placemark pin3 = Placemark.createPointPlacemark(pinDescriptor, "P3", "", "", new PixelPos(9.5, 9.5), null, geoCoding);
        final Placemark gcp1 = Placemark.createPointPlacemark(gcpDescriptor, "G1", "", "", new PixelPos(3, 3), null, geoCoding);
        final Placemark gcp2 = Placemark.createPointPlacemark(gcpDescriptor, "G2", "", "", new PixelPos(4.5, 4.5), null, geoCoding);
        final Placemark gcp3 = Placemark.createPointPlacemark(gcpDescriptor, "G3", "", "", new PixelPos(10.5, 10.5), null, geoCoding);

        product.getPinGroup().add(pin1);
        product.getPinGroup().add(pin2);
        product.getPinGroup().add(pin3);
        product.getGcpGroup().add(gcp1);
        product.getGcpGroup().add(gcp2);
        product.getGcpGroup().add(gcp3);

        assertEquals(3, product.getPinGroup().getNodeCount());
        assertEquals(3, product.getGcpGroup().getNodeCount());

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(2, 2, 5, 5);
        final Product product2 = ProductSubsetBuilder.createProductSubset(product, subsetDef, "subset", "");

        assertEquals("P2", product2.getPinGroup().get(0).getName());
        assertEquals("G2", product2.getGcpGroup().get(1).getName());
    }

    @Test
    public void testCopyPlacemarkGroupsOnlyForNullSubset() throws IOException {
        final PlacemarkDescriptor pinDescriptor = PinDescriptor.getInstance();
        final PlacemarkDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        GeoCoding geoCoding = product.getSceneGeoCoding();
        final Placemark pin1 = Placemark.createPointPlacemark(pinDescriptor, "P1", "", "", new PixelPos(1.5, 1.5), null, geoCoding);
        final Placemark pin2 = Placemark.createPointPlacemark(pinDescriptor, "P2", "", "", new PixelPos(3.5, 3.5), null, geoCoding);
        final Placemark pin3 = Placemark.createPointPlacemark(pinDescriptor, "P3", "", "", new PixelPos(9.5, 9.5), null, geoCoding);
        final Placemark gcp1 = Placemark.createPointPlacemark(gcpDescriptor, "G1", "", "", new PixelPos(2.5, 2.5), null, geoCoding);
        final Placemark gcp2 = Placemark.createPointPlacemark(gcpDescriptor, "G2", "", "", new PixelPos(4.5, 4.5), null, geoCoding);
        final Placemark gcp3 = Placemark.createPointPlacemark(gcpDescriptor, "G3", "", "", new PixelPos(10.5, 10.5), null, geoCoding);

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
        final Band band = createDataBand(0, 1, INDEX_CODED_BAND_NAME);
        final IndexCoding indexCoding = new IndexCoding(INDEX_CODING_NAME);
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
        final Band band = createDataBand(0, 255, COLORED_BAND_NAME);
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
