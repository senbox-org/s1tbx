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

public class ProcessorConfigTest extends TestCase {

    public ProcessorConfigTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProcessorConfigTest.class);
    }

    /**
     * GuiTest_DialogAndModalDialog the functionality for set and getDescription()
     */
    public void testSetGetDescription() {
        ProcessorConfig config = new ProcessorConfig();

        // the config shall return an empty string when no description was set
        assertEquals("", config.getDescription());

        // the config shall return the same description as was set
        config.setDescription("test");
        assertEquals("test", config.getDescription());
    }

    /**
     * Tests the functionality for set and getTypeID()
     */
    public void testSetGetName() {
        ProcessorConfig config = new ProcessorConfig();

        // the config shall return an empty string when no name was set
        assertEquals("", config.getName());

        // the config shall return the same description as was set
        config.setName("test");
        assertEquals("test", config.getName());
    }
}