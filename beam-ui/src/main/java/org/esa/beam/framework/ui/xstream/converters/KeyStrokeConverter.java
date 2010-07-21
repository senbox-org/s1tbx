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

package org.esa.beam.framework.ui.xstream.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import javax.swing.*;

/**
 * An {@link com.thoughtworks.xstream.XStream XStream} converter for {@link KeyStroke}s.
 */
public class KeyStrokeConverter implements Converter {

    public boolean canConvert(Class aClass) {
        return KeyStroke.class.equals(aClass);
    }

    public void marshal(Object object, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
        if (object != null) {
            hierarchicalStreamWriter.setValue(object.toString());
        }
    }

    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        String text = hierarchicalStreamReader.getValue();
        if (text != null) {
            return KeyStroke.getKeyStroke(text);
        }
        return null;
    }

}
