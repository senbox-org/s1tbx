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
package org.esa.beam.gpf.operators.standard;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.HashMap;

public class SubsetOpTest extends TestCase {


    public void testConstructorUsage() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final String[] bandNames = {"radiance_1", "radiance_3"};

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(bandNames);

        assertSame(sp, op.getSourceProduct());
        assertNotSame(bandNames, op.getBandNames());

        Product tp = op.getTargetProduct();

        assertEquals(2, tp.getNumBands());
        assertNotNull(tp.getBand("radiance_1"));
        assertNull(tp.getBand("radiance_2"));
        assertNotNull(tp.getBand("radiance_3"));
    }

    public void testInstantiationWithGPF() throws GraphException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        GeometryFactory gf = new GeometryFactory();
        Polygon polygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(-5, 5),
                new Coordinate(5, 5),
                new Coordinate(5, -5),
                new Coordinate(-5, -5),
                new Coordinate(-5, 5),
        }), null);

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("geoRegion", polygon.toText());
        parameters.put("fullSwath", true);
        final Product sp = createTestProduct(100, 100);
        assertNotNull(sp.getGeoCoding());

        Product tp = GPF.createProduct("Subset", parameters, sp);
        assertNotNull(tp);
        assertEquals(100, tp.getSceneRasterWidth());
        assertEquals(51, tp.getSceneRasterHeight());
    }


    public void testGeometry() throws Exception {
        final GeometryFactory gf = new GeometryFactory();
        final Product sp = createTestProduct(100, 100);
        assertNotNull(sp.getGeoCoding());

        final Polygon roi = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(-5, 5),
                new Coordinate(5, 5),
                new Coordinate(5, -5),
                new Coordinate(-5, -5),
                new Coordinate(-5, 5),
        }), null);

        final SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(new String[]{"radiance_1", "radiance_3"});
        op.setGeoRegion(roi);

        assertEquals(roi, op.getGeoRegion());

        final Product tp = op.getTargetProduct();
        assertNotNull(tp);
        assertEquals(new Rectangle(25, 25, 51, 51), op.getRegion());
        assertEquals(51, tp.getSceneRasterWidth());
        assertEquals(51, tp.getSceneRasterHeight());
        assertNotNull(tp.getBand("radiance_1"));
        assertNotNull(tp.getBand("radiance_3"));
    }

    @Test
    public void testComputationOfProductGeometriesAndPixelRegions() throws TransformException, FactoryException {
        Geometry geometry;

        Product product = new Product("N", "T", 360, 180);
        AffineTransform at = AffineTransform.getTranslateInstance(-180, -90);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(360, 180), at);
        product.setGeoCoding(geoCoding);
        geometry = SubsetOp.computeProductGeometry(product);
        assertTrue(geometry instanceof Polygon);
        assertEquals("POLYGON ((-179.5 -89.5, -179.5 89.5, 179.5 89.5, 179.5 -89.5, -179.5 -89.5))", geometry.toString());

        Rectangle rectangle;

        rectangle = SubsetOp.computePixelRegion(product, geometry, 0);
        assertEquals(new Rectangle(360, 180), rectangle);

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(product);
        op.setRegion(new Rectangle(180 - 50, 90 - 25, 100, 50));
        product = op.getTargetProduct();
        geometry = SubsetOp.computeProductGeometry(product);
        assertTrue(geometry instanceof Polygon);
        assertEquals("POLYGON ((-49.5 -24.5, -49.5 24.5, 49.5 24.5, 49.5 -24.5, -49.5 -24.5))", geometry.toString());

        // BBOX fully contained, with border=0
        rectangle = SubsetOp.computePixelRegion(product, createBBOX(0.0, 0.0, 10.0, 10.0), 0);
        assertEquals(new Rectangle(50, 25, 11, 11), rectangle);

        // BBOX fully contained, with border=1
        rectangle = SubsetOp.computePixelRegion(product, createBBOX(0.0, 0.0, 10.0, 10.0), 1);
        assertEquals(new Rectangle(49, 24, 13, 13), rectangle);

        // BBOX intersects product rect in upper left
        rectangle = SubsetOp.computePixelRegion(product, createBBOX(45.5, 20.5, 100.0, 50.0), 0);
        assertEquals(new Rectangle(95, 45, 5, 5), rectangle);

        // Product bounds fully contained in BBOX
        rectangle = SubsetOp.computePixelRegion(product, createBBOX(-180, -90, 360, 180), 0);
        assertEquals(new Rectangle(0, 0, 100, 50), rectangle);

        // BBOX not contained
        rectangle = SubsetOp.computePixelRegion(product, createBBOX(60.0, 0.0, 10.0, 10.0), 0);
        assertEquals(true, rectangle.isEmpty());
    }

    private static Polygon createBBOX(double x, double y, double w, double h) {
        GeometryFactory factory = new GeometryFactory();
        final LinearRing ring = factory.createLinearRing(new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + w, y),
                new Coordinate(x + w, y + h),
                new Coordinate(x, y + h),
                new Coordinate(x, y)
        });
        return factory.createPolygon(ring, null);
    }


    public void testCopyMetadata() throws Exception {
        final Product sp = createTestProduct(100, 100);
        addMetadata(sp);
        final String[] bandNames = {"radiance_1", "radiance_3"};

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(bandNames);
        op.setCopyMetadata(true);

        assertSame(sp, op.getSourceProduct());
        assertNotSame(bandNames, op.getBandNames());

        Product tp = op.getTargetProduct();

        assertEquals(2, tp.getNumBands());
        assertNotNull(tp.getBand("radiance_1"));
        assertNull(tp.getBand("radiance_2"));
        assertNotNull(tp.getBand("radiance_3"));

        final MetadataElement root = tp.getMetadataRoot();
        assertNotNull(root);
        final MetadataAttribute attribRoot = root.getAttribute("attribRoot");
        assertNotNull(attribRoot);
        assertEquals("rootValue", attribRoot.getData().getElemString());
        assertTrue(root.containsElement("meta1"));
        final MetadataAttribute attrib1 = root.getElement("meta1").getAttribute("attrib1");
        assertNotNull(attrib1);
        assertEquals("value", attrib1.getData().getElemString());
        final MetadataElement meta2 = root.getElement("meta2");
        assertNotNull(meta2);
        final MetadataElement meta2_1 = meta2.getElement("meta2_1");
        assertNotNull(meta2_1);
        final MetadataAttribute attrib2_1 = meta2_1.getAttribute("attrib2_1");
        assertEquals("meta2_1_value", attrib2_1.getData().getElemString());
    }

    private void addMetadata(Product sp) {
        final MetadataElement meta1 = new MetadataElement("meta1");
        meta1.addAttribute(new MetadataAttribute("attrib1", ProductData.createInstance("value"), true));
        final MetadataElement meta2 = new MetadataElement("meta2");
        final MetadataElement meta2_1 = new MetadataElement("meta2_1");
        meta2_1.addAttribute(new MetadataAttribute("attrib2_1", ProductData.createInstance("meta2_1_value"), true));
        meta2.addElement(meta2_1);

        final MetadataElement metadataRoot = sp.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("attribRoot", ProductData.createInstance("rootValue"), true));
        metadataRoot.addElement(meta1);
        metadataRoot.addElement(meta2);
    }

    private Product createTestProduct(int w, int h) {
        Product product = new Product("p", "t", w, h);

        Placemark[] gcps = {
                new Placemark("p1", "p1", "", new PixelPos(0.5f, 0.5f), new GeoPos(10, -10), GcpDescriptor.INSTANCE,
                              null),
                new Placemark("p2", "p2", "", new PixelPos(w - 0.5f, 0.5f), new GeoPos(10, 10), GcpDescriptor.INSTANCE,
                              null),
                new Placemark("p3", "p3", "", new PixelPos(w - 0.5f, h - 0.5f), new GeoPos(-10, 10),
                              GcpDescriptor.INSTANCE, null),
                new Placemark("p4", "p4", "", new PixelPos(0.5f, h - 0.5f), new GeoPos(-10, -10),
                              GcpDescriptor.INSTANCE, null),
        };
        product.setGeoCoding(new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, gcps, w, h, Datum.WGS_84));

        Band band1 = product.addBand("radiance_1", ProductData.TYPE_INT32);
        int[] intValues = new int[w * h];
        Arrays.fill(intValues, 1);
        band1.setData(ProductData.createInstance(intValues));

        Band band2 = product.addBand("radiance_2", ProductData.TYPE_FLOAT32);
        float[] floatValues = new float[w * h];
        Arrays.fill(floatValues, 2.5f);
        band2.setData(ProductData.createInstance(floatValues));

        Band band3 = product.addBand("radiance_3", ProductData.TYPE_INT16);
        band3.setScalingFactor(0.5);
        short[] shortValues = new short[w * h];
        Arrays.fill(shortValues, (short) 6);
        band3.setData(ProductData.createInstance(shortValues));

        return product;
    }
}