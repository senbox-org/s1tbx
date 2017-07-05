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
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class for file template holders.
 *
 * @author Cosmin Cara
 */
@XStreamAlias("template")
public class FileTemplate implements Template {

    private static final String defaultNamePattern = "template%s.%s";
    private static int counter = 1;

    @XStreamOmitField
    private TemplateType templateType;
    @XStreamOmitField
    private TemplateEngine engine;
    @XStreamAlias("file")
    private String fileName;
    @XStreamOmitField
    private String contents;
    @XStreamAsAttribute
    private String type = "file";

    static Template fromFile(String path) throws TemplateException, IOException {
        FileTemplate template = null;
        if (path != null) {
            File file = new File(path);
            String extension = FileUtils.getExtension(file);
            TemplateType templateType = FileTemplate.getTypeByExtension(extension);
            if (templateType != null) {
                template = new FileTemplate(templateType);
            } else {
                throw new TemplateException("Unsupported template extension");
            }
            template.setName(file.getName());
            template.read();
        }
        return template;
    }

    public FileTemplate() { super(); }

    public FileTemplate(TemplateType templateType) {
        this.templateType = templateType;
    }

    public FileTemplate(TemplateEngine templateEngine) {
        this(templateEngine, null);
    }

    public FileTemplate(TemplateEngine templateEngine, String templateFileName) {
        this.engine = templateEngine;
        this.templateType = this.engine.getType();
        if (templateFileName != null) {
            setName(templateFileName);
        }
    }

    @Override
    public String getContents() throws IOException {
        if (this.contents == null) {
            read();
        }
        return this.contents;
    }

    @Override
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

    @Override
    public String getName() {
        if (fileName == null) {
            /*if (engine != null) {
                Path adapterPath = engine.getTemplateBasePath();
                fileName = adapterPath.getName(adapterPath.getNameCount() - 1) + "." + getExtension();
            } else {*/
                fileName = String.format(defaultNamePattern, counter++, getExtension());
            /*}*/
        }
        return fileName;
    }
    @Override
    public void setName(String value) {
        String extension = FileUtils.getExtension(value);
        templateType = getTypeByExtension(extension);
        if (templateType == null) {
            throw new IllegalArgumentException("Unsupported file extension");
        }
        fileName = FileUtils.getFileNameFromPath(value);
    }

    @Override
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

    @Override
    public File getPath() {
        Path thisPath;
        if (this.engine != null) {
            Path adapterPath = engine.getTemplateBasePath();
            thisPath = adapterPath.resolve(getName());
        } else {
            thisPath = Paths.get(getName());
        }
        return thisPath.toFile();
    }

    @Override
    public TemplateType getType() {
        if (templateType == null) {
            if (fileName != null) {
                templateType = getTypeByExtension(FileUtils.getExtension(fileName));
            } else {
                templateType = TemplateType.VELOCITY;
            }
        }
        return templateType;
    }

    @Override
    public void setType(TemplateType value) {
        this.templateType = value;
    }

    @Override
    public boolean isInMemory() { return false; }

    @Override
    public void save() throws IOException {
        File path = getPath();
        if (path != null) {
            try (FileWriter writer = new FileWriter(path)) {
                writer.write(getContents());
                writer.flush();
                writer.close();
            }
        }
    }

    @Override
    public Template copy() throws IOException {
        FileTemplate newTemplate = new FileTemplate(this.templateType);
        newTemplate.fileName = this.fileName;
        newTemplate.contents = getContents();
        return newTemplate;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    private String getExtension() {
        String ext;
        switch (getType()) {
            case JAVASCRIPT:
                ext = "js";
                break;
            case XSLT:
                ext = "xsl";
                break;
            case VELOCITY:
            default:
                ext = "vm";
        }
        return ext;
    }

    private static TemplateType getTypeByExtension(String extension) {
        TemplateType templateType = null;
        if (extension != null && !extension.isEmpty()) {
            extension = extension.toLowerCase();
            switch (extension) {
                case ".vm":
                    templateType = TemplateType.VELOCITY;
                    break;
                case ".js":
                    templateType = TemplateType.JAVASCRIPT;
                    break;
                case ".xsl":
                    templateType = TemplateType.XSLT;
                    break;
            }
        }
        return templateType;
    }

    private String read() throws IOException {
        if (this.contents == null) {
            Path templatePath = getPath().toPath();
            byte[] encoded = Files.readAllBytes(templatePath);
            this.contents = new String(encoded, Charset.defaultCharset());
        }
        return this.contents;
    }
}
