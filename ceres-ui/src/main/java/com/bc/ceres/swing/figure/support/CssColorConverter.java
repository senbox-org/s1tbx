package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.awt.Color;

public class CssColorConverter implements Converter<Color> {

    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    @Override
    public Class<? extends Color> getValueType() {
        return Color.class;
    }

    /**
     * Converts a value from its plain text representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param text The textual representation of the value.
     * @return The converted value.
     * @throws com.bc.ceres.binding.ConversionException
     *          If the conversion fails.
     */
    @Override
    public Color parse(String text) throws ConversionException {
        // todo - parse CSS stuff like "pink", "black", etc.
        try {
            return Color.decode("0x"+(text.startsWith("#") ? text.substring(1) : text));
        } catch (NumberFormatException e) {
            throw new ConversionException(e);
        }
    }

    /**
     * Converts a value of the type returned by {@link #getValueType()} to its
     * plain text representation.
     *
     * @param value The value to be converted to text.
     * @return The textual representation of the value.
     */
    @Override
    public String format(Color value) {
        StringBuilder sb = new StringBuilder("#");
        append2DigitHex(sb, value.getRed());
        append2DigitHex(sb, value.getGreen());
        append2DigitHex(sb, value.getBlue());
        return sb.toString();
    }

    private void append2DigitHex(StringBuilder sb, int red) {
        String s = Integer.toHexString(red);
        if (s.length() == 1) {
            sb.append("0");
            sb.append(s);
        } else if (s.length() == 2) {
            sb.append(s);
        } else {
            sb.append("00");
        }
    }
}
