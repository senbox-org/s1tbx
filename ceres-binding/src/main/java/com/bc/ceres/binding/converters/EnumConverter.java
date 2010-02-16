package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

/**
 * Class for converting enumeration types.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EnumConverter<T extends Enum<T>> implements Converter<T> {
    private Class<T> type;

    public EnumConverter(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getValueType() {
        return type;
    }

    @Override
    public T parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(T value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

}
