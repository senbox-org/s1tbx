package org.esa.beam.dataio.geometry;

import junit.framework.TestCase;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class VectorDataNodeWriterTest extends TestCase {

    public void testContents1() throws IOException {
        testContents(VectorDataNodeReaderTest.CONTENTS_1);
    }

    public void testContents2() throws IOException {
        testContents(VectorDataNodeReaderTest.CONTENTS_2);
    }

    private void testContents(String contents) throws IOException {
        StringReader reader = new StringReader(contents);
        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader(null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);

        StringWriter writer = new StringWriter();
        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        vectorDataNodeWriter.writeFeatures(fc, writer);

        assertEquals(contents, writer.toString());
    }


}