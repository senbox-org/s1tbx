package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

public class EnvisatProductReaderPluginTest extends TestCase {

    public void testGetDefaultFileExtension() {
        final EnvisatProductReaderPlugIn plugIn = new EnvisatProductReaderPlugIn();

        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertEquals(".N1", defaultFileExtensions[0]);
        assertEquals(".E1", defaultFileExtensions[1]);
        assertEquals(".E2", defaultFileExtensions[2]);
    }
}
