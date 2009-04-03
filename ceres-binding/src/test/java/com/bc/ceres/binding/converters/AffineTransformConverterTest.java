package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.awt.geom.AffineTransform;

public class AffineTransformConverterTest extends AbstractConverterTest {

    private AffineTransformConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new AffineTransformConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(AffineTransform.class);

        testParseSuccess(new AffineTransform(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), "1.0, 2.0,3.0,4.0,5.0,6.0");
        testParseSuccess(new AffineTransform(1.0, 2.0, 3.0, 4.0, 0.0, 0.0), "1.0,\t2.0,3.0,4.0");
        testParseSuccess(null, "");

        testFormatSuccess("1.0,2.0,3.0,4.0,5.0,6.0", new AffineTransform(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();

        try {
            converter.parse("1.0,2.0");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }
    }
}
