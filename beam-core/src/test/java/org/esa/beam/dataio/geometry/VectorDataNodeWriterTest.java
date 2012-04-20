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

package org.esa.beam.dataio.geometry;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptorRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.FeatureUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class VectorDataNodeWriterTest extends TestCase {

    static final String OUTPUT_1 =
            "#beam.placemarkDescriptor.class=GeometryDescriptor\n"
            + "#defaultGeometry=geom\n"
            + "#styleCss=color:0,0,255\n"
            + "#separator=TAB\n"
            + "org.esa.beam.FT1\tname:String\tgeom:Geometry\tpixel:Integer\tdescription:String\n"
            + "ID65\tmark1\tPOINT (12.3 45.6)\t0\tThis is mark1.\n"
            + "ID66\tmark2\tPOINT (78.9 10.1)\t1\t[null]\n"
            + "ID67\tmark3\tPOINT (23.4 56.7)\t2\tThis is mark3.\n";

    static final String OUTPUT_2 = VectorDataNodeReaderTest.INPUT_2;

    private VectorDataNodeReader2.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    @Override
    public void setUp() throws Exception {
        placemarkDescriptorProvider = new VectorDataNodeReader2.PlacemarkDescriptorProvider() {
            @Override
            public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
                return PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(org.esa.beam.framework.datamodel.GeometryDescriptor.class);
            }
        };
    }

    public void testOutput1() throws IOException {
        testInputOutput(VectorDataNodeReaderTest.INPUT_1, OUTPUT_1);
    }

    public void testOutput2() throws IOException {
        testInputOutput(VectorDataNodeReaderTest.INPUT_2, OUTPUT_2);
    }

    private void testInputOutput(String input, String output) throws IOException {
        final VectorDataNode dataNode = VectorDataNodeReader2.read("mem", new StringReader(input), createDummyProduct(), new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product product) {
                return DefaultGeographicCRS.WGS84;
            }
        }, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : dataNode.getFeatureType().getUserData().entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue().toString());
        }
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = dataNode.getFeatureCollection();

        StringWriter writer = new StringWriter();
        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        vectorDataNodeWriter.writeProperties(properties, writer);
        vectorDataNodeWriter.writeFeatures(fc, writer);

        assertEquals(output, writer.toString());
    }

    private static Product createDummyProduct() {
        Product dummyProduct = new Product("blah", "blahType", 360, 180);
        dummyProduct.setGeoCoding(new VectorDataNodeReader2Test.DummyGeoCoding());
        return dummyProduct;
    }


}