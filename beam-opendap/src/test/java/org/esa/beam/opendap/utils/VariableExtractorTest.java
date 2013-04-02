package org.esa.beam.opendap.utils;

import opendap.dap.DAP2Exception;
import opendap.dap.DArrayDimension;
import opendap.dap.DDS;
import opendap.dap.parser.ParseException;
import org.esa.beam.opendap.datamodel.DAPVariable;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.Test;
import thredds.catalog.InvDataset;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class VariableExtractorTest {

    @Test
    public void testThatNoVariableCanBeExtractedFromEmptyDDS() {
        OpendapLeaf leaf = new OpendapLeaf("empty", new InvDataset(null, "") {
                });

        final DAPVariable[] dapVariables = new VariableExtractor().extractVariables(leaf);

        assertEquals(0, dapVariables.length);
    }

    @Test
    public void testThatAVariableCanBeExtractedFromADDSWithOneVariable() throws DAP2Exception, ParseException {
        DDS dds = createDDSWithOneVariable();

        final DAPVariable[] dapVariables = new VariableExtractor().extractVariables(dds);

        assertEquals(1, dapVariables.length);
        assertEquals("Chlorophyll", dapVariables[0].getName());
        assertEquals("Grid", dapVariables[0].getType());
        assertEquals("Float32", dapVariables[0].getDataType());
        assertEquals(2, dapVariables[0].getNumDimensions());
        final DArrayDimension[] dimensions = dapVariables[0].getDimensions();
        assertEquals("Y", dimensions[0].getName());
        assertEquals(849, dimensions[0].getSize());
        assertEquals("X", dimensions[1].getName());
        assertEquals(1121, dimensions[1].getSize());
    }

    @Test
    public void testThatByteVariableCanBeRead() throws DAP2Exception, ParseException {
        DDS dds = createDDSWithByteVariable();

        final DAPVariable[] dapVariables = new VariableExtractor().extractVariables(dds);

        assertEquals(1, dapVariables.length);
        assertEquals("metadata", dapVariables[0].getName());
        assertEquals("atomic", dapVariables[0].getType());
        assertEquals("Byte", dapVariables[0].getDataType());
        assertEquals(0, dapVariables[0].getNumDimensions());
    }

    @Test
    public void testThatFloatVariableCanBeRead() throws DAP2Exception, ParseException {
        DDS dds = createDDSWithFloatVariable();

        final DAPVariable[] dapVariables = new VariableExtractor().extractVariables(dds);

        assertEquals(1, dapVariables.length);
        assertEquals("metadata", dapVariables[0].getName());
        assertEquals("atomic", dapVariables[0].getType());
        assertEquals("Float32", dapVariables[0].getDataType());
        assertEquals(0, dapVariables[0].getNumDimensions());
    }

    @Test
    public void testThatMultipleVariablesCanBeExtractedFromADDSWithMultipleVariables() throws DAP2Exception, ParseException {
        DDS dds = createDDSWithMultipleVariables();

        final DAPVariable[] dapVariables = new VariableExtractor().extractVariables(dds);

        assertEquals(6, dapVariables.length);
        assertEquals("Chlorophyll", dapVariables[0].getName());
        assertEquals("Total_suspended_matter", dapVariables[1].getName());
        assertEquals("Yellow_substance", dapVariables[2].getName());
        assertEquals("l2_flags", dapVariables[3].getName());
        assertEquals("X", dapVariables[4].getName());
        assertEquals("Y", dapVariables[5].getName());
    }

    private DDS createDDSWithByteVariable() throws DAP2Exception, ParseException {
        DDS dds = new DDS();
        String ddsString = "Dataset {\n" +
                           "    Byte metadata;\n" +
                           "} coastcolour%2ftasmania24948_0001%2enc;";
        dds.parse(new ByteArrayInputStream(ddsString.getBytes()));
        return dds;
    }

    private DDS createDDSWithFloatVariable() throws DAP2Exception, ParseException {
        DDS dds = new DDS();
        String ddsString = "Dataset {\n" +
                           "    Float32 metadata;\n" +
                           "} coastcolour%2ftasmania24948_0001%2enc;";
        dds.parse(new ByteArrayInputStream(ddsString.getBytes()));
        return dds;
    }

    private DDS createDDSWithOneVariable() throws DAP2Exception, ParseException {
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