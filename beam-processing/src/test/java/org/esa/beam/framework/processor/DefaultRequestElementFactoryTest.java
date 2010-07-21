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

package org.esa.beam.framework.processor;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.SystemUtils;

public class DefaultRequestElementFactoryTest extends TestCase {

    private DefaultRequestElementFactory _dREF;

    private File _file;

    public DefaultRequestElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DefaultRequestElementFactoryTest.class);
    }

    @Override
    public void setUp() {
        _dREF = DefaultRequestElementFactory.getInstance();
        assertNotNull(_dREF);
        _file = new File(SystemUtils.convertToLocalPath("c:/test/test.txt"));
    }

    public void testCreateParameter() {
        Parameter param = null;
        try {
            param = _dREF.createParameter("name", "value");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertEquals("name", param.getName());
        assertEquals("value", param.getValue());
        assertEquals(String.class, param.getProperties().getValueType());
        // other name and value
        try {
            param = _dREF.createParameter("namename", "valuevalue");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertEquals("namename", param.getName());
        assertEquals("valuevalue", param.getValue());
        assertEquals(String.class, param.getProperties().getValueType());
    }

    public void testCreateParameter_exceptions() {
        // Exception expected if name is null
        try {
            _dREF.createParameter(null, "value");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        // Exception expected if name is empty
        try {
            _dREF.createParameter("", "value");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        // Exception expected if value is null
        try {
            _dREF.createParameter("name", null);
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        // Exception expected if value is empty
        try {
            _dREF.createParameter("name", "");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
    }

    public void testExceptionCreateProductRefs() {
        // throw an RequestElementFactoryException if url is null
        ProductRef ipRef = null;
        try {
            ipRef = _dREF.createInputProductRef(null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNull(ipRef);

        // throw an RequestElementFactoryException if url is null
        ProductRef opRef = null;
        try {
            _dREF.createOutputProductRef(null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNull(opRef);
    }

    public void testReturnedValidProductRefs_Url_Null_Null() {
        // InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _dREF.createInputProductRef(_file, null, null);
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), ipRef.getFile().toString());
        assertNull(ipRef.getTypeId());
        // OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _dREF.createOutputProductRef(_file, null, null);
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), opRef.getFile().toString());
        assertNull(opRef.getTypeId());
    }

    public void testReturnedValidProductRefs_Url_Null_emptyString() {
        // InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _dREF.createInputProductRef(_file, null, "");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), ipRef.getFile().toString());
        assertNull(ipRef.getFileFormat());
        assertEquals("", ipRef.getTypeId());
        // OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _dREF.createOutputProductRef(_file, null, "");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), opRef.getFile().toString());
        assertNull(opRef.getFileFormat());
        assertEquals("", opRef.getTypeId());
    }

    public void testReturnedValidProductRefs_Url_Null_String() {
        // InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _dREF.createInputProductRef(_file, null, "meris");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), ipRef.getFile().toString());
        assertEquals("meris", ipRef.getTypeId());
        // OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _dREF.createOutputProductRef(_file, null, "aatsr");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen
        // verwendet
        assertEquals(_file.toString(), opRef.getFile().toString());
        assertEquals("aatsr", opRef.getTypeId());
    }
}