package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.io.xml.AbstractDocumentReader;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class XStreamDomElementReader extends AbstractDocumentReader {

    private DomElement current;
    private String[] attributeNames;

    public XStreamDomElementReader(DomElement child) {
        this(child, new XmlFriendlyReplacer());
    }

    public XStreamDomElementReader(DomElement child, XmlFriendlyReplacer xmlFriendlyReplacer) {
        super(child, xmlFriendlyReplacer);
    }

    @Override
    protected void reassignCurrentElement(Object o) {
        current = (DomElement) o;
        attributeNames = current.getAttributeNames();
    }

    @Override
    protected Object getParent() {
        return current.getParent();
    }

    @Override
    protected Object getChild(int i) {
        return current.getChild(i);
    }

    @Override
    protected int getChildCount() {
        return current.getChildCount();
    }

    @Override
    public String getNodeName() {
        return current.getName();
    }

    @Override
    public String getValue() {
        return current.getValue();
    }

    @Override
    public String getAttribute(String s) {
        return current.getAttribute(s);
    }

    @Override
    public String getAttribute(int i) {
        return current.getAttribute(attributeNames[i]);
    }

    @Override
    public int getAttributeCount() {
        return attributeNames.length;
    }

    @Override
    public String getAttributeName(int i) {
        return attributeNames[i];
    }
}
