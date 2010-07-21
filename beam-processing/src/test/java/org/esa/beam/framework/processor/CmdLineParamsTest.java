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

import java.io.File;

public class CmdLineParamsTest extends TestCase {

    public CmdLineParamsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CmdLineParamsTest.class);
    }

    /**
     * Tests the functionality of the constructors
     */
    public void testCmdLineParams() {
        CmdLineParams param1 = new CmdLineParams();

        // after default constructor, all parameters shall be on their default values
        assertEquals(false, param1.isInteractive());
        assertEquals(false, param1.isQuiet());
        assertEquals(false, param1.isVerbose());
        assertEquals(null, param1.getConfigFile());
        assertEquals(null, param1.getRequestFile());

        // after using the parametreized constructor, all values shall
        // return the values set

        File config = new File("config.tst");
        File request = new File("request.tst");

        CmdLineParams param2 = new CmdLineParams(true, true, true, config, request);
        assertEquals(true, param2.isInteractive());
        assertEquals(true, param2.isQuiet());
        assertEquals(true, param2.isVerbose());
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(config.toString(), param2.getConfigFile().toString());
        assertEquals(request.toString(), param2.getRequestFile().toString());
    }

    /**
     * Tests the functionality of the setter and getter for the interactive flag
     */
    public void testSetIsInteractive() {
        CmdLineParams param = new CmdLineParams();

        // we expect the initial value false
        assertEquals(false, param.isInteractive());

        // we expect to get returned what we set
        param.setInteractive(true);
        assertEquals(true, param.isInteractive());

        param.setInteractive(false);
        assertEquals(false, param.isInteractive());
    }

    /**
     * Tests the functionality of the setter and getter for the interactive flag
     */
    public void testSetIsProgress() {
        CmdLineParams param = new CmdLineParams();

        // we expect the initial value false
        assertEquals(false, param.isProgress());

        // we expect to return true if progress set true and interactive set false
        param.setProgress(true);
        param.setInteractive(false);
        assertEquals(true, param.isProgress());

        // we expect to return false if progress set true and interactive also set to true
        param.setProgress(true);
        param.setInteractive(true);
        assertEquals(false, param.isProgress());

        // we expect to return false anytime progress is set to false
        param.setProgress(false);
        param.setInteractive(true);
        assertEquals(false, param.isProgress());

        param.setProgress(false);
        param.setInteractive(false);
        assertEquals(false, param.isProgress());
    }

    /**
     * Tests the functionality of the setter and getter for the quiet flag
     */
    public void testSetIsQuiet() {
        CmdLineParams param = new CmdLineParams();

        // we expect to get returned what we set
        param.setQuiet(true);
        assertTrue(param.isQuiet());

        param.setQuiet(false);
        assertTrue(!param.isQuiet());
    }

    /**
     * Tests the functionality of the setter and getter for the verbose flag
     */
    public void testSetIsVerbose() {
        CmdLineParams param = new CmdLineParams();

        // we expect to get returned what we set
        param.setVerbose(true);
        assertTrue(param.isVerbose());

        param.setVerbose(false);
        assertTrue(!param.isVerbose());
    }

    /**
     * Tests the functionality of the setter and getter for the config file
     */
    public void testSetGetConfigFile() {
        CmdLineParams param = new CmdLineParams();
        File config = new File("config.tst");

        // we expect to get returned what we set
        param.setConfigFile(config);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(config.toString(), param.getConfigFile().toString());

        param.setConfigFile(null);
        assertNull(param.getConfigFile());
    }

    /**
     * Tests the functionality of the setter and getter for the request file
     */
    public void testSetGetRequestFile() {
        CmdLineParams param = new CmdLineParams();
        File request = new File("config.tst");

        // we expect to get returned what we set
        param.setRequestFile(request);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(request.toString(), param.getRequestFile().toString());

        param.setRequestFile(null);
        assertNull(param.getRequestFile());
    }

}
