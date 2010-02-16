package com.bc.ceres.binding.dom;

import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import java.io.StringWriter;
import java.util.HashMap;


public class Xpp3DomElement implements DomElement {
    private final Xpp3Dom xpp3Dom;
    private DomElement parent;
    private HashMap<Xpp3Dom, DomElement> children;

    public Xpp3DomElement(String name) {
        this(new Xpp3Dom(name));
    }

    public Xpp3DomElement(Xpp3Dom xpp3Dom) {
        Assert.notNull(xpp3Dom, "xpp3Dom");
        Assert.notNull(xpp3Dom.getName(), "xpp3Dom.getName()");
        this.xpp3Dom = xpp3Dom;
    }

    public Xpp3Dom getXpp3Dom() {
        return xpp3Dom;
    }

    @Override
    public String getName() {
        return xpp3Dom.getName();
    }

    @Override
    public String getValue() {
        return xpp3Dom.getValue();
    }

    @Override
    public void setValue(String value) {
        xpp3Dom.setValue(value);
    }

    @Override
    public DomElement getParent() {
        return parent;
    }

    @Override
    public void setParent(DomElement parent) {
        if (parent instanceof Xpp3DomElement) {
            Xpp3DomElement xpp3DomElement = (Xpp3DomElement) parent;
            xpp3Dom.setParent(xpp3DomElement.getXpp3Dom());
        }
        this.parent = parent;
    }

    @Override
    public DomElement[] getChildren() {
        return getChildren(xpp3Dom.getChildren());
    }

    @Override
    public DomElement[] getChildren(String elementName) {
        return getChildren(xpp3Dom.getChildren(elementName));
    }

    @Override
    public DomElement getChild(int index) {
        Xpp3Dom child = xpp3Dom.getChild(index);
        if (child == null) {
            return null;
        }
        return getChild(child);
    }

    @Override
    public DomElement getChild(String elementName) {
        final Xpp3Dom child = xpp3Dom.getChild(elementName);
        if (child == null) {
            return null;
        }
        return getChild(child);
    }

    @Override
    public int getChildCount() {
        return xpp3Dom.getChildCount();
    }

    @Override
    public DomElement createChild(String name) {
        final Xpp3DomElement child = new Xpp3DomElement(new Xpp3Dom(name));
        addChild(child);
        return child;
    }

    @Override
    public void addChild(DomElement child) {
        final Xpp3Dom dom;
        if (child instanceof Xpp3DomElement) {
            dom = ((Xpp3DomElement) child).getXpp3Dom();
        } else {
            dom = toXpp3Dom(child);
        }
        xpp3Dom.addChild(dom);
        child.setParent(this);
    }

    @Override
    public String[] getAttributeNames() {
        return xpp3Dom.getAttributeNames();
    }

    @Override
    public String getAttribute(String attributeName) {
        return xpp3Dom.getAttribute(attributeName);
    }

    @Override
    public void setAttribute(String name, String value) {
        xpp3Dom.setAttribute(name, value);
    }

    @Override
    public String toXml() {
        final StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(new XppDomReader(xpp3Dom), new PrettyPrintWriter(writer));
        return writer.toString();
    }

    private Xpp3Dom toXpp3Dom(DomElement domElement) {
        Xpp3Dom xpp3Dom = new Xpp3Dom(domElement.getName());
        if (domElement.getValue() != null) {
            xpp3Dom.setValue(domElement.getValue());
        }
        String[] names = domElement.getAttributeNames();
        for (String name : names) {
            xpp3Dom.setAttribute(name, domElement.getAttribute(name));
        }
        DomElement[] children = domElement.getChildren();
        for (DomElement child : children) {
            xpp3Dom.addChild(toXpp3Dom(child));
        }
        return xpp3Dom;
    }

    private DomElement[] getChildren(Xpp3Dom[] xppChildren) {
        final DomElement[] domElements = new Xpp3DomElement[xppChildren.length];
        for (int i = 0; i < xppChildren.length; i++) {
            domElements[i] = getChild(xppChildren[i]);
        }
        return domElements;
    }

    private DomElement getChild(Xpp3Dom child) {
        if (children != null) {
            DomElement childElement = children.get(child);
            if (childElement != null) {
                return childElement;
            }
        } else {
            children = new HashMap<Xpp3Dom, DomElement>();
        }

        DomElement childElement = new Xpp3DomElement(child);
        childElement.setParent(this);

        children.put(child, childElement);

        return childElement;
    }

}
