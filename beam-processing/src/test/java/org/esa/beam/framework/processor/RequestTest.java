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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Debug;

import java.io.File;

public class RequestTest extends TestCase {

    private File _file;
    private File _log_1_file;
    private File _log_2_file;

    public RequestTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RequestTest.class);
    }

    @Override
    public void setUp() {
        _file = new File("bla.test");
        _log_1_file = new File("logFile1.txt");
        _log_2_file = new File("logFile2.txt");
    }

    /**
     * Tests the functionality of the constructor(s)
     */
    public void testRequest() {
        // a default constructed object shall not have any data
        Request req = new Request();

        assertEquals(0, req.getNumInputProducts());
        assertEquals(0, req.getNumOutputProducts());
        assertEquals(0, req.getNumParameters());

        // a parameterized constructed object shall contain the stuff passed in
        ProductRef[] inProds = {new ProductRef(_file)};
        ProductRef[] outProds = {new ProductRef(_file)};
        Parameter[] params = {new Parameter("test")};

        req = new Request(inProds, outProds, params);

        assertEquals(1, req.getNumInputProducts());
        assertEquals(1, req.getNumOutputProducts());
        assertEquals(1, req.getNumParameters());
    }

    /**
     * Tests the functionality for input products
     */
    public void testAddGetInputProduct() {
        Request req = new Request();
        ProductRef prod = new ProductRef(_file);

        // it shall not be possible to add null products
        try {
            req.addInputProduct(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when adding a product, the request shall contain it
        req.addInputProduct(prod);
        assertEquals(prod, req.getInputProductAt(0));
        assertEquals(1, req.getNumInputProducts());

        // when removing the product it shall be away
        req.removeInputProduct(prod);
        assertEquals(0, req.getNumInputProducts());

        // when requesting a product out of scope, an ArrayIndexOutOfBounds exception mut be thrown
        try {
            req.getInputProductAt(34);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Tests that all input products are removed on a call to clearInputProduct
     */
    public void testClearInputProducts() {
        Request req = new Request();
        ProductRef prod1 = new ProductRef(_file);
        ProductRef prod2 = new ProductRef(_file);
        ProductRef prod3 = new ProductRef(_file);
        ProductRef prod4 = new ProductRef(_file);

        // one test
        req.addInputProduct(prod1);
        req.addInputProduct(prod2);
        assertEquals(2, req.getNumInputProducts());
        req.clearInputProducts();
        assertEquals(0, req.getNumInputProducts());

        // and another one
        req.addInputProduct(prod1);
        req.addInputProduct(prod2);
        req.addInputProduct(prod3);
        req.addInputProduct(prod4);
        assertEquals(4, req.getNumInputProducts());
        req.clearInputProducts();
        assertEquals(0, req.getNumInputProducts());

    }

    /**
     * Tests the functionality for output products
     */
    public void testAddGetOutputProduct() {
        Request req = new Request();
        ProductRef prod = new ProductRef(_file);

        // it shall not be possible to add null products
        try {
            req.addOutputProduct(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when adding a product, the request shall contain it
        req.addOutputProduct(prod);
        assertEquals(prod, req.getOutputProductAt(0));
        assertEquals(1, req.getNumOutputProducts());

        // when removing the product it shall be away
        req.removeOutputProduct(prod);
        assertEquals(0, req.getNumOutputProducts());

        // when requesting a product out of scope, an ArrayIndexOutOfBounds exception must be thrown
        try {
            req.getInputProductAt(34);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Tests the functionality of the logfile parameters
     */

    public void testAddGetLogFileLocation() {
        Request req = new Request();

        // it shall not be possible to set a null logfile location
        try {
            req.addLogFileLocation((File) null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {

        }

        // when adding a logging file, the request shall contain it
        req.addLogFileLocation(_log_1_file);
        assertEquals(1, req.getNumLogFileLocations());
        assertEquals(_log_1_file.toString(), req.getLogFileLocationAt(0).toString());

        // when removing the logging file location it shall be away
        req.removeLogFileLocation(_log_1_file);
        assertEquals(0, req.getNumLogFileLocations());

        // when adding two logging files, the request shall contain them
        req.addLogFileLocation(_log_1_file);
        req.addLogFileLocation(_log_2_file);
        assertEquals(2, req.getNumLogFileLocations());
        assertEquals(_log_1_file.toString(), req.getLogFileLocationAt(0).toString());
        assertEquals(_log_2_file.toString(), req.getLogFileLocationAt(1).toString());

        // when removing one of them, the other shall be present
        req.removeLogFileLocation(_log_1_file);
        assertEquals(1, req.getNumLogFileLocations());
        assertEquals(_log_2_file.toString(), req.getLogFileLocationAt(0).toString());

        // when removing the one that is not in, nothing shall happen
        req.removeLogFileLocation(_log_1_file);
        assertEquals(1, req.getNumLogFileLocations());

        // removing a null shall be allowed - and nothig shall happen
        req.removeLogFileLocation((File) null);
        assertEquals(1, req.getNumLogFileLocations());

        // when removing the last logging file location the request shall not contain anything anymore
        req.removeLogFileLocation(_log_2_file);
        assertEquals(0, req.getNumLogFileLocations());

        // when requesting a logging file of scope, an ArrayIndexOutOfBounds exception mut be thrown
        try {
            req.getLogFileLocationAt(9);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

    }

    /**
     * Tests the functionality of the parameter accessors
     */
    public void testAddGetParameter() {
        Request req = new Request();
        Parameter param = new Parameter("GuiTest_DialogAndModalDialog");

        // it shall not be possible to add null parameter
        try {
            req.addParameter(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when adding a parameter, the request shall contain it
        req.addParameter(param);
        assertEquals(param, req.getParameterAt(0));
        assertEquals(1, req.getNumParameters());

        // when removing the parameter it shall be away
        req.removeParameter(param);
        assertEquals(0, req.getNumParameters());

        // when requesting a parameter out of scope, an ArrayIndexOutOfBounds exception mut be thrown
        try {
            req.getParameterAt(34);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        // when we ge a parameter by name, null shall not ne accepted as name
        try {
            req.getParameter(null);
            fail("illegal null parameter");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testLoad() {
        Debug.traceMethodNotImplemented(Request.class, "load");
    }

    public void testSave() {
        Debug.traceMethodNotImplemented(Request.class, "save");
    }

    /**
     * Tests the correct functionality of clearLogFileLocations()
     */
    public void testClearLogFileLocations() {
        Request req = new Request();

        // set two logs and assure they're set
        req.addLogFileLocation(_log_1_file);
        req.addLogFileLocation(_log_2_file);
        assertEquals(2, req.getNumLogFileLocations());

        // now clear all - and check again
        req.clearLogFileLocations();
        assertEquals(0, req.getNumLogFileLocations());

        // and nothing shall happen when method is invoked with no values set
        req.clearLogFileLocations();
        assertEquals(0, req.getNumLogFileLocations());
    }

    /**
     * Tests the functionality of setType and getType
     */
    public void testSetGetType() {
        Request req = new Request();
        String type1 = "TestType";
        String type2 = "AnotherType";

        // when no type is set the request shall return an empty string
        assertEquals("", req.getType());

        // it shall not be possible to set null types
        try {
            req.setType(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when a type is set, the same shall be returned
        req.setType(type1);
        assertEquals(type1, req.getType());
        req.setType(type2);
        assertEquals(type2, req.getType());
    }

    /**
     * Tests the functionality of the request file accessors
     */
    public void testSetGetFile() {
        Request req = new Request();
        File testFile = new File("testFile");

        // a virgin request has no file
        assertNull(req.getFile());

        // whe a file is set, it shall be retrieveable again
        req.setFile(testFile);
        assertEquals(testFile, req.getFile());
    }

    public void testConvertToMetadata_WithParametersWithoutQualifier() {
        final Request request = new Request();
        request.setType("RequestType");
        request.setFile(new File("D:\\in\\request.xml"));
        request.addParameter(new Parameter("param1", "value1"));
        request.addParameter(new Parameter("param2", "value2"));
        request.addParameter(new Parameter("param3", "value3"));

        MetadataElement metadataElement = request.convertToMetadata();

        assertNotNull(metadataElement);
        assertEquals("processing_request", metadataElement.getName());
        assertEquals(2, metadataElement.getNumAttributes());

        MetadataAttribute attribute = metadataElement.getAttributeAt(0);
        assertNotNull(attribute);
        assertEquals("path", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("D:\\in\\request.xml", attribute.getData().getElemString());

        attribute = metadataElement.getAttributeAt(1);
        assertNotNull(attribute);
        assertEquals("type", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("RequestType", attribute.getData().getElemString());

        assertEquals(1, metadataElement.getNumElements());
        metadataElement = metadataElement.getElementAt(0);

        assertNotNull(metadataElement);
        assertEquals("parameters", metadataElement.getName());
        assertEquals(3, metadataElement.getNumAttributes());

        attribute = metadataElement.getAttributeAt(0);
        assertNotNull(attribute);
        assertEquals("param1", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value1", attribute.getData().getElemString());

        attribute = metadataElement.getAttributeAt(1);
        assertNotNull(attribute);
        assertEquals("param2", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value2", attribute.getData().getElemString());

        attribute = metadataElement.getAttributeAt(2);
        assertNotNull(attribute);
        assertEquals("param3", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value3", attribute.getData().getElemString());
    }

    public void testConvertToMetadata_WithQualifiedParameters() {
        final Request request = new Request();
        request.setType("RequestType");
        request.setFile(new File("D:\\in\\request.xml"));

        Parameter param1 = new Parameter("param1", "value1");
        param1.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "qualiName1", "qualiValue1");
        request.addParameter(param1);

        Parameter param2 = new Parameter("param2", "value2");
        param2.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "qualiName2", "qualiValue2");
        param2.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "qualiName3", "qualiValue3");
        request.addParameter(param2);

        Parameter param3 = new Parameter("param3", "value3");
        request.addParameter(param3);

        MetadataElement metadataElement = request.convertToMetadata();

        assertNotNull(metadataElement);
        assertEquals("processing_request", metadataElement.getName());
        assertEquals(2, metadataElement.getNumAttributes());

        MetadataAttribute attribute = metadataElement.getAttributeAt(0);
        assertNotNull(attribute);
        assertEquals("path", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("D:\\in\\request.xml", attribute.getData().getElemString());

        attribute = metadataElement.getAttributeAt(1);
        assertNotNull(attribute);
        assertEquals("type", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("RequestType", attribute.getData().getElemString());

        assertEquals(1, metadataElement.getNumElements());
        metadataElement = metadataElement.getElementAt(0);

        assertNotNull(metadataElement);
        assertEquals("parameters", metadataElement.getName());
        assertEquals(1, metadataElement.getNumAttributes());
        assertEquals(2, metadataElement.getNumElements());

        attribute = metadataElement.getAttributeAt(0);
        assertNotNull(attribute);
        assertEquals("param3", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value3", attribute.getData().getElemString());

        // First parameter metadata element
        MetadataElement paramElement = metadataElement.getElementAt(0);
        assertNotNull(paramElement);
        assertEquals("param1", paramElement.getName());
        assertEquals(2, paramElement.getNumAttributes());

        // Value MetadataAttribute from first parameter
        attribute = paramElement.getAttributeAt(0);
        assertNotNull(attribute);
        assertEquals("value", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value1", attribute.getData().getElemString());

        // first qualifier MetadataAttribute from first parameter
        attribute = paramElement.getAttributeAt(1);
        assertNotNull(attribute);
        assertEquals("qualiName1", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("qualiValue1", attribute.getData().getElemString());

        // Second parameter metadata element
        paramElement = metadataElement.getElementAt(1);
        assertNotNull(paramElement);
        assertEquals("param2", paramElement.getName());
        assertEquals(3, paramElement.getNumAttributes());

        // Value MetadataAttribute from first parameter
        // no garanteed order of attributes - don't test with getAttributeAt(index);
        attribute = paramElement.getAttribute("value");
        assertNotNull(attribute);
        assertEquals("value", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("value2", attribute.getData().getElemString());

        // first qualifier MetadataAttribute from second parameter
        // no garanteed order of attributes - don't test with getAttributeAt(index);
        attribute = paramElement.getAttribute("qualiName2");
        assertNotNull(attribute);
        assertEquals("qualiName2", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("qualiValue2", attribute.getData().getElemString());

        // second qualifier MetadataAttribute from second parameter
        // no garanteed order of attributes - don't test with getAttributeAt(index);
        attribute = paramElement.getAttribute("qualiName3");
        assertNotNull(attribute);
        assertEquals("qualiName3", attribute.getName());
        assertTrue(attribute.getData() instanceof ProductData.ASCII);
        assertEquals("qualiValue3", attribute.getData().getElemString());
    }
}