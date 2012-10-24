package org.esa.beam.opendap;

import opendap.dap.DArrayDimension;
import org.esa.beam.opendap.datamodel.DAPVariable;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class DAPVariableTest {

    private String vType;
    private String vDataType;
    private String vName;
    private DArrayDimension validXDim;
    private DArrayDimension validYDim;
    private DArrayDimension[] vDimensions;
    private DAPVariable dapVariable;

    @Before
    public void setUp() throws Exception {
        vName = "validName";
        vType = "validType";
        vDataType = "Float32";
        validXDim = new DArrayDimension(1121, "X");
        validYDim = new DArrayDimension(812, "Y");
        vDimensions = new DArrayDimension[]{validYDim, validXDim};
        dapVariable = new DAPVariable(vName, vType, vDataType, vDimensions);
    }

    @Test
    public void testGetInfoString() {
        assertEquals("Float32 validName (Y:812, X:1121)", dapVariable.getInfotext());

        DAPVariable metadataVariable = new DAPVariable("metadata", "someType", "byte", new DArrayDimension[0]);

        assertEquals("byte metadata", metadataVariable.getInfotext());
    }

    @Test
    public void testCompareTo() {
        //preparation
        DAPVariable dapVariable2 = new DAPVariable(vName, vType, vDataType, vDimensions);
        DAPVariable dapVariable3 = new DAPVariable("zzzzzzName", vType, vDataType, vDimensions);
        DAPVariable dapVariable4 = new DAPVariable("ZzzzzzName", vType, vDataType, vDimensions);
        DAPVariable dapVariable5 = new DAPVariable("aAAnotherName", vType, vDataType, vDimensions);
        DAPVariable dapVariable6 = new DAPVariable("AAAnotherName", vType, vDataType, vDimensions);
        DAPVariable dapVariable7 = new DAPVariable(vName, vType, vDataType, new DArrayDimension[0]);

        //verification
        assertEquals(0, dapVariable.compareTo(dapVariable2));
        assertEquals(-1, dapVariable.compareTo(dapVariable3));
        assertEquals(-1, dapVariable.compareTo(dapVariable4));
        assertEquals(1, dapVariable.compareTo(dapVariable5));
        assertEquals(1, dapVariable.compareTo(dapVariable6));
        assertEquals(1, dapVariable.compareTo(dapVariable7));
    }

    @Test
    public void testEquals() throws Exception {
        //preparation
        DAPVariable dapVariable2 = new DAPVariable(vName, vType, vDataType, vDimensions);
        final DArrayDimension otherDimension = new DArrayDimension(1000, "otherDimension");
        DAPVariable dapVariable3 = new DAPVariable(vName, vType, vDataType, new DArrayDimension[]{otherDimension});

        final DArrayDimension otherXDimension = new DArrayDimension(1121, "X");
        final DArrayDimension otherYDimension = new DArrayDimension(812, "Y");
        DArrayDimension[] otherDimensions = {otherYDimension, otherXDimension};
        DAPVariable dapVariable4 = new DAPVariable(vName, vType, vDataType, otherDimensions);

        //verification
        assertTrue(dapVariable.equals(dapVariable2));
        assertFalse(dapVariable.equals(dapVariable3));
        assertTrue(dapVariable.equals(dapVariable4));
    }

    @Test
    public void testIllegalArgumentExceptionIsThrownIfNameIsNotValid() {
        final String invalidName1 = null;
        final String invalidName2 = "";
        final String invalidName3 = "    ";

        try {
            new DAPVariable(invalidName1, vType, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("name", e.getMessage());
        }

        try {
            new DAPVariable(invalidName2, vType, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("name", e.getMessage());
        }

        try {
            new DAPVariable(invalidName3, vType, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("'    ' is not a valid name", e.getMessage());
        }
    }

    @Test
    public void testIllegalArgumentExceptionIsThrownIfTypeIsNotValid() {
        final String invalidType1 = null;
        final String invalidType2 = "";
        final String invalidType3 = "    ";

        try {
            new DAPVariable(vName, invalidType1, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("type", e.getMessage());
        }

        try {
            new DAPVariable(vName, invalidType2, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("type", e.getMessage());
        }

        try {
            new DAPVariable(vName, invalidType3, vDataType, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("'    ' is not a valid type", e.getMessage());
        }
    }

    @Test
    public void testIllegalArgumentExceptionIsThrownIfDataTypeIsNotValid() {
        final String invalidDataType1 = null;
        final String invalidDataType2 = "";
        final String invalidDataType3 = "    ";

        try {
            new DAPVariable(vName, vType, invalidDataType1, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("dataType", e.getMessage());
        }

        try {
            new DAPVariable(vName, vType, invalidDataType2, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("dataType", e.getMessage());
        }

        try {
            new DAPVariable(vName, vType, invalidDataType3, vDimensions);
            fail("never come here");
        } catch (IllegalArgumentException e) {
            assertEquals("'    ' is not a valid dataType", e.getMessage());
        }
    }

    @Test
    public void testIllegalArgumentExceptionIsThrownIfDimensionsIsNotValid() {
        final DArrayDimension[] invalidDimensions = null;

        try {
            new DAPVariable(vName, vType, vDataType, invalidDimensions);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("dimensions may not be null", e.getMessage());
        }
    }
}