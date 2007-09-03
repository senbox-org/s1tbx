/*
 * $Id: FlhMciRequestElementFactoryTest.java,v 1.3 2007/04/13 15:31:19 marcop Exp $
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
package org.esa.beam.processor.flh_mci;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.SystemUtils;

import java.io.File;

public class FlhMciRequestElementFactoryTest extends TestCase {

    private FlhMciRequestElementFactory _reqElemFactory;
    private File file;
    private static final String _illegalParamName = "illegal parameter name";
    private static final String _testPath = "c:\\test\\test.txt";

    public FlhMciRequestElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FlhMciRequestElementFactoryTest.class);
    }

    @Override
    public void setUp() {

        _reqElemFactory = FlhMciRequestElementFactory.getInstance();
        assertNotNull(_reqElemFactory);

        file = null;
            String filePath = SystemUtils.convertToLocalPath(_testPath);
            this.file = new File(filePath);
        assertNotNull(this.file);
    }

    /**
     * Tests the functionality of createParameter
     */
    public void testCreateParameter() {
        String name = FlhMciConstants.BAND_LOW_PARAM_NAME;
        String value = "low_band";
        Parameter param = null;

        try {
            param = _reqElemFactory.createParameter(name, value);
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }

        assertNotNull(param);
        assertEquals(name, param.getName());
        assertNotNull(param.getEditor());
        assertEquals(ComboBoxEditor.class, param.getEditor().getClass());
        assertEquals(value, param.getValueAsText());
    }

    /**
     * Tests createParameter for different error conditions
     */
    public void testCreateParameterErrors() {
        String name = FlhMciConstants.BAND_SIGNAL_PARAM_NAME;
        String value = "685, 700";

        // throws exception on null arguments
        try {
            _reqElemFactory.createParameter(null, value);
            fail("illegal null argument");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            _reqElemFactory.createParameter(name, null);
            fail("illegal null argument");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // throws exception on empty name string
        try {
            _reqElemFactory.createParameter("", value);
            fail("illegal empty string argument");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // throws exception on invalid parameter name
        try {
            _reqElemFactory.createParameter(_illegalParamName, value);
            fail("illegal parameter name");
        } catch (RequestElementFactoryException e) {
        }

        // throws exception on empty value string
        try {
            _reqElemFactory.createParameter(name, "");
            fail("illegal empty string argument");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }


    /**
     * Tests the correct functionality of the lower band name parameter creation.
     */
    public void testLowBandParameter() {
        Parameter param;
        String lowBandParamName = FlhMciConstants.BAND_LOW_PARAM_NAME;
        String correctVal = "radiance_6";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(lowBandParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(lowBandParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(lowBandParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(lowBandParamName, param.getName());
            // value class must match
            assertEquals(String.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_LOW_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_LOW_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_VALUEUNIT,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_LOW,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct functionality of the mid band name parameter creation.
     */
    public void testMidBandParameter() {
        Parameter param;
        String midBandParamName = FlhMciConstants.BAND_SIGNAL_PARAM_NAME;
        String correctVal = "radiance_7";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(midBandParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(midBandParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(midBandParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(midBandParamName, param.getName());
            // value class must match
            assertEquals(String.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_SIGNAL_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_SIGNAL_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_VALUEUNIT,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_SIGNAL,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct functionality of the High band name parameter creation.
     */
    public void testHighBandParameter() {
        Parameter param;
        String highBandParamName = FlhMciConstants.BAND_HIGH_PARAM_NAME;
        String correctVal = "radiance_8";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(highBandParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(highBandParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(highBandParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(highBandParamName, param.getName());
            // value class must match
            assertEquals(String.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_HIGH_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_HIGH_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_VALUEUNIT,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_HIGH,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the functionality of the invalidPixel request parameter
     */
    public void testInvalidPixelParameter() {
        Parameter param;
        String invalidPixelParamName = FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME;
        String correctVal = "0.0";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(invalidPixelParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(invalidPixelParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(invalidPixelParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(invalidPixelParamName, param.getName());
            // value class must match
            assertEquals(Float.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_VALUEUNIT,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE,
                         ((Float) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct behaviour of createInputProductRef on invalid parameters
     */
    public void testInputProductErrors() {
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        // must throw exception when no url is set
        try {
            _reqElemFactory.createInputProductRef(null, fileFormat, typeId);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // other parameter may be null
        try {
            _reqElemFactory.createInputProductRef(file, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createInputProductRef(file, fileFormat, null);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }
    }

    /**
     * Tests that createInputProductRef() returns the correct values when fed with an url and two null parameters
     */
    public void testInputProductResults_url_null_null() {
        ProductRef prod = null;

        try {
            prod = _reqElemFactory.createInputProductRef(file, null, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertNull(prod.getFileFormat());
            assertNull(prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests that createInputProductRef() returns the correct values when fed with an url and a file format
     */
    public void testInputProductResults_url_file_null() {
        ProductRef prod = null;
        String fileFormat = "testFileFormat";

        try {
            prod = _reqElemFactory.createInputProductRef(file, fileFormat, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertEquals(fileFormat, prod.getFileFormat());
            assertNull(prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests that createInputProductRef() returns the correct values when fed with all parameters
     */
    public void testInputProductResults_url_file_type() {
        ProductRef prod = null;
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        try {
            prod = _reqElemFactory.createInputProductRef(file, fileFormat, typeId);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertEquals(fileFormat, prod.getFileFormat());
            assertEquals(typeId, prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests the correct behaviour of createOutputProductRef on invalid parameters
     */
    public void testOutputProductErrors() {
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        // must throw exception when no url is set
        try {
            _reqElemFactory.createOutputProductRef(null, fileFormat, typeId);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // other parameter may be null
        try {
            _reqElemFactory.createOutputProductRef(file, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createOutputProductRef(file, fileFormat, null);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }
    }

    /**
     * Tests that createOutputProductRef() returns the correct values when fed with an url and two null parameters
     */
    public void testOutputProductResults_url_null_null() {
        ProductRef prod = null;

        try {
            prod = _reqElemFactory.createOutputProductRef(file, null, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertNull(prod.getFileFormat());
            assertNull(prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests that createOutputProductRef() returns the correct values when fed with an url and a file format
     */
    public void testOutputProductResults_url_file_null() {
        ProductRef prod = null;
        String fileFormat = "testFileFormat";

        try {
            prod = _reqElemFactory.createOutputProductRef(file, fileFormat, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertEquals(fileFormat, prod.getFileFormat());
            assertNull(prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests that createOutputProductRef() returns the correct values when fed with all parameters
     */
    public void testOutputProductResults_url_file_type() {
        ProductRef prod = null;
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        try {
            prod = _reqElemFactory.createOutputProductRef(file, fileFormat, typeId);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(file.toString(), prod.getFile().toString());
            assertEquals(fileFormat, prod.getFileFormat());
            assertEquals(typeId, prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests the preset parameter functionality
     */
    public void testPresetParam() {
        Parameter param;
        String presetParamName = FlhMciConstants.PRESET_PARAM_NAME;
        String[] correctVal = FlhMciConstants.PRESET_PARAM_VALUE_SET;

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(presetParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(presetParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(presetParamName, correctVal[2]);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(presetParamName, param.getName());
            // value class must match
            assertEquals(String[].class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.PRESET_PARAM_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.PRESET_PARAM_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(FlhMciConstants.DEFAULT_BAND_VALUEUNIT,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(correctVal[3],
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the functionality of the invalidPixel request parameter
     */
    public void testCloudCorrectionFactorParameter() {
        Parameter param;
        String cloudCorrectParamName = FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME;
        String correctVal = "1.0";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(cloudCorrectParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // must throw exception on empty value argument
        try {
            param = _reqElemFactory.createParameter(cloudCorrectParamName, "");
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(cloudCorrectParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(cloudCorrectParamName, param.getName());
            // value class must match
            assertEquals(Float.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(FlhMciConstants.CLOUD_CORRECTION_FACTOR_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(FlhMciConstants.CLOUD_CORRECTION_FACTOR_DESCRIPTION,
                         param.getProperties().getDescription());
            // value unit must fit
            assertEquals(null,
                         param.getProperties().getPhysicalUnit());
            // default value must fit
            assertEquals(FlhMciConstants.DEFAULT_CLOUD_CORRECTION_FACTOR,
                         ((Float) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }
}
