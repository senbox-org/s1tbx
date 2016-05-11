package org.esa.snap.dataio.netcdf.nc;

import org.junit.Test;
import ucar.ma2.DataType;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.stream.IntStream;


/**
 * @author Marco Peters
 */
public class MaxStringAttributeLengthTest {

    private static final int TOO_LONG = N4Variable.MAX_ATTRIBUTE_LENGTH + 10;

    @Test
    public void testMaxStringGlobalAttributeLengthNC4() throws IOException {
        NFileWriteable ncFile = N4FileWriteable.create(Files.createTempFile(getClass().getSimpleName(), null).toString());
        ncFile.addGlobalAttribute("longGlobalAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringGlobalAttributeLengthNC3() throws IOException {
        NFileWriteable ncFile = N3FileWriteable.create(Files.createTempFile(getClass().getSimpleName(), null).toString());
        ncFile.addGlobalAttribute("longGlobalAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringVariableAttributeLengthNC4() throws IOException {
        NFileWriteable ncFile = N4FileWriteable.create(Files.createTempFile(getClass().getSimpleName(), null).toString());
        NVariable variable = ncFile.addScalarVariable("metadataVariable", DataType.BYTE);
        variable.addAttribute("longVariableAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringVariableAttributeLengthNC3() throws IOException {
        NFileWriteable ncFile = N3FileWriteable.create(Files.createTempFile(getClass().getSimpleName(), null).toString());
        NVariable variable = ncFile.addScalarVariable("metadataVariable", DataType.BYTE);
        variable.addAttribute("longVariableAttributeValue", createLongString(TOO_LONG));
    }

    private String createLongString(int length) {
        StringBuilder sb = new StringBuilder();
        IntStream randomStream = new Random(123456).ints(length);
        randomStream.forEach(value -> sb.append((char) (value & 0xFF)));
        return sb.toString();
    }
}