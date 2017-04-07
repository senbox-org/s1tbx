/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.core.gpf.descriptor.template;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.io.File;
import java.io.IOException;

/**
 * Class for simple in-memory template holders.
 *
 * @author Cosmin Cara
 */
@XStreamAlias("template")
public class MemoryTemplate implements Template {
    private static final String DEFAULT_NAME = "Command Template";

    @XStreamOmitField
    private TemplateType templateType;
    @XStreamOmitField
    private TemplateEngine engine;
    @XStreamOmitField
    private String name;
    @XStreamAlias("contents")
    private String contents;
    @XStreamAsAttribute
    private String type = "simple";


    public MemoryTemplate() {
        this.name = DEFAULT_NAME;
    }

    public MemoryTemplate(TemplateType templateType) {
        this.templateType = templateType;
        this.name = DEFAULT_NAME;
    }

    public MemoryTemplate(TemplateEngine templateEngine) {
        this(templateEngine, null);
    }

    public MemoryTemplate(TemplateEngine templateEngine, String templateName) {
        this.engine = templateEngine;
        this.templateType = this.engine.getType();
        if (templateName != null) {
            setName(templateName);
        } else {
            setName(DEFAULT_NAME);
        }
    }

    public void associateWith(TemplateEngine engine) throws TemplateException {
        if (engine == null) {
            throw new TemplateException("Null template engine");
        }
        if (this.templateType == null || engine.getType().equals(this.templateType)) {
            this.engine = engine;
        } else {
            throw new TemplateException("Wrong template engine type");
        }
    }

    public String getContents() throws IOException {
        return this.contents;
    }

    public void setContents(String text, boolean shouldParse) throws TemplateException {
        String oldValue = this.contents;
        this.contents = text;
        if (this.contents != null) {
            this.contents = this.contents.replace("\r", "");
        }
        try {
            if (shouldParse) {
                this.engine.parse(this);
            }
        } catch (TemplateException e) {
            this.contents = oldValue;
            throw e;
        }
    }

    public String getName() {
        return this.name == null ? DEFAULT_NAME : this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public TemplateType getType() {
        return templateType;
    }

    public void setType(TemplateType value) {
        templateType = value;
    }

    public boolean isInMemory() {
        return true;
    }

    public void save() throws IOException {
        // noop
    }

    @Override
    public File getPath() {
        return new File(getName());
    }

    public Template copy() throws IOException {
        MemoryTemplate newTemplate = new MemoryTemplate(this.templateType);
        newTemplate.contents = this.contents;
        return newTemplate;
    }
}
