package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.thoughtworks.xstream.XStream;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class XStreamDomConverter implements DomConverter {

    private final Class<?> valueType;
    private XStream xStream;

    public XStreamDomConverter(Class<?> valueType) {
        this.valueType = valueType;
    }

    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                   ValidationException {
        try {
            return getXStream().unmarshal(new XStreamDomElementReader(parentElement), value);
        } catch (Throwable e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        getXStream().marshal(value, new XStreamDomElementWriter(parentElement));
    }

    public XStream getXStream() {
        if (xStream == null) {
            xStream = new XStream();
            configureXStream(xStream);
        }
        return xStream;
    }

    protected void configureXStream(XStream xStream) {

    }
}

