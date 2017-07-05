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
package org.esa.snap.core.gpf.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.core.gpf.descriptor.template.FileTemplate;
import org.esa.snap.core.gpf.descriptor.template.Template;
import org.esa.snap.core.gpf.descriptor.template.TemplateContext;
import org.esa.snap.core.gpf.descriptor.template.TemplateEngine;
import org.esa.snap.core.gpf.descriptor.template.TemplateException;
import org.esa.snap.core.gpf.descriptor.template.TemplateType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Specialization class for parameters based on a template, with their own parameters.
 *
 * @author Ramona Manda
 */
@XStreamAlias("templateparameter")
public class TemplateParameterDescriptor extends ToolParameterDescriptor {
    @XStreamAlias("parameters")
    private List<ToolParameterDescriptor> parameterDescriptors = new ArrayList<>();
    private Template template;
    @XStreamAlias("outputFile")
    private File outputFile;
    @XStreamOmitField
    private TemplateEngine engine;
    @XStreamOmitField
    private Logger logger = Logger.getLogger(TemplateParameterDescriptor.class.getName());

    public TemplateParameterDescriptor(){
        super();
        setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(String name, Class<?> type){
        super(name, type);
        setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
    }

    /**
     * Conversion constructor from a regular parameter
     *
     * @param object    The source parameter
     */
    public TemplateParameterDescriptor(ToolParameterDescriptor object) {
        super(object);
        setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
        this.template = new FileTemplate(TemplateType.VELOCITY);
    }

    /**
     * Copy constructor
     *
     * @param object    The template parameter to be copied.
     */
    public TemplateParameterDescriptor(TemplateParameterDescriptor object) {
        super(object, object.getParameterType());
        this.parameterDescriptors = new ArrayList<>();
        this.parameterDescriptors.addAll(object.getParameterDescriptors().stream().map(ToolParameterDescriptor::new).collect(Collectors.toList()));
        this.engine = object.engine;
        if (object.template != null) {
            try {
                this.template = object.template.copy();
                if (this.engine != null) {
                    this.template.associateWith(this.engine);
                }
            } catch (IOException | TemplateException ex) {
                this.template = new FileTemplate();
                try {
                    this.template.setContents("Error: " + ex.getMessage(), false);
                    if (this.engine != null) {
                        this.template.associateWith(this.engine);
                    }
                } catch (TemplateException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
        this.outputFile = object.outputFile;
    }

    /**
     * Adds a parameter descriptor to this template parameter
     */
    public void addParameterDescriptor(ToolParameterDescriptor descriptor){
        this.parameterDescriptors.add(descriptor);
    }

    /**
     * Removes a parameter descriptor from this template parameter
     */
    public void removeParameterDescriptor(ToolParameterDescriptor descriptor){
        this.parameterDescriptors.remove(descriptor);
    }

    /**
     * Returns the list of the parameter descriptors of this instance
     */
    public List<ToolParameterDescriptor> getParameterDescriptors(){
        if(this.parameterDescriptors == null){
            this.parameterDescriptors = new ArrayList<>();
        }
        return this.parameterDescriptors;
    }

    /**
     * Sets the scripting engine for this parameter and associates its template, if any,
     * with the engine.
     *
     * @param engine                The scripting engine
     * @throws TemplateException
     */
    public void setTemplateEngine(TemplateEngine engine) throws TemplateException {
        this.engine = engine;
        if (this.template != null) {
            this.template.associateWith(this.engine);
        }
    }

    /**
     * Returns the last scripting context resulted from the execution of the scripting engine
     */
    public TemplateContext getLastContext() {
        return (this.engine != null ? this.engine.getContext() : null);
    }

    /**
     * Sets the template of this parameter
     *
     * @param template              The template object
     * @throws TemplateException
     */
    public void setTemplate(Template template) throws TemplateException {
        this.template = template;
        this.template.associateWith(engine);
    }

    /**
     * Returns the template of this object. If it has none, a new template will be created
     * and returned.
     */
    public Template getTemplate() {
        if (this.template == null) {
            if (engine != null) {
                this.template = new FileTemplate(engine.getType());
                try {
                    this.template.associateWith(engine);
                } catch (TemplateException e) {
                    e.printStackTrace();
                }
            } else {
                this.template = new FileTemplate(TemplateType.VELOCITY);
            }

        }
        return this.template;
    }

    /**
     * Transforms (or executes) the template of this instance given a set of parameters.
     *
     * @param params                A collection of parameter values
     * @return                      The transformed template
     * @throws TemplateException
     */
    public String executeTemplate(Map<String, Object> params) throws TemplateException {
        if (params == null) {
            params = new HashMap<>();
        }
        for (ToolParameterDescriptor param : getParameterDescriptors()) {
            //if the parameter is already in the map paraemter,
            // the new value gets ignored, since the one from the parameter is supposed to be the user one,
            // while the parameter one is the default value
            if(!params.containsKey(param.getName())) {
                params.put(param.getName(), param.getDefaultValue());
            }
        }

        return this.engine.execute(this.template, params);
    }

    /**
     *
     * @return the output file of this template parameter, after the template is parsed
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Sets the output file of this template parameter, after the template is parsed.
     * The output file path may be a relative one or an absolute one
     * @param outputFile output file of this template parameter
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void copyFrom(TemplateParameterDescriptor source) {
        super.copyFrom(source);
        try {
            setTemplateEngine(source.engine);
            Template sourceTemplate = source.getTemplate();
            if (sourceTemplate != null) {
                setTemplate(sourceTemplate.copy());
            }
        } catch (TemplateException | IOException e) {
            logger.warning(e.getMessage());
        }
        setOutputFile(source.getOutputFile());
        if (this.parameterDescriptors == null) {
            this.parameterDescriptors = new ArrayList<>();
        } else {
            this.parameterDescriptors.clear();
        }
        List<ToolParameterDescriptor> sourceParameterDescriptors = source.getParameterDescriptors();
        if (sourceParameterDescriptors != null) {
            for (ToolParameterDescriptor sourceDescriptor : sourceParameterDescriptors) {
                this.parameterDescriptors.add(new ToolParameterDescriptor(sourceDescriptor));
            }
        }
    }
}
