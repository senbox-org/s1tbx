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
        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader("mem", null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);

        StringWriter writer = new StringWriter();
        VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
        vectorDataNodeWriter.writeFeatures(fc, writer);

        assertEquals(contents, writer.toString());
    }


}