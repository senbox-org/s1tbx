package org.esa.beam.dataio.landsat;

import com.bc.ceres.core.VirtualDir;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LandsatTMReaderPluginTest {

    @Test
    public void testGetInput_Directory_File() {
        final File testDirectory = TestUtil.getTestDirectory("");

        final VirtualDir input = LandsatTMReaderPlugIn.getInput(testDirectory);
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Directory_String() {
        final File testDirectory = TestUtil.getTestDirectory("");

        final VirtualDir input = LandsatTMReaderPlugIn.getInput(testDirectory.getPath());
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }



    // @todo 1 tb/tb test for
    // - file inside directory
    // - zip file
    // - tar file
    // - tar.gz file
}
