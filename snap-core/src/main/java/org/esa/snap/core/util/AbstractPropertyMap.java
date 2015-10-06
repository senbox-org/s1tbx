package org.esa.snap.core.util;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.ColorConverter;
import com.bc.ceres.binding.converters.FontConverter;

import java.awt.Color;
import java.awt.Font;

/**
 * Abstract implementation of the {@link PropertyMap} interface.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public abstract class AbstractPropertyMap implements PropertyMap {

    public static final Font DEFAULT_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
    public static final Color DEFAULT_COLOR = Color.BLACK;



    /**
     * Gets a value of type <code>boolean</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>false</code> if the key is not contained in this property set.
     */
    @Override
    public boolean getPropertyBool(String key) {
        return getPropertyBool(key, false);
    }

    /**
     * Gets a value of type <code>Boolean</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Boolean getPropertyBool(String key, Boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.valueOf(value) : defaultValue;
    }

    /**
     * Sets a value of type <code>Boolean</code>.
     *
     * @param key      the key
     * @param newValue the new value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyBool(String key, Boolean newValue) {
        set(key, newValue != null ? Boolean.toString(newValue) : null);
    }

    /**
     * Gets a value of type <code>int</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0</code> (zero) if the key is not contained in this property set.
     */
    @Override
    public int getPropertyInt(String key) {
        return getPropertyInt(key, 0);
    }

    /**
     * Gets a value of type <code>Integer</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Integer getPropertyInt(String key, Integer defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value of type <code>Integer</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyInt(String key, Integer value) {
        set(key, value != null ? Integer.toString(value) : null);
    }

    /**
     * Gets a value of type <code>double</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0.0</code> (zero) if the key is not contained in this property
     * set.
     */
    @Override
    public double getPropertyDouble(String key) {
        return getPropertyDouble(key, 0.0);
    }

    /**
     * Gets a value of type <code>Double</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Double getPropertyDouble(String key, Double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value of type <code>Double</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyDouble(String key, Double value) {
        set(key, value != null ? Double.toString(value) : null);
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>""</code> (empty string) if the key is not contained in this
     * property set, never <code>null</code>.
     */
    @Override
    public String getPropertyString(String key) {
        return get(key, "");
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public String getPropertyString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Sets a value of type <code>String</code>.
     *
     * @param key   the key
     * @param value the new value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyString(String key, String value) {
        set(key, value);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>Color.black</code> if the key is not contained in this property
     * set, never <code>null</code>.
     */
    @Override
    public Color getPropertyColor(String key) {
        return getPropertyColor(key, DEFAULT_COLOR);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Color getPropertyColor(String key, Color defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ColorConverter.INSTANCE.parse(value);
        } catch (ConversionException e) {
            return DEFAULT_COLOR;
        }
    }

    /**
     * Sets a value of type <code>Color</code>.
     *
     * @param key      the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyColor(String key, Color value) {
        set(key, ColorConverter.INSTANCE.format(value));
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key the key
     * @return the value for the given key, or a plain, 12-point "SandSerif" font if the key is not contained in this
     * property set, never <code>null</code>.
     */
    @Override
    public Font getPropertyFont(String key) {
        return getPropertyFont(key, DEFAULT_FONT);
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Font getPropertyFont(String key, Font defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return FontConverter.INSTANCE.parse(value);
        } catch (ConversionException e) {
            return DEFAULT_FONT;
        }
    }

    /**
     * Sets a font of type <code>Font</code>. The method actually puts three keys in this property set in order to store
     * the font's properties:
     * <ul>
     * <li><code>&lt;key&gt;.name</code> for the font's name</li>
     * <li><code>&lt;key&gt;.style</code> for the font's style (an integer font)</li>
     * <li><code>&lt;key&gt;.name</code> for the font's size in points (an integer font)</li>
     * </ul>
     *
     * @param key  the key
     * @param font the font
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyFont(String key, Font font) {
        String value = null;
        if (font != null) {
            value = FontConverter.INSTANCE.format(font);
        }
        set(key, value);
    }

    protected abstract String get(String key);

    protected abstract String get(String key, String defaultValue);

    protected abstract String set(String key, String value);

    protected abstract void firePropertyChange(String key, String oldValue, String newValue);
}
