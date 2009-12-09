package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Converter;
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

public class DefaultFigureStyle extends PropertyContainer implements FigureStyle {

    private final static DefaultFigureStyle PROTOTYPE;

    private String name;
    private Map<String, Object> values;
    private Stroke stroke;

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
        addPropertyChangeListener(new StrokeNuller());
    }

    public static FigureStyle createLineStyle(Color strokePaint) {
        return createLineStyle(strokePaint, 0.0);
    }

    public static FigureStyle createLineStyle(Color strokePaint, double strokeWidth) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("line-style");
        setStroke(figureStyle, strokePaint, strokeWidth);
        return figureStyle;
    }

    public static DefaultFigureStyle createPolygonStyle(Color fillPaint) {
        return createPolygonStyle(fillPaint, null);
    }

    public static DefaultFigureStyle createPolygonStyle(Color fillPaint, Color strokePaint) {
        return createPolygonStyle(fillPaint, strokePaint, 0.0);
    }

    public static DefaultFigureStyle createPolygonStyle(Color fillPaint, Color strokePaint, double strokeWidth) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("polygon-style");
        setFill(figureStyle, fillPaint);
        setStroke(figureStyle, strokePaint, strokeWidth);
        return figureStyle;
    }

    private static void setFill(DefaultFigureStyle figureStyle, Color fillPaint) {
        if (fillPaint != null) {
            figureStyle.setFillPaint(fillPaint);
            if (fillPaint.getAlpha() != 255) {
                figureStyle.setFillOpacity(getOpacity(fillPaint));
            }
        }
    }

    private static void setStroke(DefaultFigureStyle figureStyle, Color strokePaint, double strokeWidth) {
        if (strokePaint != null) {
            figureStyle.setStrokePaint(strokePaint);
            if (strokePaint.getAlpha() != 255) {
                figureStyle.setStrokeOpacity(getOpacity(strokePaint));
            }
            figureStyle.setStrokeWidth(strokeWidth);
        }
    }

    private static double getOpacity(Color color) {
        return Math.round(100.0 * (color.getAlpha() / 255.0)) / 100.0;
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
            defineProperty(property, value);
        } else {
            // be tolerant, do nothing!
            // todo - really do nothing or log warning, exception?  (nf)
        }
    }

    @Override
    public Stroke getStroke() {
        if (stroke == null) {
            if (getValue(STROKE_COLOR.getName()) != null) {
                Number number = (Number) getValue(STROKE_WIDTH.getName());
                if (number != null) {
                    float width = number.floatValue();
                    stroke = new BasicStroke(width);
                } else {
                    stroke = new BasicStroke();
                }
            }
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

    @Override
    public Color getStrokeColor() {
        Object value = getValue(STROKE_COLOR.getName());
        if (value instanceof Color) {
            return (Color) value;
        } else {
            return null;
        }
    }

    public void setStrokePaint(Paint strokePaint) {
        setValue(STROKE_COLOR.getName(), strokePaint);
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

    @Override
    public Color getFillColor() {
        Object value = getValue(FILL_COLOR.getName());
        if (value instanceof Color) {
            return (Color) value;
        } else {
            return null;
        }
    }

    public void setFillPaint(Paint fillPaint) {
        setValue(FILL_COLOR.getName(), fillPaint);
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

    // Only called once by prototype singleton
    private void initPrototypeProperties() {
        Field[] declaredFields = FigureStyle.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getType() == Property.class) {
                try {
                    Property prototypeProperty = (Property) declaredField.get(null);
                    prototypeProperty.getDescriptor().setDefaultConverter();
                    if (Color.class.isAssignableFrom(prototypeProperty.getType())) {
                        prototypeProperty.getDescriptor().setConverter(new CssColorConverter());
                    }
                    defineProperty(prototypeProperty, prototypeProperty.getValue());
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

    private synchronized Map<String, Property> getOrderedMap() {
        // Using a TreeMap makes sure that entries are ordered by key
        Property[] properties = getProperties();
        Map<String, Property> propertyMap = new TreeMap<String, Property>();
        for (Property property1 : properties) {
            propertyMap.put(property1.getName(), property1);
        }
        return propertyMap;
    }


    @Override
    public void fromCssString(String css) {
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
                                defineProperty(property, value);
                            }
                        }
                    }
                } catch (Exception e) {
                    // be tolerant, do nothing!
                    // todo - really do nothing or log warning, exception?  (nf)
                }
            }
        }
    }

    private void defineProperty(Property prototypeProperty, Object value) {
        MapEntryAccessor accessor = new MapEntryAccessor(values, prototypeProperty.getName());
        Property property = new Property(prototypeProperty.getDescriptor(), accessor);
        addProperty(property);
        setValue(property.getName(), value);
    }

    private class StrokeNuller implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().startsWith("stroke")) {
                stroke = null;
            }
        }
    }
}
