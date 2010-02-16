package com.bc.ceres.binding.dom;

public class DefaultDomElementTest extends AbstractDomElementTest {

    @Override
    protected DomElement createDomElement(String name) {
        return new DefaultDomElement(name);
    }

    @Override
    protected DomElement createDomElement(String name, String value) {
        return new DefaultDomElement(name, value);
    }

    public void testConstructor() {
        try {
            new DefaultDomElement(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new DefaultDomElement(null, "red");
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new DefaultDomElement("color", null);
            // ok
        } catch (NullPointerException e) {
            fail("NPE not expected");
        }
    }

    public void testMixChildren() {
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new Xpp3DomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new Xpp3DomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new Xpp3DomElement("b"), new Xpp3DomElement("c"));
        testMixChildren(new Xpp3DomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new Xpp3DomElement("a"), new DefaultDomElement("b"), new Xpp3DomElement("c"));
        testMixChildren(new Xpp3DomElement("a"), new Xpp3DomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new Xpp3DomElement("a"), new Xpp3DomElement("b"), new Xpp3DomElement("c"));
    }

    private void testMixChildren(DomElement a, DomElement b, DomElement c) {
        b.addChild(c);
        a.addChild(b);

        assertNotNull(a.getChild("b"));
        assertNotNull(a.getChild("b").getChild("c"));

        assertNull(a.getParent());
        assertSame(a, a.getChild("b").getParent());
        assertSame(a.getChild("b"), a.getChild("b").getChild("c").getParent());

        assertEquals("<a>\n" +
                "<b>\n" +
                "<c/>\n" +
                "</b>\n" +
                "</a>", a.toXml().replace("  ", ""));
    }
}
