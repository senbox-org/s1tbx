package com.bc.ceres.binding.dom;

import com.bc.ceres.core.runtime.ConfigurationElementBase;

public interface DomElement extends ConfigurationElementBase<DomElement> {
    void setParent(DomElement parent);
    int getChildCount();
    DomElement getChild(int index);
    void setAttribute(String name, String value);
    DomElement createChild(String name);
    void addChild(DomElement childElement);
    void setValue(String value);
    String toXml();
}
