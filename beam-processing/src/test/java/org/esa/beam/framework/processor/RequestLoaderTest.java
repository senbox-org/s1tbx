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

import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.framework.param.Parameter;

public class RequestLoaderTest extends TestCase {

    private static final String _merisTag = "useMeris";
    private static final String _waterTag = "waterVapour";
    private static final String _ozoneTag = "ozone";
    private static final String _taupTag = "taup550";
    private static final String _pressTag = "airPressure";
    private static final String _bitmaskTag = "Bitmask";
    private static final String _bitmaskForwardTag = "BitmaskForward";
    private static final String _bitmaskNadirTag = "BitmaskNadir";

    private static final String _req_0_inputName = "EnvisatTestFile_1";
    private static final String _req_0_inputFormat = "ENVISAT";
    private static final String _req_0_inputTypeId = "L1B";
    private static final String _req_0_outputName = "CorrectedFile_1";
    private static final String _req_0_outputFormat = "BEAM";
    private static final String _req_0_outputTypeId = "CORR";
    private static final int _req_0_numParams = 5;
    private static final String _req_0_type = "SMAC";
    private static final String _req_0_merisUse = "true";
    private static final String _req_0_water = "0.3";
    private static final String _req_0_ozone = "0.2";
    private static final String _req_0_taup = "0.4";
    private static final String _req_0_press = "1008";

    private static final String _req_1_inputName = "EnvisatTestFile_2";
    private static final String _req_1_inputFormat = "";
    private static final String _req_1_inputTypeId = "";
    private static final String _req_1_inputBitmask = "not flags.INVALID";
    private static final String _req_1_outputName = "CorrectedFile_2";
    private static final String _req_1_outputFormat = "";
    private static final String _req_1_outputTypeId = "";
    private static final String _req_1_logLocation = "SmacLog.txt";
    private static final int _req_1_numParams = 7;
    private static final String _req_1_type = "SMAC";
    private static final String _req_1_merisUse = "false";
    private static final String _req_1_water = "0.3";
    private static final String _req_1_ozone = "0.2";
    private static final String _req_1_taup = "0.4";
    private static final String _req_1_press = "1008";

    private static final String _req_2_inputName = "EnvisatTestFile_3";
    private static final String _req_2_inputFormat = "";
    private static final String _req_2_inputTypeId = "";
    private static final String _req_2_inputBitmask = "flags.COASTLINE and not flags.INVALID";
    private static final String _req_2_outputName = "CorrectedFile_3";
    private static final String _req_2_outputFormat = "BEAM";
    private static final String _req_2_outputTypeId = "L1B_AC";
    private static final int _req_2_numParams = 3;
    private static final String _req_2_type = "SMAC";
    private static final String _req_2_merisUse = "true";
    private static final String _req_2_taup = "0.4";

    private static final String _req_3_inputName = "EnvisatTestFile_4";
    private static final String _req_3_inputFormat = "";
    private static final String _req_3_inputTypeId = "";
    private static final String _req_3_inputBitmask = "flags.GLINT_RISK";
    private static final String _req_3_outputName = "CorrectedFile_4";
    private static final String _req_3_outputFormat = "ENVI";
    private static final String _req_3_outputTypeId = "A_CORR";
    private static final String _req_3_logLocation_1 = "C:/BeamOut/SmacLog.txt";
    private static final String _req_3_logLocation_2 = "C:/BeamOut/AnotherSmacLog.txt";
    private static final int _req_3_numParams = 3;
    private static final String _req_3_type = "SMAC";
    private static final String _req_3_merisUse = "true";
    private static final String _req_3_taup = "0.4";

    private static final String _req_4_inputName = "";
    private static final String _req_4_inputFormat = "";
    private static final String _req_4_inputTypeId = "";
    private static final String _req_4_inputBitmask = "";
    private static final String _req_4_outputName = "CorrectedFile_3";
    private static final String _req_4_outputFormat = "BEAM";
    private static final String _req_4_outputTypeId = "L1B_AC";
    private static final int _req_4_numParams = 3;
    private static final String _req_4_type = "SMAC";
    private static final String _req_4_merisUse = "true";
    private static final String _req_4_taup = "0.4";


    private final  File _testRequestFile;
    private final static int _numRequests = 5;

    public RequestLoaderTest(final String testName) throws Exception {
        super(testName);
        URL resource = RequestLoaderTest.class.getResource("TestRequest.xml");
        _testRequestFile = new File(resource.toURI());
    }

    public static Test suite() {
        return new TestSuite(RequestLoaderTest.class);
    }

    /**
     * Tests the functionality of the constructors
     */
    public void testRequestLoader() throws RequestElementFactoryException {
        // when constructed with default parameters, the request loader shall have
        // no requests in the list.
        RequestLoader loader = new RequestLoader();

        assertEquals(0, loader.getNumRequests());

        // when fed with the test request file, the loader shall contain 4 requests
        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
    }

