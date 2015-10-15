package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.measurement.Measurement;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PixExFormatStrategyTest_writeMeasurement {


    private RasterNamesFactory dummyRasterNamesFactory;

    @Before
    public void setUp() throws Exception {
        dummyRasterNamesFactory = newDummyRasterNamesFactory();
    }

    @Test
    public void testWriteMeasurements_oneMeasurement_withNaN() throws Exception {
        // preparation
        final Measurement measurement = newMeasurement(12, new Number[]{12.4, Double.NaN, 1.0345, 7}, true);
        final Measurement[] oneMeasurement = {measurement};
        final DefaultFormatStrategy defaultFormat;
        defaultFormat = new DefaultFormatStrategy(dummyRasterNamesFactory, 1, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter();
        defaultFormat.writeMeasurements(null, new PrintWriter(stringWriter), oneMeasurement);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(),
                   equalTo("14\t13\tname12\t20.799999\t21.900000\t15.300\t16.400\t2000-01-18\t00:00:18\t12.4\t\t1.0345\t7"));
        assertThat(reader.readLine(), equalTo(null));
    }

    @Test
    public void testWriteMeasurements_oneMeasurement_withoutDate() throws Exception {
        // preparation
        final Measurement measurement = newMeasurementWithoutDate(12, new Number[]{12.4, Double.NaN, 1.0345, 7}, true);
        final Measurement[] oneMeasurement = {measurement};
        final DefaultFormatStrategy defaultFormat;
        defaultFormat = new DefaultFormatStrategy(dummyRasterNamesFactory, 1, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter();
        defaultFormat.writeMeasurements(null, new PrintWriter(stringWriter), oneMeasurement);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(),
                   equalTo("14\t13\tname12\t20.799999\t21.900000\t15.300\t16.400\t \t \t12.4\t\t1.0345\t7"));
        assertThat(reader.readLine(), equalTo(null));
    }

    @Test
    public void testWriteMeasurements_twoMeasurements_() throws Exception {
        // preparation
        final Measurement measurement1 = newMeasurement(1, new Number[]{12.4, Double.NaN, 1.0345, 7}, true);
        final Measurement measurement2 = newMeasurement(2, new Number[]{14.4, 2.345, 1.666, 8}, false);
        final DefaultFormatStrategy defaultFormat;
        final boolean exportInvalids = true;
        defaultFormat = new DefaultFormatStrategy(dummyRasterNamesFactory, 1, "expression", exportInvalids);

        // execution
        final StringWriter stringWriter = new StringWriter();
        defaultFormat.writeMeasurements(null, new PrintWriter(stringWriter), new Measurement[]{measurement1, measurement2});

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(),
                   equalTo("true\t3\t2\tname1\t9.800000\t10.900000\t4.300\t5.400\t2000-01-07\t00:00:07\t12.4\t\t1.0345\t7"));
        assertThat(reader.readLine(),
                   equalTo("false\t4\t3\tname2\t10.800000\t11.900000\t5.300\t6.400\t2000-01-08\t00:00:08\t14.4\t2.345\t1.666\t8"));
        assertThat(reader.readLine(), equalTo(null));
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement newMeasurementWithoutDate(int offset, final Number[] values, final boolean valid) {
        return newMeasurement(offset, values, valid, false);
    }

    private Measurement newMeasurement(int offset, final Number[] values, final boolean valid) {
        return newMeasurement(offset, values, valid, true);
    }

    private Measurement newMeasurement(int offset, final Number[] values, final boolean valid,
                                       final boolean createDate) {
        final int coordinateID = 1 + offset;
        final String name = "name" + offset;
        final int productId = 2 + offset;
        final float pixelX = 3.3f + offset;
        final float pixelY = 4.4f + offset;

        final ProductData.UTC time;
        if (createDate) {
            final int days = 5 + offset;
            final int seconds = 6 + offset;
            final int microSeconds = 7 + offset;
            time = new ProductData.UTC(days, seconds, microSeconds);
        } else {
            time = null;
        }

        final float lat = 8.8f + offset;
        final float lon = 9.9f + offset;
        final GeoPos geoPos = new GeoPos(lat, lon);

        return new Measurement(coordinateID, name, productId, pixelX, pixelY, time, geoPos, values, valid);
    }

    private RasterNamesFactory newDummyRasterNamesFactory() {
        return new RasterNamesFactory() {
            @Override
            public String[] getRasterNames(Product product) {
                return new String[0];  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public String[] getUniqueRasterNames(Product product) {
                return getRasterNames(product);
            }
        };
    }
}
