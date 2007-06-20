package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ConfigurationShemaElement;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import java.util.HashSet;
import java.util.Set;

public class ConfigurationShemaElementImpl extends ConfigurationElementBaseImpl<ConfigurationShemaElement>
        implements ConfigurationShemaElement {

    private ExtensionPointImpl declaringExtensionPoint;
    private XStream xStream;
    private Set<Class> classesWithConfiguredAliases;

    public ConfigurationShemaElementImpl(ConfigurationShemaElementImpl parent, Xpp3Dom dom) {
        super(parent, dom);
    }

    public ExtensionPointImpl getDeclaringExtensionPoint() {
        return declaringExtensionPoint;
    }

    @Override
    protected ConfigurationShemaElement[] createChildren(Xpp3Dom[] doms) {
        ConfigurationShemaElement[] children = createEmptyArray(doms.length);
        for (int i = 0; i < doms.length; i++) {
            ConfigurationShemaElementImpl child = new ConfigurationShemaElementImpl(this, doms[i]);
            child.setDeclaringExtensionPoint(declaringExtensionPoint);
            children[i] = child;
        }
        return children;
    }

    @Override
    protected ConfigurationShemaElement[] createEmptyArray(int n) {
        return new ConfigurationShemaElement[n];
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