    /**
     * Tests the functionality of getRequestAt()
     */
    public void testGetRequestAt() throws MalformedURLException,
                                          RequestElementFactoryException {
        final RequestLoader loader;
        Request request;

        // check the requests fed in with test file
        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());

        request = loader.getRequestAt(0);
        assertNotNull(request);

        request = loader.getRequestAt(1);
        assertNotNull(request);

        request = loader.getRequestAt(2);
        assertNotNull(request);

        request = loader.getRequestAt(3);
        assertNotNull(request);

        request = loader.getRequestAt(4);
        assertNotNull(request);

        // must throw exception when requesting an object beyond scope
        try {
            request = loader.getRequestAt(5);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
    }

    /**
     * Tests the functionality if setRequestFile
     */
    public void testSetRequestFile() throws MalformedURLException,
                                            RequestElementFactoryException {
        final RequestLoader loader = new RequestLoader();

        assertEquals(0, loader.getNumRequests());

        loader.setAndParseRequestFile(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
    }

    /**
     * Tests if request_0 is correctly read
     */
    public void testRequest_0_content() throws MalformedURLException,
                                               RequestElementFactoryException {
        RequestLoader loader;
        Request request;
        Parameter param;
        ProductRef prodRef;

        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
        // get the request
        request = loader.getRequestAt(0);
        assertNotNull(request);

        // get the first input product
        prodRef = request.getInputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_0_inputName, prodRef.getFilePath());
        assertEquals(_req_0_inputFormat, prodRef.getFileFormat());
        assertEquals(_req_0_inputTypeId, prodRef.getTypeId());

        // check the output product
        prodRef = request.getOutputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_0_outputName, prodRef.getFilePath());
        assertEquals(_req_0_outputFormat, prodRef.getFileFormat());
        assertEquals(_req_0_outputTypeId, prodRef.getTypeId());

        // check bitmask expression - none is present
        assertNull(request.getParameter(_bitmaskTag));

