package org.esa.nest.dataio.TAXI;

import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

/**
 * Created by lveci on 17/12/2014.
 */
public class TestTaxiParameterFileReader {

    public final static File inputParameterFile = new File(TestData.inputSAR+"InSAR"+File.separator+"pp_m20140809_s20140821_s1a-slc-vv_SS1_with_comments.xml");

    @Test
    public void testOpen() throws Exception {
        final File inputFile = inputParameterFile;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }

        final TAXIParameterFileReader reader = new TAXIParameterFileReader(inputFile);
        reader.readParameterFile();

    }
}
