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
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.GlobalTestTools;
import org.esa.beam.framework.param.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;

public class RequestWriterTest extends TestCase {

    private RequestWriter _writer;
    private File _testFile;
    private File _smacTestFile;
    private File _flhTestFile;
    private File _multiQualifierTestFile;

    RandomAccessFile _reader;

    private static final File _smacLogFile = new File("/data/test/SmacLog.txt");
    private static final File _flhLogFile = new File("/data/test/FlhRtiLog.txt");
    private static final File _inputProduct = new File("/data/MERIS/MERIS_L1B_TEST.n1");
    private static final File _smacOutProduct = new File("/data/OUTPUT/MERIS_L1B_AC.dim");
    private static final File _flhOutProduct = new File("/data/OUTPUT/MERIS_L1B_FLH_RTI.dim");

    private static final String _expHeaderLine = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";
    private static final String _expListOpen = "<RequestList>";
    private static final String _expListClose = "</RequestList>";
    private static final String _expEmptyReq = "    <Request />";
    private static final String _expReqClose = "    </Request>";
    private static final String _expSmacReqOpen = "    <Request type=\"SMAC\">";
    private static final String _expFlhReqOpen = "    <Request type=\"FLH_RTI\">";
    private static final String _expUseMeris = "        <Parameter name=\"useMerisADS\" value=\"true\" />";
    private static final String _expWaterVapour = "        <Parameter name=\"u_h2o\" value=\"0.3\" />";
    private static final String _expMerisBands = "        <Parameter name=\"bands\" value=\"radiance_8\" />";

    private static final String _expFlhWaveLow = "        <Parameter name=\"flh_wavelength_low\" value=\"664.314\" />";
    private static final String _expFlhWaveMid = "        <Parameter name=\"flh_wavelength_mid\" value=\"680.556\" />";
    private static final String[] _expStringOneQalifier = new String[]{"<Parameter name=\"band_names\"",
            "value=\"radiance_1,radiance_2,radiance_4,radiance_13\"",
            "productTypes=\"MER_RR__1P,MER_FR__1P\""};
    private static final String[] _expStringTwoQualifiers = new String[]{"<Parameter name=\"virtual_band_names\"",
            "value=\"red1,green1,blue1\"",
            "otherQualifier1=\"otherQualifierValue1\"",
            "otherQualifier2=\"otherQualifierValue2\""};

    private final String _expSmacLog;
    private final String _expInput;
    private final String _expSmacOut;
    private final String _expFlhOut;
    private final String _expFlhlog;

    public RequestWriterTest(String testName) {
        super(testName);

        _expSmacLog = "        <LogFile file=\"" + _smacLogFile + "\" />";
        _expInput = "        <InputProduct file=\"" + _inputProduct + "\" format=\"ENVISAT\" typeId=\"L1B\" />";
        _expSmacOut = "        <OutputProduct file=\"" + _smacOutProduct + "\" format=\"DIMAP\" typeId=\"L1B_AC\" />";
        _expFlhOut = "        <OutputProduct file=\"" + _flhOutProduct + "\" format=\"DIMAP\" typeId=\"FLH_RTI\" />";
        _expFlhlog = "        <LogFile file=\"" + _flhLogFile + "\" />";
    }

    public static Test suite() {
        return new TestSuite(RequestWriterTest.class);
    }

    @Override
    protected void setUp() {
        _writer = new RequestWriter();

        _testFile = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(),
                             "testRequest.xml");
        if (_testFile.exists()) {
            assertTrue(_testFile.delete());
        }

