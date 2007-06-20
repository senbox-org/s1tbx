/*
 * $Id: SstRequestElementFactoryTest.java,v 1.3 2007/04/13 14:39:34 norman Exp $
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
package org.esa.beam.processor.sst;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.SystemUtils;

public class SstRequestElementFactoryTest extends TestCase {

    private SstRequestElementFactory _reqElemFactory;
    private File _url;

    public SstRequestElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SstRequestElementFactoryTest.class);
    }

    @Override
    protected void setUp() {
        _reqElemFactory = SstRequestElementFactory.getInstance();
        assertNotNull(_reqElemFactory);
        _url = new File("TestFile");
    }

    /**
     * Tests the correct functionality of the singleton interface
     */
    public void testSingletonInterface() {
        SstRequestElementFactory factory;
        SstRequestElementFactory factory2;

        // we must get something
        factory = SstRequestElementFactory.getInstance();
        assertNotNull(factory);

        // and now we must get the same again
        factory2 = SstRequestElementFactory.getInstance();
        assertNotNull(factory2);
        assertEquals(factory, factory2);
    }

    /**
     * Tests the input product method for expected behaviour
     */
    public void testInputProductErrors() {
        String fileFormat = "SstFileFormat";
        String typeId = "SstTypeID";

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
            _reqElemFactory.createInputProductRef(_url, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createInputProductRef(_url, fileFormat, null);
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
        ProductRef prod;
        File actualFile;

        try {
            prod = _reqElemFactory.createInputProductRef(_url, null, null);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            actualFile = prod.getFile();
            assertNotNull(actualFile);
            // compare url as string - PERFORMANCE!
            assertEquals(_url.toString(), actualFile.toString());
            // fileFormat must be null
            assertNull(prod.getFileFormat());
            // typeID must be null
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
        ProductRef prod;
        File actualFile;
        String fileFormat = "SstFileFormat";

        try {
            prod = _reqElemFactory.createInputProductRef(_url, fileFormat, null);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            actualFile = prod.getFile();
            assertNotNull(actualFile);
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_url.toString(), actualFile.toString());
            // fileFormat must be correct
            assertEquals(fileFormat, prod.getFileFormat());
            // typeID must be null
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
        File url;
        String fileFormat = "SstFileFormat";
        String typeId = "SstTypeID";

        try {
            prod = _reqElemFactory.createInputProductRef(_url, fileFormat, typeId);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            url = prod.getFile();
            assertNotNull(url);
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_url.toString(), url.toString());
            // fileFormat must be correct
            assertEquals(fileFormat, prod.getFileFormat());
            // typeID must be correct
            assertEquals(typeId, prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests the output product method for expected behaviour
     */
    public void testOutputProductErrors() {
        String fileFormat = "SstFileFormat";
        String typeId = "SstTypeID";

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
           _reqElemFactory.createOutputProductRef(_url, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createOutputProductRef(_url, fileFormat, null);
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
        ProductRef prod;
        File actualFile;

        try {
            prod = _reqElemFactory.createOutputProductRef(_url, null, null);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            actualFile = prod.getFile();
            assertNotNull(actualFile);
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_url.toString(), actualFile.toString());
            // fileFormat must be null
            assertNull(prod.getFileFormat());
            // typeID must be null
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
        ProductRef prod;
        File actualFile;
        String fileFormat = "SstOutFormat";

        try {
            prod = _reqElemFactory.createOutputProductRef(_url, fileFormat, null);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            actualFile = prod.getFile();
            assertNotNull(actualFile);
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_url.toString(), actualFile.toString());
            // fileFormat must be correct
            assertEquals(fileFormat, prod.getFileFormat());
            // typeID must be null
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
        ProductRef prod;
        File actualFile;
        String fileFormat = "SstFormat";
        String typeId = "SST";

        try {
            prod = _reqElemFactory.createOutputProductRef(_url, fileFormat, typeId);
            // we must get something in return
            assertNotNull(prod);
            // prod must give the correct url
            actualFile = prod.getFile();
            assertNotNull(actualFile);
            // toString wurde wegen Performancesteigerung bei URL vergleichen verwendet
            assertEquals(_url.toString(), actualFile.toString());
            // fileFormat must be correct
            assertEquals(fileFormat, prod.getFileFormat());
            // typeID must be correct
            assertEquals(typeId, prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests the correct creation of the parameter Process dual view sst
     */
    public void testProcessDualViewParameter() {
        Parameter param;
        String processDualParamName = SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME;
        String correctVal = "true";

        // must throw exception on null value argument
        try {
             _reqElemFactory.createParameter(processDualParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(processDualParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(processDualParamName, param.getName());
            // value class must match
            assertEquals(Boolean.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // param info must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.PROCESS_DUAL_VIEW_SST_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.PROCESS_DUAL_VIEW_SST_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_PROCESS_DUAL_VIEW_SST,
                         ((Boolean) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter dual view sst coefficient file
     */
    public void testDualViewCoefficientFile() {
        Parameter param;
        String processDualParamName = SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME;
        String correctVal = "c:\\testfile.coef";

        // must throw exception on null value argument
        try {
            _reqElemFactory.createParameter(processDualParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(processDualParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(processDualParamName, param.getName());
            // value class must match
            assertEquals(File.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.DUAL_VIEW_COEFF_FILE_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.DUAL_VIEW_COEFF_FILE_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_DUAL_VIEW_COEFF_FILE,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter dual view bitmask
     */
    public void testDualViewBitmask() {
        Parameter param;
        String dualBitmaskName = SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME;
        String correctVal = "flags.INVALID";

        // must throw exception on null value argument
        try {
            _reqElemFactory.createParameter(dualBitmaskName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(dualBitmaskName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(dualBitmaskName, param.getName());
            // value class must match
            assertEquals(String.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.DUAL_VIEW_BITMASK_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.DUAL_VIEW_BITMASK_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_DUAL_VIEW_BITMASK,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter Process nadir view sst
     */
    public void testProcessNadirViewParameter() {
        Parameter param;
        String processNadirParamName = SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME;
        String correctVal = "true";

        // must throw exception on null value argument
        try {
            _reqElemFactory.createParameter(processNadirParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(processNadirParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(processNadirParamName, param.getName());
            // value class must match
            assertEquals(Boolean.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.PROCESS_NADIR_VIEW_SST_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.PROCESS_NADIR_VIEW_SST_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_PROCESS_NADIR_VIEW_SST,
                         ((Boolean) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter nadir view sst coefficient file
     */
    public void testNadirViewCoefficientFile() {
        Parameter param;
        String processNadirParamName = SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME;
        String correctVal = "c:\\testfile.coef";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(processNadirParamName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(processNadirParamName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(processNadirParamName, param.getName());
            // value class must match
            assertEquals(File.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.NADIR_VIEW_COEFF_FILE_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.NADIR_VIEW_COEFF_FILE_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_NADIR_VIEW_COEFF_FILE,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter nadir view bitmask
     */
    public void testNadirViewBitmask() {
        Parameter param;
        String nadirBitmaskName = SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME;
        String correctVal = "flags.INVALID";

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(nadirBitmaskName, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(nadirBitmaskName, correctVal);
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(nadirBitmaskName, param.getName());
            // value class must match
            assertEquals(String.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.NADIR_VIEW_BITMASK_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.NADIR_VIEW_BITMASK_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_NADIR_VIEW_BITMASK,
                         ((String) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter invalid pixel value
     */
    public void testInvalidPixel() {
        Parameter param;
        String invPixParam = SstConstants.INVALID_PIXEL_PARAM_NAME;
        Float correctVal = new Float(0.65f);

        // must throw exception on null value argument
        try {
            param = _reqElemFactory.createParameter(invPixParam, null);
            fail("IllegalArgumentException expected");
        } catch (RequestElementFactoryException e) {
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // now check the parameter returned for correct settings
        try {
            param = _reqElemFactory.createParameter(invPixParam, correctVal.toString());
            // we must get a parameter
            assertNotNull(param);
            // name must fit
            assertEquals(invPixParam, param.getName());
            // value class must match
            assertEquals(Float.class, param.getValueType());
            // editor must be present
            assertNotNull(param.getEditor());
            // parameter properties must be present
            assertNotNull(param.getProperties());
            // label must fit
            assertEquals(SstConstants.INVALID_PIXEL_LABELTEXT,
                         param.getProperties().getLabel());
            // description must fit
            assertEquals(SstConstants.INVALID_PIXEL_DESCRIPTION,
                         param.getProperties().getDescription());
            // default value must fit
            assertEquals(SstConstants.DEFAULT_INVALID_PIXEL,
                         ((Float) param.getProperties().getDefaultValue()));
        } catch (RequestElementFactoryException e) {
            fail("no RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the default parameter for the input product
     */
    public void testCreateDefaultInputParameter() {
        Parameter param = null;
        File expectedIn = new File("");

        param = _reqElemFactory.createDefaultInputProductParameter();
        // must return something
        assertNotNull(param);
        // parameter must have the expected default value
        assertEquals(expectedIn.toString(), param.getValueAsText());
        // parameter must have the expected description
        assertEquals(DefaultRequestElementFactory.INPUT_PRODUCT_DESCRIPTION, param.getProperties().getDescription());
        // parameter must have the expected label
        assertEquals(DefaultRequestElementFactory.INPUT_PRODUCT_LABELTEXT, param.getProperties().getLabel());
    }

    /**
     * Tests the correct creation of the default parameter for the output product
     */
    public void testCreateDefaultOutputParameter() {
        Parameter param = null;
        File expectedOut = new File(SystemUtils.getUserHomeDir(), SstConstants.DEFAULT_FILE_NAME);

        param = _reqElemFactory.createDefaultOutputProductParameter();
        // must return something
        assertNotNull(param);
        // parameter must have the expected default value
        assertEquals(expectedOut.toString(), param.getValueAsText());
        // parameter must have the expected description
        assertEquals(SstConstants.OUTPUT_PRODUCT_DESCRIPTION, param.getProperties().getDescription());
        // parameter must have the expected label
        assertEquals(SstConstants.OUTPUT_PRODUCT_LABELTEXT, param.getProperties().getLabel());
    }

    /**
     * Tests the correct creation of the default parameter for the logfile location
     */
    public void testCreateDefaultLogfile() {
        Parameter param = null;

        param = _reqElemFactory.createDefaultLogfileParameter();
        // must return something
        assertNotNull(param);
        // parameter must have the expected description
        assertEquals(SstConstants.LOG_FILE_DESCRIPTION, param.getProperties().getDescription());
        // parameter must have the expected label
        assertEquals(SstConstants.LOG_FILE_LABELTEXT, param.getProperties().getLabel());
    }
}
