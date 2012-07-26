package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.FormatStrategy;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNull;

public class WriteWithOriginalOutputFormatStrategyTest {

    @Test
    public void testFindMatchingMeasurements_1() throws Exception {
        //preparation
        Measurement originalMeasurement1 = new Measurement(-1, "original1", -1, -1, -1, null, new GeoPos(70f, 50f),
                                                           new Number[0], true);
        Measurement originalMeasurement2 = new Measurement(-1, "original2", -1, -1, -1, null, new GeoPos(71f, 49f),
                                                           new Number[0], true);

        Measurement masterMeasurement = new Measurement(1, "master", 1234, 2, 2, null, new GeoPos(70f, 50f),
                                                        new Number[0], true);

        //execution
        final Measurement[] originalMeasurements = new Measurement[]{
                originalMeasurement1,
                originalMeasurement2
        };
        final WriteWithOriginalOutputFormatStrategy strategy = new WriteWithOriginalOutputFormatStrategy(
                originalMeasurements, null, -1, "", false);
        final Measurement matchingMeasurement = strategy.findMatchingMeasurement(masterMeasurement);

        //verification
        assertSame(originalMeasurement1, matchingMeasurement);
    }

    @Test
    public void testFindMatchingMeasurements_2() throws Exception {
        //preparation
        Measurement originalMeasurement1 = new Measurement(-1, "original1", -1, -1, -1, null, new GeoPos(70f, 50f),
                                                           new Number[0], true);
        Measurement originalMeasurement2 = new Measurement(-1, "original2", -1, -1, -1, null, new GeoPos(71f, 49f),
                                                           new Number[0], true);

        Measurement masterMeasurement = new Measurement(1, "master", 1234, 2, 2, null, new GeoPos(71f, 50f),
                                                        new Number[0], true);

        //execution
        final Measurement[] originalMeasurements = new Measurement[]{
                originalMeasurement1,
                originalMeasurement2
        };
        final WriteWithOriginalOutputFormatStrategy strategy = new WriteWithOriginalOutputFormatStrategy(
                originalMeasurements, null, -1, "", false);
        final Measurement matchingMeasurement = strategy.findMatchingMeasurement(masterMeasurement);

        //verification
        assertSame(originalMeasurement1, matchingMeasurement);
    }

    @Test
    public void testFindMatchingMeasurements_3() throws Exception {
        //preparation
        Measurement originalMeasurement1 = new Measurement(-1, "original1", -1, -1, -1, null, new GeoPos(70f, 50f),
                                                           new Number[0], true);
        Measurement originalMeasurement2 = new Measurement(-1, "original2", -1, -1, -1, null, new GeoPos(71f, 50f),
                                                           new Number[0], true);
        Measurement originalMeasurement3 = new Measurement(-1, "original2", -1, -1, -1, null, new GeoPos(72f, 50f),
                                                           new Number[0], true);

        Measurement masterMeasurement = new Measurement(1, "master", 1234, 2, 2, null, new GeoPos(71.01f, 50f),
                                                        new Number[0], true);

        //execution
        final Measurement[] originalMeasurements = new Measurement[]{
                originalMeasurement1,
                originalMeasurement2,
                originalMeasurement3
        };
        final WriteWithOriginalOutputFormatStrategy strategy = new WriteWithOriginalOutputFormatStrategy(
                originalMeasurements, null, -1, "", false);
        final Measurement matchingMeasurement = strategy.findMatchingMeasurement(masterMeasurement);

        //verification
        assertSame(originalMeasurement2, matchingMeasurement);
    }

    @Test
    public void testWriteMeasurements_oneMeasurement_withNaN() throws Exception {
        // preparation
        final Number[] values = {12.4, Double.NaN, 1.0345, 7};
        final Measurement measurement = new Measurement(14, "name", 13, 1, 1, null, new GeoPos(10, 10), values, true);

        final Number[] originalValues = {13.4, Double.NaN, 2.0345, 12};
        final Measurement originalMeasurement = new Measurement(-1, null, -1, -1, -1, null, new GeoPos(10.1F, 10.01F),
                                                                originalValues, true);

        final FormatStrategy pixExFormat = new WriteWithOriginalOutputFormatStrategy(
                new Measurement[]{originalMeasurement},
                null, 1, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter();
        pixExFormat.writeMeasurements(new PrintWriter(stringWriter), new Measurement[]{measurement});

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertEquals("10.100000\t10.010000\t13.4\t\t2.0345\t12" +      // original measurement
                     "\t13\t14\tname\t10.000000\t10.000000\t1.000\t1.000\t \t \t12.4\t\t1.0345\t7", reader.readLine());
        assertNull(reader.readLine());
    }
}
