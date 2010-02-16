package com.bc.ceres.launcher.internal;

import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;

import java.io.File;

import junit.framework.TestCase;

public class BootstrapClasspathFactoryTest extends TestCase {

    public void testGetFiles() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(new File("./lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        File[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(new File("./modules/ceres-core-1.0.jar"), files[0]);
        assertEquals(new File("./lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(new File("./lib/xstream-1.2.jar"), files[2]);
        assertEquals(new File("./lib/jdom-1.0.jar"), files[3]);
        assertEquals(new File("./lib/jhall-2.0.4.jar"), files[4]);
    }

    public void testGetFilesWithMultCeresCore_1() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(new File("./lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/ceres-core-1.0.1.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        File[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(new File("./modules/ceres-core-1.0.1.jar"), files[0]);
        assertEquals(new File("./lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(new File("./lib/xstream-1.2.jar"), files[2]);
        assertEquals(new File("./lib/jdom-1.0.jar"), files[3]);
        assertEquals(new File("./lib/jhall-2.0.4.jar"), files[4]);
    }

    public void testGetFilesWithMultCeresCore_2() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(new File("./lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(new File("./modules/ceres-core-1.0.1.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/ceres-core-1.2.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(new File("./modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        File[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(new File("./modules/ceres-core-1.2.jar"), files[0]);
        assertEquals(new File("./lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(new File("./lib/xstream-1.2.jar"), files[2]);
        assertEquals(new File("./lib/jdom-1.0.jar"), files[3]);
        assertEquals(new File("./lib/jhall-2.0.4.jar"), files[4]);
    }
}
