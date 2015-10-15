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

package org.esa.snap.pixex;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.MeasurementWriter;
import org.esa.snap.pixex.output.DefaultFormatStrategy;
import org.esa.snap.pixex.output.PixExMeasurementFactory;
import org.esa.snap.pixex.output.PixExProductRegistry;
import org.esa.snap.pixex.output.PixExRasterNamesFactory;
import org.esa.snap.pixex.output.TargetWriterFactoryAndMap;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static junit.framework.Assert.*;

public class PixExMeasurementReaderTest {

    private File inputDir;
    private int windowSize;

    @Before
    public void setup() throws Exception {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        inputDir = new File(tmpDir, getClass().getSimpleName());
        if (!inputDir.mkdir()) { // already exists, so delete contents
            for (File file : inputDir.listFiles()) {
                file.delete();
            }
        }
        windowSize = 3;
        final int upperLeftX = 20;
        final int upperLeftY = 42;
        final int centerX = upperLeftX + 1;
        final int centerY = upperLeftY + 1;
        final int width = 360;
        final int height = 180;

        final boolean exportMasks = false;
        final String filenamePrefix = "MeasurementReaderTest";
        final String expression = "expression";
        final boolean exportExpressionResult = true;

        final PixExRasterNamesFactory rasterNamesFactory = new PixExRasterNamesFactory(true, true, exportMasks, null);
        final PixExProductRegistry productRegistry = new PixExProductRegistry(filenamePrefix, inputDir);
        final PixExMeasurementFactory measurementFactory = new PixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                       productRegistry);
        final TargetWriterFactoryAndMap targetFactory = new TargetWriterFactoryAndMap(filenamePrefix, inputDir);
        final DefaultFormatStrategy formatStrategy = new DefaultFormatStrategy(rasterNamesFactory, windowSize, expression,
                                                                               exportExpressionResult);
        final MeasurementWriter writer = new MeasurementWriter(measurementFactory, targetFactory, formatStrategy);

        final String[] radianceNames = {"rad_1", "rad_2", "rad_3"};
        final Product p1 = MeasurementWriterTest.createTestProduct("N1", "T1", radianceNames, width, height);
        final Product p2 = MeasurementWriterTest.createTestProduct("N2", "T1", radianceNames, width, height);
        final String[] reflectNames = {"refl_1", "refl_2", "refl_3"};
        final Product p3 = MeasurementWriterTest.createTestProduct("N3", "T2", reflectNames, width, height);
        final RenderedOp renderedOp = ConstantDescriptor.create((float) width, (float) height, new Byte[]{-1}, null);
        final Raster validData = renderedOp.getData(new Rectangle(upperLeftX, upperLeftY, windowSize, windowSize));

        writer.writeMeasurements(centerX, centerY, 0, "coord" + 0, p1, validData);
        writer.writeMeasurements(centerX, centerY, 1, "coord" + 1, p1, validData);
        writer.writeMeasurements(centerX, centerY, 2, "coord" + 2, p2, validData);
        writer.writeMeasurements(centerX, centerY, 3, "coord" + 3, p2, validData);
        writer.writeMeasurements(centerX, centerY, 4, "coord" + 4, p3, validData);
        writer.writeMeasurements(centerX, centerY, 5, "coord" + 5, p3, validData);
    }

    @Test
    public void testReading() {
        final PixExMeasurementReader reader = new PixExMeasurementReader(inputDir);
        final ArrayList<Measurement> measurementList = new ArrayList<Measurement>();
        while (reader.hasNext()) {
            measurementList.add(reader.next());
        }
        assertEquals(windowSize * windowSize * 6, measurementList.size());
        testForExistingMeasurement(measurementList, 0, "coord" + 0);
        testForExistingMeasurement(measurementList, 1, "coord" + 1);
        testForExistingMeasurement(measurementList, 2, "coord" + 2);
        testForExistingMeasurement(measurementList, 3, "coord" + 3);
        testForExistingMeasurement(measurementList, 4, "coord" + 4);
        testForExistingMeasurement(measurementList, 5, "coord" + 5);
    }

    @Test
    public void testReadingWithEmptyColumns() throws Exception {
        final Measurement measurement = PixExMeasurementReader.readMeasurement(
                "12\t83744\t10083743\t57.936592\t10.130839\t520.5\t240.5\t2005-07-09\t10:12:03\t65.272634\t \t42.278252\t0\t500",
                false);
        assertEquals(12, measurement.getProductId());
        assertEquals(83744, measurement.getCoordinateID());
        assertEquals("10083743", measurement.getCoordinateName());
        assertEquals(57.936592f, measurement.getLat(), 1.0e-6);
        assertEquals(10.130839, measurement.getLon(), 1.0e-6);
        assertEquals(520.5, measurement.getPixelX(), 1.0e-6);
        assertEquals(240.5, measurement.getPixelY(), 1.0e-6);
        final Date expectedDate = ProductData.UTC.parse("2005-07-09T10:12:03", "yyyy-MM-dd'T'hh:mm:ss").getAsDate();
        assertEquals(expectedDate, measurement.getTime().getAsDate());
        final Object[] values = measurement.getValues();
        assertEquals(65.272634, ((Number) values[0]).doubleValue(), 1.0e-6);
        assertEquals(Double.NaN, ((Number) values[1]).doubleValue(), 1.0e-6);
        assertEquals(42.278252, ((Number) values[2]).doubleValue(), 1.0e-6);
        assertEquals(0, ((Number) values[3]).intValue());
        assertEquals(500, ((Number) values[4]).intValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveThrowsException() {
        final PixExMeasurementReader reader = new PixExMeasurementReader(inputDir);
        reader.remove();
    }

    private void testForExistingMeasurement(ArrayList<Measurement> measurementList, int coordId, String coordName) {
        for (Measurement measurement : measurementList) {
            if (measurement.getCoordinateID() == coordId &&
                    measurement.getCoordinateName().equals(coordName) &&
                    Double.compare(measurement.getPixelX(), 20.5) == 0 &&
                    Double.compare(measurement.getPixelY(), 42.5) == 0) {
                return;
            }
        }
        fail("Measurement with name '" + coordName + "' not found");
    }

}
