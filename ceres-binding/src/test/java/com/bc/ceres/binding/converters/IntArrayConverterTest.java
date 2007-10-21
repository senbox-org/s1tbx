package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class IntArrayConverterTest extends AbstractConverterTest {

    public IntArrayConverterTest() {
        super(new ArrayConverter(int[].class, new IntegerConverter()));
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(int[].class);
        testParseSuccess(new int[]{-2,-1,0,1,2,3,4}, "-2,  -1,0  ,1,\t2,3,4");
        testFormatSuccess("-2,-1,0,1,2,3,4", new int[]{-2,-1,0,1,2,3,4});
        assertNullCorrectlyHandled();
    }
}