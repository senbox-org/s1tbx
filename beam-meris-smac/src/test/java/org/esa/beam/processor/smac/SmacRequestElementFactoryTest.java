/*
 * $Id: SmacRequestElementFactoryTest.java,v 1.4 2007/04/13 14:50:06 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.processor.smac;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.ParamValidator;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.FileEditor;
import org.esa.beam.framework.param.editors.TextFieldEditor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.SystemUtils;

import java.io.File;

public class SmacRequestElementFactoryTest extends TestCase {

    private SmacRequestElementFactory _smacReqElemFactory;
    private File _url;
    private static final String _testPath = "c:\\test\\test.txt";

    public SmacRequestElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SmacRequestElementFactoryTest.class);
    }

    @Override
    public void setUp() {
        _smacReqElemFactory = SmacRequestElementFactory.getInstance();
        assertNotNull(_smacReqElemFactory);
        _url = new File(SystemUtils.convertToLocalPath(_testPath));
    }

    public void testCreateParameter() {
        String name;
        String value;
        name = SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME;
        value = "0.75";
        Parameter param = null;
        try {
            param = _smacReqElemFactory.createParameter(name, value);
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertEquals(name, param.getName());
        assertNotNull(param.getEditor());
        assertEquals(TextFieldEditor.class, param.getEditor().getClass());
        assertEquals(new Float(0.75).toString(), param.getValue().toString());
    }

    public void testCreateParameter_Exceptions() {
        String name;
        String value;
//        throws IllegalArgumentException if name == null
        name = null;
        try {
            _smacReqElemFactory.createParameter(name, "value");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
//        throws IllegalArgumentException if name == empty
        name = "";
        try {
            _smacReqElemFactory.createParameter(name, "value");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
//        throws IllegalArgumentException if name in not a valid name
        name = "invalid param name";
        try {
            _smacReqElemFactory.createParameter(name, "value");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

//        throws IllegalArgumentException if value == null
        name = SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME;
        value = null;
        try {
            _smacReqElemFactory.createParameter(name, value);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if value == empty
        value = "";
        try {
            _smacReqElemFactory.createParameter(name, value);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if value is out of Range
        value = "101";
        try {
            _smacReqElemFactory.createParameter(name, value);
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
    }

    public void testCreateDefValParam_Exceptions() {
//        throws IllegalArgumentException if paramName == null
        String paramName = null;
        try {
            _smacReqElemFactory.createParamWithDefaultValueSet(paramName);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if paramName == ""
        paramName = "";
        try {
            _smacReqElemFactory.createParamWithDefaultValueSet(paramName);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if paramName is invalid
        paramName = "invalid paramName";
        try {
            _smacReqElemFactory.createParamWithDefaultValueSet(paramName);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCreateDefValParam() {
        String paramName = SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME;
        Parameter paramA = _smacReqElemFactory.createParamWithDefaultValueSet(paramName);
        Parameter paramB = _smacReqElemFactory.createParamWithDefaultValueSet(paramName);
        assertNotNull(paramA);
        assertNotNull(paramB);
        assertEquals(false, paramA.equals(paramB));
        assertSame(paramA.getName(), paramB.getName());
        assertSame(paramA.getProperties(), paramB.getProperties());
        assertSame(paramA.getEditor().getClass(), paramB.getEditor().getClass());
        assertSame(paramA.getValue(), paramB.getValue());
        assertSame(paramA.getValidator(), paramB.getValidator());
        assertSame(paramA.getValueType(), paramB.getValueType());
        assertEquals(paramA.getValueAsText(), paramB.getValueAsText());
    }

    public void testGetParamInfo_exeption() {
        String name;
//        throws IllegalArgumentException if name is null
        name = null;
        try {
            _smacReqElemFactory.getParamProperties(name);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if name is empty
        name = "";
        try {
            _smacReqElemFactory.getParamProperties(name);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
//        throws IllegalArgumentException if name is invalid
        name = "invalid name";
        try {
            _smacReqElemFactory.getParamProperties(name);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetParamInfoFor_aero_type() {
        String name = SmacConstants.AEROSOL_TYPE_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(String.class, paramInfo.getValueType());
        String[] expStrArr = new String[]{"Desert", "Continental"};
        assertEquals(true, ArrayUtils.equalArrays(expStrArr, paramInfo.getValueSet()));
        assertEquals(true, paramInfo.isValueSetBound());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals("Continental", paramInfo.getDefaultValue());
    }

    public void testGetParamInfoFor_product_type() throws IllegalAccessException, InstantiationException,
                                                          ParamValidateException, RequestElementFactoryException {
        String name = SmacConstants.PRODUCT_TYPE_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(String.class, paramInfo.getValueType());
        String[] expStrArr = new String[]{
            EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME
        };
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME, paramInfo.getDefaultValue());

        ParamValidator validator = (ParamValidator) paramInfo.getValidatorClass().newInstance();
        Parameter parameter = _smacReqElemFactory.createParameter(SmacConstants.PRODUCT_TYPE_PARAM_NAME,
                                                                  EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME);
        for (String type : expStrArr) {
           validator.validate(parameter, type);
        }
    }

    public void testGetParamInfoFor_tau_aero_550() {
        String name = SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(0.2), paramInfo.getDefaultValue());
        assertEquals(new Float(0.0784), paramInfo.getMinValue());
        assertEquals(new Float(2), paramInfo.getMaxValue());
        assertNotNull(paramInfo.getPhysicalUnit());
    }

    public void testGetParamInfoFor_log_file_dir() {
        String name = SmacConstants.LOG_FILE_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(File.class, paramInfo.getValueType());
        assertEquals(FileEditor.class, paramInfo.getEditorClass());
        String curWorkDir = SystemUtils.getCurrentWorkingDir().toString();
        assertEquals(curWorkDir + SystemUtils.convertToLocalPath("/smac_log.txt"),
                     paramInfo.getDefaultValue());
        assertEquals(ProcessorConstants.LOG_FILE_LABELTEXT, paramInfo.getLabel());
        assertEquals(ProcessorConstants.LOG_FILE_DESCRIPTION, paramInfo.getDescription());
    }

    public void testGetParamInfoFor_bands() {
        String name = SmacConstants.BANDS_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(String[].class, paramInfo.getValueType());
        String[] expected;
        expected = new String[]{
            "radiance_1", "radiance_2", "radiance_3", "radiance_4", "radiance_5",
            "radiance_6", "radiance_7", "radiance_8", "radiance_9", "radiance_10",
            "radiance_11", "radiance_12", "radiance_13", "radiance_14", "radiance_15",
            "reflec_nadir_1600", "reflec_nadir_0870", "reflec_nadir_0670", "reflec_nadir_0550",
            "reflec_fward_1600", "reflec_fward_0870", "reflec_fward_0670", "reflec_fward_0550"
        };
        assertEquals(true, ArrayUtils.equalArrays(expected, paramInfo.getValueSet()));
    }

    public void testGetParamInfoFor_default_reflectance() {
        String name = SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        assertEquals(new Float(0.0), paramInfo.getDefaultValue());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(0), paramInfo.getMinValue());
        assertEquals(new Float(1), paramInfo.getMaxValue());
        assertNotNull(paramInfo.getPhysicalUnit());
    }

    public void testGetParamInfoFor_relative_humidity() {
        String name = SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        //assertEquals(new Float(.77), paramInfo.getDefaultValue());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(0), paramInfo.getMinValue());
        assertEquals(new Float(7), paramInfo.getMaxValue());
        assertEquals("g/cm^2", paramInfo.getPhysicalUnit());
    }

    public void testGetParamInfoFor_ozone_content() {
        String name = SmacConstants.OZONE_CONTENT_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        assertEquals(new Float(0.15), paramInfo.getDefaultValue());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(0), paramInfo.getMinValue());
        assertEquals(new Float(1), paramInfo.getMaxValue());
        assertEquals("cm * atm", paramInfo.getPhysicalUnit());
    }

    public void testGetParamInfoFor_surf_press() {
        String name = SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        assertEquals(new Float(1013), paramInfo.getDefaultValue());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(100), paramInfo.getMinValue());
        assertEquals(new Float(1100), paramInfo.getMaxValue());
        assertEquals("hPa", paramInfo.getPhysicalUnit());
    }

    public void testGetParamInfoFor_use_meris_ecmwf_data() {
        String name = SmacConstants.USE_MERIS_ADS_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Boolean.class, paramInfo.getValueType());
        assertEquals(Boolean.TRUE, paramInfo.getDefaultValue());
        assertEquals("Use MERIS ECMWF Data", paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
    }

    public void testGetParamInfoFor_horizontal_visibility() {
        String name = SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME;
        ParamProperties paramInfo = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo);
        ParamProperties paramInfo2 = _smacReqElemFactory.getParamProperties(name);
        assertNotNull(paramInfo2);
        assertSame(paramInfo, paramInfo2);
        assertEquals(Float.class, paramInfo.getValueType());
        assertNotNull(paramInfo.getLabel());
        assertNotNull(paramInfo.getDescription());
        assertEquals(new Float(39.2), paramInfo.getDefaultValue());
        assertEquals(SmacConstants.DEFAULT_HORIZ_VIS_DEFAULTVALUE, paramInfo.getDefaultValue());

        assertEquals(new Float(3.92), paramInfo.getMinValue());
        assertEquals(new Float(100), paramInfo.getMaxValue());
        assertEquals(SmacConstants.DEFAULT_MAX_HORIZ_VIS_MAXVALUE, paramInfo.getMaxValue());
        assertEquals("km", paramInfo.getPhysicalUnit());
    }

    public void testExceptionCreateProductRefs() {
//        throw an RequestElementFactoryException if url is null
        ProductRef ipRef = null;
        try {
            ipRef = _smacReqElemFactory.createInputProductRef(null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNull(ipRef);

//        throw an RequestElementFactoryException if url is null
        ProductRef opRef = null;
        try {
            ipRef = _smacReqElemFactory.createOutputProductRef(null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (RequestElementFactoryException e) {
            fail("RequestElementFactoryException not expected");
        }
        assertNull(opRef);
    }

    public void testReturnedValidProductRefs_Url_Null_Null() {
//        InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _smacReqElemFactory.createInputProductRef(_url, null, null);
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), ipRef.getFile().toString());
        assertNull(ipRef.getTypeId());
//        OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _smacReqElemFactory.createOutputProductRef(_url, null, null);
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), opRef.getFile().toString());
        assertNull(opRef.getTypeId());
    }

    public void testReturnedValidProductRefs_Url_Null_emptyString() {
//        InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _smacReqElemFactory.createInputProductRef(_url, null, "");
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), ipRef.getFile().toString());
        assertNull(ipRef.getFileFormat());
        assertEquals("", ipRef.getTypeId());

//        OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _smacReqElemFactory.createOutputProductRef(_url, null, "");
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), opRef.getFile().toString());
        assertNull(opRef.getFileFormat());
        assertEquals("", opRef.getTypeId());
    }

    public void testReturnedValidProductRefs_Url_Null_String() {
//        InputProductRef
        ProductRef ipRef = null;
        try {
            ipRef = _smacReqElemFactory.createInputProductRef(_url, null, "meris");
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(ipRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), ipRef.getFile().toString());
        assertEquals("meris", ipRef.getTypeId());
//        OutputProductRef
        ProductRef opRef = null;
        try {
            opRef = _smacReqElemFactory.createOutputProductRef(_url, null, "aatsr");
        } catch (RequestElementFactoryException e) {
            fail("not RequestElementFactoryException expected");
        }
        assertNotNull(opRef);
        // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
        assertEquals(_url.toString(), opRef.getFile().toString());
        assertEquals("aatsr", opRef.getTypeId());
    }
}