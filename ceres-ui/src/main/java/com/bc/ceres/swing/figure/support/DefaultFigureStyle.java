package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.swing.figure.FigureStyle;

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

import static java.lang.Math.*;

public class DefaultFigureStyle extends PropertyContainer implements FigureStyle {
    //
    //  The following property descriptors are SVG/CSS standards (see http://www.w3.org/TR/SVG/styling.html)
    //
    public static final PropertyDescriptor FILL_COLOR = createFillColorDescriptor();
    public static final PropertyDescriptor FILL_OPACITY = createFillOpacityDescriptor();
    public static final PropertyDescriptor STROKE_COLOR = createStrokeColorDescriptor();
    public static final PropertyDescriptor STROKE_OPACITY = createStrokeOpacityDescriptor();
    public static final PropertyDescriptor STROKE_WIDTH = createStrokeWidthDescriptor();

    private static final DefaultFigureStyle PROTOTYPE;

    private String name;
    private Map<String, Object> values;
    private Stroke stroke;
    private Paint strokePaint;
    private Paint fillPaint;

    public DefaultFigureStyle() {
        this("");
    }

    static {
        PROTOTYPE = new DefaultFigureStyle();
        PROTOTYPE.initPrototypeProperties();
    }

    public DefaultFigureStyle(String name) {
        this.name = name;
        this.values = new HashMap<String, Object>();
        addPropertyChangeListener(new EffectivePropertyNuller());
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

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object getValue(String name) {
        if (isPropertyDefined(name)) {
            return super.getValue(name);
        }
        Property protoTypeProperty = PROTOTYPE.getProperty(name);
        if (protoTypeProperty != null) {
            return protoTypeProperty.getValue();
        }
        return null;
    }

    @Override
    public void setValue(String name, Object value) throws IllegalArgumentException {
        if (isPropertyDefined(name)) {
            super.setValue(name, value);
            return;
        }
        Property property = PROTOTYPE.getProperty(name);
        if (property != null) {
            defineProperty(property.getDescriptor(), value);
        } else {
            // be tolerant, do nothing!
            // todo - really do nothing or log warning, exception?  (nf)
        }
    }

    @Override
    public Stroke getStroke() {
        if (stroke == null) {
            stroke = createEffectiveStroke(getStrokeWidth());
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


    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
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
        Object value = getValue(STROKE_OPACITY.getName());
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return 1.0;
        }
    }

    public void setStrokeOpacity(double opacity) {
        setValue(STROKE_OPACITY.getName(), opacity);
    }

    @Override
    public double getStrokeWidth() {
        Object value = getValue(STROKE_WIDTH.getName());
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return 0.0;
        }
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
        Object value = getValue(FILL_OPACITY.getName());
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return 1.0;
        }
    }

    public void setFillOpacity(double opacity) {
        setValue(FILL_OPACITY.getName(), opacity);
    }

    @Override
    public String toCssString() {
        Map<String, Property> all = getOrderedMap();
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
        resetEffectiveProperties();
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

    private synchronized Map<String, Property> getOrderedMap() {
        // Using a TreeMap makes sure that entries are ordered by key
        Property[] properties = getProperties();
        Map<String, Property> propertyMap = new TreeMap<String, Property>();
        for (Property property1 : properties) {
            propertyMap.put(property1.getName(), property1);
        }
        return propertyMap;
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
        try {
            setDefaultValues();
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void defineProperty(PropertyDescriptor propertyDescriptor, Object value) {
        MapEntryAccessor accessor = new MapEntryAccessor(values, propertyDescriptor.getName());
        Property property = new Property(propertyDescriptor, accessor);
        addProperty(property);
        setValue(property.getName(), value);
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

    private static Stroke createEffectiveStroke(double width) {
        return new BasicStroke((float) width);
    }

    private void resetEffectiveProperties() {
        resetEffectiveStrokeProperties();
        resetEffectiveFillProperties();
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
            }
        }
    }

    private static PropertyDescriptor createFillColorDescriptor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("fill", Color.class);
        descriptor.setDefaultValue(Color.BLACK);
        descriptor.setNotNull(false);
        return descriptor;
    }

    private static PropertyDescriptor createFillOpacityDescriptor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("fill-opacity", Double.class);
        descriptor.setDefaultValue(1.0);
        descriptor.setValueRange(new ValueRange(0.0, 1.0));
        descriptor.setNotNull(false);
        return descriptor;
    }

    private static PropertyDescriptor createStrokeColorDescriptor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("stroke", Color.class);
        descriptor.setDefaultValue(null);
        descriptor.setNotNull(false);
        return descriptor;
    }

    private static PropertyDescriptor createStrokeOpacityDescriptor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("stroke-opacity", Double.class);
        descriptor.setDefaultValue(1.0);
        descriptor.setValueRange(new ValueRange(0.0, 1.0));
        descriptor.setNotNull(false);
        return descriptor;
    }

    private static PropertyDescriptor createStrokeWidthDescriptor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("stroke-width", Double.class);
        descriptor.setDefaultValue(0.0);
        descriptor.setNotNull(false);
        return descriptor;
    }

}
