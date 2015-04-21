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
import com.bc.ceres.core.runtime.ConfigurationSchemaElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.Module;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.JavaClassConverter;
import com.thoughtworks.xstream.core.util.ClassLoaderReference;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.text.MessageFormat;

/**
 * A configuration element, with its attributes and children,
 * directly reflects the content and structure of the extension
 * section within the declaring plug-in's manifest (plugin.xml) file.
 * <p>This interface also provides a way to create executable extension objects.
 * This interface is not intended to be implemented by clients.
 */
public class ConfigurationElementImpl extends ConfigurationElementBaseImpl<ConfigurationElement>
        implements ConfigurationElement {

    private ExtensionImpl declaringExtension;
    private ConfigurationSchemaElementImpl schemaElement;

    public ConfigurationElementImpl(ConfigurationElementImpl parent, XppDom dom) {
        super(parent, dom);
    }

    @Override
    public ConfigurationSchemaElement getSchemaElement() {
        return schemaElement;
    }

    @Override
    public Extension getDeclaringExtension() {
        return declaringExtension;
    }

    @Override
    public <T> T createExecutableExtension(Class<T> extensionType) throws CoreException {

        ////////////////////////////////////////////////////////////
        // Compute extensionDefaultClass

        String extensionClassElementName = null;
        String extensionClassAttributeName = null;
        Class<T> extensionDefaultClass = null;
        if (schemaElement != null) {
            String typeAttributeValue = schemaElement.getAttribute("type");
            checkExtensionType(extensionType, typeAttributeValue);

            String classAttributeValue = schemaElement.getAttribute("class");
            String extensionDefaultClassName = classAttributeValue;
            if (classAttributeValue != null) {
                if (classAttributeValue.startsWith("@")) {
                    // '@' is used to bind class name to an element, whose value is the actual class name value
                    extensionClassElementName = classAttributeValue.substring(1);
                    ConfigurationSchemaElement extensionDefaultClassNameElement = schemaElement.getChild(
                            extensionClassElementName);
                    if (extensionDefaultClassNameElement != null) {
                        extensionDefaultClassName = extensionDefaultClassNameElement.getValue();
                    }
                } else if (classAttributeValue.startsWith("#")) {
                    // '#' is used to bind class name to an attribute, whose value is the actual class name value
                    extensionClassAttributeName = classAttributeValue.substring(1);
                }
            }
            if (extensionDefaultClassName != null) {
                extensionDefaultClassName = extensionDefaultClassName.trim();
                extensionDefaultClass = loadClass(extensionDefaultClassName, extensionType);
            }
        }

        Class<T> extensionClass = getExtensionClass(extensionType,
                                                    extensionDefaultClass,
                                                    extensionClassAttributeName,
                                                    extensionClassElementName);

        T instance = createInstance(extensionClass);

        XStream xStream = getXStream(extensionType, extensionClass, extensionDefaultClass);
        if (xStream != null) {
            try {
                xStream.unmarshal(new XppDomReader(getDom()), instance);
            } catch (Throwable e) {
                throw new CoreException(
                        MessageFormat.format(
                                "Module [{0}]: Failed to unmarshal executable extension [{1}]: {2}",
                                getDeclaringModule().getSymbolicName(),
                                getName(),
                                e.getMessage()),
                        e);
            }
        }

        if (instance instanceof ConfigurableExtension) {
            ConfigurableExtension configurableExtension = (ConfigurableExtension) instance;
            configurableExtension.configure(this);
        }

        return instance;
    }

    private <T> T createInstance(Class<T> someClass) throws CoreException {
        T instance;
        try {
            instance = someClass.newInstance();
        } catch (Throwable e) {
            throw new CoreException(
                    MessageFormat.format(
                            "Module [{0}]: Failed to instantiate object for extension [{1}]: {2}",
                            getDeclaringModule().getSymbolicName(),
                            getName(),
                            e.getMessage()),
                    e);
        }
        return instance;
    }

    private <T> Class<T> getExtensionClass(Class<T> extensionType,
                                           Class<T> extensionDefaultClass,
                                           String extensionClassAttributeName,
                                           String extensionClassElementName) throws CoreException {
        Class<T> extensionClass = null;
        String extensionClassName = null;
        if (extensionClassElementName != null) {
            ConfigurationElement extensionClassNameElement = getChild(extensionClassElementName);
            if (extensionClassNameElement != null) {
                extensionClassName = extensionClassNameElement.getValue();
            }
        } else if (extensionClassAttributeName != null) {
            extensionClassName = getAttribute(extensionClassAttributeName);
        } else {
            extensionClassName = getAttribute("class");
        }
        if (extensionClassName != null) {
            extensionClassName = extensionClassName.trim();
            extensionClass = loadClass(extensionClassName, extensionType);
        }
        if (extensionClass == null) {
            extensionClass = extensionDefaultClass;
        }
        if (extensionClass == null) {
            throw new CoreException(
                    MessageFormat.format(
                            "Module [{0}]: Missing class definition for executable extension [{1}]",
                            getDeclaringModule().getSymbolicName(),
                            getName()));
        }
        return extensionClass;
    }

    private <T> void checkExtensionType(Class<T> extensionType, String typeAttributeValue) throws CoreException {
        if (typeAttributeValue != null) {
            Class<?> declaredExtensionType = loadClass(typeAttributeValue);
            if (!declaredExtensionType.equals(extensionType)) {
                throw new CoreException(
                        MessageFormat.format(
                                "Module [{0}]: Illegal type definition for executable extension [{1}]: must be [{2}]",
                                getDeclaringModule().getSymbolicName(),
                                getName(),
                                extensionType.getName()));
            }
        }
    }

    private Class<?> loadClass(String className) throws CoreException {
        try {
            return getDeclaringModule().loadClass(className);
        } catch (Throwable e) {
            throw new CoreException(
                    MessageFormat.format(
                            "Module [{0}]: Executable extension [{1}]: Failed to load class [{2}]",
                            getDeclaringModule().getSymbolicName(),
                            getName(),
                            className),
                    e);
        }
    }

    private <T> Class<T> loadClass(String className, Class<T> requiredType) throws CoreException {
        Class<?> someClass = loadClass(className);
        if (!requiredType.isAssignableFrom(someClass)) {
            throw new CoreException(
                    MessageFormat.format(
                            "Module [{0}]: Executable extension [{1}]: Class [{2}] is not a [{3}]",
                            getDeclaringModule().getSymbolicName(),
                            getName(),
                            someClass.getName(),
                            requiredType.getName()));
        }
        return (Class<T>) someClass;
    }

    private <T> Module getDeclaringModule() {
        return getDeclaringExtension().getDeclaringModule();
    }

    private <T> XStream getXStream(Class<T> extensionType,
                                   Class<T> extensionClass,
                                   Class<T> extensionDefaultClass) {
        if (schemaElement == null) {
            return null;
        }

        String attribute = schemaElement.getAttribute("autoConfig");
        if (attribute == null || !attribute.equalsIgnoreCase("true")) {
            return null;
        }

        XStream xStream = schemaElement.getXStream();
        if (xStream == null) {
            xStream = createXStream(extensionType, extensionClass);
            schemaElement.setXStream(xStream);
            if (extensionDefaultClass != null) {
                schemaElement.configureAliases(extensionDefaultClass);
            }
        }
        schemaElement.configureAliases(extensionClass);
        xStream.setClassLoader(getDeclaringModule().getClassLoader());
        return xStream;
    }

    private <T> XStream createXStream(Class<T> extensionType, Class<T> extensionClass) {
        ClassLoaderReference classLoaderReference = new ClassLoaderReference(new CompositeClassLoader());
        XStream xStream = new XStream(null, new XppDriver(), classLoaderReference);
        xStream.aliasType(getName(), extensionType);
        ConfigurationSchemaElement[] children = schemaElement.getChildren();
        for (ConfigurationSchemaElement child : children) {
            String fieldName = child.getAttribute("field");
            if (fieldName != null) {
                xStream.aliasField(child.getName(), extensionClass, fieldName);
            }
        }
        JavaClassConverter classConverter = new WhitespaceIgnoringJavaClassConverter(classLoaderReference);
        xStream.registerConverter(classConverter, XStream.PRIORITY_VERY_HIGH);
        return xStream;
    }

    @Override
    protected ConfigurationElement[] createChildren(XppDom[] doms) {
        ConfigurationElement[] children = createEmptyArray(doms.length);
        for (int i = 0; i < doms.length; i++) {
            ConfigurationElementImpl child = new ConfigurationElementImpl(this, doms[i]);
            child.setDeclaringExtension(declaringExtension);
            if (schemaElement != null) {
                child.setSchemaElement((ConfigurationSchemaElementImpl) schemaElement.getChild(child.getName()));
            }
            children[i] = child;
        }
        return children;
    }

    @Override
    protected ConfigurationElement[] createEmptyArray(int n) {
        return new ConfigurationElement[n];
    }

    void setDeclaringExtension(ExtensionImpl declaringExtension) {
        this.declaringExtension = declaringExtension;
    }

    void setSchemaElement(ConfigurationSchemaElementImpl schemaElement) {
        this.schemaElement = schemaElement;
    }

    private static class WhitespaceIgnoringJavaClassConverter extends JavaClassConverter {
        public WhitespaceIgnoringJavaClassConverter(ClassLoader classLoader) {
            super(classLoader);
        }

        @Override
        public Object fromString(String str) {
            return super.fromString(str.trim());
        }
    }
}
