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
