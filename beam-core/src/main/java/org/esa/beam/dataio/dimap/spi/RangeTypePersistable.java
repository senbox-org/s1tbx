package org.esa.beam.dataio.dimap.spi;

import org.jdom.Element;
import org.esa.beam.framework.datamodel.Product;

public class RangeTypePersistable implements DimapPersistable {

    @Override
    public Object createObjectFromXml(Element element, Product product) {
        return null;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        final Element element = new Element("mask");
        element.setAttribute("type", "range");
        
        return element;
    }
}
