package org.esa.beam.util.converters;

import junit.framework.TestCase;
import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import java.util.Date;

import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.converters.JtsGeometryConverter;


public class JavaTypeConverterTest extends TestCase {
    static {
        JtsGeometryConverter.registerConverter();
    }

    public void testSuccess() throws ConversionException {
        JavaTypeConverter typeConverter = new JavaTypeConverter();

        assertEquals(String.class, typeConverter.parse("String"));
        assertEquals(Integer.class, typeConverter.parse("Integer"));
        assertEquals(Geometry.class, typeConverter.parse("Geometry"));
        assertEquals(Polygon.class, typeConverter.parse("Polygon"));
        assertEquals(Date.class, typeConverter.parse("Date"));
        assertEquals(JavaTypeConverter.class, typeConverter.parse("org.esa.beam.util.converters.JavaTypeConverter"));

        assertEquals("String", typeConverter.format(String.class));
        assertEquals("Integer", typeConverter.format(Integer.class));
        assertEquals("Geometry", typeConverter.format(Geometry.class));
        assertEquals("Polygon", typeConverter.format(Polygon.class));
        assertEquals("Date", typeConverter.format(Date.class));
        assertEquals("org.esa.beam.util.converters.JavaTypeConverter", typeConverter.format(JavaTypeConverter.class));
    }

    public void testFailure()  {
        JavaTypeConverter typeConverter = new JavaTypeConverter();

        try {
            typeConverter.parse("");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            typeConverter.parse("string");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            typeConverter.parse("java.util.String");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }
    }
}
