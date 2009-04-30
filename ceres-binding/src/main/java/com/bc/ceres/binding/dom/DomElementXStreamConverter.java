package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;

public class DomElementXStreamConverter implements Converter {

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
            XStreamDomElementReader source = new XStreamDomElementReader(child);
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

}
