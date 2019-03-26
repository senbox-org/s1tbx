package org.esa.snap.vfs.utils;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtil {

    public static final String PROPERTY_NAME_DATA_DIR = "snap.vfs.tests.data.dir";

    public static boolean testDataAvailable() {
        String testDataDir = System.getProperty(PROPERTY_NAME_DATA_DIR);
        return (testDataDir != null) && !testDataDir.isEmpty() && Files.exists(Paths.get(testDataDir));
    }

}
