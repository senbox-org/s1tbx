/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UtilConstantsTest extends TestCase {

    public UtilConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(UtilConstantsTest.class);
    }

    public void testTheMessages() {
        assertEquals("Object is not an array.", UtilConstants.MSG_OBJ_NO_ARRAY);
        assertEquals("Object is null.", UtilConstants.MSG_OBJECT_NULL);
        assertEquals("String is null or empty.", UtilConstants.MSG_STRING_NULL_OR_EMPTY);

        assertEquals("An exception occurred: ", UtilConstants.MSG_EXCEPTION_OCCURRED);

        assertEquals("Method not implemented: ", UtilConstants.MSG_METHOD_NOT_IMPLEMENTED);

        assertEquals("Unable to initialize help system.", UtilConstants.MSG_INIT_HELP_FAILED);

        assertEquals("PlugInLoader: class not found: ", UtilConstants.MSG_CLASS_NOT_FOUND);
        assertEquals("PlugInLoader: directory not found: ", UtilConstants.MSG_DIR_NOT_FOUND);
        assertEquals("PlugInLoader: not a directory: ", UtilConstants.MSG_NOT_A_DIR);
        assertEquals("PlugInLoader: invalid directory path: ", UtilConstants.MSG_INVALID_DIR);
        assertEquals("PlugInLoader: invalid JAR path: ", UtilConstants.MSG_INVALID_JAR);
        assertEquals("No class loader set!", UtilConstants.MSG_NO_CLASS_LOADER);
        assertEquals("PlugInLoader: scanning directory: ", UtilConstants.MSG_SCAN_DIR);
        assertEquals("PlugInLoader: scanning JAR file: ", UtilConstants.MSG_SCAN_JAR);
        assertEquals("PlugInLoader: I/O error: ", UtilConstants.MSG_IO_ERR);

        assertEquals("Product without geo-coding.", UtilConstants.MSG_NO_GEO_CODING);

        assertEquals("Separators must not be null or empty.", UtilConstants.MSG_NULL_OR_EMPTY_SEPARATOR);
        assertEquals("Separators must not be null.", UtilConstants.MSG_NULL_SEPARATOR);
        assertEquals("Tokens must not be null.", UtilConstants.MSG_NULL_TOKEN);
    }

    public void testTheBufferNames() {
        assertEquals("DataBuffer.TYPE_BYTE", UtilConstants.BUFFER_BYTE_NAME);
        assertEquals("DataBuffer.TYPE_SHORT", UtilConstants.BUFFER_SHORT_NAME);
        assertEquals("DataBuffer.TYPE_USHORT", UtilConstants.BUFFER_USHORT_NAME);
        assertEquals("DataBuffer.TYPE_INT", UtilConstants.BUFFER_INT_NAME);
        assertEquals("DataBuffer.TYPE_FLOAT", UtilConstants.BUFFER_FLOAT_NAME);
        assertEquals("DataBuffer.TYPE_DOUBLE", UtilConstants.BUFFER_DOUBLE_NAME);
        assertEquals("DataBuffer.TYPE_UNDEFINED", UtilConstants.BUFFER_UNDEFINED_NAME);
        assertEquals("DataBuffer.TYPE_<?>", UtilConstants.BUFFER_UNKNOWN_NAME);
    }

    public void testColourSpaceConstants() {
        assertEquals("ColorSpace.TYPE_XYZ", UtilConstants.CS_TYPE_XYZ);
        assertEquals("ColorSpace.TYPE_Lab", UtilConstants.CS_TYPE_LAB);
        assertEquals("ColorSpace.TYPE_Luv", UtilConstants.CS_TYPE_LUV);
        assertEquals("ColorSpace.TYPE_YCbCr", UtilConstants.CS_TYPE_YCBCR);
        assertEquals("ColorSpace.TYPE_Yxy", UtilConstants.CS_TYPE_YXY);
        assertEquals("ColorSpace.TYPE_RGB", UtilConstants.CS_TYPE_RGB);
        assertEquals("ColorSpace.TYPE_GRAY", UtilConstants.CS_TYPE_GRAY);
        assertEquals("ColorSpace.TYPE_HSV", UtilConstants.CS_TYPE_HSV);
        assertEquals("ColorSpace.TYPE_HLS", UtilConstants.CS_TYPE_HLS);
        assertEquals("ColorSpace.TYPE_CMYK", UtilConstants.CS_TYPE_CMYK);
        assertEquals("ColorSpace.TYPE_CMY", UtilConstants.CS_TYPE_CMY);
        assertEquals("ColorSpace.TYPE_2CLR", UtilConstants.CS_TYPE_2CLR);
        assertEquals("ColorSpace.TYPE_3CLR", UtilConstants.CS_TYPE_3CLR);
        assertEquals("ColorSpace.TYPE_4CLR", UtilConstants.CS_TYPE_4CLR);
        assertEquals("ColorSpace.TYPE_5CLR", UtilConstants.CS_TYPE_5CLR);
        assertEquals("ColorSpace.TYPE_6CLR", UtilConstants.CS_TYPE_6CLR);
        assertEquals("ColorSpace.TYPE_7CLR", UtilConstants.CS_TYPE_7CLR);
        assertEquals("ColorSpace.TYPE_8CLR", UtilConstants.CS_TYPE_8CLR);
        assertEquals("ColorSpace.TYPE_9CLR", UtilConstants.CS_TYPE_9CLR);
        assertEquals("ColorSpace.TYPE_ACLR", UtilConstants.CS_TYPE_ACLR);
        assertEquals("ColorSpace.TYPE_BCLR", UtilConstants.CS_TYPE_BCLR);
        assertEquals("ColorSpace.TYPE_CCLR", UtilConstants.CS_TYPE_CCLR);
        assertEquals("ColorSpace.TYPE_DCLR", UtilConstants.CS_TYPE_DCLR);
        assertEquals("ColorSpace.TYPE_ECLR", UtilConstants.CS_TYPE_ECLR);
        assertEquals("ColorSpace.TYPE_FCLR", UtilConstants.CS_TYPE_FCLR);
        assertEquals("ColorSpace.TYPE_<?>", UtilConstants.CS_TYPE_UNKNOWN);
    }
}
