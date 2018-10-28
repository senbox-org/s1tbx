
package org.esa.s1tbx.io.uavsar;

import junit.framework.TestCase;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;

import java.io.File;

public class TestUAVSARReader extends TestCase {

    private final static File inputFile = new File("I:\\ESA-Data\\UAVSAR\\UA_HaitiQ_05701_10011_008_100127_L090_CX_01\\HaitiQ_05701_10011_008_100127_L090HHHH_CX_01.mlc");

    private UAVSARReaderPlugIn readerPlugin;
    private ProductReader reader;

    public TestUAVSARReader(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        readerPlugin = new UAVSARReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        reader = null;
        readerPlugin = null;
    }

    /**
     * Open a file
     *
     * @throws Exception anything
     */
    public void testOpen() throws Exception {
        if (!inputFile.exists()) return;

        assertTrue(readerPlugin.getDecodeQualification(inputFile) == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        //TestUtils.verifyProduct(product, true, true);
    }
}
