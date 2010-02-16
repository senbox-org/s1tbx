package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class DoubleArrayConverterTest extends AbstractConverterTest {

    private ArrayConverter converter;


    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ArrayConverter(double[].class, new DoubleConverter());
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(double[].class);
        testParseSuccess(new double[]{-2.3, -1.1, 0.09, 1.8, 2.1, 3.45, 4.3}, "-2.3,-1.1, 0.09,1.8  \n,2.1,3.45,4.3");
        testFormatSuccess("-2.3,-1.1,0.09,1.8,2.1,3.45,4.3", new double[]{-2.3, -1.1, 0.09, 1.8, 2.1, 3.45, 4.3});
        assertNullCorrectlyHandled();
    }
}