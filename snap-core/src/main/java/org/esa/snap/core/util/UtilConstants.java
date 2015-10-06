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

/**
 * Stores some constants used in this package.
 */
public class UtilConstants {

    // Array Utils
    public static final String MSG_OBJ_NO_ARRAY = "Object is not an array.";
    public static final String MSG_OBJECT_NULL = "Object is null.";
    public static final String MSG_STRING_NULL_OR_EMPTY = "String is null or empty.";

    // Debug
    public static final String MSG_EXCEPTION_OCCURRED = "An exception occurred: ";
    public static final String MSG_METHOD_NOT_IMPLEMENTED = "Method not implemented: ";

    // HelpSys
    public static final String MSG_INIT_HELP_FAILED = "Unable to initialize help system.";

    // PluginLoader
    public static final String MSG_NO_CLASS_DEF_FOUND = "PlugInLoader: no class def. found: ";
    public static final String MSG_CLASS_NOT_FOUND = "PlugInLoader: class not found: ";
    public static final String MSG_DIR_NOT_FOUND = "PlugInLoader: directory not found: ";
    public static final String MSG_NOT_A_DIR = "PlugInLoader: not a directory: ";
    public static final String MSG_INVALID_DIR = "PlugInLoader: invalid directory path: ";
    public static final String MSG_INVALID_JAR = "PlugInLoader: invalid JAR path: ";
    public static final String MSG_NO_CLASS_LOADER = "No class loader set!";
    public static final String MSG_SCAN_DIR = "PlugInLoader: scanning directory: ";
    public static final String MSG_SCAN_JAR = "PlugInLoader: scanning JAR file: ";
    public static final String MSG_IO_ERR = "PlugInLoader: I/O error: ";

    // ProductUtils
    public static final String MSG_NO_GEO_CODING = "Product without geo-coding.";

    //StringUtils
    public static final String MSG_NULL_OR_EMPTY_SEPARATOR = "Separators must not be null or empty.";
    public static final String MSG_NULL_SEPARATOR = "Separators must not be null.";
    public static final String MSG_NULL_TOKEN = "Tokens must not be null.";

    // ImageUtils - Buffer names
    public static final String BUFFER_BYTE_NAME = "DataBuffer.TYPE_BYTE";
    public static final String BUFFER_SHORT_NAME = "DataBuffer.TYPE_SHORT";
    public static final String BUFFER_USHORT_NAME = "DataBuffer.TYPE_USHORT";
    public static final String BUFFER_INT_NAME = "DataBuffer.TYPE_INT";
    public static final String BUFFER_FLOAT_NAME = "DataBuffer.TYPE_FLOAT";
    public static final String BUFFER_DOUBLE_NAME = "DataBuffer.TYPE_DOUBLE";
    public static final String BUFFER_UNDEFINED_NAME = "DataBuffer.TYPE_UNDEFINED";
    public static final String BUFFER_UNKNOWN_NAME = "DataBuffer.TYPE_<?>";

    // ImageUtils - Color space names
    public static final String CS_TYPE_XYZ = "ColorSpace.TYPE_XYZ";
    public static final String CS_TYPE_LAB = "ColorSpace.TYPE_Lab";
    public static final String CS_TYPE_LUV = "ColorSpace.TYPE_Luv";
    public static final String CS_TYPE_YCBCR = "ColorSpace.TYPE_YCbCr";
    public static final String CS_TYPE_YXY = "ColorSpace.TYPE_Yxy";
    public static final String CS_TYPE_RGB = "ColorSpace.TYPE_RGB";
    public static final String CS_TYPE_GRAY = "ColorSpace.TYPE_GRAY";
    public static final String CS_TYPE_HSV = "ColorSpace.TYPE_HSV";
    public static final String CS_TYPE_HLS = "ColorSpace.TYPE_HLS";
    public static final String CS_TYPE_CMYK = "ColorSpace.TYPE_CMYK";
    public static final String CS_TYPE_CMY = "ColorSpace.TYPE_CMY";
    public static final String CS_TYPE_2CLR = "ColorSpace.TYPE_2CLR";
    public static final String CS_TYPE_3CLR = "ColorSpace.TYPE_3CLR";
    public static final String CS_TYPE_4CLR = "ColorSpace.TYPE_4CLR";
    public static final String CS_TYPE_5CLR = "ColorSpace.TYPE_5CLR";
    public static final String CS_TYPE_6CLR = "ColorSpace.TYPE_6CLR";
    public static final String CS_TYPE_7CLR = "ColorSpace.TYPE_7CLR";
    public static final String CS_TYPE_8CLR = "ColorSpace.TYPE_8CLR";
    public static final String CS_TYPE_9CLR = "ColorSpace.TYPE_9CLR";
    public static final String CS_TYPE_ACLR = "ColorSpace.TYPE_ACLR";
    public static final String CS_TYPE_BCLR = "ColorSpace.TYPE_BCLR";
    public static final String CS_TYPE_CCLR = "ColorSpace.TYPE_CCLR";
    public static final String CS_TYPE_DCLR = "ColorSpace.TYPE_DCLR";
    public static final String CS_TYPE_ECLR = "ColorSpace.TYPE_ECLR";
    public static final String CS_TYPE_FCLR = "ColorSpace.TYPE_FCLR";
    public static final String CS_TYPE_UNKNOWN = "ColorSpace.TYPE_<?>";
}
