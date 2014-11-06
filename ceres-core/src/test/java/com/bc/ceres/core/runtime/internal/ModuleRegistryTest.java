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
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import junit.framework.TestCase;

import java.io.IOException;

public class ModuleRegistryTest extends TestCase {

    public void testNullArgConvention() throws CoreException {
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        try {
            moduleRegistry.registerModule(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testNewModuleRegistryIsEmpty() {
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        assertNotNull(moduleRegistry.getModules());
        assertEquals(0, moduleRegistry.getModules().length);
    }

    public void testRegisteringDuplicates() throws CoreException, IOException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/module-a.xml",
                "xml/module-b.xml",
                "xml/module-c.xml",
                "xml/module-c-v2.xml",
                "xml/module-c-v3.xml",
        });
        ModuleImpl moduleAClone = TestHelpers.parseModuleManifestWithDummyLocation("xml/module-a.xml");
        ModuleImpl moduleBClone = TestHelpers.parseModuleManifestWithDummyLocation("xml/module-b.xml");
        ModuleImpl moduleCClone = TestHelpers.parseModuleManifestWithDummyLocation("xml/module-c.xml");

        ModuleImpl moduleA = moduleRegistry.getModule(1);
        ModuleImpl moduleB = moduleRegistry.getModule(2);
        ModuleImpl moduleC = moduleRegistry.getModule(3);
        ModuleImpl moduleCv2 = moduleRegistry.getModule(4);
        ModuleImpl moduleCv3 = moduleRegistry.getModule(5);

        assertNotNull(moduleA);
        assertSame(moduleA, moduleRegistry.getModule(moduleA.getModuleId()));
        assertSame(moduleA, moduleRegistry.getModule(moduleA.getLocation()));
        assertEquals(new ModuleImpl[]{moduleA}, moduleRegistry.getModules(moduleA.getSymbolicName()));


        assertNotNull(moduleB);
        assertSame(moduleB, moduleRegistry.getModule(moduleB.getModuleId()));
        assertSame(moduleB, moduleRegistry.getModule(moduleB.getLocation()));
        assertEquals(new ModuleImpl[]{moduleB}, moduleRegistry.getModules(moduleB.getSymbolicName()));

        assertSame(moduleC, moduleRegistry.getModule(moduleC.getModuleId()));
        assertSame(moduleC, moduleRegistry.getModule(moduleC.getLocation()));
        assertSame(moduleCv2, moduleRegistry.getModule(moduleCv2.getModuleId()));
        assertSame(moduleCv2, moduleRegistry.getModule(moduleCv2.getLocation()));
        assertSame(moduleCv3, moduleRegistry.getModule(moduleCv3.getModuleId()));
        assertSame(moduleCv3, moduleRegistry.getModule(moduleCv3.getLocation()));
        assertEquals(new ModuleImpl[]{moduleC, moduleCv2, moduleCv3},
                     moduleRegistry.getModules(moduleC.getSymbolicName()));
        assertEquals(new ModuleImpl[]{moduleC, moduleCv2, moduleCv3},
                     moduleRegistry.getModules(moduleCv2.getSymbolicName()));
        assertEquals(new ModuleImpl[]{moduleC, moduleCv2, moduleCv3},
                     moduleRegistry.getModules(moduleCv3.getSymbolicName()));

        try {
            moduleRegistry.registerModule(moduleAClone);
            fail("CoreException expected");
        } catch (CoreException e) {
            // expected
        }

        try {
            moduleRegistry.registerModule(moduleBClone);
            fail("CoreException expected");
        } catch (CoreException e) {
            // expected
        }

        try {
            moduleRegistry.registerModule(moduleCClone);
            fail("CoreException expected");
        } catch (CoreException e) {
            // expected
        }
    }

