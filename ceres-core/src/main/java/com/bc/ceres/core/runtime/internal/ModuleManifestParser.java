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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Version;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A reader for module XML files.
 */
public class ModuleManifestParser {
    private static final Pattern REPLACE_WHITESPACE_PATTERN = Pattern.compile("\\s{2,}");

    public ModuleManifestParser() {
    }

    public ModuleImpl parse(String xml) throws CoreException {
        Assert.notNull(xml, "xml");
        try {
            ModuleImpl module = (ModuleImpl) createXStream().fromXML(xml);
            postProcessModule(module);
            return module;
        } catch (XStreamException e) {
            throw toCoreException(e);
        }
    }

    public ModuleImpl parse(InputStream stream) throws CoreException {
        Assert.notNull(stream, "stream");
        try {
            ModuleImpl module = (ModuleImpl) createXStream().fromXML(stream);
            postProcessModule(module);
            return module;
        } catch (XStreamException e) {
            throw toCoreException(e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public ModuleImpl parse(Reader reader) throws CoreException {
        Assert.notNull(reader, "reader");
        try {
            ModuleImpl module = (ModuleImpl) createXStream().fromXML(reader);
            postProcessModule(module);
            return module;
        } catch (XStreamException e) {
            throw toCoreException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void postProcessModule(ModuleImpl module) throws CoreException {
        trimStringFields(module);

        module.setModuleId(-1);

        if (module.getManifestVersion() == null) {
            throw new CoreException("Missing manifest version");
        }
        if (module.getSymbolicName() == null) {
            throw new CoreException("Missing module identifier");
        }
        if (module.getSymbolicName().length() == 0) {
            throw new CoreException("Empty module identifier");
        }
        if (module.getVersion() == null) {
            module.setVersion(Version.parseVersion("1.0"));
        }
        if (module.getCategoriesString() != null) {
            module.setCategories(toArray(module.getCategoriesString()));
        } else {
            module.setCategories(new String[0]);
        }

        if (module.getPackaging() == null) {
            module.setPackaging("jar");
        }
        if (module.getActivatorClassName() == null) {
            module.setActivatorClassName(DefaultActivator.class.getName());
        }
        module.setState(ModuleState.NULL);
        module.initDeclaredComponents();
    }

    private static void trimStringFields(ModuleImpl module) {
        Field[] declaredFields = ModuleImpl.class.getDeclaredFields();
        List<Field> stringFields = new ArrayList<Field>();
        for (Field field : declaredFields) {
            if (field.getType().equals(String.class)) {
                stringFields.add(field);
            }
        }

        Field.setAccessible(stringFields.toArray(new Field[stringFields.size()]), true);
        try {
            for (Field field : stringFields) {
                try {
                    String value = (String) field.get(module);
                    if (value != null) {
                        field.set(module, trim(value));
                    }
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        } finally {
            Field.setAccessible(stringFields.toArray(new Field[stringFields.size()]), false);
        }
    }

    /*
     * Replace all sequenced whitespace characters with a single whitespace character.
     */
    private static String trim(String value) {
        return REPLACE_WHITESPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
    }


    private static XStream createXStream() {
        XStream xstream = new XStream();

        // Module
        xstream.alias("module", ModuleImpl.class);
        xstream.aliasField("activator", ModuleImpl.class, "activatorClassName");
        xstream.aliasField("dependencies", ModuleImpl.class, "declaredDependencies");
        xstream.aliasField("categories", ModuleImpl.class, "categoriesString");
        xstream.aliasField("native", ModuleImpl.class, "usingJni");
        xstream.omitField(ModuleImpl.class, "state");
        xstream.omitField(ModuleImpl.class, "activator");
        xstream.omitField(ModuleImpl.class, "registry");
        xstream.omitField(ModuleImpl.class, "location");
        xstream.omitField(ModuleImpl.class, "context");
        xstream.omitField(ModuleImpl.class, "privateClasspath");
        xstream.omitField(ModuleImpl.class, "declaredLibs");
        xstream.omitField(ModuleImpl.class, "impliciteLibs");
        xstream.omitField(ModuleImpl.class, "impliciteNativeLibs");
        xstream.registerConverter(new VersionConverter());

        // Dependency
        xstream.alias("dependency", DependencyImpl.class);
        xstream.aliasField("lib", DependencyImpl.class, "libName");
        xstream.aliasField("module", DependencyImpl.class, "moduleSymbolicName");
        xstream.omitField(DependencyImpl.class, "declaringModule");

        // ExtensionPoint
        xstream.alias("extensionPoint", ExtensionPointImpl.class);
        xstream.registerConverter(new ExtensionPointConverter());

        // Extension
        xstream.alias("extension", ExtensionImpl.class);
        xstream.registerConverter(new ExtensionConverter());

        xstream.useAttributeFor("id", String.class);
        xstream.useAttributeFor("point", String.class);
        xstream.addImplicitCollection(ModuleImpl.class, "declaredDependencies", "dependency", DependencyImpl.class);
        xstream.addImplicitCollection(ModuleImpl.class, "extensions", "extension", ExtensionImpl.class);
        xstream.addImplicitCollection(ModuleImpl.class, "extensionPoints", "extensionPoint", ExtensionPointImpl.class);

        return xstream;
    }

    private static XppDom readDom(HierarchicalStreamReader source) {
        XppDomWriter destination = new XppDomWriter();
        new HierarchicalStreamCopier().copy(source, destination);
        return destination.getConfiguration();
    }

    private static void writeDom(XppDom dom, HierarchicalStreamWriter destination) {
        XppDom[] children = dom.getChildren();
        for (XppDom child : children) {
            new HierarchicalStreamCopier().copy(new XppDomReader(child), destination);
        }
    }

    private static String[] toArray(String csvString) {
        StringTokenizer stringTokenizer = new StringTokenizer(csvString, ",", false);
        ArrayList<String> stringList = new ArrayList<String>(8);
        while (stringTokenizer.hasMoreElements()) {
            String stringElement = stringTokenizer.nextElement().toString().trim();
            if (stringElement.length() > 0) {
                stringList.add(stringElement);
            }
        }
        return stringList.toArray(new String[stringList.size()]);
    }

    private CoreException toCoreException(XStreamException e) {
        return new CoreException("Failed to parse module manifest: " + e.getMessage(), e);
    }

    private static class ExtensionConverter implements Converter {

        public boolean canConvert(Class aClass) {
            return aClass.equals(ExtensionImpl.class);
        }

        public void marshal(Object object, HierarchicalStreamWriter destination,
                            MarshallingContext marshallingContext) {
            ExtensionImpl extension = (ExtensionImpl) object;
            destination.addAttribute("id", extension.getId());
            destination.addAttribute("point", extension.getPoint());
            ConfigurationElementImpl configurationElementImpl = (ConfigurationElementImpl) extension.getConfigurationElement();
            writeDom(configurationElementImpl.getDom(), destination);
        }

        public Object unmarshal(HierarchicalStreamReader source, UnmarshallingContext unmarshallingContext) {
            String id = source.getAttribute("id");
            String point = source.getAttribute("point");
            if (point == null) {
                throw new ConversionException(
                        MessageFormat.format("element [{0}]: missing attribute [point]", source.getNodeName()));
            }
            XppDom dom = readDom(source);
            return new ExtensionImpl(point, new ConfigurationElementImpl(null, dom), id);
        }
    }

    private static class VersionConverter implements SingleValueConverter {

        public boolean canConvert(Class aClass) {
            return Version.class.equals(aClass);
        }

        public Object fromString(String string) {
            if (string.length() == 0) {
                throw new ConversionException("empty version string");
            }
            if (!Character.isDigit(string.charAt(0))) {
                throw new ConversionException("invalid version string");
            }
            return Version.parseVersion(string);
        }

        public String toString(Object object) {
            return object.toString();
        }
    }

    private static class ExtensionPointConverter implements Converter {

        public boolean canConvert(Class aClass) {
            return aClass.equals(ExtensionPointImpl.class);
        }

        public void marshal(Object object, HierarchicalStreamWriter destination,
                            MarshallingContext marshallingContext) {
            ExtensionPointImpl extensionPoint = (ExtensionPointImpl) object;
            destination.addAttribute("id", extensionPoint.getId());
            ConfigurationSchemaElementImpl configurationElementImpl = (ConfigurationSchemaElementImpl) extensionPoint.getConfigurationSchemaElement();
            writeDom(configurationElementImpl.getDom(), destination);
        }

        public Object unmarshal(HierarchicalStreamReader source, UnmarshallingContext unmarshallingContext) {
            String id = source.getAttribute("id");
            if (id == null) {
                throw new ConversionException(
                        MessageFormat.format("element [{0}]: missing attribute [id]", source.getNodeName()));
            }
            XppDom dom = readDom(source);
            return new ExtensionPointImpl(id, new ConfigurationSchemaElementImpl(null, dom));
        }
    }
}
