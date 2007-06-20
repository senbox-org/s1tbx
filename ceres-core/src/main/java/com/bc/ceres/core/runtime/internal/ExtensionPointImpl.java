package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.ConfigurationShemaElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;

public class ExtensionPointImpl implements ExtensionPoint {

    public static final ExtensionPointImpl[] EMPTY_ARRAY = new ExtensionPointImpl[0];

    private final String id;
    private final ConfigurationShemaElementImpl configurationShemaElement;

    private transient String qualifiedId;
    private transient ModuleImpl declaringModule;

    public ExtensionPointImpl(String id, ConfigurationShemaElementImpl configurationShemaElement) {
        Assert.notNull(id, "id");
        Assert.notNull(configurationShemaElement, "configurationShemaElement");
        this.id = id;
        this.configurationShemaElement = configurationShemaElement;
        this.configurationShemaElement.setDeclaringExtensionPoint(this);
    }

    public String getId() {
        return id;
    }

    public String getQualifiedId() {
        return qualifiedId;
    }

    public Module getDeclaringModule() {
        return declaringModule;
    }

    void setDeclaringModule(ModuleImpl declaringModule) {
        this.declaringModule = declaringModule;
        this.qualifiedId = declaringModule.getSymbolicName() + ':' + id;
    }

    public Extension getExtension(String extensionId) {
        Extension[] extensions = getExtensions();
        for (Extension extension : extensions) {
            if (extensionId.equals(extension.getId())) {
                return extension;
            }
        }
        return null;
    }

    public Extension[] getExtensions() {
        ModuleRegistry registry = declaringModule.getRegistry();
        return registry != null ? registry.getExtensions(qualifiedId) : null;
    }

    public ConfigurationElement[] getConfigurationElements() {
        Extension[] extensions = getExtensions();
        if (extensions == null) {
            return null;
        }
        ConfigurationElement[] configurationElements = new ConfigurationElement[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            configurationElements[i] = extensions[i].getConfigurationElement();
        }
        return configurationElements;
    }

    public ConfigurationShemaElement getConfigurationShemaElement() {
        return configurationShemaElement;
    }
}