    private void assertEquals(ModuleImpl[] expected, ModuleImpl[] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].getSymbolicName() + "?", expected[i], actual[i]);
        }
    }

    public void testGetModulesReturnsArrayCopies() throws CoreException, IOException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/module-a.xml",
                "xml/module-b.xml",
        });

        Module[] modulesFromCall1 = moduleRegistry.getModules();
        assertNotNull(modulesFromCall1);
        assertEquals(2, modulesFromCall1.length);

        Module[] modulesFromCall2 = moduleRegistry.getModules();
        assertNotNull(modulesFromCall2);
        assertEquals(modulesFromCall1.length, modulesFromCall2.length);

        assertNotSame(modulesFromCall1, modulesFromCall2);

        assertEquals(modulesFromCall1[0].getSymbolicName(), modulesFromCall2[0].getSymbolicName());
        assertEquals(modulesFromCall1[1].getSymbolicName(), modulesFromCall2[1].getSymbolicName());

        modulesFromCall1[0] = null;
        modulesFromCall1[1] = null;
        modulesFromCall1 = moduleRegistry.getModules();
        assertNotNull(modulesFromCall1[0]);
        assertNotNull(modulesFromCall1[1]);
    }

    public void testRegisterAndGetModule() throws CoreException, IOException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                /*id=1*/ "xml/module-a.xml",
                /*id=2*/ "xml/module-b.xml",
                /*id=3*/ "xml/module-c.xml",
                /*id=4*/ "xml/module-d.xml",
                /*id=5*/ "xml/module-e.xml",
        });

        Module module_1 = moduleRegistry.getModule(1);
        Module module_2 = moduleRegistry.getModule(2);
        Module module_3 = moduleRegistry.getModule(3);
        Module module_4 = moduleRegistry.getModule(4);
        Module module_5 = moduleRegistry.getModule(5);

        assertNotNull(module_1);
        assertNotNull(module_2);
        assertNotNull(module_3);
        assertNotNull(module_4);
        assertNotNull(module_5);

        Extension e_31 = module_3.getExtensions()[0];
        Extension e_32 = module_3.getExtensions()[1];
        Extension e_33 = module_3.getExtensions()[2];
        Extension e_34 = module_3.getExtensions()[3];
        Extension e_35 = module_3.getExtensions()[4];
        Extension e_36 = module_3.getExtensions()[5];

        assertEquals(3, module_2.getExtensionPoints().length);
        ExtensionPoint ep_21 = module_2.getExtensionPoints()[0];
        ExtensionPoint ep_22 = module_2.getExtensionPoints()[1];
        ExtensionPoint ep_23 = module_2.getExtensionPoints()[2];

        assertEquals(2, module_4.getExtensionPoints().length);
        ExtensionPoint ep_41 = module_4.getExtensionPoints()[0];
        ExtensionPoint ep_42 = module_4.getExtensionPoints()[1];

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
        assertEquals(5, ep_23.getExtensions().length);
        assertEquals(0, ep_41.getExtensions().length);
        assertEquals(2, ep_42.getExtensions().length);

        assertEquals(3, moduleRegistry.getExtensions("module-b:ep-1").length);
        assertEquals(4, moduleRegistry.getExtensions("module-b:ep-2").length);
        assertEquals(5, moduleRegistry.getExtensions("module-b:ep-3").length);
        assertEquals(0, moduleRegistry.getExtensions("module-d:ep-1").length);
        assertEquals(2, moduleRegistry.getExtensions("module-d:ep-2").length);

        // Test that the two extension points declared in module-b are returned first
        // and appear in the order they are declared.
        Extension[] extensions = moduleRegistry.getExtensions("module-b:ep-2");
        assertEquals("e-bb21", extensions[0].getId());
        assertEquals("e-bb22", extensions[1].getId());


        assertSame(e_31, module_3.getExtension("e-cb11"));
        assertSame(e_32, module_3.getExtension("e-cb12"));
        assertSame(e_33, module_3.getExtension("e-cb21"));
        assertSame(e_34, module_3.getExtension("e-cb31"));
        assertSame(e_35, module_3.getExtension("e-cb32"));
        assertSame(e_36, module_3.getExtension("e-cb33"));
    }

}
