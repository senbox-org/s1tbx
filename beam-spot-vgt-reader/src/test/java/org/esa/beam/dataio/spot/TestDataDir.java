package org.esa.beam.dataio.spot;

import junit.framework.Assert;

import java.io.File;

public class TestDataDir {
    public static File get() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./beam-spot-vgt-reader/src/test/data/");
            if (!dir.exists()) {
                Assert.fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    public static File get(String path) {
        return new File(get(), path);
    }
}
