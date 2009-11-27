package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

    public static DefaultFigureStyle createShapeStyle(Paint fillPaint, Paint strokePaint) {
        return createShapeStyle(fillPaint, strokePaint, new BasicStroke(1.0f));
    }

    public static DefaultFigureStyle createShapeStyle(Paint fillPaint, Paint strokePaint, Stroke stroke) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle("", DEFAULT_STYLE);
        figureStyle.setFillPaint(fillPaint);
        figureStyle.setStrokePaint(strokePaint);
        figureStyle.setStroke(stroke);
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
                Number number = (Number) getValue("stroke-width");
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

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public Paint getStrokePaint() {
        return (Paint) getValue(STROKE.getName());
    }

    public void setStrokePaint(Paint strokePaint) {
        setValue(STROKE.getName(), strokePaint);
    }

    @Override
    public Paint getFillPaint() {
        return (Paint) getValue(FILL.getName());
    }

    public void setFillPaint(Paint fillPaint) {
        setValue(FILL.getName(), fillPaint);
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

    private class StrokeNuller implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().startsWith("stroke")) {
                stroke = null;
            }
        }
    }
}
