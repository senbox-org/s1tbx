/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.*;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;

public class ModuleResolverTest extends TestCase {

    private ModuleRegistry moduleRegistry;

    @Override
    protected void setUp() throws Exception {
        moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/module-a.xml",
                "xml/module-b.xml",
                "xml/module-c.xml",
                "xml/module-d.xml",
                "xml/module-e.xml",
        });
    }

    @Override
    protected void tearDown() throws Exception {
        moduleRegistry = null;
    }

    public void testNullArgConvention() throws IOException, CoreException {
        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            moduleResolver.resolve(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        try {
            final ModuleResolver moduleResolver = new ModuleResolver(null, false);
            moduleResolver.resolve(getModule("module-a"));
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            moduleResolver.resolve(getModule("module-a"));
            // ok
        } catch (NullPointerException e) {
            fail();
        }

    }

    public void testResolvingAResolvesOnlyA() throws CoreException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-a"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 1, new ModuleImpl[]{});

        TestHelpers.assertModuleIsInstalled(getModule("module-b"));
        TestHelpers.assertModuleIsInstalled(getModule("module-c"));
        TestHelpers.assertModuleIsInstalled(getModule("module-d"));
        TestHelpers.assertModuleIsInstalled(getModule("module-e"));

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
    }

    public void testResolvingBResolvesBAndA() throws CoreException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-b"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 1, new ModuleImpl[]{});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsInstalled(getModule("module-c"));
        TestHelpers.assertModuleIsInstalled(getModule("module-d"));
        TestHelpers.assertModuleIsInstalled(getModule("module-e"));

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-b").getClassLoader() instanceof ModuleClassLoader);
    }

    public void testResolvingCResolvesCAndBAndA() throws CoreException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-c"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 1, new ModuleImpl[]{});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-c"), 1, new ModuleImpl[]{getModule("module-b")});
        TestHelpers.assertModuleIsInstalled(getModule("module-d"));
        TestHelpers.assertModuleIsInstalled(getModule("module-e"));

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-b").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-c").getClassLoader() instanceof ModuleClassLoader);
    }

    public void testResolvingDResolvesDAndA() throws CoreException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-d"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 1, new ModuleImpl[]{});
        TestHelpers.assertModuleIsInstalled(getModule("module-b"));
        TestHelpers.assertModuleIsInstalled(getModule("module-c"));
        TestHelpers.assertModuleIsResolved(getModule("module-d"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsInstalled(getModule("module-e"));

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-d").getClassLoader() instanceof ModuleClassLoader);

    }

    public void testResolvingEResolvesAllExceptC() throws CoreException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-e"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 2, new ModuleImpl[]{});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsInstalled(getModule("module-c"));
        TestHelpers.assertModuleIsResolved(getModule("module-d"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-e"), 1,
                                           new ModuleImpl[]{getModule("module-b"), getModule("module-d")});

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-b").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-d").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-e").getClassLoader() instanceof ModuleClassLoader);
    }

    public void testResolvingCAndEResolvesAll() throws CoreException {
        final ModuleResolver moduleResolver1 = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                  false);
        moduleResolver1.resolve(getModule("module-c"));
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-e"));

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 3, new ModuleImpl[]{});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 2, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-c"), 1, new ModuleImpl[]{getModule("module-b")});
        TestHelpers.assertModuleIsResolved(getModule("module-d"), 1, new ModuleImpl[]{getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-e"), 1,
                                           new ModuleImpl[]{getModule("module-b"), getModule("module-d")});

        assertTrue(getModule("module-a").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-b").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-c").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-d").getClassLoader() instanceof ModuleClassLoader);
        assertTrue(getModule("module-e").getClassLoader() instanceof ModuleClassLoader);
    }

    public void testModuleClassLoaderE() throws CoreException, URISyntaxException, IOException {
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve(getModule("module-e"));

        ClassLoader clE = getModule("module-e").getClassLoader();

        assertTrue(clE instanceof ModuleClassLoader);
        ModuleClassLoader mclE = (ModuleClassLoader) clE;
        URL[] dependencyUrls = mclE.getURLs();
        assertTrue(dependencyUrls.length > 0);
        File location = getCanonicalFile(getModule("module-e").getLocation());
        File dependencyUrl = getCanonicalFile(dependencyUrls[0]);
        assertEquals(dependencyUrl, location);

        File expectedFile;
        File actualFile;

        ModuleClassLoader delegateToB = (ModuleClassLoader) mclE.getDelegates()[0];
        expectedFile = getCanonicalFile(delegateToB.getURLs()[0]);
        actualFile = getCanonicalFile(getModule("module-b").getLocation());
        assertEquals(expectedFile, actualFile);

        ModuleClassLoader delegateToD = (ModuleClassLoader) mclE.getDelegates()[1];
        expectedFile = getCanonicalFile(delegateToD.getURLs()[0]);
        actualFile = getCanonicalFile(getModule("module-d").getLocation());
        assertEquals(expectedFile, actualFile);

        ModuleClassLoader delegateToA = (ModuleClassLoader) delegateToB.getDelegates()[0];
        expectedFile = getCanonicalFile(delegateToA.getURLs()[0]);
        actualFile = getCanonicalFile(getModule("module-a").getLocation());
        assertEquals(expectedFile, actualFile);

        ModuleClassLoader delegateToA2 = (ModuleClassLoader) delegateToD.getDelegates()[0];
        expectedFile = getCanonicalFile(delegateToA2.getURLs()[0]);
        actualFile = getCanonicalFile(getModule("module-a").getLocation());
        assertEquals(expectedFile, actualFile);
    }

    private static File getCanonicalFile(URL url) throws IOException, URISyntaxException {
        return new File(url.toURI()).getCanonicalFile();
    }

    public void testExtensionsAndExtensionPoints() throws CoreException, IOException {

        Module module_a = moduleRegistry.getModule(1);
        Module module_b = moduleRegistry.getModule(2);
        Module module_c = moduleRegistry.getModule(3);
        Module module_d = moduleRegistry.getModule(4);
        Module module_e = moduleRegistry.getModule(5);

        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                 false);
        moduleResolver.resolve((ModuleImpl) module_a);
        moduleResolver.resolve((ModuleImpl) module_b);
        moduleResolver.resolve((ModuleImpl) module_c);
        moduleResolver.resolve((ModuleImpl) module_d);
        moduleResolver.resolve((ModuleImpl) module_e);


        assertNotNull(module_a);
        assertNotNull(module_b);
        assertNotNull(module_c);
        assertNotNull(module_d);
        assertNotNull(module_e);

        Extension e_31 = module_c.getExtensions()[0];
        Extension e_32 = module_c.getExtensions()[1];
        Extension e_33 = module_c.getExtensions()[2];
        Extension e_34 = module_c.getExtensions()[3];
        Extension e_35 = module_c.getExtensions()[4];
        Extension e_36 = module_c.getExtensions()[5];

        assertEquals(3, module_b.getExtensionPoints().length);
        ExtensionPoint ep_21 = module_b.getExtensionPoints()[0];
        ExtensionPoint ep_22 = module_b.getExtensionPoints()[1];
        ExtensionPoint ep_23 = module_b.getExtensionPoints()[2];

        assertEquals(2, module_d.getExtensionPoints().length);
        ExtensionPoint ep_41 = module_d.getExtensionPoints()[0];
        ExtensionPoint ep_42 = module_d.getExtensionPoints()[1];

        assertSame(ep_21, e_31.getExtensionPoint());
        assertSame(ep_21, e_32.getExtensionPoint());
        assertSame(ep_22, e_33.getExtensionPoint());
        assertSame(ep_23, e_34.getExtensionPoint());
        assertSame(ep_23, e_35.getExtensionPoint());
        assertSame(ep_23, e_36.getExtensionPoint());

        assertSame(ep_23, e_35.getExtensionPoint());
        assertSame(ep_23, e_36.getExtensionPoint());

        assertSame(ep_21, moduleRegistry.getExtensionPoint("module-b:ep-1"));
        assertSame(ep_22, moduleRegistry.getExtensionPoint("module-b:ep-2"));
        assertSame(ep_23, moduleRegistry.getExtensionPoint("module-b:ep-3"));

        assertEquals(3, ep_21.getExtensions().length);
        assertEquals(4, ep_22.getExtensions().length);
        assertEquals(6, ep_23.getExtensions().length);
        assertEquals(0, ep_41.getExtensions().length);
        assertEquals(2, ep_42.getExtensions().length);

        assertEquals(3, moduleRegistry.getExtensions("module-b:ep-1").length);
        assertEquals(4, moduleRegistry.getExtensions("module-b:ep-2").length);
        assertEquals(6, moduleRegistry.getExtensions("module-b:ep-3").length);
        assertEquals(0, moduleRegistry.getExtensions("module-d:ep-1").length);
        assertEquals(2, moduleRegistry.getExtensions("module-d:ep-2").length);

        // Test that the two extension points declared in module-b are returned first
        // and appear in the order they are declared.
        Extension[] extensions = moduleRegistry.getExtensions("module-b:ep-2");
        assertEquals("e-bb21", extensions[0].getId());
        assertEquals("e-bb22", extensions[1].getId());


        assertSame(e_31, module_c.getExtension("e-cb11"));
        assertSame(e_32, module_c.getExtension("e-cb12"));
        assertSame(e_33, module_c.getExtension("e-cb21"));
        assertSame(e_34, module_c.getExtension("e-cb31"));
        assertSame(e_35, module_c.getExtension("e-cb32"));
        assertSame(e_36, module_c.getExtension("e-cb33"));

        final Extension[] module_5_extensions = module_e.getExtensions();
        assertEquals(7, module_5_extensions.length);
        assertSame(module_b.getExtensionPoint("ep-1"), module_5_extensions[0].getExtensionPoint());
        assertSame(module_b.getExtensionPoint("ep-2"), module_5_extensions[1].getExtensionPoint());
        assertSame(module_b.getExtensionPoint("ep-3"), module_5_extensions[2].getExtensionPoint());
        assertSame(module_b.getExtensionPoint("ep-3"), module_5_extensions[3].getExtensionPoint());
        assertSame(module_d.getExtensionPoint("ep-2"), module_5_extensions[4].getExtensionPoint());
        assertSame(module_d.getExtensionPoint("ep-2"), module_5_extensions[5].getExtensionPoint());
        // Test inherited extension point module-b:ep-3
        assertSame(module_b.getExtensionPoint("ep-3"), module_5_extensions[6].getExtensionPoint());

        final Extension[] module_2_extensions = moduleRegistry.getExtensions("module-b:ep-3");
        assertEquals(6, module_2_extensions.length);
        assertSame(module_c.getExtension("e-cb31"), module_2_extensions[0]);
        assertSame(module_c.getExtension("e-cb32"), module_2_extensions[1]);
        assertSame(module_c.getExtension("e-cb33"), module_2_extensions[2]);
        assertSame(module_e.getExtension("e-eb31"), module_2_extensions[3]);
        assertSame(module_e.getExtension("e-eb32"), module_2_extensions[4]);
        // Test inherited extension point module-b:ep-3
        assertSame(module_e.getExtension("e-ed33"), module_2_extensions[5]);
    }



    public void testCyclicModuleDependencies() throws CoreException, IOException {
        ModuleRegistry cyclicModuleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/cyclic/module-a.xml",
                "xml/cyclic/module-b.xml",
                "xml/cyclic/module-c.xml",
                "xml/cyclic/module-d.xml",
                "xml/cyclic/module-e.xml",
        });

        testForExpectedCyclicDependencies(cyclicModuleRegistry.getModules("module-a")[0]);
        testForExpectedCyclicDependencies(cyclicModuleRegistry.getModules("module-e")[0]);

    }

    public void testResolverIgnoresErrorInOptionalDependency() throws IOException, CoreException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/dependencies/module-i-optionally-wants-f.xml",
                "xml/dependencies/module-f-wants-nonexistent-module.xml",
        });
        ModuleImpl moduleI = moduleRegistry.getModules("module-i")[0];
        ModuleImpl moduleF = moduleRegistry.getModules("module-f")[0];

        assertEquals(ModuleState.INSTALLED, moduleI.getState());
        assertEquals(ModuleState.INSTALLED, moduleF.getState());

        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            moduleResolver.resolve(moduleI);
        } catch (ResolveException e) {
            fail("ResolveException not expected, dependency is optional");
        }
        assertEquals(ModuleState.RESOLVED, moduleI.getState());
        assertEquals(ModuleState.INSTALLED, moduleF.getState());
    }

    public void testResolverFindsBestMatchingDependencyVersion() throws IOException, CoreException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/dependencies/module-a-wants-b-ge-1.0.0.xml",
                "xml/dependencies/module-b-1.0.2.xml",
                "xml/dependencies/module-b-1.5-M3.xml",
                "xml/dependencies/module-b-2.3.5.xml",
                "xml/dependencies/module-c-wants-b-ge-2.3.xml",
                "xml/dependencies/module-d-wants-b-eq-1.5-M3.xml",
                "xml/dependencies/module-e-wants-b-latest.xml",
                "xml/dependencies/module-f-optionally-wants-nonexistent-module.xml",
                "xml/dependencies/module-g-wants-b-latest-optional.xml",
                "xml/dependencies/module-h-wants-b-1.0.2-optional.xml"
        });
        testSingleModuleDependency(moduleRegistry, "module-a", "module-b", "1.0.0", "1.0.2", 1);
        testSingleModuleDependency(moduleRegistry, "module-c", "module-b", "2.3", "2.3.5", 1);
        testSingleModuleDependency(moduleRegistry, "module-d", "module-b", "1.5-M3", "1.5-M3", 1);
        testSingleModuleDependency(moduleRegistry, "module-e", "module-b", null, "2.3.5", 1);
        testSingleModuleDependency(moduleRegistry, "module-f", "module-_", null, null, 0);
        testSingleModuleDependency(moduleRegistry, "module-g", "module-b", null, "2.3.5", 1);
        testSingleModuleDependency(moduleRegistry, "module-h", "module-b", "1.0.2", "1.0.2", 1);
    }

    private static void testSingleModuleDependency(ModuleRegistry moduleRegistry, String declaringModuleName,
                                                   String declaredModuleName, String declaredVersion,
                                                   String actualVersion, int moduleDependyCount) throws
                                                                                                 CoreException {
        ModuleImpl moduleA = moduleRegistry.getModules(declaringModuleName)[0];
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
        moduleResolver.resolve(moduleA);
        assertEquals(declaredModuleName, moduleA.getDeclaredDependencies()[0].getModuleSymbolicName());
        assertEquals(declaredVersion, moduleA.getDeclaredDependencies()[0].getVersion());
        ModuleImpl[] dependencies = moduleA.getModuleDependencies();
        assertEquals(moduleDependyCount, dependencies.length);
        if (moduleDependyCount == 1) {
            assertEquals(declaredModuleName, dependencies[0].getSymbolicName());
            assertEquals(Version.parseVersion(actualVersion), dependencies[0].getVersion());
        }
    }

    private static void testForExpectedCyclicDependencies(ModuleImpl module) {
        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(),
                                                                     false);
            moduleResolver.resolve(module);
            fail("ResolveException expected because of cyclic dependencies");
        } catch (ResolveException e) {
        }
    }


    private ModuleImpl getModule(String symbolicName) {
        return moduleRegistry.getModules(symbolicName)[0];
    }

}
