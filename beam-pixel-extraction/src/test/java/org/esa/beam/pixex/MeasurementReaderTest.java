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

package org.esa.beam.pixex;

import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static junit.framework.Assert.*;

public class MeasurementReaderTest {

    private File inputDir;

    @Before
    public void setup() throws Exception {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        inputDir = new File(tmpDir, getClass().getSimpleName());
        if (!inputDir.mkdir()) { // already exists, so delete contents
            for (File file : inputDir.listFiles()) {
                file.delete();
            }
        }
        final MeasurementWriter writer = new MeasurementWriter(inputDir, "MeasurementReaderTest", 1,
                                                               "expression", true);
        final Product p1 = MeasurementWriterTest.createTestProduct("N1", "T1",
                                                                   new String[]{"rad_1", "rad_2", "rad_3"});
        final Product p2 = MeasurementWriterTest.createTestProduct("N2", "T1",
                                                                   new String[]{"rad_1", "rad_2", "rad_3"});
        final Product p3 = MeasurementWriterTest.createTestProduct("N3", "T2",
                                                                   new String[]{"refl_1", "refl_2", "refl_3"});
        writer.write(p1, MeasurementWriterTest.getMeasurement(0, 0));
        writer.write(p1, MeasurementWriterTest.getMeasurement(1, 0));
        writer.write(p2, MeasurementWriterTest.getMeasurement(2, 1));
        writer.write(p2, MeasurementWriterTest.getMeasurement(3, 1));
        writer.write(p3, MeasurementWriterTest.getMeasurement(4, 2));
        writer.write(p3, MeasurementWriterTest.getMeasurement(5, 2));
    }

    @Test
    public void testReading() throws Exception {
        final MeasurementReader reader = new MeasurementReader(inputDir);
        final ArrayList<Measurement> measurementList = new ArrayList<Measurement>();
        while (reader.hasNext()) {
            measurementList.add(reader.next());
        }
        assertEquals(6, measurementList.size());
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(0, 0)));
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(1, 0)));
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(2, 1)));
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(3, 1)));
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(4, 2)));
        assertTrue(measurementList.contains(MeasurementWriterTest.getMeasurement(5, 2)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveThrowsException() throws Exception {
        final MeasurementReader reader = new MeasurementReader(inputDir);
        reader.remove();
    }
}
