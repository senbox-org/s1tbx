package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class DoubleArrayConverterTest extends AbstractConverterTest {

    public DoubleArrayConverterTest() {
        super(new ArrayConverter(double[].class, new DoubleConverter()));
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(double[].class);
        testParseSuccess(new double[]{-2.3,-1.1,0.09,1.8,2.1,3.45,4.3}, "-2.3,-1.1, 0.09,1.8  \n,2.1,3.45,4.3");
        testFormatSuccess("-2.3,-1.1,0.09,1.8,2.1,3.45,4.3", new double[]{-2.3,-1.1,0.09,1.8,2.1,3.45,4.3});
        assertNullCorrectlyHandled();
    }
}