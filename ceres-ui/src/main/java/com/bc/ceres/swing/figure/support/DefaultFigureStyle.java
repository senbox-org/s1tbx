package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

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

    public DefaultFigureStyle(Paint fillPaint, Paint strokePaint) {
        this("", fillPaint, strokePaint, null);
    }

    public DefaultFigureStyle(String name, Paint fillPaint, Paint strokePaint, Stroke stroke) {
        this(name, DEFAULT_STYLE);
        setFillPaint(fillPaint);
        setStrokePaint(strokePaint);
        setStroke(stroke);
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
            Object value = getValue(STROKE.getName());
            if (value != null) {
                Number number = (Number) getValue("stroke-width");
                if (number != null) {
                    float width = number.floatValue();
                    stroke = new BasicStroke(width);
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
                    Property property = new Property(prototype.getDescriptor(),
                                                     new MapEntryAccessor(values, prototype.getDescriptor().getName()));
                    addProperty(property);
                    setValue(property.getName(), prototype.getValue());
                } catch (IllegalAccessException e) {
                    throw new  IllegalStateException(e);
                }
            }
        }
        try {
            setDefaultValues();
        } catch (ValidationException e) {
            throw new  IllegalStateException(e);
        }
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

    private class StrokeNuller implements PropertyChangeListener {
        @Override
            public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().startsWith("stroke")) {
                DefaultFigureStyle.this.stroke = null;
            }
        }
    }
}
