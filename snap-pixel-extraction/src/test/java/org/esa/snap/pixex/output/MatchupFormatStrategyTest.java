package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.FormatStrategy;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MatchupFormatStrategyTest {

    @Test
    public void testWriteMeasurements_oneMeasurement_withNaN() throws Exception {
        // preparation
        final Number[] values = {12.4, Double.NaN, 1.0345, 7};
        final Measurement measurement = new Measurement(14, "name", 13, 1, 1, null, new GeoPos(10, 10), values, true);

        final Number[] originalValues = {13.4, Double.NaN, 2.0345, 12};
        final Measurement originalMeasurement = new Measurement(14, null, -1, -1, -1, null, new GeoPos(10.1F, 10.01F),
                                                                originalValues, new String[]{"sst", "tsm", "pigs", "cows"}, true);

        final FormatStrategy pixExFormat = new MatchupFormatStrategy(
                new Measurement[]{originalMeasurement},
                null, 1, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter();
        pixExFormat.writeMeasurements(null, new PrintWriter(stringWriter), new Measurement[]{measurement});

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String originalMeasurementString = "13.4\t\t2.0345\t12";
        String newMeasurementString = "\t13\t14\tname\t10.000000\t10.000000\t1.000\t1.000\t \t \t12.4\t\t1.0345\t7";
        assertEquals(originalMeasurementString + newMeasurementString, reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testWriteTwoMeasurements() throws Exception {
        final Measurement originalMeasurement1 = createMeasurement(14, new String[]{"sst", "tsm"}, "_1");
        final Measurement originalMeasurement2 = createMeasurement(23, new String[]{"pigs", "cows"}, "_2");

        final Number[] values = {12.4, 7};
        Measurement additionalMeasurement = new Measurement(14, "name", 13, 1, 1, null, new GeoPos(10, 10), values, true);

        final FormatStrategy pixExFormat = new MatchupFormatStrategy(
                new Measurement[]{
                        originalMeasurement1,
                        originalMeasurement2
                },
                null, 1, "expression", false);

        StringWriter stringWriter = new StringWriter();
        pixExFormat.writeMeasurements(null, new PrintWriter(stringWriter), new Measurement[]{additionalMeasurement});

        String originalMeasurementString = "value1_1\tvalue2_1\t\t";
        String newMeasurementString = "\t13\t14\tname\t10.000000\t10.000000\t1.000\t1.000\t \t \t12.4\t7\n";
        assertEquals(originalMeasurementString + newMeasurementString, stringWriter.toString());

        additionalMeasurement = new Measurement(23, "name", 13, 1, 1, null, new GeoPos(10, 10), values, true);
        stringWriter = new StringWriter();

        pixExFormat.writeMeasurements(null, new PrintWriter(stringWriter), new Measurement[]{additionalMeasurement});

        originalMeasurementString = "\t\tvalue1_2\tvalue2_2";
        newMeasurementString = "\t13\t23\tname\t10.000000\t10.000000\t1.000\t1.000\t \t \t12.4\t7\n";
        assertEquals(originalMeasurementString + newMeasurementString, stringWriter.toString());
    }

    private static Measurement createMeasurement(int coordinateId, String[] attributeNames, String msmntIndex) {
        final Object[] originalValues = {"value1" + msmntIndex, "value2" + msmntIndex};
        return new Measurement(coordinateId, null, -1, -1, -1, null, new GeoPos(10.1F, 10.01F),
                               originalValues, attributeNames, true);
    }
}
