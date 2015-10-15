package org.esa.snap.core.gpf.annotations;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

// todo - use this class to encode/decode parameter blocks in graph XML, parameter XML for the CLI (gpt) and generated GUIs (nf 2012-02-10) 
// Note: method that are marked unused are already in use in Calvalus!

/**
 * A utility class that converts Java objects with {@link Parameter} annotations into XML and vice versa.
 *
 * @author MarcoZ
 * @author Norman
 * @since BEAM 4.10
 */
@SuppressWarnings("UnusedDeclaration")
public class ParameterBlockConverter {

    private final ParameterDescriptorFactory parameterDescriptorFactory;
    private String parameterElementName;

    public ParameterBlockConverter() {
        this(new ParameterDescriptorFactory(), "parameters");
    }

    public ParameterBlockConverter(ParameterDescriptorFactory parameterDescriptorFactory) {
        this(parameterDescriptorFactory, "parameters");
    }

    public ParameterBlockConverter(ParameterDescriptorFactory parameterDescriptorFactory, String parameterElementName) {
        Assert.notNull(parameterDescriptorFactory, "parameterDescriptorFactory");
        Assert.notNull(parameterElementName, "parameterElementName");
        this.parameterDescriptorFactory = parameterDescriptorFactory;
        this.parameterElementName = parameterElementName;
    }

    public DomElement convertXmlToDomElement(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        XppDom xppDom = domWriter.getConfiguration();
        return new XppDomElement(xppDom);
    }

    public <T> T convertXmlToObject(String xml, T object) throws BindingException {
        convertXmlToPropertySet(xml, object.getClass(),
                                PropertyContainer.createObjectBacked(object, parameterDescriptorFactory));
        return object;
    }

    public Map<String, Object> convertXmlToMap(String xml, Class<?> schema) throws ValidationException, ConversionException {
        Map<String, Object> map = new HashMap<String, Object>();
        convertXmlToPropertySet(xml, schema,
                                PropertyContainer.createMapBacked(map, schema, parameterDescriptorFactory));
        return map;
    }

    public String convertObjectToXml(Object object) throws ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(object.getClass(), parameterDescriptorFactory);
        DomElement parametersDom = new XppDomElement(parameterElementName);
        domConverter.convertValueToDom(object, parametersDom);
        return parametersDom.toXml();
    }

    private void convertXmlToPropertySet(String xml, Class<? extends Object> schema, PropertySet propertySet) throws ValidationException, ConversionException {
        propertySet.setDefaultValues();
        DefaultDomConverter domConverter = new DefaultDomConverter(schema, parameterDescriptorFactory);
        DomElement domElement = convertXmlToDomElement(xml);
        domConverter.convertDomToValue(domElement, propertySet);
    }
}