        // check the request parameter
        assertEquals(_req_0_numParams, request.getNumParameters());
        assertEquals(_req_0_type, request.getType());
        param = request.getParameter(_merisTag);
        assertNotNull(param);
        assertEquals(_req_0_merisUse, (String) param.getValue());
        param = request.getParameter(_waterTag);
        assertNotNull(param);
        assertEquals(_req_0_water, (String) param.getValue());
        param = request.getParameter(_ozoneTag);
        assertNotNull(param);
        assertEquals(_req_0_ozone, (String) param.getValue());
        param = request.getParameter(_taupTag);
        assertNotNull(param);
        assertEquals(_req_0_taup, (String) param.getValue());
        param = request.getParameter(_pressTag);
        assertNotNull(param);
        assertEquals(_req_0_press, (String) param.getValue());
    }

    /**
     * Tests if request_1 is correctly read
     */
    public void testRequest_1_content() throws MalformedURLException,
                                               RequestElementFactoryException {
        RequestLoader loader;
        Request request;
        ProductRef prodRef;
        Parameter param;
        File logFile;

        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
        // get the request
        request = loader.getRequestAt(1);
        assertNotNull(request);

        // get the first input product
        prodRef = request.getInputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_1_inputName, prodRef.getFilePath());
        assertEquals(_req_1_inputFormat, prodRef.getFileFormat());
        assertEquals(_req_1_inputTypeId, prodRef.getTypeId());

        // check the output product
        prodRef = request.getOutputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_1_outputName, prodRef.getFilePath());
        assertEquals(_req_1_outputFormat, prodRef.getFileFormat());
        assertEquals(_req_1_outputTypeId, prodRef.getTypeId());

        // check the bitmask expression
        param = request.getParameter(_bitmaskForwardTag);
        assertNotNull(param);
        assertEquals(_req_1_inputBitmask, param.getValue());

        param = request.getParameter(_bitmaskNadirTag);
        assertNotNull(param);
        assertEquals(_req_1_inputBitmask, param.getValue());

        // check the logging file location
        assertEquals(1, request.getNumLogFileLocations());
        logFile = request.getLogFileLocationAt(0);
        assertNotNull(logFile);
        assertEquals(_req_1_logLocation, logFile.getPath());

        // check the request parameter
        assertEquals(_req_1_numParams, request.getNumParameters());
        assertEquals(_req_1_type, request.getType());
        assertEquals(_req_1_merisUse, (String) request.getParameter(_merisTag).getValue());
        assertEquals(_req_1_water, (String) request.getParameter(_waterTag).getValue());
        assertEquals(_req_1_ozone, (String) request.getParameter(_ozoneTag).getValue());
        assertEquals(_req_1_taup, (String) request.getParameter(_taupTag).getValue());
        assertEquals(_req_1_press, (String) request.getParameter(_pressTag).getValue());
    }

    /**
     * Tests if request_2 is correctly read
     */
    public void testRequest_2_content() throws MalformedURLException,
                                               RequestElementFactoryException {
        RequestLoader loader;
        Request request;
        ProductRef prodRef;

        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
        // get the request
        request = loader.getRequestAt(2);
        assertNotNull(request);

        // get the first input product
        prodRef = request.getInputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_2_inputName, prodRef.getFilePath());
        assertEquals(_req_2_inputFormat, prodRef.getFileFormat());
        assertEquals(_req_2_inputTypeId, prodRef.getTypeId());

        // check the output product
        prodRef = request.getOutputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_2_outputName, prodRef.getFilePath());
        assertEquals(_req_2_outputFormat, prodRef.getFileFormat());
        assertEquals(_req_2_outputTypeId, prodRef.getTypeId());

        // check bitmask expression
        assertEquals(_req_2_inputBitmask, request.getParameter(_bitmaskTag).getValue());

        // check the request parameter
        assertEquals(_req_2_numParams, request.getNumParameters());
        assertEquals(_req_2_type, request.getType());
        assertEquals(_req_2_merisUse, (String) request.getParameter(_merisTag).getValue());
        assertNull(request.getParameter(_waterTag));
        assertNull(request.getParameter(_ozoneTag));
        assertEquals(_req_2_taup, (String) request.getParameter(_taupTag).getValue());
        assertNull(request.getParameter(_pressTag));
    }

    /**
     * Tests if request_3 is correctly read
     */
    public void testRequest_3_content() throws MalformedURLException,
                                               RequestElementFactoryException {
        RequestLoader loader;
        Request request;
        ProductRef prodRef;
        File logFile_1;
        File logFile_2;

        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
        // get the request
        request = loader.getRequestAt(3);
        assertNotNull(request);

        // get the first input product
        prodRef = request.getInputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_3_inputName, prodRef.getFilePath());
        assertEquals(_req_3_inputFormat, prodRef.getFileFormat());
        assertEquals(_req_3_inputTypeId, prodRef.getTypeId());

        // check the output product
        prodRef = request.getOutputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_3_outputName, prodRef.getFilePath());
        assertEquals(_req_3_outputFormat, prodRef.getFileFormat());
        assertEquals(_req_3_outputTypeId, prodRef.getTypeId());

        // check bitmask expression
        assertEquals(_req_3_inputBitmask, request.getParameter(_bitmaskTag).getValue());

        // check the logging file locations
        assertEquals(2, request.getNumLogFileLocations());
        logFile_1 = request.getLogFileLocationAt(0);
        assertNotNull(logFile_1);
        assertEquals(new File(_req_3_logLocation_1), logFile_1);
        logFile_2 = request.getLogFileLocationAt(1);
        assertNotNull(logFile_2);
        assertEquals(new File(_req_3_logLocation_2), logFile_2);

        // check the request parameter
        assertEquals(_req_3_numParams, request.getNumParameters());
        assertEquals(_req_3_type, request.getType());
        assertEquals(_req_3_merisUse, (String) request.getParameter(_merisTag).getValue());
        assertNull(request.getParameter(_waterTag));
        assertNull(request.getParameter(_ozoneTag));
        assertEquals(_req_3_taup, (String) request.getParameter(_taupTag).getValue());
        assertNull(request.getParameter(_pressTag));
    }

    /**
     * Tests if request_4 is correctly read
     */
    public void testRequest_4_content() throws MalformedURLException,
                                               RequestElementFactoryException {
        RequestLoader loader;
        Request request;
        ProductRef prodRef;

        loader = new RequestLoader(_testRequestFile);
        assertEquals(_numRequests, loader.getNumRequests());
        // get the request
        request = loader.getRequestAt(4);
        assertNotNull(request);

        // get the first input product
        prodRef = request.getInputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_4_inputName, prodRef.getFilePath());
        assertEquals(_req_4_inputFormat, prodRef.getFileFormat());
        assertEquals(_req_4_inputTypeId, prodRef.getTypeId());

        // check the output product
        prodRef = request.getOutputProductAt(0);
        assertNotNull(prodRef);
        assertEquals(_req_4_outputName, prodRef.getFilePath());
        assertEquals(_req_4_outputFormat, prodRef.getFileFormat());
        assertEquals(_req_4_outputTypeId, prodRef.getTypeId());

        // check bitmask expression
        assertEquals(_req_4_inputBitmask, request.getParameter(_bitmaskTag).getValue());

        // check the request parameter
        assertEquals(_req_4_numParams, request.getNumParameters());
        assertEquals(_req_4_type, request.getType());
        assertEquals(_req_4_merisUse, (String) request.getParameter(_merisTag).getValue());
        assertNull(request.getParameter(_waterTag));
        assertNull(request.getParameter(_ozoneTag));
        assertEquals(_req_4_taup, (String) request.getParameter(_taupTag).getValue());
        assertNull(request.getParameter(_pressTag));
    }


}