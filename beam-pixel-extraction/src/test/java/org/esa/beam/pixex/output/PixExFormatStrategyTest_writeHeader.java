package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.writer.FormatStrategy;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PixExFormatStrategyTest_writeHeader {

    @Test
    public void testWriteHeaderWithExpression() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"rad_1", "rad_2", "uncert"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new PixExFormatStrategy(rasterNamesFactory, 9, "expression", true);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 9"));
        assertThat(reader.readLine(), equalTo("# Expression: expression"));
        assertThat(reader.readLine(), startsWith("# Created on:\t" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(), equalTo("Expression result\tProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\trad_1\trad_2\tuncert"));

    }

    @Test
    public void testWriteHeaderWithExpression_NotExporting() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"rad_1", "rad_2", "uncert"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new PixExFormatStrategy(rasterNamesFactory, 9, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 9"));
        assertThat(reader.readLine(), equalTo("# Expression: expression"));
        assertThat(reader.readLine(), startsWith("# Created on:\t" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(), equalTo("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\trad_1\trad_2\tuncert"));
    }

    @Test
    public void testWriteHeaderWithoutExpression() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"varA", "varB", "var C"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new PixExFormatStrategy(rasterNamesFactory, 3, null, false);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 3"));
        assertThat(reader.readLine(), startsWith("# Created on:\t" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(), equalTo("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\tvarA\tvarB\tvar C"));
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Matcher<String> startsWith(final String start) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return ((String) o).startsWith(start);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Does not start with \"" + start + "\"");
            }
        };
    }

    private RasterNamesFactory newRasterNamesFactory(final String[] rasterNames) {
        return new RasterNamesFactory() {
            @Override
            public String[] getRasterNames(Product product) {
                return rasterNames;
            }
        };
    }
}
