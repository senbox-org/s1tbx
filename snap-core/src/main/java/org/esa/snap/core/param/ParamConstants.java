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
package org.esa.snap.core.param;

public class ParamConstants {

    public static final String LOG_MSG_INVALID_VALUE_SET = "Invalid value set entry for parameter '"; /*I18N*/
    public static final String ERR_MSG_PARAM_IN_GROUP_1 = "A parameter with name '";    /*I18N*/
    public static final String ERR_MSG_PARAM_IN_GROUP_2 = "' is already in group!";    /*I18N*/

    public static final String ERR_MSG_INVALID_BITMASK = "Invalid bitmask expression:\n";    /*I18N*/
    public static final String ERR_MSG_INVALID_EXPRESSION = "Invalid expression:\n";   /*I18N*/

    public static final String ERR_MSG_INVALID_BOOLEAN = "Value must be one of true or false.";  /*I18N*/
    public static final String ERR_MSG_NOT_BOOLEAN = "Not a boolean value!";                /*I18N*/
    public static final String ERR_MSG_NOT_BOOLEAN_TYPE = "Value must be of type boolean (true or false)."; /*I18N*/

    public static final String ERR_MSG_INVALID_COLOR = "Color value must have the form R,G,B or R,G,B,Alpha (decimal)\nor #RRGGBB or #RRGGBBAA (hexadecimal).";/*I18N*/
    public static final String ERR_MSG_NOT_COLOR_TYPE = "Value must be of type 'Color'"; /*I18N*/
    public static final String ERR_MSG_NOT_COLOR = "Value must be a R,G,B color.";               /*I18N*/

    public static final String ERR_MSG_NOT_FILE = "Not a file path.";                                    /*I18N*/
    public static final String ERR_MSG_MUST_BE_FILE = "Value must be a file path.";                              /*I18N*/

    public static final String ERR_MSG_UNSUPP_FORMAT = "Unsupported number format.";            /*I18N*/
    public static final String ERR_MSG_MUST_BE_NUMBER = "Value must be a %s number.";      /*I18N*/
    public static final String ERR_MSG_NOT_NUMBER_TYPE = "Value of type 'Number' expected.";          /*I18N*/
    public static final String ERR_MSG_VALUE_IN_RANGE = "Value must be in the range ";                      /*I18N*/
    public static final String ERR_MSG_VALUE_GREATER = "Value must greater or equal ";                            /*I18N*/
    public static final String ERR_MSG_VALUE_LESS = "Value must less or equal ";                                        /*I18N*/

    public static final String ERR_MSG_NOT_STRINGARRAY = "Value of type 'String[]' expected.";/*I18N*/
    public static final String ERR_MSG_NOT_STRINGARRAY_TYPE = "Value must be an array of text strings.";/*I18N*/
    public static final String ERR_MSG_NOT_CONTAINED = "Value must be contained in predefined value set.";/*I18N*/
    public static final String ERR_MSG_EMPTY = "Value must not be empty.";/*I18N*/

    public static final String ERR_MSG_NOT_A_STRING = "Not a string value.";/*I18N*/
    public static final String ERR_MSG_MUST_BE_STRING = "Value must be a text string.";/*I18N*/
    public static final String ERR_MSG_NO_IDENTIFIER = "Value must be an identifier.\n\n "
                                                       + "A valid identifier starts with a letter or underscore and is\n"
                                                       + "followed by letters, digits or underscores.";/*I18N*/
}
