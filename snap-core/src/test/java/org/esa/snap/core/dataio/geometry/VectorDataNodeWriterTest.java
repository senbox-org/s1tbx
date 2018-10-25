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
import org.esa.snap.core.datamodel.GeometryDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class VectorDataNodeWriterTest {

    private static final String INPUT_1 =
            "# This is a test comment\n" +
            "# separator=TAB\n" +
            "# styleCss=color:0,0,255\n" +
            "\n" +
            "org.esa.snap.FT1\tname:String\tgeom:Geometry\tpixel:Integer\tdescription:String\n"
            + "ID65\tmark1\tPOINT (12.3 45.6)\t0\tThis is mark1.\n"
            + "ID66\tmark2\tPOINT (78.9 10.1)\t1\t[null]\n"
            + "ID67\tmark3\tPOINT (23.4 56.7)\t2\tThis is mark3.\n";

    private static final String INPUT_2 =
            "#defaultGeometry=geom\n"
            + "#placemarkDescriptor=org.esa.snap.core.datamodel.GeometryDescriptor\n"
            + "org.esa.snap.FT2\tname:String\tgeom:Point\tweight:Float\n"
            + "ID65\tmark1\tPOINT (12.3 45.6)\t0.4\n";

    private VectorDataNodeReader.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    @Before
    public void setUp() throws Exception {
        placemarkDescriptorProvider = simpleFeatureType -> PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
    }

    @Test
    public void testOutput1() throws IOException {
        testInputOutput(INPUT_1,
                        new String[]{
                                "#defaultGeometry=geom",
                                "#styleCss=color:0,0,255",
                                "#placemarkDescriptor=org.esa.snap.core.datamodel.GeometryDescriptor",
                                "#separator=TAB"
                        },
                        "\norg.esa.snap.FT1\tname:String\tgeom:Geometry\tpixel:Integer\tdescription:String\n"
                        + "ID65\tmark1\tPOINT (12.3 45.6)\t0\tThis is mark1.\n"
                        + "ID66\tmark2\tPOINT (78.9 10.1)\t1\t[null]\n"
                        + "ID67\tmark3\tPOINT (23.4 56.7)\t2\tThis is mark3.\n");
    }

    @Test
    public void testOutput2() throws IOException {
        testInputOutput(INPUT_2,
                        new String[]{
                                "#defaultGeometry=geom",
                                "#placemarkDescriptor=org.esa.snap.core.datamodel.GeometryDescriptor"
                        },
                        "org.esa.snap.FT2\tname:String\tgeom:Point\tweight:Float\nID65\tmark1\tPOINT (12.3 45.6)\t0.4\n");
    }

    @Test
    public void testWriteNodeProperties() throws IOException {
        final VectorDataNode vectorNode = readVectorDataNode(INPUT_1);
        vectorNode.setDescription("Some text explaining the content.");
        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        StringWriter writer = new StringWriter();
        vectorDataNodeWriter.writeNodeProperties(vectorNode, writer);
        String nodeProperties = writer.getBuffer().toString();
        //default fill color is not constant, it depends how often a VDN has already been created
        assertTrue(nodeProperties.matches("#placemarkDescriptor=org.esa.snap.core.datamodel.GeometryDescriptor\n" +
                     "#defaultGeometry=geom\n" +
                     "#separator=TAB\n" +
                     "#styleCss=color:0,0,255\n" +
                     "#description=Some text explaining the content.\n" +
                     "#defaultCSS=fill:#......; fill-opacity:0.5; stroke:#ffffff; stroke-opacity:1.0; stroke-width:1.0; symbol:cross\n"));
    }

    private void testInputOutput(String input, String[] expectedProperties, String expectedContent) throws IOException {
        final VectorDataNode dataNode = readVectorDataNode(input);
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<Object, Object> entry : dataNode.getFeatureType().getUserData().entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue().toString());
        }
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = dataNode.getFeatureCollection();

        StringWriter writer = new StringWriter();
        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        vectorDataNodeWriter.writeProperties(properties, writer);
        vectorDataNodeWriter.writeFeatures(fc, writer);


        String writtenVDN = writer.toString();
        for (String expectedProperty : expectedProperties) {
            Assert.assertTrue(writtenVDN.contains(expectedProperty));
        }
        Assert.assertTrue(writtenVDN.endsWith(expectedContent));
    }

    private VectorDataNode readVectorDataNode(String vectorString) throws IOException {
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
        return VectorDataNodeReader.read("mem", new StringReader(vectorString), createDummyProduct(),
                                         featureCrsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84,
                                         VectorDataNodeIO.DEFAULT_DELIMITER_CHAR, ProgressMonitor.NULL);
    }

    private static Product createDummyProduct() {
        Product dummyProduct = new Product("blah", "blahType", 360, 180);
        dummyProduct.setSceneGeoCoding(new VectorDataNodeReaderTest.DummyGeoCoding());
        return dummyProduct;
    }


}
