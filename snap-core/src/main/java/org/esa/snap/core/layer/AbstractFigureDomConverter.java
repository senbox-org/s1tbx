/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.layer;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.snap.core.draw.AbstractFigure;
import org.esa.snap.core.draw.AreaFigure;
import org.esa.snap.core.draw.Figure;
import org.esa.snap.core.draw.LineFigure;
import org.esa.snap.core.draw.ShapeFigure;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @since BEAM 4.6
 */
public class AbstractFigureDomConverter implements DomConverter {

    @Override
    public Class<?> getValueType() {
        return AbstractFigure.class;
    }

    @Override
    public AbstractFigure convertDomToValue(DomElement parentElement, Object figure) throws ConversionException,
            ValidationException {
        final String className = parentElement.getAttribute("class");
        final Shape shapeObject = convertDomToShape(parentElement);

        Map<String, Object> attributes = convertDomToAttributes(parentElement);
        if (LineFigure.class.getName().equals(className)) {
            return new LineFigure(shapeObject, attributes);
        } else if (AreaFigure.class.getName().equals(className)) {
            return new AreaFigure(shapeObject, attributes);
        } else if (ShapeFigure.class.getName().equals(className)) {
            final DomElement dimensionalChild = parentElement.getChild("oneDimensional");
            final boolean oneDimensional = Boolean.valueOf(dimensionalChild.getValue());

            return new ShapeFigure(shapeObject, oneDimensional, attributes);
        }
        return null;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        final Class<?> valueType = value.getClass();
        parentElement.setAttribute("class", valueType.getName());
        
        AbstractFigure figure = (AbstractFigure) value;
        final Shape figureShape = figure.getShape();
        convertShapeToDom(figureShape, parentElement);

        final Map<String, Object> attributes = figure.getAttributes();
        convertAttributesToDom(attributes, parentElement);
        if (valueType == ShapeFigure.class) {
            ShapeFigure shapeFigure = (ShapeFigure) value;

            final DomElement dimensionalChild = parentElement.createChild("oneDimensional");
            dimensionalChild.setValue(String.valueOf(shapeFigure.isOneDimensional()));
        }

    }

    private void convertShapeToDom(Shape figureShape, DomElement parentElement) throws ConversionException {
        final DomElement shapeChild = parentElement.createChild("shape");
        shapeChild.setAttribute("class", figureShape.getClass().getName());
        final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
        domConverter.convertValueToDom(figureShape, shapeChild);
    }

    private Shape convertDomToShape(DomElement parentElement) throws ConversionException, ValidationException {
        final DomElement shapeChild = parentElement.getChild("shape");
        final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
        return (Shape) domConverter.convertDomToValue(shapeChild, null);
    }


