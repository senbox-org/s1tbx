package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

/**
 * Class for converting enumeration types.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EnumConverter implements Converter {
    private Class<? extends Enum> type;

    public EnumConverter(Class<? extends Enum> type) {
        this.type = type;
    }

    public Class<?> getValueType() {
        return type;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

}
