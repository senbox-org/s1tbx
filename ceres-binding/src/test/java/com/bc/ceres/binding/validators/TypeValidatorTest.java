package com.bc.ceres.binding.validators;

import junit.framework.TestCase;
import com.bc.ceres.binding.*;

public class TypeValidatorTest extends TestCase {
    private boolean booleanValue;
    private char charValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    private float floatValue;
    private double doubleValue;
    private String stringValue;
    private TestCase baseValue;
    private TypeValidatorTest derivedValue;

    public void testValidation() throws ValidationException {
        final TypeValidator typeValidator = new TypeValidator();
        final ValueContainer valueContainer = ValueContainer.createObjectBacked(this);

        String s;

        s = "booleanValue";
        testValid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "byteValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testValid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "shortValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testValid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "intValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testValid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "longValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testValid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "floatValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testValid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "doubleValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testValid(typeValidator, valueContainer, s, 123.0);
        testInvalid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "stringValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testValid(typeValidator, valueContainer, s, null);
        testValid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testInvalid(typeValidator, valueContainer, s, this);

        s = "baseValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testValid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testValid(typeValidator, valueContainer, s, new TestCase(){});
        testValid(typeValidator, valueContainer, s, this);

        s = "derivedValue";
        testInvalid(typeValidator, valueContainer, s, true);
        testInvalid(typeValidator, valueContainer, s, (char)123);
        testInvalid(typeValidator, valueContainer, s, (byte)123);
        testInvalid(typeValidator, valueContainer, s, (short)123);
        testInvalid(typeValidator, valueContainer, s, 123);
        testInvalid(typeValidator, valueContainer, s, 123L);
        testInvalid(typeValidator, valueContainer, s, 123.0F);
        testInvalid(typeValidator, valueContainer, s, 123.0);
        testValid(typeValidator, valueContainer, s, null);
        testInvalid(typeValidator, valueContainer, s, "xyz");
        testInvalid(typeValidator, valueContainer, s, new TestCase(){});
        testValid(typeValidator, valueContainer, s, this);
    }

    private void testInvalid(TypeValidator typeValidator, ValueContainer valueContainer, String name, Object invalidValue){
        final ValueModel valueModel = valueContainer.getModel(name);
        try {
            typeValidator.validateValue(valueModel, invalidValue);
            fail("ValidationException expected");
        } catch (ValidationException e) {
            // expected
            try {
                valueModel.setValue(invalidValue);
                fail("ValidationException expected");
            } catch (ValidationException e1) {
                // expected
            }
        }
    }

    private void testValid(TypeValidator typeValidator, ValueContainer valueContainer, String name, Object validValue) {
        final ValueModel valueModel = valueContainer.getModel(name);
        try {
            typeValidator.validateValue(valueModel, validValue);
            try {
                valueModel.setValue(validValue);
            } catch (ValidationException e) {
                fail("ValidationException not expected");
                e.printStackTrace();
            }
        } catch (ValidationException e) {
            fail("ValidationException not expected");
            e.printStackTrace();
        }
    }
}
