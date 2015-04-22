/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.thoughtworks.xstream.XStream;

/**
 * todo - add API doc
 *
 * @author Marco Peters
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

