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
import org.esa.snap.core.gpf.descriptor.template.TemplateEngine;
import org.esa.snap.core.gpf.descriptor.template.TemplateException;
import org.esa.snap.core.gpf.descriptor.template.TemplateFile;
import org.esa.snap.core.gpf.descriptor.template.TemplateType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ramona Manda
 */
@XStreamAlias("templateparameter")
public class TemplateParameterDescriptor extends ToolParameterDescriptor {
    @XStreamAlias("parameters")
    private List<ToolParameterDescriptor> parameterDescriptors = new ArrayList<>();
    private TemplateFile template;
    @XStreamOmitField
    private TemplateEngine engine;

    public TemplateParameterDescriptor(){
        super();
        setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(String name, Class<?> type){
        super(name, type);
        setParameterType(ToolAdapterConstants.REGULAR_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
    }

    public TemplateParameterDescriptor(ToolParameterDescriptor object) {
        super(object);
        setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
        this.parameterDescriptors = new ArrayList<>();
        this.template = new TemplateFile(TemplateType.VELOCITY);
    }

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
                this.template = new TemplateFile();
                try {
                    this.template.setContents("Error: " + ex.getMessage(), false);
                    if (this.engine != null) {
                        this.template.associateWith(this.engine);
                    }
                } catch (TemplateException e) {
                    SystemUtils.LOG.warning(e.getMessage());
                }
            }
        }
    }

    public void addParameterDescriptor(ToolParameterDescriptor descriptor){
        this.parameterDescriptors.add(descriptor);
    }

    public void removeParameterDescriptor(ToolParameterDescriptor descriptor){
        this.parameterDescriptors.remove(descriptor);
    }

    public List<ToolParameterDescriptor> getParameterDescriptors(){
        if(this.parameterDescriptors == null){
            this.parameterDescriptors = new ArrayList<>();
        }
        return this.parameterDescriptors;
    }

    public void setTemplateEngine(TemplateEngine engine) throws TemplateException {
        this.engine = engine;
        if (this.template != null) {
            this.template.associateWith(this.engine);
        }
    }

    public void setTemplate(TemplateFile template) throws TemplateException {
        this.template = template;
        this.template.associateWith(engine);
    }

    public TemplateFile getTemplate() {
        if (this.template == null) {
            if (engine != null) {
                this.template = new TemplateFile(engine.getType());
                try {
                    this.template.associateWith(engine);
                } catch (TemplateException e) {
                    e.printStackTrace();
                }
            } else {
                this.template = new TemplateFile(TemplateType.VELOCITY);
            }

        }
        return this.template;
    }

    public String executeTemplate() throws TemplateException {
        return executeTemplate(null);
    }

    public String executeTemplate(Map<String, Object> params) throws TemplateException {
        if (params == null) {
            params = new HashMap<>();
        }
        for (ToolParameterDescriptor param : getParameterDescriptors()) {
            params.put(param.getName(), param.getDefaultValue());
        }

        return this.engine.execute(this.template, params);
    }
}
