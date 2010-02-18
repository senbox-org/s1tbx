/*
 * $Id: L3RequestElementFactoryTest.java,v 1.4 2007/04/13 14:52:35 marcop Exp $
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
package org.esa.beam.processor.binning;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.SystemUtils;

import java.io.File;

public class L3RequestElementFactoryTest extends TestCase {

    private L3RequestElementFactory _reqElemFactory;
    private File _file;
    private static final String _testPath = "c:\\test\\test.txt";

    public L3RequestElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(L3RequestElementFactoryTest.class);
    }


    @Override
    protected void setUp() {
        _reqElemFactory = L3RequestElementFactory.getInstance();
        assertNotNull(_reqElemFactory);
        _file = new File(SystemUtils.convertToLocalPath(_testPath));
    }

    /**
     * Simple test - doe we get a reference and is ist the same on the second call
     */
    public void testSingletonInterface() {
        L3RequestElementFactory fac1;
        L3RequestElementFactory fac2;

        fac1 = L3RequestElementFactory.getInstance();
        assertNotNull(fac1);

        fac2 = L3RequestElementFactory.getInstance();
        assertNotNull(fac2);
        assertEquals(fac1, fac2);
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
            _reqElemFactory.createInputProductRef(_file, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createInputProductRef(_file, fileFormat, null);
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

        try {
            prod = _reqElemFactory.createInputProductRef(_file, null, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
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
        ProductRef prod;
        String fileFormat = "testFileFormat";

        try {
            prod = _reqElemFactory.createInputProductRef(_file, fileFormat, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
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
        ProductRef prod;
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        try {
            prod = _reqElemFactory.createInputProductRef(_file, fileFormat, typeId);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
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
            _reqElemFactory.createOutputProductRef(_file, null, typeId);
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected expected");
        }

        try {
            _reqElemFactory.createOutputProductRef(_file, fileFormat, null);
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

        try {
            prod = _reqElemFactory.createOutputProductRef(_file, null, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
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
        ProductRef prod;
        String fileFormat = "testFileFormat";

        try {
            prod = _reqElemFactory.createOutputProductRef(_file, fileFormat, null);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
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
        ProductRef prod;
        String fileFormat = "testFileFormat";
        String typeId = "testTypeID";

        try {
            prod = _reqElemFactory.createOutputProductRef(_file, fileFormat, typeId);
            assertNotNull(prod);
            assertNotNull(prod.getFile());
            assertEquals(_file.toString(), prod.getFile().toString());
            assertEquals(fileFormat, prod.getFileFormat());
            assertEquals(typeId, prod.getTypeId());
        } catch (RequestElementFactoryException e) {
            fail("No Exception expected");
        } catch (IllegalArgumentException e) {
            fail("No Exception expected");
        }
    }

    /**
     * Tests the correct creation of the parameter process type
     */
    public void testProcessTypeParameter() {
        Parameter param;

        // check that we get the parameter
        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.PROCESS_TYPE_PARAM_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.PROCESS_TYPE_INIT, param.getValueAsText());
        assertEquals(true,
                     ObjectUtils.equalObjects(
                             new String[]{L3Constants.PROCESS_TYPE_INIT,
                                          L3Constants.PROCESS_TYPE_UPDATE,
                                          L3Constants.PROCESS_TYPE_FINALIZE},
                             param.getProperties().getValueSet()));
        assertEquals(true, param.getProperties().isValueSetBound());
        assertEquals(String.class, param.getProperties().getValueType());

        // must fail if we try to create with a value not in the valueset
        try {
            param = _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, "gnumpf");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        // must succeed when we try to create with correct value
        try {
            param = _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT);
            assertNotNull(param);
            assertEquals(L3Constants.PROCESS_TYPE_INIT, param.getValueAsText());

            param = _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                    L3Constants.PROCESS_TYPE_UPDATE);
            assertNotNull(param);
            assertEquals(L3Constants.PROCESS_TYPE_UPDATE, param.getValueAsText());

            param = _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                    L3Constants.PROCESS_TYPE_FINALIZE);
            assertNotNull(param);
            assertEquals(L3Constants.PROCESS_TYPE_FINALIZE, param.getValueAsText());

        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter database_dir
     */
    public void testDatabaseDirParamerter() {
        Parameter param;
        File expDbDir = new File(SystemUtils.getUserHomeDir(), L3Constants.DEFAULT_DATABASE_NAME);

        // check that we get the parameter
        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.DATABASE_PARAM_NAME);
        assertNotNull(param);
        assertEquals(expDbDir.toString(), param.getValueAsText());
        assertEquals(File.class, param.getProperties().getValueType());

        // must succeed when we try to create with correct value
        try {
            String testDir1 = "C:\testdata1";
            String testDir2 = "D:\testdata2";

            param = _reqElemFactory.createParameter(L3Constants.DATABASE_PARAM_NAME, testDir1);
            assertNotNull(param);
            assertEquals(testDir1, param.getValueAsText());

            param = _reqElemFactory.createParameter(L3Constants.DATABASE_PARAM_NAME, testDir2);
            assertNotNull(param);
            assertEquals(testDir2, param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter grid_cell_size
     */
    public void testGridCellSizeParameter() {
        Parameter param;
        ParamProperties props = null;

        // check that we get the parameter
        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.GRID_CELL_SIZE_DEFAULT.floatValue(), ((Float) param.getValue()).floatValue(), 1e-6);
        assertEquals(L3Constants.GRID_CELL_SIZE_MIN_VALUE.floatValue(),
                     (param.getProperties().getMinValue()).floatValue(), 1e-6);
        assertEquals(L3Constants.GRID_CELL_SIZE_MAX_VALUE.floatValue(),
                     (param.getProperties().getMaxValue()).floatValue(), 1e-6);
        assertEquals(Float.class, param.getProperties().getValueType());

        props = param.getProperties();
        assertNotNull(props);
        assertEquals(L3Constants.GRID_CELL_SIZE_LABEL, props.getLabel());
        assertEquals(L3Constants.GRID_CELL_SIZE_DESCRIPTION, props.getDescription());
        assertEquals(L3Constants.GRID_CELL_SIZE_UNIT, props.getPhysicalUnit());

        // force exception by too small value
        try {
            param = _reqElemFactory.createParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME, "0.00001");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        // create correct parameter
        try {
            String test1 = "0.9";
            String test2 = "6.89";

            param = _reqElemFactory.createParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME, test1);
            assertNotNull(param);
            assertEquals(test1, param.getValueAsText());

            param = _reqElemFactory.createParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME, test2);
            assertNotNull(param);
            assertEquals(test2, param.getValueAsText());

        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter binning_algorithm
     */
    public void testParameterBinningAlgorithm() {
        Parameter param;
        ParamProperties props = null;
        String[] valueSet;

        // check that we get the parameter
        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.ALGORITHM_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.ALGORITHM_VALUE_SET[0], param.getValueAsText());
        props = param.getProperties();
        assertNotNull(props);
        valueSet = props.getValueSet();
        assertEquals(L3Constants.ALGORITHM_VALUE_SET.length, valueSet.length);
        for (int n = 0; n < L3Constants.ALGORITHM_VALUE_SET.length; n++) {
            assertEquals(L3Constants.ALGORITHM_VALUE_SET[n], valueSet[n]);
        }
        assertEquals(true, props.isValueSetBound());
        assertEquals(String.class, props.getValueType());
        assertEquals(L3Constants.ALGORITHM_LABEL, props.getLabel());
        assertEquals(L3Constants.ALGORITHM_DESCRIPTION, props.getDescription());


        // force exception by setting invalid algorithm
        try {
            param = _reqElemFactory.createParameter(L3Constants.ALGORITHM_PARAMETER_NAME, "EXPONENTIAL");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.ALGORITHM_PARAMETER_NAME, "Arithmetic Mean");
            assertNotNull(param);
            assertEquals("Arithmetic Mean", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.ALGORITHM_PARAMETER_NAME, "Maximum Likelihood");
            assertNotNull(param);
            assertEquals("Maximum Likelihood", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    /**
     * Tests the correct creation of the parameter binning_algorithm
     */
    public void testParameterWeightCoefficient() {
        Parameter param;
        ParamProperties props = null;

        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(0.5f, ((Float) param.getValue()).floatValue(), 1e-6);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(Float.class, props.getValueType());
        assertEquals(L3Constants.WEIGHT_COEFFICIENT_LABEL, props.getLabel());
        assertEquals(L3Constants.WEIGHT_COEFFICIENT_DESCRIPTION, props.getDescription());

        String val1 = "0.98";
        String val2 = "-1.23";

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME, val1);
            assertNotNull(param);
            assertEquals(val1, param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME, val2);
            assertNotNull(param);
            assertEquals(val2, param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    public void testDeleteDBParameter() {
        Parameter param;
        ParamProperties props = null;

        // check that we get the parameter
        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.DELETE_DB_PARAMETER_NAME);
        assertNotNull(param);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(Boolean.class, props.getValueType());
        assertEquals(new Boolean(false), props.getDefaultValue());
        assertEquals(L3Constants.DELETE_DB_LABEL, props.getLabel());
        assertEquals(L3Constants.DELETE_DB_DESCRIPTION, props.getDescription());

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.DELETE_DB_PARAMETER_NAME, "false");
            assertNotNull(param);
            assertEquals("false", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.DELETE_DB_PARAMETER_NAME, "true");
            assertNotNull(param);
            assertEquals("true", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }
    }

    public void testLatMinParameter() {
        Parameter param;
        ParamProperties props = null;

        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.LAT_MIN_DEFAULT_VALUE.floatValue(), ((Float) param.getValue()).floatValue(), 1e-6f);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(L3Constants.LAT_MINIMUM_VALUE.floatValue(), props.getMinValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LAT_MAXIMUM_VALUE.floatValue(), props.getMaxValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LAT_MIN_LABEL, props.getLabel());
        assertEquals(L3Constants.LAT_MIN_DESCRIPTION, props.getDescription());
        assertEquals(L3Constants.LAT_LON_PHYS_UNIT, props.getPhysicalUnit());

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MIN_PARAMETER_NAME, "52.0");
            assertNotNull(param);
            assertEquals("52.0", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.LAT_MIN_PARAMETER_NAME, "-36.8");
            assertNotNull(param);
            assertEquals("-36.8", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }

        // trigger error
        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MIN_PARAMETER_NAME, "-152.0");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MIN_PARAMETER_NAME, "678.9");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
    }

    public void testLatMaxParameter() {
        Parameter param;
        ParamProperties props = null;

        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.LAT_MAX_DEFAULT_VALUE.floatValue(), ((Float) param.getValue()).floatValue(), 1e-6f);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(L3Constants.LAT_MINIMUM_VALUE.floatValue(), props.getMinValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LAT_MAXIMUM_VALUE.floatValue(), props.getMaxValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LAT_MAX_LABEL, props.getLabel());
        assertEquals(L3Constants.LAT_MAX_DESCRIPTION, props.getDescription());
        assertEquals(L3Constants.LAT_LON_PHYS_UNIT, props.getPhysicalUnit());

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MAX_PARAMETER_NAME, "52.0");
            assertNotNull(param);
            assertEquals("52.0", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.LAT_MAX_PARAMETER_NAME, "-36.8");
            assertNotNull(param);
            assertEquals("-36.8", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }

        // trigger error
        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MAX_PARAMETER_NAME, "-192.0");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        try {
            param = _reqElemFactory.createParameter(L3Constants.LAT_MAX_PARAMETER_NAME, "678.9");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
    }

    public void testLonMinParameter() {
        Parameter param;
        ParamProperties props = null;

        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.LON_MIN_DEFAULT_VALUE.floatValue(), ((Float) param.getValue()).floatValue(), 1e-6f);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(L3Constants.LON_MINIMUM_VALUE.floatValue(), props.getMinValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LON_MAXIMUM_VALUE.floatValue(), props.getMaxValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LON_MIN_LABEL, props.getLabel());
        assertEquals(L3Constants.LON_MIN_DESCRIPTION, props.getDescription());
        assertEquals(L3Constants.LAT_LON_PHYS_UNIT, props.getPhysicalUnit());

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MIN_PARAMETER_NAME, "52.0");
            assertNotNull(param);
            assertEquals("52.0", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.LON_MIN_PARAMETER_NAME, "-36.8");
            assertNotNull(param);
            assertEquals("-36.8", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }

        // trigger error
        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MIN_PARAMETER_NAME, "-252.0");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MIN_PARAMETER_NAME, "878.9");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
    }

    public void testLonMaxParameter() {
        Parameter param;
        ParamProperties props = null;

        param = _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(L3Constants.LON_MAX_DEFAULT_VALUE.floatValue(), ((Float) param.getValue()).floatValue(), 1e-6f);
        props = param.getProperties();
        assertNotNull(props);
        assertEquals(L3Constants.LON_MINIMUM_VALUE.floatValue(), props.getMinValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LON_MAXIMUM_VALUE.floatValue(), props.getMaxValue().floatValue(), 1e-6);
        assertEquals(L3Constants.LON_MAX_LABEL, props.getLabel());
        assertEquals(L3Constants.LON_MAX_DESCRIPTION, props.getDescription());
        assertEquals(L3Constants.LAT_LON_PHYS_UNIT, props.getPhysicalUnit());

        // create with correct values
        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MAX_PARAMETER_NAME, "152.0");
            assertNotNull(param);
            assertEquals("152.0", param.getValueAsText());
            param = _reqElemFactory.createParameter(L3Constants.LON_MAX_PARAMETER_NAME, "-136.8");
            assertNotNull(param);
            assertEquals("-136.8", param.getValueAsText());
        } catch (RequestElementFactoryException e) {
            fail("No RequestElementFactoryException expected");
        }

        // trigger error
        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MAX_PARAMETER_NAME, "-556.0");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }

        try {
            param = _reqElemFactory.createParameter(L3Constants.LON_MAX_PARAMETER_NAME, "278.9");
            fail("RequestElementFactoryException expected");
        } catch (RequestElementFactoryException e) {
        }
    }
}

