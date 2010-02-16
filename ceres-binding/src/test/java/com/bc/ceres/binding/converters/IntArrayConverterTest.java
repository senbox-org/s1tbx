package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class IntArrayConverterTest extends AbstractConverterTest {

    private ArrayConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ArrayConverter(int[].class, new IntegerConverter());
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(int[].class);
        testParseSuccess(new int[]{-2, -1, 0, 1, 2, 3, 4}, "-2,  -1,0  ,1,\t2,3,4");
        testFormatSuccess("-2,-1,0,1,2,3,4", new int[]{-2, -1, 0, 1, 2, 3, 4});
        assertNullCorrectlyHandled();
    }
}