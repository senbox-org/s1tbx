package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

public class Xpp3DomElementTest extends AbstractDomElementTest {
    @Override
    protected DomElement createDomElement(String name) {
        return new Xpp3DomElement(new Xpp3Dom(name));
    }

    @Override
    protected DomElement createDomElement(String name, String value) {
        final Xpp3Dom xpp3Dom = new Xpp3Dom(name);
        xpp3Dom.setValue(value);
        return new Xpp3DomElement(xpp3Dom);
    }

    public void testConstructor() {
        try {
            new Xpp3DomElement((Xpp3Dom) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Xpp3DomElement((String) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
    }

}