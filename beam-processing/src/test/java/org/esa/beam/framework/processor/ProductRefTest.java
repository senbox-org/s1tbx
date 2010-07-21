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

public class ProductRefTest extends TestCase {

    private File _file;
    private static final String _testFormat = "testFileFormat";
    private static final String _testID = "testTypeId";
    private static final String _testFileName = "bla.test";

    public ProductRefTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductRefTest.class);
    }

    @Override
    public void setUp() {
        _file = new File(_testFileName);
    }

    /**
     * Tests the functionality of the different constructors
     */
    public void testProduct() {
        // constructor with URL argument shall set the correct URL
        ProductRef prod = new ProductRef(_file);

        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_file.toString(), prod.getFile().toString());
        assertEquals(null, prod.getTypeId());
        assertEquals(0, prod.getNumComponents());
        assertEquals("bla.test", prod.getFilePath());

        // a construction with a null as URL shall not be possible
        try {
            new ProductRef((File) null);
            fail("illegal null argument in constructor");
        } catch (IllegalArgumentException e) {
        }

        // now check constructor with all arguments
        try {
            prod = new ProductRef(_file, _testFormat, _testID);

            // now everything must be set
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_file.toString(), prod.getFile().toString());
            assertEquals(_testFormat, prod.getFileFormat());
            assertEquals(_testID, prod.getTypeId());
        } catch (IllegalArgumentException e) {
            fail("no exception expected here");
        }
    }

    /**
     * Tests the functionality of component related stuff
     */
    public void testComponentFunctionality() {
        ProductRef prod = new ProductRef(_file);
        ProductRef component = new ProductRef(_file);
        // a null component shall not be added
        try {
            prod.addComponent(null);
            fail("null shall not be added as component");
        } catch (IllegalArgumentException e) {
        }

        // if one component is added, the getter functions shall react appropriate
        prod.addComponent(component);

        assertEquals(1, prod.getNumComponents());
        assertEquals(component, prod.getComponentAt(0));

        // if a component with illegal index is requested -> exception
        try {
            prod.getComponentAt(12);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        // now remove the component and check again
        prod.removeComponent(component);
        assertEquals(0, prod.getNumComponents());
        assertEquals(null, prod.getComponentAt(0));
    }

    /**
     * Tests the functionality of the Info stuff
     */
    public void testGetTypeId() {
        ProductRef prod = new ProductRef(_file);
        // an empty product shall not have ID info
        assertEquals(null, prod.getTypeId());

    }

}