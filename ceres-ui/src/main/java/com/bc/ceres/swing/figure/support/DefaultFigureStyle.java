/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Symbol;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class DefaultFigureStyle extends PropertyContainer implements FigureStyle {
    //
    //  The following property descriptors are SVG/CSS standards (see http://www.w3.org/TR/SVG/styling.html)
    //
    public static final PropertyDescriptor FILL_COLOR = createFillColorDescriptor();
    public static final PropertyDescriptor FILL_OPACITY = createFillOpacityDescriptor();
    public static final PropertyDescriptor STROKE_COLOR = createStrokeColorDescriptor();
    public static final PropertyDescriptor STROKE_OPACITY = createStrokeOpacityDescriptor();
    public static final PropertyDescriptor STROKE_WIDTH = createStrokeWidthDescriptor();
    //
    //  The following property descriptors are not really SVG/CSS standards
    //
    public static final PropertyDescriptor SYMBOL_NAME = createSymbolNameDescriptor();
    public static final PropertyDescriptor SYMBOL_IMAGE = createSymbolImageDescriptor();
    public static final PropertyDescriptor SYMBOL_REF_X = createSymbolRefXDescriptor();
    public static final PropertyDescriptor SYMBOL_REF_Y = createSymbolRefYDescriptor();

    private static final DefaultFigureStyle PROTOTYPE;

    private FigureStyle parentStyle;
    private String name;
    private Map<String, Object> values;
    private Stroke stroke;
    private Paint strokePaint;
    private Paint fillPaint;
    private Symbol symbol;

    static {
        PROTOTYPE = new DefaultFigureStyle();
        PROTOTYPE.initPrototypeProperties();
    }

    public DefaultFigureStyle() {
        this("");
    }

    public DefaultFigureStyle(String name) {
        this(name, PROTOTYPE);
    }

    public DefaultFigureStyle(FigureStyle parentStyle) {
        this(null, parentStyle);
    }

    public DefaultFigureStyle(String name, FigureStyle parentStyle) {
        this.name = name != null ? name : (parentStyle != null ? parentStyle.getName() : "");
        this.parentStyle = parentStyle != null ? parentStyle : PROTOTYPE;
        this.values = new HashMap<>();
        addPropertyChangeListener(new EffectivePropertyNuller());
    }

    public static FigureStyle createFromCss(String css) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.fromCssString(css);
        return figureStyle;
    }

    public static FigureStyle createPointStyle(Symbol symbol) {
        return createPointStyle(symbol, null, null, null);
    }

    public static FigureStyle createPointStyle(Symbol symbol, Paint strokePaint, Stroke stroke) {
        return createPointStyle(symbol, null, strokePaint, stroke);
    }

    public static FigureStyle createPointStyle(Symbol symbol, Paint fillPaint, Paint strokePaint, Stroke stroke) {
        DefaultFigureStyle figureStyle = setSymbol(symbol);
        setStroke(figureStyle, strokePaint, stroke);
        setFill(figureStyle, fillPaint);
        return figureStyle;
    }

    public static FigureStyle createLineStyle(Paint strokePaint) {
        return createLineStyle(strokePaint, null);
    }

    public static FigureStyle createLineStyle(Paint strokePaint, Stroke stroke) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("line-style");
        setStroke(figureStyle, strokePaint, stroke);
        setFill(figureStyle, null);
        return figureStyle;
    }

    public static DefaultFigureStyle createPolygonStyle(Paint fillPaint) {
        return createPolygonStyle(fillPaint, null);
    }

    public static DefaultFigureStyle createPolygonStyle(Paint fillPaint, Paint strokePaint) {
        return createPolygonStyle(fillPaint, strokePaint, null);
    }

    public static DefaultFigureStyle createPolygonStyle(Paint fillPaint, Paint strokePaint, Stroke stroke) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("polygon-style");
        setFill(figureStyle, fillPaint);
        setStroke(figureStyle, strokePaint, stroke);
        return figureStyle;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T> T getValue(String name) {
        if (isPropertyDefined(name)) {
            return (T) super.getValue(name);
        }
        Property property = parentStyle.getProperty(name);
        if (property != null) {
            return (T) property.getValue();
        }
        return null;
    }

    @Override
    public void setValue(String name, Object value) throws IllegalArgumentException {
        if (isPropertyDefined(name)) {
            super.setValue(name, value);
            return;
        }
        Property property = parentStyle.getProperty(name);
        if (property != null) {
            defineProperty(property.getDescriptor(), value);
        } else {
            // be tolerant, do nothing!
        }
    }

    @Override
    public Symbol getSymbol() {
        if (symbol == null) {
            symbol = getEffectiveSymbol(getSymbolName(), getSymbolImagePath(), getSymbolRefX(), getSymbolRefY());
        }
        return symbol;
    }

    @Override
    public String getSymbolName() {
        return getValue(SYMBOL_NAME.getName(), null);
    }

    public void setSymbolName(String symbolName) {
        setValue(SYMBOL_NAME.getName(), symbolName);
    }

    @Override
    public String getSymbolImagePath() {
        return getValue(SYMBOL_IMAGE.getName(), null);
    }

    public void setSymbolImagePath(String symbolName) {
        setValue(SYMBOL_IMAGE.getName(), symbolName);
    }

    @Override
    public double getSymbolRefX() {
        return getValue(SYMBOL_REF_X.getName(), 0.0);
    }

    public void setSymbolRefX(double refX) {
        setValue(SYMBOL_REF_X.getName(), refX);
    }

    @Override
    public double getSymbolRefY() {
        return getValue(SYMBOL_REF_Y.getName(), 0.0);
    }

    public void setSymbolRefY(double refY) {
        setValue(SYMBOL_REF_Y.getName(), refY);
    }

    @Override
    public Stroke getStroke() {
        if (stroke == null) {
            stroke = getEffectiveStroke(getStrokeWidth());
        }
        return stroke;
    }

    @Override
    public Stroke getStroke(double scale) {
        Stroke stroke = getStroke();
        if (scale != 1.0 && stroke instanceof BasicStroke) {
            BasicStroke basicStroke = (BasicStroke) stroke;
            return new BasicStroke((float) (basicStroke.getLineWidth() * scale),
                    basicStroke.getEndCap(),
                    basicStroke.getLineJoin(),
                    basicStroke.getMiterLimit(),
                    basicStroke.getDashArray(),
                    basicStroke.getDashPhase());
        }
        return stroke;
    }

    /**
     * Gets the effective stroke paint used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective stroke paint used for drawing.
     */
    @Override
    public Paint getStrokePaint() {
        if (this.strokePaint == null) {
            Color strokeColor = getStrokeColor();
            if (strokeColor != null) {
                this.strokePaint = getEffectivePaint(strokeColor, getStrokeOpacity());
            }
        }
        return strokePaint;
    }

    @Override
    public Color getStrokeColor() {
        Object value = getValue(STROKE_COLOR.getName());
        if (value instanceof Color) {
            return (Color) value;
        } else {
            return null;
        }
    }

    public void setStrokeColor(Color strokeColor) {
        setValue(STROKE_COLOR.getName(), strokeColor);
    }

    @Override
    public double getStrokeOpacity() {
        return getValue(STROKE_OPACITY.getName(), 1.0);
    }

    public void setStrokeOpacity(double opacity) {
        setValue(STROKE_OPACITY.getName(), opacity);
    }

    @Override
    public double getStrokeWidth() {
        String name1 = STROKE_WIDTH.getName();
        double defaultValue = 0.0;
        return getValue(name1, defaultValue);
    }

    public void setStrokeWidth(double width) {
        setValue(STROKE_WIDTH.getName(), width);
    }

    /**
     * Gets the effective fill paint used for drawing the interior of a polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective fill paint used for drawing.
     */
    @Override
    public Paint getFillPaint() {
        if (fillPaint == null) {
            Color fillColor = getFillColor();
            if (fillColor != null) {
                this.fillPaint = getEffectivePaint(fillColor, getFillOpacity());
            }
        }
        return fillPaint;
    }

    @Override
    public Color getFillColor() {
        Object value = getValue(FILL_COLOR.getName());
        if (value instanceof Color) {
            return (Color) value;
        } else {
            return null;
        }
    }

    public void setFillColor(Color fillColor) {
        setValue(FILL_COLOR.getName(), fillColor);
    }

    @Override
    public double getFillOpacity() {
        return getValue(FILL_OPACITY.getName(), 1.0);
    }

    public void setFillOpacity(double opacity) {
        setValue(FILL_OPACITY.getName(), opacity);
    }

    @Override
    public String toCssString() {
        Map<String, Property> all = getOrderedPropertyMap();
        Set<Map.Entry<String, Property>> entries = all.entrySet();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Property> entry : entries) {
            Property property = entry.getValue();
            Object value = property.getValue();
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(entry.getKey());
                sb.append(":");
                sb.append(property.getValueAsText());
            }
        }
        return sb.toString();
    }

    @Override
    public void fromCssString(String css) {
        resetAllEffectiveProperties();
        StringTokenizer st = new StringTokenizer(css, ";", false);
        while (st.hasMoreElements()) {
            String token = st.nextToken();
            int i = token.indexOf(':');
            if (i > 0) {
                String name = token.substring(0, i).trim();
                String textValue = token.substring(i + 1).trim();
                Property property = getProperty(name);
                try {
                    if (property != null) {
                        property.setValueFromText(textValue);
                    } else {
                        property = PROTOTYPE.getProperty(name);
                        if (property != null) {
                            Converter<?> converter = property.getDescriptor().getConverter();
                            if (converter != null) {
                                Object value = converter.parse(textValue);
                                defineProperty(property.getDescriptor(), value);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // be tolerant, do nothing!
                    // todo - really do nothing or log warning, exception?  (nf)
                }
            }
        }
    }

    private synchronized Map<String, Property> getOrderedPropertyMap() {
        // Using a TreeMap makes sure that entries are ordered by key
        Map<String, Property> propertyMap = new TreeMap<>();
        if (parentStyle != PROTOTYPE) {
            collectProperties(parentStyle.getProperties(), propertyMap);
        }
        collectProperties(getProperties(), propertyMap);
        return propertyMap;
    }

    private static void collectProperties(Property[] properties, Map<String, Property> propertyMap) {
        for (Property property : properties) {
            propertyMap.put(property.getName(), property);
        }
    }

    private static DefaultFigureStyle setSymbol(Symbol symbol) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("point-style");
        if (symbol instanceof NamedSymbol) {
            NamedSymbol namedSymbol = (NamedSymbol) symbol;
            figureStyle.setSymbolName(namedSymbol.getName());
        } else if (symbol instanceof ImageSymbol) {
            ImageSymbol imageSymbol = (ImageSymbol) symbol;
            figureStyle.setSymbolImagePath(imageSymbol.getResourcePath());
            figureStyle.setSymbolRefX(imageSymbol.getRefX());
            figureStyle.setSymbolRefY(imageSymbol.getRefY());
        }
        figureStyle.symbol = symbol;
        return figureStyle;
    }

    private static void setFill(DefaultFigureStyle figureStyle, Paint fillPaint) {
        if (fillPaint instanceof Color) {
            Color fillColor = (Color) fillPaint;
            if (fillColor.getAlpha() == 255) {
                figureStyle.setFillColor(fillColor);
            } else {
                figureStyle.setFillColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue()));
                figureStyle.setFillOpacity(getOpacity(fillColor));
            }
        } else if (fillPaint == null) {
            figureStyle.setFillColor(null);
        }
        figureStyle.fillPaint = fillPaint;
    }

    private static void setStroke(DefaultFigureStyle figureStyle, Paint strokePaint, Stroke stroke) {
        if (strokePaint instanceof Color) {
            Color strokeColor = (Color) strokePaint;
            if (strokeColor.getAlpha() == 255) {
                figureStyle.setStrokeColor(strokeColor);
            } else {
                figureStyle.setStrokeColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue()));
                figureStyle.setStrokeOpacity(getOpacity(strokeColor));
            }
        } else if (strokePaint == null) {
            figureStyle.setStrokeColor(null);
        }
        // check for other paints here
        if (stroke instanceof BasicStroke) {
            BasicStroke basicStroke = (BasicStroke) stroke;
            figureStyle.setStrokeWidth(basicStroke.getLineWidth());
            // add other stuff here
        }
        figureStyle.strokePaint = strokePaint;
        figureStyle.stroke = stroke;
    }

    private static double getOpacity(Color strokeColor) {
        return Math.round(100.0 / 255.0 * strokeColor.getAlpha()) / 100.0;
    }

    // Only called once by prototype singleton

    private void initPrototypeProperties() {
        Field[] declaredFields = DefaultFigureStyle.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getType() == PropertyDescriptor.class) {
                try {
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) declaredField.get(null);
                    propertyDescriptor.setDefaultConverter();
                    if (Color.class.isAssignableFrom(propertyDescriptor.getType())) {
                        propertyDescriptor.setConverter(new CssColorConverter());
                    }
                    defineProperty(propertyDescriptor, propertyDescriptor.getDefaultValue());
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        setDefaultValues();
    }

    private void defineProperty(PropertyDescriptor propertyDescriptor, Object value) {
        MapEntryAccessor accessor = new MapEntryAccessor(values, propertyDescriptor.getName());
        Property property = new Property(propertyDescriptor, accessor);
        addProperty(property);
        setValue(property.getName(), value);
    }

    private Symbol getEffectiveSymbol(String symbolName, String symbolImagePath, double symbolRefX, double symbolRefY) {
        if (symbolName != null) {
            return NamedSymbol.getSymbol(symbolName);
        }
        if (symbolImagePath != null) {
            return ImageSymbol.createIcon(symbolImagePath, symbolRefX, symbolRefY, this.getClass());
        }
        return null;
    }

    private static Paint getEffectivePaint(Color color, double opacity) {
        int alpha = min(max((int) (opacity * 255), 0), 255);
        if (color.getAlpha() == alpha) {
            return color;
        }
        return new Color(color.getRed(),
                         color.getGreen(),
                         color.getBlue(),
                         alpha);
    }

    private static Stroke getEffectiveStroke(double width) {
        return new BasicStroke((float) width);
    }

    private void resetAllEffectiveProperties() {
        resetEffectiveStrokeProperties();
        resetEffectiveFillProperties();
        resetEffectiveSymbolProperties();
    }

    private void resetEffectiveSymbolProperties() {
        symbol = null;
    }

    private void resetEffectiveStrokeProperties() {
        stroke = null;
        strokePaint = null;
    }

    private void resetEffectiveFillProperties() {
        fillPaint = null;
    }

    private class EffectivePropertyNuller implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().startsWith("stroke")) {
                resetEffectiveStrokeProperties();
            } else if (evt.getPropertyName().startsWith("fill")) {
                resetEffectiveFillProperties();
            } else if (evt.getPropertyName().startsWith("symbol")) {
                resetEffectiveSymbolProperties();
            }
        }
    }

    private static PropertyDescriptor createFillColorDescriptor() {
        return createPropertyDescriptor("fill", Color.class, Color.BLACK, false);
    }

    private static PropertyDescriptor createFillOpacityDescriptor() {
        PropertyDescriptor descriptor = createPropertyDescriptor("fill-opacity", Double.class, 1.0, false);
        descriptor.setValueRange(new ValueRange(0.0, 1.0));
        return descriptor;
    }

    private static PropertyDescriptor createStrokeColorDescriptor() {
        return createPropertyDescriptor("stroke", Color.class, null, false);
    }

    private static PropertyDescriptor createStrokeOpacityDescriptor() {
        PropertyDescriptor descriptor = createPropertyDescriptor("stroke-opacity", Double.class, 1.0, false);
        descriptor.setValueRange(new ValueRange(0.0, 1.0));
        return descriptor;
    }

    private static PropertyDescriptor createStrokeWidthDescriptor() {
        PropertyDescriptor descriptor = createPropertyDescriptor("stroke-width", Double.class, 0.0, false);
        descriptor.setValueRange(new ValueRange(0.0, Double.POSITIVE_INFINITY));
        return descriptor;
    }

    private static PropertyDescriptor createSymbolNameDescriptor() {
        return createPropertyDescriptor("symbol", String.class, null, false);
    }

    private static PropertyDescriptor createSymbolImageDescriptor() {
        return createPropertyDescriptor("symbol-image", String.class, null, false);
    }

    private static PropertyDescriptor createSymbolRefXDescriptor() {
        return createPropertyDescriptor("symbol-ref-x", Double.class, 0.0, false);
    }

    private static PropertyDescriptor createSymbolRefYDescriptor() {
        return createPropertyDescriptor("symbol-ref-y", Double.class, 0.0, false);
    }

    private static PropertyDescriptor createPropertyDescriptor(String propertyName, Class type, Object defaultValue, boolean notNull) {
        PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, type);
        descriptor.setDefaultValue(defaultValue);
        descriptor.setNotNull(notNull);
        return descriptor;
    }

    private double getValue(String name1, double defaultValue) {
        Object value = getValue(name1);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return defaultValue;
        }
    }

    private String getValue(String name, String defaultValue) {
        Object value = getValue(name);
        if (value instanceof String) {
            return (String) value;
        } else {
            return defaultValue;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DefaultFigureStyle)) {
            return false;
        }
        DefaultFigureStyle other = (DefaultFigureStyle) obj;
        Property[] properties = this.getProperties();
        for (Property property : properties) {
            Property otherProperty = other.getProperty(property.getName());
            if (otherProperty == null || otherProperty.getValue() != null && property.getValue() == null ||
                    (property.getValue() != null && !property.getValue().equals(otherProperty.getValue()))) {
                return false;
            }
        }
        return true;
    }
}
