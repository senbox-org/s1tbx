package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Constants;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 07.09.2006
 * Time: 10:32:02
 * To change this template use File | Settings | File Templates.
 */
public class RuntimeAppBTest extends TestCase {

    private RuntimeImpl runtime;

    @Override
    public void setUp() throws CoreException, RuntimeConfigException {
        System.setProperty("ceres.context", "appB");
        System.setProperty("appB.home", Config.getDirForAppB().toString());
        DefaultRuntimeConfig defaultRuntimeConfig = new DefaultRuntimeConfig();
        runtime = new RuntimeImpl(defaultRuntimeConfig, new String[0], ProgressMonitor.NULL);
        runtime.start();
    }

    @Override
    protected void tearDown() throws Exception {
        runtime.stop();
        runtime = null;
    }


    public void testAllExpectedModulesPresent() {

        Module[] modules = runtime.getModules();
        assertNotNull(modules);
        assertTrue(modules.length >= 6);

        HashMap<String, Module> map = new HashMap<String, Module>(modules.length);
        for (Module module : modules) {
            map.put(module.getSymbolicName(), module);
        }
        assertNotNull(map.get("a-module-dir-with-classes"));
        assertNotNull(map.get("a-module-dir-with-jars"));
        assertNotNull(map.get("a-module-dir-with-jars-and-classes"));
        assertNotNull(map.get("a-spi-host-module-jar"));
        assertNotNull(map.get("a-spi-client-module-jar"));
        assertNotNull(map.get("a-native-module"));
        assertNotNull(map.get("a-native-module"));
        assertNotNull(map.get("an-empty-module-dir"));
        assertNotNull(map.get("an-empty-module-jar"));
        assertNotNull(map.get(Constants.SYSTEM_MODULE_NAME));

    }


    public void testServicesLoaded() {
        ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();

        ServiceRegistry<com.acme.TestSpi1> serviceRegistry1 = factory.getServiceRegistry(com.acme.TestSpi1.class);
        Set<com.acme.TestSpi1> services1 = serviceRegistry1.getServices();
        assertEquals(1, services1.size());
        assertTrue(services1.toArray()[0] instanceof com.foo.TestSpi1Impl);

        ServiceRegistry<com.acme.TestSpi2> serviceRegistry2 = factory.getServiceRegistry(com.acme.TestSpi2.class);
        Set<com.acme.TestSpi2> services2 = serviceRegistry2.getServices();
        assertEquals(1, services2.size());
        assertTrue(services2.toArray()[0] instanceof com.foo.TestSpi2Impl);
    }
}
