package org.esa.beam.dataio.ceos;

import junit.framework.TestCase;

import java.io.File;

public class CeosHelperTest extends TestCase {

    public void testGetFileFromInput() {
        String inputString = "testFile";

        File result = CeosHelper.getFileFromInput(inputString);
        assertEquals(inputString, result.getName());

        File inputFile = new File("anotherTest");
        result = CeosHelper.getFileFromInput(inputFile);
        assertEquals(inputFile.getName(), result.getName());

        result = CeosHelper.getFileFromInput(new Double(9));
        assertNull(result);
    }

}
