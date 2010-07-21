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

package com.bc.ceres.jai.opimage;


import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.RenderedOp;
import javax.media.jai.EnumeratedParameter;
import javax.media.jai.OperationDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.lang.reflect.Field;

/**
 * @see com.sun.media.jai.opimage.ConvolveOpImage
 */
public class XmlRIF implements RenderedImageFactory {

    /**
     * Constructor.
     */
    public XmlRIF() {
    }

    /**
     * Create a new instance of ConvolveOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock The source image and the convolution kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderingHints) {
        URI location = (URI) paramBlock.getObjectParameter(0);
        Map<String, Object> configuration = (Map<String, Object>) paramBlock.getObjectParameter(1);
        if (configuration == null) {
            configuration = new HashMap<String, Object>();
        }
        try {
            return create(location, configuration, renderingHints);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private RenderedImage create(URI location, Map<String, Object> configuration, RenderingHints renderingHints) throws JDOMException, IOException, IllegalArgumentException {
        configuration = new HashMap<String, Object>(configuration);

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(location.toURL());
        Element rootElement = document.getRootElement();
        Map<String, Element> sourceMap = getElementMap(rootElement, "source");
        Map<String, Element> parameterMap = getElementMap(rootElement, "parameter");
        Element targetElement = rootElement.getChild("target");
        return parseImage(targetElement, sourceMap, parameterMap, configuration, renderingHints, "rendered");
    }

    private RenderedOp parseImage(Element targetElement, Map<String, Element> definedSourceElements, Map<String, Element> definedParameterElements, Map<String, Object> configuration, RenderingHints renderHints, String modeName) {
        Element opElement = targetElement.getChild("op");
        String opName = opElement.getValue();

        ParameterBlockJAI parameterBlock = new ParameterBlockJAI(opName, modeName);

        parseSources(parameterBlock,
                     targetElement,
                     definedSourceElements,
                     definedParameterElements,
                     configuration,
                     renderHints);

        parseParameters(parameterBlock,
                        targetElement,
                        definedParameterElements,
                        configuration);

        return JAI.create(opName, parameterBlock, renderHints);
    }

    private void parseSources(ParameterBlockJAI parameterBlock,
                              Element targetElement,
                              Map<String, Element> definedSourceElements,
                              Map<String, Element> definedParameterElements,
                              Map<String, Object> configuration,
                              RenderingHints renderingHints) {
        List sourceElements = targetElement.getChildren("source");
        for (int i = 0; i < sourceElements.size(); i++) {
            Element sourceElement = (Element) sourceElements.get(i);
            String sourceName = sourceElement.getAttributeValue("name");
            String sourceId = sourceElement.getAttributeValue("refid");
            Object source;
            if (sourceId != null) {
                source = configuration.get(sourceId);
                if (source == null) {
                    Element definedSourceElement = definedSourceElements.get(sourceId);
                    if (definedSourceElement != null) {
                        source = parseImage(definedSourceElement,
                                            definedSourceElements,
                                            definedParameterElements,
                                            configuration,
                                            renderingHints,
                                            parameterBlock.getMode());
                        configuration.put(sourceId, source);
                    }
                }
            } else {
                source = parseImage(sourceElement,
                                    definedSourceElements,
                                    definedParameterElements,
                                    configuration,
                                    renderingHints,
                                    parameterBlock.getMode());
            }
            if (sourceName != null) {
                parameterBlock.setSource(sourceName, source);
            } else {
                parameterBlock.setSource(source, i);
            }
        }
    }

    private void parseParameters(ParameterBlockJAI parameterBlock,
                                 Element targetElement,
                                 Map<String, Element> definedParameterElements,
                                 Map<String, Object> configuration) {
        List parameterElements = targetElement.getChildren("parameter");
        for (int i = 0; i < parameterElements.size(); i++) {
            Element parameterElement = (Element) parameterElements.get(i);
            String parameterName = parameterElement.getAttributeValue("name");
            if (parameterName == null) {
                String[] paramNames = parameterBlock.getParameterListDescriptor().getParamNames();
                if (i < paramNames.length) {
                    parameterName = paramNames[i];
                } else {
                    throw new IllegalArgumentException(MessageFormat.format("Operation ''{0}'': Unknown parameter #{1}'", parameterBlock.getOperationDescriptor().getName(), i));
                }
            }
            String parameterId = parameterElement.getAttributeValue("refid");
            Object parameterValue;

            if (parameterId != null) {
                parameterValue = configuration.get(parameterId);
                if (parameterValue == null) {
                    Element definedParameterElement = definedParameterElements.get(parameterId);
                    parameterValue = parseParameterValue(parameterBlock,
                                                         parameterName,
                                                         definedParameterElement.getValue());
                    configuration.put(parameterId, parameterValue);
                }
            } else {
                parameterValue = parseParameterValue(parameterBlock,
                                                     parameterName,
                                                     parameterElement.getValue());
            }
            if (parameterName != null) {
                parameterBlock.setParameter(parameterName, parameterValue);
            } else {
                parameterBlock.add(parameterValue);
            }
        }
    }

    private static Map<String, Element> getElementMap(Element rootElement, String elementName) {
        Map<String, Element> elementMap = new HashMap<String, Element>();
        List elements = rootElement.getChildren(elementName);
        for (int i = 0; i < elements.size(); i++) {
            Element element = (Element) elements.get(i);
            String name = element.getAttributeValue("id");
            if (name == null) {
                throw new IllegalArgumentException(MessageFormat.format("Missing attribute ''id'' in element ''{0}''", elementName));
            }
            elementMap.put(name, element);
        }
        return elementMap;
    }

    private Object parseParameterValue(ParameterBlockJAI parameterBlock, String parameterName, String text) {
        ParameterListDescriptor descriptor = parameterBlock.getParameterListDescriptor();
        int parameterIndex = getParameterIndex(descriptor, parameterName);
        if (parameterIndex == -1) {
            throw new IllegalArgumentException(MessageFormat.format("Operation ''{0}'': Unknown parameter ''{1}''", parameterBlock.getOperationDescriptor().getName(), parameterName));
        }
        Class[] types = descriptor.getParamClasses();
        return parse(parameterBlock.getOperationDescriptor(), types[parameterIndex], text);
    }

    private int getParameterIndex(ParameterListDescriptor descriptor, String parameterName) {
        String[] names = descriptor.getParamNames();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(parameterName)) {
                return i;
            }
        }
        return -1;
    }

    private Object parse(OperationDescriptor operationDescriptor, Class type, String text) {
        if (type.equals(String.class)) {
            return text;
        } else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
            return Byte.parseByte(text);
        } else if (type.equals(Short.class) || type.equals(Short.TYPE)) {
            return Short.parseShort(text);
        } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
            return Integer.parseInt(text);
        } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
            return Long.parseLong(text);
        } else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
            return Float.parseFloat(text);
        } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
            return Double.parseDouble(text);
        } else if (type.equals(int[].class)) {
            return parseIntArray(text);
        } else if (type.equals(float[].class)) {
            return parseFloatArray(text);
        } else if (type.equals(double[].class)) {
            return parseDoubleArray(text);
        } else if (EnumeratedParameter.class.isAssignableFrom(type)) {
            try {
                Field field = operationDescriptor.getClass().getField(text);
                field.setAccessible(true);
                Object  value = field.get(operationDescriptor);
                return (EnumeratedParameter) value;
            } catch (Exception e) {
                throw new IllegalArgumentException("Enumerated value not found: " + text);
            }
        }
        throw new IllegalArgumentException("Unhandled type: " + type);
    }

    private int[] parseIntArray(String text) {
        String[] tokens = text.split(",");
        int[] value = new int[tokens.length];
        for (int i = 0; i < value.length; i++) {
            value[i] = Integer.parseInt(tokens[i]);
        }
        return value;
    }

    private float[] parseFloatArray(String text) {
        String[] tokens = text.split(",");
        float[] value = new float[tokens.length];
        for (int i = 0; i < value.length; i++) {
            value[i] = Float.parseFloat(tokens[i]);
        }
        return value;
    }

    private double[] parseDoubleArray(String text) {
        String[] tokens = text.split(",");
        double[] value = new double[tokens.length];
        for (int i = 0; i < value.length; i++) {
            value[i] = Double.parseDouble(tokens[i]);
        }
        return value;
    }

}