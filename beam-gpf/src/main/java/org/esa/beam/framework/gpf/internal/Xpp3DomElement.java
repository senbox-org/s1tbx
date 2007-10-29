package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.binding.dom.DomElement;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import java.io.StringWriter;


public class Xpp3DomElement implements DomElement {
    private final Xpp3Dom xpp3Dom;

    public static Xpp3DomElement createDomElement(String name) {
        return createDomElement(new Xpp3Dom(name));
    }

    public static Xpp3DomElement createDomElement(Xpp3Dom parentElement) {
        return new Xpp3DomElement(parentElement);
    }

    public Xpp3DomElement(Xpp3Dom xpp3Dom) {
        this.xpp3Dom = xpp3Dom;
    }

    public Xpp3Dom getXpp3Dom() {
        return xpp3Dom;
    }

    public void setAttribute(String name, String value) {
        xpp3Dom.setAttribute(name, value);
    }

    public DomElement getChild(int index) {
        return new Xpp3DomElement(xpp3Dom.getChild(index));
    }

    public int getChildCount() {
        return xpp3Dom.getChildCount();
    }

    public DomElement createChild(String name) {
        final Xpp3DomElement child = createDomElement(new Xpp3Dom(name));
        addChild(child);
        return child;
    }

    public void addChild(DomElement child) {
        xpp3Dom.addChild(((Xpp3DomElement) child).getXpp3Dom());
    }

    public void setValue(String value) {
        xpp3Dom.setValue(value);
    }

    public String getName() {
        return xpp3Dom.getName();
    }

    public String getValue() {
        return xpp3Dom.getValue();
    }

    public String getAttribute(String attributeName) {
        return xpp3Dom.getAttribute(attributeName);
    }

    public String[] getAttributeNames() {
        return xpp3Dom.getAttributeNames();
    }

    public DomElement getParent() {
        return createDomElement(xpp3Dom.getParent());
    }

    public DomElement getChild(String elementName) {
        return createDomElement(xpp3Dom.getChild(elementName));
    }

    public DomElement[] getChildren() {
        final Xpp3Dom[] xppChildren = xpp3Dom.getChildren();
        return createXpp3DomElementArray(xppChildren);
    }

    public DomElement[] getChildren(String elementName) {
        final Xpp3Dom[] xppChildren = xpp3Dom.getChildren(elementName);
        return createXpp3DomElementArray(xppChildren);
    }

    public String toXml() {
        final StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(new XppDomReader(xpp3Dom), new PrettyPrintWriter(writer));
        return writer.toString();
    }

    private Xpp3DomElement[] createXpp3DomElementArray(Xpp3Dom[] xppChildren) {
        final Xpp3DomElement[] domElements = new Xpp3DomElement[xppChildren.length];
        for (int i = 0; i < xppChildren.length; i++) {
            domElements[i] = createDomElement(xppChildren[i]);
        }
        return domElements;
    }
}
