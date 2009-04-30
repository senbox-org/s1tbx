package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.io.xml.AbstractDocumentWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class XStreamDomElementWriter extends AbstractDocumentWriter {

    public XStreamDomElementWriter(DomElement child) {
        this(child, new XmlFriendlyReplacer());
    }

    public XStreamDomElementWriter(DomElement child, XmlFriendlyReplacer xmlFriendlyReplacer) {
        super(child, xmlFriendlyReplacer);
    }

    @Override
    protected Object createNode(String name) {
        final DomElement top = top();
        if (top != null) {
            return top.createChild(escapeXmlName(name));
        }
        return new DefaultDomElement(escapeXmlName(name));
    }

    @Override
    public void addAttribute(String name, String value) {
        top().setAttribute(escapeXmlName(name), value);
    }

    @Override
    public void setValue(String text) {
        top().setValue(text);
    }

    private DomElement top() {
        return (DomElement) getCurrent();
    }
}