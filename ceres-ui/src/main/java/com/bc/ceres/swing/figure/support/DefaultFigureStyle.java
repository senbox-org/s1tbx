package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.converters.ColorConverter;
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
import java.util.TreeMap;

public class DefaultFigureStyle extends PropertyContainer implements FigureStyle {
    private static final DefaultFigureStyle DEFAULT_STYLE;

    private String name;
    private FigureStyle defaultStyle;
    private Stroke stroke;

    private Map<String, Object> values;

    static {
        DEFAULT_STYLE = new DefaultFigureStyle("", null);
        DEFAULT_STYLE.initDefaultProperties();
    }

    public DefaultFigureStyle() {
        this("", DEFAULT_STYLE);
    }

    public DefaultFigureStyle(String name, FigureStyle defaultStyle) {
        this.name = name;
        this.defaultStyle = defaultStyle;
        this.values = new HashMap<String, Object>();
        addPropertyChangeListener(new StrokeNuller());
    }

    public static FigureStyle createLineStyle(Color strokePaint) {
        return createLineStyle(strokePaint, 0.0);
    }

    public static FigureStyle createLineStyle(Color strokePaint, double strokeWidth) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("line-style", DEFAULT_STYLE);
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
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("polygon-style", DEFAULT_STYLE);
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
    public FigureStyle getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(FigureStyle defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    @Override
    public Stroke getStroke() {
        if (stroke == null) {
            if (getValue(STROKE.getName()) != null) {
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
    public Paint getStrokePaint() {
        Object value = getValue(STROKE.getName());
        if (value instanceof Paint) {
            return (Paint) value;
        } else {
            return null;
        }
    }

    public void setStrokePaint(Paint strokePaint) {
        setValue(STROKE.getName(), strokePaint);
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
    public Paint getFillPaint() {
        Object value = getValue(FILL.getName());
        if (value instanceof Paint) {
            return (Paint) value;
        } else {
            return null;
        }
    }

    public void setFillPaint(Paint fillPaint) {
        setValue(FILL.getName(), fillPaint);
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

    private void initDefaultProperties() {
        Field[] declaredFields = FigureStyle.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getType() == Property.class) {
                try {
                    Property prototype = (Property) declaredField.get(null);
                    defineProperty(prototype, prototype.getValue());
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

    private void defineProperty(Property prototype, Object value) {
        MapEntryAccessor accessor = new MapEntryAccessor(values, prototype.getName());
        Property property = new Property(prototype.getDescriptor(), accessor);
        addProperty(property);
        setValue(property.getName(), value);
    }

    @Override
    public Property getProperty(String name) {
        Property property = super.getProperty(name);
        if (property != null) {
            return property;
        }
        if (defaultStyle != null) {
            return defaultStyle.getProperty(name);
        }
        return null;
    }

    @Override
    public void setValue(String name, Object value) throws IllegalArgumentException {
        Property property = super.getProperty(name);
        if (property == null) {
            Property prototype = getProperty(name);
            if (prototype == null) {
                throw new IllegalArgumentException("Unknown property: " + name);
            }
            defineProperty(prototype, value);
        } else {
            super.setValue(name, value);
        }
    }

    @Override
    public String toCssString() {
        ConverterRegistry.getInstance().setConverter(Color.class, new ColorConverter());
        Map<String, Property> all = new TreeMap<String, Property>();
        collect(this, all);
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
                String txt;
                if (value instanceof Color) {
                    Color color = (Color) value;
                    txt = "#" + h(color.getRed()) + h(color.getGreen()) + h(color.getBlue());
                } else {
                    txt = value.toString();
                }

                sb.append(txt);
            }
        }
        return sb.toString();
    }

    private String h(int red) {
        String s = Integer.toHexString(red);
        if (s.length() == 1) {
            return "0" + s;
        }
        return s;
    }

    @Override
    public void fromCssString(String css) {
    }

    private void collect(FigureStyle figureStyle, Map<String, Property> all) {
        FigureStyle defStyle = figureStyle.getDefaultStyle();
        if (defStyle != null) {
            //collect(defStyle, all);
        }
        Property[] properties = figureStyle.getProperties();
        for (Property property : properties) {
            all.put(property.getName(), property);
        }
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
