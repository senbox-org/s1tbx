package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ConfigurationShemaElement;
import com.bc.ceres.core.runtime.ConfigurationSchemaElement;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import java.util.HashSet;
import java.util.Set;

public class ConfigurationSchemaElementImpl extends ConfigurationElementBaseImpl<ConfigurationSchemaElement>
        implements ConfigurationSchemaElement, ConfigurationShemaElement {

    private ExtensionPointImpl declaringExtensionPoint;
    private XStream xStream;
    private Set<Class> classesWithConfiguredAliases;

    public ConfigurationSchemaElementImpl(ConfigurationSchemaElementImpl parent, Xpp3Dom dom) {
        super(parent, dom);
    }

    public ExtensionPointImpl getDeclaringExtensionPoint() {
        return declaringExtensionPoint;
    }

    @Override
    protected ConfigurationSchemaElement[] createChildren(Xpp3Dom[] doms) {
        ConfigurationSchemaElement[] children = createEmptyArray(doms.length);
        for (int i = 0; i < doms.length; i++) {
            ConfigurationSchemaElementImpl child = new ConfigurationSchemaElementImpl(this, doms[i]);
            child.setDeclaringExtensionPoint(declaringExtensionPoint);
            children[i] = child;
        }
        return children;
    }

    @Override
    protected ConfigurationSchemaElement[] createEmptyArray(int n) {
        return new ConfigurationSchemaElement[n];
    }

    void setDeclaringExtensionPoint(ExtensionPointImpl declaringExtensionPoint) {
        this.declaringExtensionPoint = declaringExtensionPoint;
    }

    XStream getXStream() {
        return xStream;
    }

    void setXStream(XStream xStream) {
        this.xStream = xStream;
    }

    void configureAliases(Class someClass) {
        if (classesWithConfiguredAliases == null) {
            classesWithConfiguredAliases = new HashSet<Class>(4);
        }
        if (!classesWithConfiguredAliases.contains(someClass)) {
            Annotations.configureAliases(xStream, someClass);
            classesWithConfiguredAliases.add(someClass);
        }
    }
}
