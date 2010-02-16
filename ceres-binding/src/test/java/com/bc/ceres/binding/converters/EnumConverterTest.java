package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EnumConverterTest extends AbstractConverterTest {

    private EnumConverter<Tests> converter;

    private enum Tests {

        TEST1("Description 1") {
            @Override
            public String toString() {
                return "Test 1";
            }
        },
        TEST2("Description 2") {
            @Override
            public String toString() {
                return "Test 2";
            }
        },
        TEST3("Description 3") {
            @Override
            public String toString() {
                return "Test 3";
            }
        };

        private String description;

        private Tests(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new EnumConverter<Tests>(Tests.class);
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Tests.class);

        testParseSuccess(Tests.TEST1, "TEST1");
        testParseSuccess(Tests.TEST2, "TEST2");
        testParseSuccess(Tests.TEST3, "TEST3");
        testParseSuccess(null, "");

        testFormatSuccess("Test 1", Tests.TEST1);
        testFormatSuccess("Test 2", Tests.TEST2);
        testFormatSuccess("Test 3", Tests.TEST3);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
