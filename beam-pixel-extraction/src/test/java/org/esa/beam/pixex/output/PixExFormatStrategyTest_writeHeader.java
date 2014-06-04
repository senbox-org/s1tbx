/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.measurement.writer.FormatStrategy;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PixExFormatStrategyTest_writeHeader {

    @Test
    public void testWriteHeaderWithExpression() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"rad_1", "rad_2", "uncert"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new DefaultFormatStrategy(rasterNamesFactory, 9, "expression", true);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 9"));
        assertThat(reader.readLine(), equalTo("# Expression: expression"));
        assertThat(reader.readLine(), startsWith(
                "# Created on:\t" + ProductData.UTC.createDateFormat("yyyy-MM-dd").format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(),
                   equalTo("Expression result\tProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH_mm_ss)\trad_1\trad_2\tuncert"));

    }

    @Test
    public void testWriteHeaderWithExpression_NotExporting() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"rad_1", "rad_2", "uncert"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new DefaultFormatStrategy(rasterNamesFactory, 9, "expression", false);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 9"));
        assertThat(reader.readLine(), equalTo("# Expression: expression"));
        assertThat(reader.readLine(), startsWith(
                "# Created on:\t" + ProductData.UTC.createDateFormat("yyyy-MM-dd").format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(),
                   equalTo("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH_mm_ss)\trad_1\trad_2\tuncert"));
    }

    @Test
    public void testWriteHeaderWithoutExpression() throws Exception {
        // preparation
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(new String[]{"varA", "varB", "var C"});
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new DefaultFormatStrategy(rasterNamesFactory, 3, null, false);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), null);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 3"));
        assertThat(reader.readLine(), startsWith(
                "# Created on:\t" + ProductData.UTC.createDateFormat("yyyy-MM-dd").format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(),
                   equalTo("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH_mm_ss)\tvarA\tvarB\tvar C"));
    }

    @Test
    public void testWriteHeaderWithoutExpression_WithProduct() throws Exception {
        // preparation
        String[] rasterNames = {"varA", "varB", "var C"};
        final RasterNamesFactory rasterNamesFactory = newRasterNamesFactory(rasterNames);
        final FormatStrategy pixExFormatStrategy;
        pixExFormatStrategy = new DefaultFormatStrategy(rasterNamesFactory, 3, null, false);

        // execution
        final StringWriter stringWriter = new StringWriter(200);
        Product product = new Product("p", "t", 10, 10);
        float wavelength = 500.0f;
        for (String rasterName : rasterNames) {
            Band band = product.addBand(rasterName, ProductData.TYPE_INT32);
            band.setSpectralWavelength(wavelength++);
        }
        pixExFormatStrategy.writeHeader(new PrintWriter(stringWriter), product);

        // verifying
        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        assertThat(reader.readLine(), equalTo("# BEAM pixel extraction export table"));
        assertThat(reader.readLine(), equalTo("#"));
        assertThat(reader.readLine(), equalTo("# Window size: 3"));
        assertThat(reader.readLine(), startsWith(
                "# Created on:\t" + ProductData.UTC.createDateFormat("yyyy-MM-dd").format(new Date())));
        assertThat(reader.readLine(), equalTo(""));
        assertThat(reader.readLine(), startsWith("# Wavelength:\t \t \t \t \t \t \t \t \t500.0\t501.0\t502.0"));
        assertThat(reader.readLine(),
                   equalTo("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH_mm_ss)\tvarA\tvarB\tvar C"));
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

            @Override
            public String[] getUniqueRasterNames(Product product) {
                return getRasterNames(product);
            }
        };
    }
}
