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
        return new XppDomElement(xppDomWriter.getConfiguration());
    }

}
