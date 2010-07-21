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
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ModuleState;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import junit.framework.TestCase;

import java.io.IOException;

public class ConfigurationElementImplTest extends TestCase {

    public void testThatX() throws CoreException, IOException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/module-thing-declarer.xml",
                "xml/module-thing-provider.xml"
        });

        ModuleImpl[] modules = moduleRegistry.getModules();
        assertEquals(2, modules.length);
        for (ModuleImpl module : modules) {
            final ModuleResolver moduleResolver = new ModuleResolver(ConfigurationElementImplTest.class.getClassLoader(), false);
            moduleResolver.resolve(module);
        }

        ModuleImpl declarerModule = modules[0];
        ModuleImpl providerModule = modules[1];
        assertEquals(ModuleState.RESOLVED, declarerModule.getState());
        assertEquals(ModuleState.RESOLVED, providerModule.getState());

        Extension[] extensions = providerModule.getExtensions();
        assertEquals(1, extensions.length);

        Extension thingsExtension = extensions[0];
        assertNotNull(thingsExtension);

        assertEquals("things", thingsExtension.getExtensionPoint().getId());
        assertEquals("module-thing-declarer:things", thingsExtension.getExtensionPoint().getQualifiedId());

        ConfigurationElement[] configurationElements = thingsExtension.getExtensionPoint().getConfigurationElements();
        assertEquals(1, configurationElements.length);

        ConfigurationElement configurationElement = configurationElements[0];
        assertSame(thingsExtension, configurationElement.getDeclaringExtension());

        ConfigurationElement[] children = configurationElement.getChildren("thing");
        assertEquals(5, children.length);

        ConfigurationElement simplestThingElement = children[0];
        assertSame(thingsExtension, simplestThingElement.getDeclaringExtension());
        Thing simplestThing = simplestThingElement.createExecutableExtension(Thing.class);
        assertTrue(simplestThing instanceof DefaultThing);
        assertEquals(null, ((DefaultThing) simplestThing).value1);
        assertEquals(null, ((DefaultThing) simplestThing).value2);

        ConfigurationElement simpleThingElement = children[1];
        assertSame(thingsExtension, simpleThingElement.getDeclaringExtension());
        Thing simpleThing = simpleThingElement.createExecutableExtension(Thing.class);
        assertTrue(simpleThing instanceof DefaultThing);
        assertEquals("A", ((DefaultThing) simpleThing).value1);
        assertEquals("B", ((DefaultThing) simpleThing).value2);

        ConfigurationElement overspecThingElement = children[2];
        assertSame(thingsExtension, overspecThingElement.getDeclaringExtension());
        Thing overspecThing = overspecThingElement.createExecutableExtension(Thing.class);
        assertTrue(overspecThing instanceof DefaultThing);
        assertEquals("C", ((DefaultThing) overspecThing).value1);
        assertEquals("D", ((DefaultThing) overspecThing).value2);

        ConfigurationElement extraThingElement = children[3];
        assertSame(thingsExtension, extraThingElement.getDeclaringExtension());
        Thing extraThing = extraThingElement.createExecutableExtension(Thing.class);
        assertTrue(extraThing instanceof ExtraThing);
        assertEquals("A", ((ExtraThing) extraThing).extraValue1);
        assertEquals("B", ((ExtraThing) extraThing).extraValue2);

        ConfigurationElement executableThingElement = children[4];
        assertSame(thingsExtension, executableThingElement.getDeclaringExtension());
        Thing executableThing = executableThingElement.createExecutableExtension(Thing.class);
        assertTrue(executableThing instanceof ConfigurableThing);
        assertEquals("init();", ((ConfigurableThing) executableThing).initCalls.toString());
    }


    public interface Thing {

    }

    public static class DefaultThing implements Thing {

        @XStreamAlias("V1")
        String value1;
        @XStreamAlias("V2")
        String value2;
    }

    public static class ExtraThing implements Thing {

        @XStreamAlias("EV1")
        String extraValue1;

        @XStreamAlias("EV2")
        String extraValue2;

        transient String transientField = "A transient value (not set by XStream)";
        String nonTransientField = "Another value";
    }

    public static class ConfigurableThing implements Thing, ConfigurableExtension {

        StringBuilder initCalls = new StringBuilder();

        public void configure(ConfigurationElement config) throws CoreException {
            initCalls.append("init();");
        }
    }
}
