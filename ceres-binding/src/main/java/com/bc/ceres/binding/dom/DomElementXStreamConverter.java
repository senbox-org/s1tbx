package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.AbstractDocumentReader;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;

public class DomElementXStreamConverter implements Converter {

    public DomElementXStreamConverter() {
    }

    @Override
    public boolean canConvert(Class aClass) {
        return DomElement.class.isAssignableFrom(aClass);
    }

    @Override
    public void marshal(Object object,
                        HierarchicalStreamWriter hierarchicalStreamWriter,
                        MarshallingContext marshallingContext) {
        DomElement configuration = (DomElement) object;
        final DomElement[] children = configuration.getChildren();
        for (DomElement child : children) {
            HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
            DomElementReader source = new DomElementReader(child);
            copier.copy(source, hierarchicalStreamWriter);
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader,
                            UnmarshallingContext unmarshallingContext) {
        HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
        XppDomWriter xppDomWriter = new XppDomWriter();
        copier.copy(hierarchicalStreamReader, xppDomWriter);
        return new Xpp3DomElement(xppDomWriter.getConfiguration());
    }

    private static class DomElementReader extends AbstractDocumentReader {

        private DomElement current;
        private String[] attributeNames;

        public DomElementReader(DomElement child) {
            super(child);
            reassignCurrentElement(child);
        }

        public DomElementReader(DomElement child, XmlFriendlyReplacer xmlFriendlyReplacer) {
            super(child, xmlFriendlyReplacer);
            reassignCurrentElement(child);
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

}
