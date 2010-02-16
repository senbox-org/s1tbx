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
        final PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this);

        String s;

        s = "booleanValue";
        testValid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "byteValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testValid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "shortValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testValid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "intValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testValid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "longValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testValid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "floatValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testValid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "doubleValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testValid(typeValidator, propertyContainer, s, 123.0);
        testInvalid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "stringValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testValid(typeValidator, propertyContainer, s, null);
        testValid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testInvalid(typeValidator, propertyContainer, s, this);

        s = "baseValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testValid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testValid(typeValidator, propertyContainer, s, new TestCase(){});
        testValid(typeValidator, propertyContainer, s, this);

        s = "derivedValue";
        testInvalid(typeValidator, propertyContainer, s, true);
        testInvalid(typeValidator, propertyContainer, s, (char)123);
        testInvalid(typeValidator, propertyContainer, s, (byte)123);
        testInvalid(typeValidator, propertyContainer, s, (short)123);
        testInvalid(typeValidator, propertyContainer, s, 123);
        testInvalid(typeValidator, propertyContainer, s, 123L);
        testInvalid(typeValidator, propertyContainer, s, 123.0F);
        testInvalid(typeValidator, propertyContainer, s, 123.0);
        testValid(typeValidator, propertyContainer, s, null);
        testInvalid(typeValidator, propertyContainer, s, "xyz");
        testInvalid(typeValidator, propertyContainer, s, new TestCase(){});
        testValid(typeValidator, propertyContainer, s, this);
    }

    private void testInvalid(TypeValidator typeValidator, PropertyContainer propertyContainer, String name, Object invalidValue){
        final Property property = propertyContainer.getProperty(name);
        try {
            typeValidator.validateValue(property, invalidValue);
            fail("ValidationException expected");
        } catch (ValidationException e) {
            // expected
            try {
                property.setValue(invalidValue);
                fail("ValidationException expected");
            } catch (ValidationException e1) {
                // expected
            }
        }
    }

    private void testValid(TypeValidator typeValidator, PropertyContainer propertyContainer, String name, Object validValue) {
        final Property property = propertyContainer.getProperty(name);
        try {
            typeValidator.validateValue(property, validValue);
            try {
                property.setValue(validValue);
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
