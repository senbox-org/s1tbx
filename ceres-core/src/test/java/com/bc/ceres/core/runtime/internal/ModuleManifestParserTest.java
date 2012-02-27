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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Unit test for simple ModuleReader.
 */
public class ModuleManifestParserTest
        extends TestCase {

    private static class Foo {

        String name;
        int age;
    }

    public void testXStreamUnmarshal() {
        XppDom fooElem = new XppDom(Foo.class.getName());
        XppDom nameElem = new XppDom("name");
        nameElem.setValue("Bibo");
        fooElem.addChild(nameElem);
        XppDom ageElem = new XppDom("age");
        ageElem.setValue("41");
        fooElem.addChild(ageElem);

        XppDomReader domReader = new XppDomReader(fooElem);
        Foo foo = new Foo();
        new XStream().unmarshal(domReader, foo);

        assertEquals("Bibo", foo.name);
        assertEquals(41, foo.age);
    }

    public void testXStreamMarshal() {
        Foo foo = new Foo();
        foo.name = "Bert";
        foo.age = 63;

        XppDomWriter domWriter = new XppDomWriter();
        new XStream().marshal(foo, domWriter);

        XppDom fooElem = domWriter.getConfiguration();
        assertNotNull(fooElem);
        assertNotNull(fooElem.getChild("name"));
        assertNotNull(fooElem.getChild("age"));
        assertEquals("Bert", fooElem.getChild("name").getValue());
        assertEquals("63", fooElem.getChild("age").getValue());
    }

    public void testNullArgConvention() throws CoreException {
        try {
            new ModuleManifestParser().parse((String) null);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new ModuleManifestParser().parse((InputStream) null);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new ModuleManifestParser().parse((Reader) null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testAllParseMethodsProduceSameResults() throws CoreException {
        Module ma = new ModuleManifestParser().parse(Resources.loadText("xml/module-a.xml"));
        Module mb = new ModuleManifestParser().parse(Resources.openStream("xml/module-a.xml"));
        Module mc = new ModuleManifestParser().parse(Resources.openReader("xml/module-a.xml"));
        assertEquals(mb.getSymbolicName(), ma.getSymbolicName());
        assertEquals(mc.getSymbolicName(), mb.getSymbolicName());
    }

    public void testModuleA() throws IOException, CoreException {
        ModuleImpl module_a = TestHelpers.parseModuleManifest("xml/module-a.xml");

        testModule(module_a,
                   "module-a", "1.0",
                   "jar", "Module A", "This is Module A",
                   DefaultActivator.class.getName(),
                   2, 0, 0);

        testDependency(module_a, 0, null, "xstream", null);
        testDependency(module_a, 1, null, "xpp3", null);

        String[] categories = module_a.getCategories();
        assertNotNull(categories);
        assertEquals(5, categories.length);
        assertEquals("CHRIS", categories[0]);
        assertEquals("MERIS", categories[1]);
        assertEquals("Processor", categories[2]);
        assertEquals("Reader", categories[3]);
        assertEquals("Writer", categories[4]);
    }


    public void testModuleB() throws IOException, CoreException {
        ModuleImpl module_b = TestHelpers.parseModuleManifest("xml/module-b.xml");

        testModule(module_b,
                   "module-b", "2.4.2-SNAPSHOT",
                   "jar", "Module B", "This is Module B",
                   "com.bc.modules.ModuleBActivator",
                   1, 3, 2);

        testDependency(module_b, 0, "module-a", null, null);

        testExtensionPoint(module_b.getExtensionPoints()[0], module_b, "ep-1");
        testExtensionPoint(module_b.getExtensionPoints()[1], module_b, "ep-2");
        testExtensionPoint(module_b.getExtensionPoints()[2], module_b, "ep-3");

        String[] categories = module_b.getCategories();
        assertNotNull(categories);
        assertEquals(0, categories.length);
    }

    public void testModuleC() throws IOException, CoreException {
        ModuleImpl module_c = TestHelpers.parseModuleManifest("xml/module-c.xml");

        testModule(module_c,
                   "module-c", "1.0",
                   "jar", null, "This is Module C",
                   DefaultActivator.class.getName(),
                   1, 0, 6);

        testDependency(module_c, 0, "module-b", null, null);
        testExtension(module_c.getExtensions()[0], module_c, "e-cb11", "module-b:ep-1");
        testExtension(module_c.getExtensions()[1], module_c, "e-cb12", "module-b:ep-1");
        testExtension(module_c.getExtensions()[2], module_c, "e-cb21", "module-b:ep-2");
        testExtension(module_c.getExtensions()[3], module_c, "e-cb31", "module-b:ep-3");
        testExtension(module_c.getExtensions()[4], module_c, "e-cb32", "module-b:ep-3");
        testExtension(module_c.getExtensions()[5], module_c, "e-cb33", "module-b:ep-3");

        Extension extension_32 = module_c.getExtensions()[1];
        assertNotNull(extension_32.getConfigurationElement());
        assertEquals("extension", extension_32.getConfigurationElement().getName());
        assertNotNull(extension_32.getConfigurationElement().getChildren());
        assertEquals(0, extension_32.getConfigurationElement().getChildren().length);

        Extension extension_31 = module_c.getExtensions()[0];
        ConfigurationElement configurationElement31 = extension_31.getConfigurationElement();
        assertNotNull(configurationElement31);
        assertEquals("extension", configurationElement31.getName());
        assertNull(configurationElement31.getParent());
        ConfigurationElement[] children = configurationElement31.getChildren();
        assertEquals(2, children.length);
        assertEquals("source", children[0].getName());
        assertEquals("A", children[0].getValue());
        assertSame(configurationElement31, children[0].getParent());
        assertEquals("target", children[1].getName());
        assertEquals("B", children[1].getValue());
        assertSame(configurationElement31, children[1].getParent());
    }

    public void testModuleD() throws IOException, CoreException {
        ModuleImpl module_d = TestHelpers.parseModuleManifest("xml/module-d.xml");

        testModule(module_d,
                   "module-d", "3",
                   "jar", null, "This is Module D",
                   "com.bc.modules.ModuleDActivator",
                   1, 2, 0);

        testDependency(module_d, 0, "module-a", null, null);
        testExtensionPoint(module_d.getExtensionPoints()[0], module_d, "ep-1");
        testExtensionPoint(module_d.getExtensionPoints()[1], module_d, "ep-2");

    }

    public void testModuleE() throws IOException, CoreException {
        ModuleImpl module_e = TestHelpers.parseModuleManifest("xml/module-e.xml");

        testModule(module_e,
                   "module-e", "1.0",
                   "jar", null, "This is Module E",
                   DefaultActivator.class.getName(),
                   3, 0, 7);

        testDependency(module_e, 0, "module-b", null, "2.4.2-SNAPSHOT");
        testDependency(module_e, 1, "module-d", null, "3");
        testDependency(module_e, 2, null, "netcdf", "1.2.4");

        testExtension(module_e.getExtensions()[0], module_e, "e-eb11", "module-b:ep-1");
        testExtension(module_e.getExtensions()[1], module_e, "e-eb21", "module-b:ep-2");
        testExtension(module_e.getExtensions()[2], module_e, "e-eb31", "module-b:ep-3");
        testExtension(module_e.getExtensions()[3], module_e, "e-eb32", "module-b:ep-3");
        testExtension(module_e.getExtensions()[4], module_e, "e-ed21", "module-d:ep-2");
        testExtension(module_e.getExtensions()[5], module_e, "e-ed22", "module-d:ep-2");
        testExtension(module_e.getExtensions()[6], module_e, "e-ed33", "module-d:ep-3");
    }

    public void testModuleMinimum() throws IOException, CoreException {
        ModuleImpl module_min = TestHelpers.parseModuleManifest("xml/module-minimum.xml");

        assertNotNull(module_min);
        assertNull(module_min.getRegistry());

        testModule(module_min,
                   "minimal-module", "1.0",
                   "jar", null, null,
                   DefaultActivator.class.getName(),
                   0, 0, 0);
    }

    public void testModuleDetails() throws IOException, CoreException {
        ModuleImpl module_details = TestHelpers.parseModuleManifest("xml/module-details.xml");

        assertNotNull(module_details);
        testModule(module_details, "module-details", "9.9.9", "jar", "Module Details",
                   "Lorem ipsum ex sint omnes intellegebat vis, mucius nostrum usu id. No probo probatus qui, has ceteros " +
                           "nostrum dissentias an, affert torquatos vim ut. In eos nulla quaerendum, est labore appareat ea. Admodum " +
                           "assueverit constituam ut vis, oratio fabulas nostrum te vel. Id eam utamur deleniti consulatu. Quo utinam graeco " +
                           "consetetur at. Nam nullam nominati interpretaris ei.",
                   DefaultActivator.class.getName(), 0, 0, 0);

        assertEquals("ACME Inc.", module_details.getVendor());
    }


    public void testModuleNoManifestVersion() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-no-manifest-version.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertTrue(e.getMessage().startsWith("Missing manifest version"));
        }
    }

    public void testModuleNoId() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-no-id.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertTrue(e.getMessage().startsWith("Missing module identifier"));
        }
    }

    public void testModuleWithLinebreakInElement() throws IOException, CoreException {
        ModuleImpl module = TestHelpers.parseModuleManifest("xml/module-element-linebreak.xml");

        assertNotNull(module);
        assertEquals("element-linebreak-module", module.getSymbolicName());

        assertEquals("Ju faka fojo pasko cia, obl subjunkcio solstariva supersigno er, onia kontra?a cis fi. Tek he metr kien. Mis co " +
                             "havi alta negativa. End nf pero ioma. De meze tuje tempolongo des. " +
                             "Sola seksa geinstruisto sur fi, mf vole estiel eksterna sia. Ato semajntago substantiva ed, ge volu kvanto " +
                             "anta?parto ili, pov em super jugoslavo. Ne cent intera vir, nea video matematika rolvorta?o am. Eksa sekvanta " +
                             "deksesuma sep ej, kv nen amen lingvonomo, eg jen stif kelka rekta. Je tuja samo aha, ko hoj pako kasedo.", module.getDescription());
    }

    public void testModuleEmptyId() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-empty-id.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertTrue(e.getMessage().startsWith("Empty module identifier"));
        }
    }

    public void testModuleEmptyVersion() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-empty-version.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertTrue(e.getMessage().contains("version"));
        }
    }

    public void testModuleInvalidVersion() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-invalid-version.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertTrue(e.getMessage().contains("version"));
        }
    }

    public void testEmptyFile() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-empty.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertNotNull(e.getCause());
        }
    }

    public void testModuleNoXml() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-no-xml.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertNotNull(e.getCause());
        }
    }

    public void testModuleMalformedXml() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-malformed-xml.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertNotNull(e.getCause());
        }
    }

    public void testModuleUnknownElement() throws IOException {
        try {
            TestHelpers.parseModuleManifest("xml/module-unknown-element.xml");
            fail("error expected");
        } catch (CoreException e) {
            assertNotNull(e.getCause());
        }
    }

    private static void testModule(ModuleImpl module, String symbolicName, String version,
                                   String packaging, String name, String description, String activatorClassName,
                                   int declaredDependencyCount, int extensionPointCount, int extensionCount) {
        assertNotNull(module);
        assertEquals(-1, module.getModuleId());
        assertEquals(symbolicName, module.getSymbolicName());
        assertEquals(Version.parseVersion(version), module.getVersion());
        assertEquals(packaging, module.getPackaging());
        assertEquals(name, module.getName());
        assertEquals(description, module.getDescription());
        assertEquals(activatorClassName, module.getActivatorClassName());
        assertNotNull(module.getDeclaredDependencies());
        assertEquals(declaredDependencyCount, module.getDeclaredDependencies().length);
        assertNotNull(module.getExtensionPoints());
        assertEquals(extensionPointCount, module.getExtensionPoints().length);
        assertNotNull(module.getExtensions());
        assertEquals(extensionCount, module.getExtensions().length);

        assertEquals(null, module.getActivator());
        assertEquals(null, module.getRegistry());
        assertEquals(null, module.getContext());
        assertEquals(ModuleState.NULL, module.getState());

        assertNull(module.getImpliciteLibs());
        assertNull(module.getImpliciteNativeLibs());
        assertNull(module.getDeclaredLibs());
        assertNull(module.getModuleDependencies());
        assertNull(module.getClassLoader());
        assertNull(module.getLocation());
        assertNull(module.getContext());
        assertNull(module.getActivator());
    }

    private static void testDependency(Module module, int index, String moduleId, String jarName, String version) {
        Dependency dependency = module.getDeclaredDependencies()[index];
        assertNotNull(dependency);
        assertSame(module, dependency.getDeclaringModule());
        assertEquals(moduleId, dependency.getModuleSymbolicName());
        assertEquals(jarName, dependency.getLibName());
        assertEquals(version, dependency.getVersion());
    }

    private static void testExtensionPoint(ExtensionPoint extensionPoint, Module declaringModule, String id) {
        assertNotNull(extensionPoint);
        assertSame(declaringModule, extensionPoint.getDeclaringModule());
        assertNull(extensionPoint.getExtensions());
        assertEquals(id, extensionPoint.getId());
    }

    private static void testExtension(Extension extension, Module declaringModule, String id, String point) {
        assertNotNull(extension);
        assertSame(declaringModule, extension.getDeclaringModule());
        assertNull(extension.getExtensionPoint());
        assertEquals(id, extension.getId());
        assertEquals(point, extension.getPoint());
    }
}