    private void convertAttributesToDom(Map<String, Object> attributes, DomElement parentElement) throws
            ConversionException {
        final DomElement attrChild = parentElement.createChild("attributes");
        Boolean filled = (Boolean) attributes.get(Figure.FILLED_KEY);
        if (filled != null) {
            convertAttributeToDom(attrChild, "filled", filled);
        }
        Composite fillComposite = (Composite) attributes.get(Figure.FILL_COMPOSITE_KEY);
        if (fillComposite != null && fillComposite instanceof AlphaComposite) {
            AlphaComposite alphaComposite = (AlphaComposite) fillComposite;
            convertAttributeToDom(attrChild, "fillTransparency", alphaComposite.getAlpha());
        }
        Paint fillPaint = (Paint) attributes.get(Figure.FILL_PAINT_KEY);
        if (fillPaint != null && fillPaint instanceof Color) {
            convertAttributeToDom(attrChild, "fillColor", fillPaint);
        }
        Stroke fillStroke = (Stroke) attributes.get(Figure.FILL_STROKE_KEY);
        if (fillStroke != null && fillStroke instanceof BasicStroke) {
            BasicStrokeDomConverter strokeDomConverter = new BasicStrokeDomConverter();
            DomElement fillStrokeElement = attrChild.createChild("fillStroke");
            strokeDomConverter.convertValueToDom(fillStroke, fillStrokeElement);
        }
        Boolean outlined = (Boolean) attributes.get(Figure.OUTLINED_KEY);
        if (outlined != null) {
            convertAttributeToDom(attrChild, "outlined", outlined);
        }
        Paint outlinePaint = (Paint) attributes.get(Figure.OUTL_COLOR_KEY);
        if (outlinePaint != null && outlinePaint instanceof Color) {
            convertAttributeToDom(attrChild, "outlineColor", outlinePaint);
        }
        Composite outlineComposite = (Composite) attributes.get(Figure.OUTL_COMPOSITE_KEY);
        if (outlineComposite != null && outlineComposite instanceof AlphaComposite) {
            AlphaComposite alphaComposite = (AlphaComposite) outlineComposite;
            convertAttributeToDom(attrChild, "outlineTransparency", alphaComposite.getAlpha());
        }
        Stroke outlineStroke = (Stroke) attributes.get(Figure.OUTL_STROKE_KEY);
        if (outlineStroke != null && outlineStroke instanceof BasicStroke) {
            BasicStrokeDomConverter strokeDomConverter = new BasicStrokeDomConverter();
            DomElement outlineStrokeElement = attrChild.createChild("outlineStroke");
            strokeDomConverter.convertValueToDom(outlineStroke, outlineStrokeElement);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void convertAttributeToDom(DomElement parentChild, String elementName, Object attributeValue) {
        ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        Converter converter = converterRegistry.getConverter(attributeValue.getClass());
        DomElement domElement = parentChild.createChild(elementName);
        domElement.setValue(converter.format(attributeValue));
    }

    private Map<String, Object> convertDomToAttributes(DomElement parentElement) throws ConversionException, ValidationException {
        ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        final DomElement attrChild = parentElement.getChild("attributes");
        final HashMap<String, Object> attributes = new HashMap<String, Object>();

        DomElement filledElement = attrChild.getChild("filled");
        if (filledElement != null) {
            Converter converter = converterRegistry.getConverter(Boolean.class);
            attributes.put(Figure.FILLED_KEY, converter.parse(filledElement.getValue()));
        }
        DomElement fillTransparencyElement = attrChild.getChild("fillTransparency");
        if (fillTransparencyElement != null) {
            Converter converter = converterRegistry.getConverter(Float.class);
            Float transparency = (Float) converter.parse(fillTransparencyElement.getValue());
            attributes.put(Figure.FILL_COMPOSITE_KEY, AlphaComposite.SrcOver.derive(transparency));
        }
        DomElement fillColorElement = attrChild.getChild("fillColor");
        if (fillColorElement != null) {
            Converter converter = converterRegistry.getConverter(Color.class);
            attributes.put(Figure.FILL_PAINT_KEY, converter.parse(fillColorElement.getValue()));
        }
        DomElement fillStrokeElement = attrChild.getChild("fillStroke");
        if (fillStrokeElement != null) {
            BasicStrokeDomConverter strokeDomConverter = new BasicStrokeDomConverter();
            Object fillStroke = strokeDomConverter.convertDomToValue(fillStrokeElement, null);
            attributes.put(Figure.FILL_STROKE_KEY, fillStroke);
        }
        DomElement outlinedElement = attrChild.getChild("outlined");
        if (outlinedElement != null) {
            Converter converter = converterRegistry.getConverter(Boolean.class);
            attributes.put(Figure.OUTLINED_KEY, converter.parse(outlinedElement.getValue()));
        }
        DomElement outlineColorElement = attrChild.getChild("outlineColor");
        if (outlineColorElement != null) {
            Converter converter = converterRegistry.getConverter(Color.class);
            attributes.put(Figure.OUTL_COLOR_KEY, converter.parse(outlineColorElement.getValue()));
        }
        DomElement outlineTransparencyElement = attrChild.getChild("outlineTransparency");
        if (outlineTransparencyElement != null) {
            Converter converter = converterRegistry.getConverter(Float.class);
            Float transparency = (Float) converter.parse(outlineTransparencyElement.getValue());
            attributes.put(Figure.OUTL_COMPOSITE_KEY, AlphaComposite.SrcOver.derive(transparency));
        }
        DomElement outlineStrokeElement = attrChild.getChild("outlineStroke");
        if (outlineStrokeElement != null) {
            BasicStrokeDomConverter strokeDomConverter = new BasicStrokeDomConverter();
            Object outlineStroke = strokeDomConverter.convertDomToValue(outlineStrokeElement, null);
            attributes.put(Figure.OUTL_STROKE_KEY, outlineStroke);
        }
        return attributes;
    }

    private static class BasicStrokeDomConverter implements DomConverter {
        @Override
        public Class<?> getValueType() {
            return BasicStroke.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
            ConverterRegistry registry = ConverterRegistry.getInstance();

            DomElement widthElement = parentElement.getChild("width");
            Float width = registry.getConverter(Float.class).parse(widthElement.getValue());

            DomElement capElement = parentElement.getChild("cap");
            Integer cap = registry.getConverter(Integer.class).parse(capElement.getValue());

            DomElement joinElement = parentElement.getChild("join");
            Integer join = registry.getConverter(Integer.class).parse(joinElement.getValue());

            DomElement miterlimitElement = parentElement.getChild("miterlimit");
            Float miterlimit = registry.getConverter(Float.class).parse(miterlimitElement.getValue());

            DomElement dashElement = parentElement.getChild("dash");
            float[] dash = null;
            if(dashElement != null) {
                dash = registry.getConverter(float[].class).parse(dashElement.getValue());
            }

            DomElement dashPhaseElement = parentElement.getChild("dashPhase");
            Float dashPhase = registry.getConverter(Float.class).parse(dashPhaseElement.getValue());

            return new BasicStroke(width, cap, join, miterlimit, dash, dashPhase);
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            ConverterRegistry registry = ConverterRegistry.getInstance();
            BasicStroke basicStroke = BasicStroke.class.cast(value);

            DomElement widthElement = parentElement.createChild("width");
            widthElement.setValue(registry.getConverter(Float.class).format(basicStroke.getLineWidth()));

            DomElement capElement = parentElement.createChild("cap");
            capElement.setValue(registry.getConverter(Integer.class).format(basicStroke.getEndCap()));

            DomElement joinElement = parentElement.createChild("join");
            joinElement.setValue(registry.getConverter(Integer.class).format(basicStroke.getLineJoin()));

            DomElement miterlimitElement = parentElement.createChild("miterlimit");
            miterlimitElement.setValue(registry.getConverter(Float.class).format(basicStroke.getMiterLimit()));

            float[] dashArray = basicStroke.getDashArray();
            if (dashArray != null) {
                DomElement dashElement = parentElement.createChild("dash");
                dashElement.setValue(registry.getConverter(float[].class).format(dashArray));
            }

            DomElement dashPhaseElement = parentElement.createChild("dashPhase");
            dashPhaseElement.setValue(registry.getConverter(Float.class).format(basicStroke.getDashPhase()));
        }
    }
}
