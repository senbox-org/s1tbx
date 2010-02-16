package com.bc.ceres.core.runtime.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;


public class Config {

    private static final File TEST_DIR;
    private static final String TEST_DIRNAME = "testdirs";

    static {
        File testDir = null;
        URL resource = Config.class.getResource("/" + TEST_DIRNAME);
        if (resource != null) {
            try {
                testDir = new File(resource.toURI());
            } catch (URISyntaxException e) {
                // ok
            }
        }
        if (testDir == null) {
            testDir = new File("target/test-classes/" + TEST_DIRNAME).getAbsoluteFile();
        }
        TEST_DIR = testDir;
    }

    public static File getDirForAppA() {
        return new File(TEST_DIR, "app-a");
    }

    public static File getDirForAppB() {
        return new File(TEST_DIR, "app-b");
    }

    public static File getRepositoryDir() {
        return new File(TEST_DIR, "repository");
    }
}