        _smacTestFile = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(),
                                 "SmacRequest.xml");
        if (_smacTestFile.exists()) {
            assertTrue(_smacTestFile.delete());
        }

        _flhTestFile = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(),
                                "FlhRtiRequest.xml");
        if (_flhTestFile.exists()) {
            assertTrue(_flhTestFile.delete());
        }

        _multiQualifierTestFile = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(),
                                           "MultiQualifier.xml");
        if (_multiQualifierTestFile.exists()) {
            assertTrue(_multiQualifierTestFile.delete());
        }
    }

    @Override
    protected void tearDown() {
        try {
            if (_reader != null) {
                _reader.close();
            }
        } catch (IOException e) {
        }
        // delete everything we've been writing to disk
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    /**
     * Tests the functionality of write
     */
    public void testWrite() {
        assertNotNull(_writer);
        assertNotNull(_testFile);

        Request testRequest = createEmptyTestRequest();
        assertNotNull(testRequest);

        // write shall not accept null parameter
        try {
            _writer.write(null, _testFile);
            fail("IllegalArgumentException expected");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            _writer.write(new Request[]{testRequest}, (File) null);
            fail("IllegalArgumentException expected");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests that a specific request is written correctly
     */
    public void testWriteEmptyRequest() {
        assertNotNull(_writer);
        assertNotNull(_testFile);

        String line;

        Request testRequest = createEmptyTestRequest();
        assertNotNull(testRequest);

        try {
            _writer.write(new Request[]{testRequest}, _testFile);

            // check that the file exists
            assertEquals(true, _testFile.exists());

            _reader = new RandomAccessFile(_testFile, "r");
            _reader.seek(0);

            // read first line and check that it contains the standard header line
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expHeaderLine, line);

            // now open request list tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListOpen, line);

            // empty request
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expEmptyReq, line);

            // finally the close request list tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListClose, line);

            _reader.close();

        } catch (IOException e) {
            fail("IOException not expected");
        }
    }

    /**
     * Tests  whether a complete smac request is correctly written down
     */
    public void testWriteSmacRequest() {
        assertNotNull(_writer);
        assertNotNull(_smacTestFile);

        String line;

        Request smacRequest = createSmacTestRequest();
        assertNotNull(smacRequest);

        try {
            _writer.write(new Request[]{smacRequest}, _smacTestFile);

            // check that the file exists
            assertEquals(true, _smacTestFile.exists());

            _reader = new RandomAccessFile(_smacTestFile, "r");
            _reader.seek(0);

            // read first line and check that it contains the standard header line
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expHeaderLine, line);

            /// now open request tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListOpen, line);

            // open request of type SMAC
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expSmacReqOpen, line);

            // has parameter of type useMerisADS
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expUseMeris, line);

            // has parameter of type water vapour
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expWaterVapour, line);

            // has parameter of type bands
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expMerisBands, line);

            // has logging file
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expSmacLog, line);

            // has input product
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expInput, line);

            // has output product
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expSmacOut, line);

            // close request
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expReqClose, line);

            // finally the close request list tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListClose, line);

            _reader.close();
        } catch (IOException e) {
            fail("IOException not expected");
        }
    }

    /**
     * Tests if the smac request can be parsed again by the appropriate loader
     */
    public void testSmacRequestFileCanBeParsedAgain() {
        assertNotNull(_writer);
        assertNotNull(_smacTestFile);

        Request smacRequest = createSmacTestRequest();
        assertNotNull(smacRequest);

        RequestLoader loader = new RequestLoader();
        assertNotNull(loader);

        try {
            _writer.write(new Request[]{smacRequest}, _smacTestFile);
            loader.setAndParseRequestFile(_smacTestFile);
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
    }

    /**
     * Tests  whether a complete flh/rti request is correctly written down
     */
    public void testFlhRtiRequest() {
        assertNotNull(_writer);
        assertNotNull(_flhTestFile);

        String line;

        Request flhRequest = createFlhRtiTestRequest();
        assertNotNull(flhRequest);

        try {
            _writer.write(new Request[]{flhRequest}, _flhTestFile);

            // check that the file exists
            assertEquals(true, _flhTestFile.exists());

            _reader = new RandomAccessFile(_flhTestFile, "r");
            _reader.seek(0);

            // read first line and check that it contains the standard header line
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expHeaderLine, line);

            // now open request list tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListOpen, line);

            // open request of type FLH_RTI
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expFlhReqOpen, line);

            // wavelength low
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expFlhWaveLow, line);

            // wavelength mid
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expFlhWaveMid, line);

            // logging file
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expFlhlog, line);

            // has input product
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expInput, line);

            // has output product
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expFlhOut, line);

            // close request
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expReqClose, line);

            // finally the close request list tag
            line = _reader.readLine();
            assertNotNull(line);
            assertEquals(_expListClose, line);

            _reader.close();
        } catch (IOException e) {
            fail("IOException not expected");
        }
    }

    /**
     * Tests if the flhrti request can be parsed again by the appropriate loader
     */
    public void testFlhRequestCanBeParsedAgain() {
        assertNotNull(_writer);
        assertNotNull(_flhTestFile);

        Request flhRequest = createFlhRtiTestRequest();
        assertNotNull(flhRequest);

        DefaultRequestElementFactory factory = DefaultRequestElementFactory.getInstance();
        assertNotNull(factory);

        RequestLoader loader = new RequestLoader();
        assertNotNull(loader);

        try {
            _writer.write(new Request[]{flhRequest}, _flhTestFile);
            loader.setAndParseRequestFile(_flhTestFile);
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
    }

    public void testWriteParameterWithMultipleQualifiers() {
        try {
            final Request request = createMultipleQualifierRequest();
            final StringWriter sw = new StringWriter();

            _writer.write(new Request[]{request}, sw);
            String xml1 = sw.toString();
            final StringBuffer buffer = sw.getBuffer();
            buffer.setLength(0);

            RequestLoader loader = new RequestLoader();
            _writer.write(new Request[]{request}, _multiQualifierTestFile);
            loader.setAndParseRequestFile(_multiQualifierTestFile);
            assertEquals(1, loader.getNumRequests());
            _writer.write(new Request[]{loader.getRequestAt(0)}, sw);
            String xml2 = sw.toString();

            assertNotNull(xml1);
            assertNotNull(xml2);
            assertEquals(xml1, xml2);
            for (String expStringOneQualifier : _expStringOneQalifier) {
                assertTrue(expStringOneQualifier + " not contained", xml1.indexOf(expStringOneQualifier) > 1);
            }

            for (String expStringTwoQualifier : _expStringTwoQualifiers) {
                assertTrue(expStringTwoQualifier + " not contained", xml1.indexOf(expStringTwoQualifier) > 1);
            }
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * creates an empty request
     */
    private Request createEmptyTestRequest() {
        return new Request();
    }

    /**
     * Creates a request for the smac processor
     */
    private Request createSmacTestRequest() {
        Request req = new Request();
        req.addInputProduct(new ProductRef(_inputProduct, "ENVISAT", "L1B"));
        req.addOutputProduct(new ProductRef(_smacOutProduct, "DIMAP", "L1B_AC"));

        // request type
        req.setType("SMAC");
        req.addParameter(new Parameter("useMerisADS", "true"));
        req.addParameter(new Parameter("u_h2o", "0.3"));
        req.addParameter(new Parameter("bands", "radiance_8"));
        req.addLogFileLocation(_smacLogFile);

        return req;
    }

    /**
     * Creates a request for the flh/rti processor
     */
    private Request createFlhRtiTestRequest() {
        Request req = new Request();
        req.addInputProduct(new ProductRef(_inputProduct, "ENVISAT", "L1B"));
        req.addOutputProduct(new ProductRef(_flhOutProduct, "DIMAP", "FLH_RTI"));

        // request type
        req.setType("FLH_RTI");
        req.addParameter(new Parameter("flh_wavelength_low", "664.314"));
        req.addParameter(new Parameter("flh_wavelength_mid", "680.556"));
        req.addLogFileLocation(_flhLogFile);
        return req;
    }

    /**
     * Creates a request for the flh/rti processor
     */
    private Request createMultipleQualifierRequest() {
        Request req = new Request();
        req.addInputProduct(new ProductRef(_inputProduct, "ENVISAT", "L1B"));
        req.addOutputProduct(new ProductRef(_flhOutProduct, "DIMAP", "FLH_RTI"));

        // request type
        req.setType("QUALIFIER_TEST");

        Parameter parameter = new Parameter("band_names", "radiance_1,radiance_2,radiance_4,radiance_13");
        parameter.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "productTypes", "MER_RR__1P,MER_FR__1P");
        req.addParameter(parameter);

        parameter = new Parameter("virtual_band_names", "red1,green1,blue1");
        parameter.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "otherQualifier1",
                                                   "otherQualifierValue1");
        parameter.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + "otherQualifier2",
                                                   "otherQualifierValue2");
        req.addParameter(parameter);

        return req;
    }
}
