package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Version;
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
