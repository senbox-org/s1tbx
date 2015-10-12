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

package org.esa.snap.core.dataio.geometry;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.core.datamodel.GeometryDescriptor;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class VectorDataNodeIOTest {

    private static final String ATTRIBUTE_NAME_LABEL = "LABEL";

    private StringWriter stringWriter;
    private DefaultFeatureCollection testCollection;
    private VectorDataNodeReader.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    @Before
    public void setUp() throws IOException {
        testCollection = createTestCollection();

        stringWriter = new StringWriter(300);
        final VectorDataNodeWriter writer = new VectorDataNodeWriter();
        writer.writeFeatures(testCollection, stringWriter);
        placemarkDescriptorProvider = new VectorDataNodeReader.PlacemarkDescriptorProvider() {
            @Override
            public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
                return PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
            }
        };
    }

    @Test
    public void testEncodingDelimiter() {
        String[] linesOut = stringWriter.toString().split("\n");
        assertEquals(4, linesOut.length);
        assertTrue(linesOut[1].endsWith("with\\tTab"));
        assertTrue(linesOut[2].endsWith("with     spaces"));
        assertTrue(linesOut[3].endsWith("with \\\\t escaped tab"));
    }

    @Test
    public void testDecodingDelimiter() throws IOException {
        final FeatureUtils.FeatureCrsProvider featureCrsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product product) {
                return DefaultGeographicCRS.WGS84;
            }

            @Override
            public boolean clipToProductBounds() {
                return true;
            }
        };
        final FeatureCollection<SimpleFeatureType, SimpleFeature> readCollection =
                VectorDataNodeReader.read("mem", new StringReader(stringWriter.toString()),
                                          createDummyProduct(), featureCrsProvider, placemarkDescriptorProvider,
                                          DefaultGeographicCRS.WGS84, VectorDataNodeIO.DEFAULT_DELIMITER_CHAR, ProgressMonitor.NULL).getFeatureCollection();

        assertEquals(testCollection.size(), readCollection.size());
        final FeatureIterator<SimpleFeature> expectedIterator = testCollection.features();
        final FeatureIterator<SimpleFeature> actualIterator = readCollection.features();
        while (expectedIterator.hasNext()) {
            final SimpleFeature expectedFeature = expectedIterator.next();
            final SimpleFeature actualFeature = actualIterator.next();
            final Object expectedAttribute = expectedFeature.getAttribute(ATTRIBUTE_NAME_LABEL);
            final Object actualAttribute = actualFeature.getAttribute(ATTRIBUTE_NAME_LABEL);
            assertEquals(expectedAttribute, actualAttribute);
        }
    }

    private DefaultFeatureCollection createTestCollection() {
        final SimpleFeatureType type = Placemark.createGeometryFeatureType();
        GeometryFactory gf = new GeometryFactory();
        Object[] data1 = {gf.toGeometry(new Envelope(0, 10, 0, 10)), "with\tTab"};
        Object[] data2 = {gf.toGeometry(new Envelope(20, 30, 0, 10)), "with     spaces"};
        Object[] data3 = {gf.toGeometry(new Envelope(40, 50, 0, 10)), "with \\t escaped tab"};
        SimpleFeatureImpl f1 = new SimpleFeatureImpl(data1, type, new FeatureIdImpl("F1"), true);
        SimpleFeatureImpl f2 = new SimpleFeatureImpl(data2, type, new FeatureIdImpl("F2"), true);
        SimpleFeatureImpl f3 = new SimpleFeatureImpl(data3, type, new FeatureIdImpl("F3"), true);
        final DefaultFeatureCollection collection = new DefaultFeatureCollection("testID", type);
        collection.add(f1);
        collection.add(f2);
        collection.add(f3);
        return collection;
    }

    @Test
    public void testEncodeTabString() {
        assertEquals("with\\tTab", VectorDataNodeIO.encodeTabString("with\tTab"));
        assertEquals("with 4    spaces", VectorDataNodeIO.encodeTabString("with 4    spaces"));
        assertEquals("with \\\\t escaped tab", VectorDataNodeIO.encodeTabString("with \\t escaped tab"));
        assertEquals("with\\t2\\ttabs", VectorDataNodeIO.encodeTabString("with\t2\ttabs"));
        assertEquals("with \\d other char", VectorDataNodeIO.encodeTabString("with \\d other char"));
        assertEquals("with \\\\d other char", VectorDataNodeIO.encodeTabString("with \\\\d other char"));
    }

    @Test
    public void testDecodeTabString() {
        assertEquals("with\tTab", VectorDataNodeIO.decodeTabString("with\\tTab"));
        assertEquals("with 4    spaces", VectorDataNodeIO.decodeTabString("with 4    spaces"));
        assertEquals("with \\t escaped tab", VectorDataNodeIO.decodeTabString("with \\\\t escaped tab"));
        assertEquals("with\t2\ttabs", VectorDataNodeIO.decodeTabString("with\\t2\\ttabs"));
        assertEquals("with \\d other char", VectorDataNodeIO.encodeTabString("with \\d other char"));
        assertEquals("with \\\\d other char", VectorDataNodeIO.encodeTabString("with \\\\d other char"));
    }

    @Test
    public void testProperties() throws Exception {
        VectorDataNode vectorDataNode = new VectorDataNode("aName", Placemark.createGeometryFeatureType());
        vectorDataNode.setDescription("Contains a set of pins");
        vectorDataNode.setDefaultStyleCss("stroke:#ff0000");

        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        File tempFile = File.createTempFile("VectorDataNodeWriterTest_testProperties", "csv");
        tempFile.deleteOnExit();
        vectorDataNodeWriter.write(vectorDataNode, tempFile);

        FileReader fileReader = new FileReader(tempFile);
        LineNumberReader lineNumberReader = new LineNumberReader(fileReader);

        String firstLine = lineNumberReader.readLine();
        assertNotNull(firstLine);
        assertEquals("#description=Contains a set of pins", firstLine);

        String secondLine = lineNumberReader.readLine();
        assertNotNull(secondLine);
        assertEquals("#defaultCSS=stroke:#ff0000", secondLine);

        final FeatureUtils.FeatureCrsProvider featureCrsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product product) {
                return DefaultGeographicCRS.WGS84;
            }

            @Override
            public boolean clipToProductBounds() {
                return true;
            }
        };
        VectorDataNode vectorDataNode2 = VectorDataNodeReader.read("mem", new FileReader(tempFile), createDummyProduct(),
                                                                   featureCrsProvider, placemarkDescriptorProvider,
                                                                   DefaultGeographicCRS.WGS84, VectorDataNodeIO.DEFAULT_DELIMITER_CHAR, ProgressMonitor.NULL);

        assertNotNull(vectorDataNode2);
        assertEquals(vectorDataNode.getDescription(), vectorDataNode2.getDescription());
        assertEquals(vectorDataNode.getDefaultStyleCss(), vectorDataNode2.getDefaultStyleCss());
    }

    @Test
    public void testGetVectorDataNodes() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("sft");
        builder.add("CAPITAL", String.class);
        SimpleFeatureType featureType = builder.buildFeatureType();
        VectorDataNode vectorDataNode = new VectorDataNode("originalName", featureType);
        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureType);
        simpleFeatureBuilder.set("CAPITAL", "Moscow");

        vectorDataNode.getFeatureCollection().add(simpleFeatureBuilder.buildFeature("0"));

        simpleFeatureBuilder.set("CAPITAL", "Coruscant");

        vectorDataNode.getFeatureCollection().add(simpleFeatureBuilder.buildFeature("1"));

        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, true, "CAPITAL");
        assertEquals(2, vectorDataNodes.length);
        assertEquals("Moscow", vectorDataNodes[0].getName());
        assertEquals("Coruscant", vectorDataNodes[1].getName());

        vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, false, "CAPITAL");
        assertEquals(1, vectorDataNodes.length);
        assertEquals("originalName", vectorDataNodes[0].getName());
    }

    @Test
    public void testGetVectorDataNodesWithEmptyAttribute() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("sft");
        builder.add("CAPITAL", String.class);
        SimpleFeatureType featureType = builder.buildFeatureType();
        VectorDataNode vectorDataNode = new VectorDataNode("originalName", featureType);
        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureType);
        simpleFeatureBuilder.set("CAPITAL", "");

        vectorDataNode.getFeatureCollection().add(simpleFeatureBuilder.buildFeature("0"));

        simpleFeatureBuilder.set("CAPITAL", "Coruscant");

        vectorDataNode.getFeatureCollection().add(simpleFeatureBuilder.buildFeature("1"));

        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, true, "CAPITAL");
        assertEquals(2, vectorDataNodes.length);
        assertEquals("originalName_1", vectorDataNodes[0].getName());
        assertEquals("Coruscant", vectorDataNodes[1].getName());
    }

    private static Product createDummyProduct() {
        Product dummyProduct = new Product("blah", "blahType", 360, 180);
        dummyProduct.setSceneGeoCoding(new VectorDataNodeReaderTest.DummyGeoCoding());
        return dummyProduct;
    }

}
