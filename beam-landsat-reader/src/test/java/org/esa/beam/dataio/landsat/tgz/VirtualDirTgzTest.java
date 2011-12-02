package org.esa.beam.dataio.landsat.tgz;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class VirtualDirTgzTest {

    @Test
    public void testOpenTgz() throws IOException {
        File testTgz = new File("./beam-landsat-reader/src/test/resources/org/esa/beam/dataio/landsat/tgz/test-archive.tgz");
        if (!testTgz.isFile()) {
            testTgz = new File("./src/test/resources/org/esa/beam/dataio/landsat/tgz/test-archive.tgz");
        }
        assertTrue(testTgz.isFile());

        final VirtualDirTgz vdTgz = new VirtualDirTgz(testTgz);
    }
}
