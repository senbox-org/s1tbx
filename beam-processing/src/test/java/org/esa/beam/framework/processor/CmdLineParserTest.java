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

public class CmdLineParserTest extends TestCase {

    public CmdLineParserTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CmdLineParserTest.class);
    }

    /**
     * Tests the correct constructor functionality.
     */
    public void testCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();

        // empty constructor shall not set anything. Must throw exception
        // when one of the get.. or is... functions is called
        try {
            parser.getCmdLineParams();
            fail("Must throw exception because no command line set");
        } catch (ProcessorException e) {
        }

        // now use constructor with null argument . must throw IllegalArgumentException
        try {
            new CmdLineParser(null);
            fail("Must throw exception because null command line is set");
        } catch (ProcessorException e) {
        } catch (IllegalArgumentException e) {
        }

        // now use constructor with arguments - must be able to parse
        String[] args = {"GuiTest_DialogAndModalDialog", "Test2"};

        try {
            parser = new CmdLineParser(args);
            parser.getCmdLineParams();
        } catch (ProcessorException e) {
            fail("Must not throw exception because command line is set");
        }
    }

    /**
     * Tests the correct functionality of setArgs()
     */
    public void testSetArgs() {
        CmdLineParser parser = new CmdLineParser();

        try {
            parser.setArgs(null);
            fail("Must throw exception because null command line is set");
        } catch (IllegalArgumentException e) {
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the correct functionality for config file parameter
     */
    public void testGetConfigFile() {
        String[] args = {"-c", "TestConfig.xml"};
        String[] args2 = {"--config", "TestConfig.xml"};
        String[] argsNotSet = {"bla"};

        CmdLineParser parser = new CmdLineParser();
        CmdLineParams params;

        // check that we get the correct argument back
        try {
            parser.setArgs(args);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals("TestConfig.xml", params.getConfigFile().getName());
            parser.setArgs(args2);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals("TestConfig.xml", params.getConfigFile().getName());
        } catch (ProcessorException e) {
        }

        // check that we get null when no appropriate parameter is set
        try {
            parser.setArgs(argsNotSet);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertNull(params.getConfigFile());
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the correct functionality for request file parameter
     */
    public void testGetRequestFile() {
        String[] args = {"TestRequest.xml"};
        String[] argsNotSet = {"-c", "bla"};

        CmdLineParser parser = new CmdLineParser();
        CmdLineParams params;

        // check that we get the correct argument back
        try {
            parser.setArgs(args);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals("TestRequest.xml", params.getRequestFile().getPath());
        } catch (ProcessorException e) {
        }

        // check that we get null when no appropriate parameter is set
        try {
            parser.setArgs(argsNotSet);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertNull(params.getRequestFile());
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the functionality of isInteractive()
     */
    public void testIsInteractive() {
        String[] args = {"-i"};
        String[] args2 = {"--interactive"};
        String[] argsNotSet = {"bla"};

        CmdLineParser parser = new CmdLineParser();
        CmdLineParams params;

        // check that we get the correct argument back
        try {
            parser.setArgs(args);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isInteractive());
            parser.setArgs(args2);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isInteractive());
        } catch (ProcessorException e) {
        }

        // check that we get false when no appropriate parameter is set
        try {
            parser.setArgs(argsNotSet);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals(false, params.isInteractive());
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the functionality of isQuiet()
     */
    public void testIsQuiet() {
        String[] args = {"-q"};
        String[] args2 = {"--quiet"};
        String[] argsNotSet = {"bla"};

        CmdLineParser parser = new CmdLineParser();
        CmdLineParams params;

        // check that we get the correct argument back
        try {
            parser.setArgs(args);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isQuiet());
            parser.setArgs(args2);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isQuiet());
        } catch (ProcessorException e) {
        }

        // check that we get false when no appropriate parameter is set
        try {
            parser.setArgs(argsNotSet);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals(false, params.isQuiet());
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the functionality of isVerbose()
     */
    public void testIsVerbose() {
        String[] args = {"-v"};
        String[] args2 = {"--verbose"};
        String[] argsNotSet = {"bla"};

        CmdLineParser parser = new CmdLineParser();
        CmdLineParams params;

        // check that we get the correct argument back
        try {
            parser.setArgs(args);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isVerbose());
            parser.setArgs(args2);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertTrue(params.isVerbose());
        } catch (ProcessorException e) {
        }

        // check that we get false when no appropriate parameter is set
        try {
            parser.setArgs(argsNotSet);
            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals(false, params.isVerbose());
        } catch (ProcessorException e) {
        }
    }

    /**
     * Performs some tests on complex command lines
     */
    public void testComplexCommandLines() {
        String[] args1 = {"-c", "TestConfig.xml", "-i", "--interactive", "--verbose", "TestRequest.xml"};

        try {
            CmdLineParser parser = new CmdLineParser(args1);
            CmdLineParams params;

            params = parser.getCmdLineParams();
            assertNotNull(params);
            assertEquals("TestConfig.xml", params.getConfigFile().getName());
            assertEquals(true, params.isInteractive());
            assertEquals(true, params.isVerbose());
            assertEquals(false, params.isQuiet());
            assertEquals("TestRequest.xml", params.getRequestFile().getPath());
        } catch (ProcessorException e) {
        }
    }
}