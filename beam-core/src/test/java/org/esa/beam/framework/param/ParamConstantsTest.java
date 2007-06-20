/*
 * $Id: ParamConstantsTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.framework.param;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ParamConstantsTest extends TestCase {

    public ParamConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ParamConstantsTest.class);
    }

    public void testLogMessages() {
        assertEquals("Invalid value set entry for parameter '", ParamConstants.LOG_MSG_INVALID_VALUE_SET);

        assertEquals("A parameter with name '", ParamConstants.ERR_MSG_PARAM_IN_GROUP_1);
        assertEquals("' is already in group!", ParamConstants.ERR_MSG_PARAM_IN_GROUP_2);

        assertEquals("Invalid bitmask expression:\n", ParamConstants.ERR_MSG_INVALID_BITMASK);

        assertEquals("Value must be one of true or false.", ParamConstants.ERR_MSG_INVALID_BOOLEAN);
        assertEquals("Not a boolean value!", ParamConstants.ERR_MSG_NOT_BOOLEAN);
        assertEquals("Value must be of type boolean (true or false).", ParamConstants.ERR_MSG_NOT_BOOLEAN_TYPE);

        assertEquals(
                "Color value must have the form R,G,B or R,G,B,Alpha (decimal)\nor #RRGGBB or #RRGGBBAA (hexadecimal).",
                ParamConstants.ERR_MSG_INVALID_COLOR);
        assertEquals("Value must be of type 'Color'", ParamConstants.ERR_MSG_NOT_COLOR_TYPE);
        assertEquals("Value must be a R,G,B color.", ParamConstants.ERR_MSG_NOT_COLOR);

        assertEquals("Not a file path.", ParamConstants.ERR_MSG_NOT_FILE);
        assertEquals("Value must be a file path.", ParamConstants.ERR_MSG_MUST_BE_FILE);

        assertEquals("Unsupported number format.", ParamConstants.ERR_MSG_UNSUPP_FORMAT);
        assertEquals("Value must be a %s number.", ParamConstants.ERR_MSG_MUST_BE_NUMBER);
        assertEquals("Value must be a integer number.",
                     String.format(ParamConstants.ERR_MSG_MUST_BE_NUMBER,
                                   new Object[]{Integer.class.getSimpleName().toLowerCase()}));
        assertEquals("Value of type 'Number' expected.", ParamConstants.ERR_MSG_NOT_NUMBER_TYPE);
        assertEquals("Value must be in the range ", ParamConstants.ERR_MSG_VALUE_IN_RANGE);
        assertEquals("Value must greater or equal ", ParamConstants.ERR_MSG_VALUE_GREATER);
        assertEquals("Value must less or equal ", ParamConstants.ERR_MSG_VALUE_LESS);

        assertEquals("Value of type 'String[]' expected.", ParamConstants.ERR_MSG_NOT_STRINGARRAY);
        assertEquals("Value must be contained in predefined value set.", ParamConstants.ERR_MSG_NOT_CONTAINED);
        assertEquals("Value must be an array of text strings.", ParamConstants.ERR_MSG_NOT_STRINGARRAY_TYPE);
        assertEquals("Value must not be empty.", ParamConstants.ERR_MSG_EMPTY);

        assertEquals("Not a string value.", ParamConstants.ERR_MSG_NOT_A_STRING);
        assertEquals("Value must be a text string.", ParamConstants.ERR_MSG_MUST_BE_STRING);
        assertEquals("Value must be an identifier.\n\n "
                     + "A valid identifier starts with a letter or underscore and is\n"
                     + "followed by letters, digits or underscores.", ParamConstants.ERR_MSG_NO_IDENTIFIER);
    }
}
