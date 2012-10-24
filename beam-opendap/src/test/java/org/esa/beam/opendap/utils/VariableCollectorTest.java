package org.esa.beam.opendap.utils;

import opendap.dap.DAP2Exception;
import opendap.dap.DDS;
import opendap.dap.parser.ParseException;
import org.esa.beam.opendap.datamodel.DAPVariable;
import org.junit.*;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.junit.Assert.*;

public class VariableCollectorTest {

    private VariableCollector variableCollector;

    @Before
    public void setUp() throws Exception {
        variableCollector = new VariableCollector();
    }

    @Test
    public void testCollectFromDDS_TwoVariables() throws DAP2Exception, ParseException {
        // preparation
        final String[] variableNames = {"Chlorophyll", "Total_suspended_matter"};
        final DDS dds = createDDSWithTwoVariables();

        // execution
        variableCollector.collectDAPVariables(dds);

        // verification
        assertExpectedVariableNamesInList(variableNames, variableCollector.getVariables());
        final Set<DAPVariable> dapVariables = variableCollector.getVariables();
        assertEquals(2, dapVariables.size());
        assertTrue(containsDAPVariableAsExpected(variableNames[0], "Grid", "Float32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[1], "Grid", "Float32", dapVariables));
    }

    @Test
    public void testCollectFromDDS_ThreeVariables() throws DAP2Exception, ParseException {
        //preparation
        final String[] variableNames = {"Baum", "Haus", "Eimer"};
        final DDS dds = getDDS(variableNames);

        //execution
        variableCollector.collectDAPVariables(dds);

        //verification
        assertExpectedVariableNamesInList(variableNames, variableCollector.getVariables());
    }

    @Test
    public void testMultipleCollectionOfTheSameDDS() throws DAP2Exception, ParseException {
        //preparation
        final String[] variableNames = new String[]{"Chlorophyll","Total_suspended_matter","Yellow_substance","l2_flags","X","Y"};
        final DDS dds = createDDSWithTwoVariables();
        final DDS dds2 = createDDSWithMultipleVariables();

        //execution
        variableCollector.collectDAPVariables(dds);
        variableCollector.collectDAPVariables(dds2);

        //verification
        assertExpectedVariableNamesInList(variableNames, variableCollector.getVariables());
        final Set<DAPVariable> dapVariables = variableCollector.getVariables();
        assertEquals(6, dapVariables.size());
        assertTrue(containsDAPVariableAsExpected(variableNames[0], "Grid", "Float32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[1], "Grid", "Float32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[2], "Grid", "Float32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[3], "Grid", "Int32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[4], "Array", "Int32", dapVariables));
        assertTrue(containsDAPVariableAsExpected(variableNames[5], "Array", "Int32", dapVariables));
    }

    private DDS getDDS(String[] variableNames) throws ParseException, DAP2Exception {
        final DDS dds = new DDS();
        final String ddsString = getDDSString(variableNames);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ddsString.getBytes());
        dds.parse(inputStream);
        return dds;
    }

    private String getDDSString(String[] variableNames) {
        final StringBuffer sb = new StringBuffer("Dataset {\n");
        for (String variableName : variableNames) {
            sb.append(getGridString(variableName));
        }
        sb.append("} MER_RR__2PNKOF20120113_101320_000001493110_00324_51631_6150.N1.nc;\n");
        return sb.toString();
    }

    private String getGridString(String variableName) {
        return "    Grid {\n" +
               "      Array:\n" +
               "        Float32 " + variableName + "[Y = 849][X = 1121];\n" +
               "      Maps:\n" +
               "        Int32 Y[Y = 849];\n" +
               "        Int32 X[X = 1121];\n" +
               "    } " + variableName + ";\n";
    }

    private void assertExpectedVariableNamesInList(String[] namesSet, Set<DAPVariable> variables) {
        assertNotNull(namesSet);
        assertEquals(variables.size(), namesSet.length);
        for (DAPVariable variable : variables) {
            boolean contained = false;
            for(String name : namesSet) {
                if(variable.getName().equals(name)) {
                    contained = true;
                }
            }
            assertEquals("Variable name " + variable.getName() + " is contained", true, contained);
        }
    }

    private boolean containsDAPVariableAsExpected(String name, String type, String dataType, Set<DAPVariable> variables) {
        for (DAPVariable variable : variables) {
            if(name.equals(variable.getName()) && type.equals(variable.getType()) && dataType.equals(variable.getDataType())) {
                return true;
            }
        }
        return false;
    }

    private DDS createDDSWithTwoVariables() throws DAP2Exception, ParseException {
        DDS dds = new DDS();
        String ddsString =
                "Dataset {\n" +
                "    Grid {\n" +
                "        Array:\n" +
                "            Float32 Chlorophyll[Y = 849][X = 1121];\n" +
                "        Maps:\n" +
                "            Int32 Y[Y = 849];\n" +
                "            Int32 X[X = 1121];\n" +
                "    } Chlorophyll;\n" +
                "    Grid {\n" +
                "      Array:\n" +
                "        Float32 Total_suspended_matter[Y = 849][X = 1121];\n" +
                "      Maps:\n" +
                "        Int32 Y[Y = 849];\n" +
                "        Int32 X[X = 1121];\n" +
                "    } Total_suspended_matter;\n" +
                "} MER_RR__2PNKOF20120113_101320_000001493110_00324_51631_6150.N1.nc;";
        dds.parse(new ByteArrayInputStream(ddsString.getBytes()));
        return dds;
    }

    private DDS createDDSWithMultipleVariables() throws DAP2Exception, ParseException {
        DDS dds = new DDS();
        String ddsString = "Dataset {\n" +
                           "    Grid {\n" +
                           "      Array:\n" +
                           "        Float32 Chlorophyll[Y = 849][X = 1121];\n" +
                           "      Maps:\n" +
                           "        Int32 Y[Y = 849];\n" +
                           "        Int32 X[X = 1121];\n" +
                           "    } Chlorophyll;\n" +
                           "    Grid {\n" +
                           "      Array:\n" +
                           "        Float32 Total_suspended_matter[Y = 849][X = 1121];\n" +
                           "      Maps:\n" +
                           "        Int32 Y[Y = 849];\n" +
                           "        Int32 X[X = 1121];\n" +
                           "    } Total_suspended_matter;\n" +
                           "    Grid {\n" +
                           "      Array:\n" +
                           "        Float32 Yellow_substance[Y = 849][X = 1121];\n" +
                           "      Maps:\n" +
                           "        Int32 Y[Y = 849];\n" +
                           "        Int32 X[X = 1121];\n" +
                           "    } Yellow_substance;\n" +
                           "    Grid {\n" +
                           "      Array:\n" +
                           "        Int32 l2_flags[Y = 849][X = 1121];\n" +
                           "      Maps:\n" +
                           "        Int32 Y[Y = 849];\n" +
                           "        Int32 X[X = 1121];\n" +
                           "    } l2_flags;\n" +
                           "    Int32 X[X = 1121];\n" +
                           "    Int32 Y[Y = 849];\n" +
                           "} MER_RR__2PNKOF20120113_101320_000001493110_00324_51631_6150.N1.nc;";
        dds.parse(new ByteArrayInputStream(ddsString.getBytes()));
        return dds;
    }

}
