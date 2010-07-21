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
package org.esa.beam.processor.smac.ui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.processor.smac.SmacConstants;
import org.esa.beam.processor.smac.SmacRequestElementFactory;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

public class SmacRequestEditorDataModulTest extends TestCase {

    private SmacRequestParameterPool _dataModul;
    private SmacRequestElementFactory _factory;

    public SmacRequestEditorDataModulTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SmacRequestEditorDataModulTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        _dataModul = new SmacRequestParameterPool(null);
        _factory = SmacRequestElementFactory.getInstance();
    }

    public void testGetRequestAfterCreateClass() {
        Request request = _dataModul.getRequest();
        assertEquals(1, request.getNumInputProducts());
        assertEquals(1, request.getNumOutputProducts());
        assertEquals(0, request.getNumLogFileLocations());
        assertEquals(12, request.getNumParameters());
        List<String> expectedParamNames = getExpectedParamNames();
        for (int i = 0; i < request.getNumParameters(); i++) {
            String paramName = request.getParameterAt(i).getName();
            assertEquals(true, expectedParamNames.contains(paramName));
        }
    }

    private static List<String> getExpectedParamNames() {
        List<String> list = new LinkedList<String>();
        list.add(SmacConstants.LOG_FILE_PARAM_NAME);
        list.add(SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        list.add(SmacConstants.BANDS_PARAM_NAME);
        list.add(SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        list.add(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        list.add(SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        list.add(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);
        list.add(SmacConstants.OZONE_CONTENT_PARAM_NAME);
        list.add(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);
        list.add(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);
        list.add(SmacConstants.BITMASK_PARAM_NAME);
        list.add(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        list.add(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);

        return list;
    }

    public void testSetRequest_InputProduct() throws MalformedURLException {
        assertNotNull(_dataModul.getRequest().getInputProductAt(0));

        Request request = new Request();
        File file = new File("/MER_.xxx");
        request.addInputProduct(new ProductRef(file));
        _dataModul.setRequest(request);

        assertEquals(file.getPath(), _dataModul.getRequest().getInputProductAt(0).getFilePath());

        request = new Request();
        file = new File("/AAT_.xxx");
        request.addInputProduct(new ProductRef(file));
        _dataModul.setRequest(request);
        assertEquals(file.getPath(), _dataModul.getRequest().getInputProductAt(0).getFilePath());
    }

    public void testSetRequest_OutputDir() {
        assertNotNull(_dataModul.getRequest().getOutputProductAt(0));

        Request request = new Request();
        request.addOutputProduct(new ProductRef(new File("c:" + File.separator + "OUT" + File.separator)));
        _dataModul.setRequest(request);
        assertEquals("c:" + File.separator + "OUT.dim", _dataModul.getRequest().getOutputProductAt(0).getFilePath());

        request = new Request();
        request.addOutputProduct(
                new ProductRef(new File(SystemUtils.convertToLocalPath("g:/Other/"))));
        _dataModul.setRequest(request);
        assertEquals(SystemUtils.convertToLocalPath("g:/Other.dim"),
                     _dataModul.getRequest().getOutputProductAt(0).getFilePath());
    }

    public void testSetRequest_ProductType() {
        String name = SmacConstants.PRODUCT_TYPE_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals(EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME, parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals(EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME, parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals(EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME, parameter.getValueAsText());

        parameter = createParameter(name, EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME);
        try {
            parameter.setValueAsText("ABCD");
            fail("ParamValidateException expected!");
        } catch (ParamParseException e) {
            // ignore
        } catch (ParamValidateException e) {
            // ignore
        }
        assertEquals(EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME, parameter.getValueAsText());
    }

    public void testSetRequest_Bands() {
        String name = SmacConstants.BANDS_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "Band5,Band8"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("Band5,Band8", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "Band4,Band5,Band12"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("Band4,Band5,Band12", parameter.getValueAsText());
    }

    public void testSetRequest_AerosolType() {
        String name = SmacConstants.AEROSOL_TYPE_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, SmacConstants.AER_TYPE_DESERT));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals(SmacConstants.AER_TYPE_DESERT, parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, SmacConstants.AER_TYPE_CONTINENTAL));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals(SmacConstants.AER_TYPE_CONTINENTAL, parameter.getValueAsText());
    }

    public void testSetRequest_AEROSOL_OPTICAL_DEPTH_PARAM_NAME() {
        String name = SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "1.2"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("1.2", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "0.4"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.4", parameter.getValueAsText());
    }

    public void testSetRequest_USE_MERIS_ADS_PARAM_NAME() {
        String name = SmacConstants.USE_MERIS_ADS_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "false"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("false", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "true"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("true", parameter.getValueAsText());
    }

    public void testSetRequest_SURFACE_AIR_PRESSURE_PARAM_NAME() {
        String name = SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "870"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("870.0", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "1023"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("1023.0", parameter.getValueAsText());
    }

    public void testSetRequest_OZONE_CONTENT_PARAM_NAME() {
        String name = SmacConstants.OZONE_CONTENT_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "0.15"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.15", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "0.265"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.265", parameter.getValueAsText());
    }

    public void testSetRequest_RELATIVE_HUMIDITY_PARAM_NAME() {
        String name = SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "0.73"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.73", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "5.2"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("5.2", parameter.getValueAsText());
    }

    public void testSetRequest_DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME() {
        String name = SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME;
        assertNotNull(_dataModul.getRequest().getParameter(name));

        Request request = new Request();
        request.addParameter(createParameter(name, "0.1"));
        _dataModul.setRequest(request);
        Parameter parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.1", parameter.getValueAsText());

        request = new Request();
        request.addParameter(createParameter(name, "0.32"));
        _dataModul.setRequest(request);
        parameter = _dataModul.getRequest().getParameter(name);
        assertEquals("0.32", parameter.getValueAsText());
    }

    private Parameter createParameter(String name, String value) {
        Parameter parameter = null;
        try {
            parameter = _factory.createParameter(name, value);
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        return parameter;
    }

}
