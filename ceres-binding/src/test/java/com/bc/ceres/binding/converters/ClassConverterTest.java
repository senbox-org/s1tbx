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

package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ValueSet;

public class ClassConverterTest extends AbstractConverterTest {
    private ClassConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ClassConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Class.class);

        testParseSuccess(Character.TYPE, "char");
        testParseSuccess(Boolean.TYPE, "boolean");
        testParseSuccess(Byte.TYPE, "byte");
        testParseSuccess(Short.TYPE, "short");
        testParseSuccess(Integer.TYPE, "int");
        testParseSuccess(Float.TYPE, "float");
        testParseSuccess(Double.TYPE, "double");

        testFormatSuccess( "char", Character.TYPE);
        testFormatSuccess("boolean", Boolean.TYPE);
        testFormatSuccess( "byte", Byte.TYPE);
        testFormatSuccess( "short", Short.TYPE);
        testFormatSuccess( "int", Integer.TYPE);
        testFormatSuccess( "float", Float.TYPE);
        testFormatSuccess( "double", Double.TYPE);

        testParseSuccess(Integer.class, "Integer");
        testParseSuccess(Float.class, "Float");
        testParseSuccess(String.class, "String");

        testFormatSuccess("Integer", Integer.class);
        testFormatSuccess("Float", Float.class);
        testFormatSuccess("String", String.class);

        testParseSuccess(String[].class, "String[]");
        testParseSuccess(String[].class, "java.lang.String[]");
        testParseSuccess(String[].class, "[Ljava.lang.String;");
        testParseSuccess(Float[].class, "Float[]");
        testParseSuccess(ValueSet[].class, "com.bc.ceres.binding.ValueSet[]");

        testFormatSuccess("String[]", String[].class);
        testFormatSuccess("Float[]", Float[].class);
        testFormatSuccess("com.bc.ceres.binding.ValueSet[]", ValueSet[].class);


        testParseSuccess(java.awt.Color.class, "java.awt.Color");
        testParseSuccess(java.util.Date.class, "java.util.Date");
        testParseSuccess(java.util.Date.class, "Date");
        testParseSuccess(org.w3c.dom.Text.class, "org.w3c.dom.Text");
        testParseSuccess(null, "");

        testFormatSuccess("java.awt.Color", java.awt.Color.class);
        testFormatSuccess("Date", java.util.Date.class);
        testFormatSuccess("org.w3c.dom.Text", org.w3c.dom.Text.class);
        testFormatSuccess("", null);

        testParseFailed("Int");
        testParseFailed("you.will.be.Assimilated");

        assertNullCorrectlyHandled();
    }
}