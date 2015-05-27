/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.framework.gpf.descriptor;

import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ramona Manda
 */
public class TemplateParameterDescriptor extends ToolParameterDescriptor {
    private List<ToolParameterDescriptor> toolParameterDescriptors = new ArrayList<>();

    public TemplateParameterDescriptor(){
        super();
        this.toolParameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(String name, Class<?> type){
        super(name, type);
        super.setParameterType(ToolAdapterConstants.REGULAR_PARAM_MASK);
        this.toolParameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(String name, Class<?> type, String parameterType){
        this(name, type);
        super.setParameterType(parameterType);
    }

    public TemplateParameterDescriptor(DefaultParameterDescriptor object, String parameterType) {
        super(object, parameterType);
        this.toolParameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(ToolParameterDescriptor object) {
        super(object, object.getParameterType());
        this.toolParameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(TemplateParameterDescriptor object) {
        super(object, object.getParameterType());
        this.toolParameterDescriptors = new ArrayList<>();
            for (ToolParameterDescriptor subparameter : object.getToolParameterDescriptors()) {
                this.toolParameterDescriptors.add(new TemplateParameterDescriptor(subparameter));
            }
    }

    public void addParameterDescriptor(ToolParameterDescriptor descriptor){
        this.toolParameterDescriptors.add(descriptor);
    }

    public void removeParameterDescriptor(ToolParameterDescriptor descriptor){
        this.toolParameterDescriptors.remove(descriptor);
    }

    public List<ToolParameterDescriptor> getToolParameterDescriptors(){
        if(this.toolParameterDescriptors == null){
            this.toolParameterDescriptors = new ArrayList<>();
        }
        return this.toolParameterDescriptors;
    }

    public String toXml(ClassLoader classLoader) {
        return createXStream(classLoader).toXML(this);
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param url         The URL pointing to a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static TemplateParameterDescriptor fromXml(URL url, ClassLoader classLoader) {
        String resourceName = url.toExternalForm();
        try {
            try (InputStreamReader streamReader = new InputStreamReader(url.openStream())) {
                TemplateParameterDescriptor operatorDescriptor;
                operatorDescriptor = fromXml(streamReader, resourceName, classLoader);
                return operatorDescriptor;
            }
        } catch (IOException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param file        The file containing a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static TemplateParameterDescriptor fromXml(File file, ClassLoader classLoader) throws OperatorException {
        String resourceName = file.getPath();
        try {
            try (FileReader reader = new FileReader(file)) {
                return TemplateParameterDescriptor.fromXml(reader, resourceName, classLoader);
            }
        } catch (IOException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
    }

    public static TemplateParameterDescriptor fromXml(Reader reader, String resourceName, ClassLoader classLoader) throws OperatorException {
        Assert.notNull(reader, "reader");
        Assert.notNull(resourceName, "resourceName");
        TemplateParameterDescriptor descriptor = new TemplateParameterDescriptor(ToolAdapterConstants.DEFAULT_PARAM_NAME, String.class);
        try {
            createXStream(classLoader).fromXML(reader, descriptor);
            if (StringUtils.isNullOrEmpty(descriptor.getName())) {
                throw new OperatorException(formatInvalidExceptionMessage(resourceName, "missing 'name' element"));
            }
        } catch (StreamException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
        return descriptor;
    }

    private static XStream createXStream(ClassLoader classLoader) {
        XStream xStream = new XStream();
        xStream.setClassLoader(classLoader);
        xStream.alias("parameter", TemplateParameterDescriptor.class);

        xStream.alias("parameter", TemplateParameterDescriptor.class);
        xStream.aliasField("toolParameterDescriptors", TemplateParameterDescriptor.class, "toolParameterDescriptors");

        return xStream;
    }

    private static String formatReadExceptionText(String resourceName, Exception e) {
        return String.format("Failed to read operator descriptor from '%s':\nError: %s", resourceName, e.getMessage());
    }

    private static String formatInvalidExceptionMessage(String resourceName, String message) {
        return String.format("Invalid operator descriptor in '%s': %s", resourceName, message);
    }
}
