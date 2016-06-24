package org.esa.snap.core.gpf.descriptor.template;

import com.thoughtworks.xstream.annotations.XStreamAlias;
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
 * Base class for command line template holders.
 *
 * @author Cosmin Cara
 */
@XStreamAlias("template")
public class TemplateFile {

    private static final String defaultNamePattern = "template%s.%s";

    @XStreamAlias("file")
    protected String fileName;
    @XStreamOmitField
    protected String contents;

    @XStreamOmitField
    protected TemplateType type;
    @XStreamOmitField
    protected TemplateEngine engine;

    public static TemplateFile fromFile(String path) throws TemplateException, IOException {
        TemplateFile template = null;
        if (path != null) {
            File file = new File(path);
            String extension = FileUtils.getExtension(file);
            if (".vm".equalsIgnoreCase(extension)) {
                template = new TemplateFile(TemplateType.VELOCITY);
            } else if (".js".equalsIgnoreCase(extension)) {
                template = new TemplateFile(TemplateType.JAVASCRIPT);
            } else {
                throw new TemplateException("Unsupported template extension");
            }
            template.setFileName(file.getName());
            template.read();
        }
        return template;
    }

    public TemplateFile() { this(TemplateType.VELOCITY); }

    public TemplateFile(TemplateType templateType) {
        this.type = templateType;
    }

    public TemplateFile(TemplateEngine templateEngine) {
        this(templateEngine, null);
    }

    public TemplateFile(TemplateEngine templateEngine, String templateFileName) {
        this.engine = templateEngine;
        this.type = this.engine.getType();

        if (templateFileName != null) {
            setFileName(templateFileName);
        }
    }

    public void associateWith(TemplateEngine engine) throws TemplateException {
        if (engine == null) {
            throw new TemplateException("Null template engine");
        }
        if (this.type == null || engine.getType().equals(this.type)) {
            this.engine = engine;
        } else {
            throw new TemplateException("Wrong template engine type");
        }
    }

    public String getContents() throws IOException {
        if (this.contents == null) {
            read();
        }
        return contents;
    }

    public void setContents(String text, boolean shouldParse) throws TemplateException {
        String oldValue = this.contents;
        this.contents = text;
        try {
            if (shouldParse) {
                this.engine.parse(this);
            }
        } catch (TemplateException e) {
            this.contents = oldValue;
            throw e;
        }
    }

    public String getFileName() {
        if (fileName == null) {
            if (engine != null) {
                Path adapterPath = engine.getTemplateBasePath();
                fileName = adapterPath.getName(adapterPath.getNameCount() - 1) + getExtension();
            } else {
                fileName = String.format(defaultNamePattern, 1, getExtension());
            }
        }
        return fileName;
    }
    public void setFileName(String value) {
        String extension = FileUtils.getExtension(value);
        if (".vm".equalsIgnoreCase(extension)) {
            type = TemplateType.VELOCITY;
        } else if (".js".equalsIgnoreCase(extension)) {
            type = TemplateType.JAVASCRIPT;
        } else {
            throw new IllegalArgumentException("Unsupported file extension");
        }
        fileName = FileUtils.getFileNameFromPath(value);
    }

    public File getTemplatePath() {
        Path thisPath;
        if (this.engine != null) {
            Path adapterPath = engine.getTemplateBasePath();
            thisPath = adapterPath.resolve(getFileName());
        } else {
            thisPath = Paths.get(getFileName());
        }
        return thisPath.toFile();
    }

    public TemplateType getType() { return type; }

    public void setType(TemplateType value) { this.type = value; }

    public String getExtension() {
        String ext;
        switch (type) {
            case JAVASCRIPT:
                ext = "js";
                break;
            case VELOCITY:
            default:
                ext = "vm";
        }
        return ext;
    }

    private String read() throws IOException {
        if (contents == null) {
            Path templatePath = getTemplatePath().toPath();
            byte[] encoded = Files.readAllBytes(templatePath);
            contents = new String(encoded, Charset.defaultCharset());
        }
        return contents;
    }

    public void save() throws IOException {
        File path = getTemplatePath();
        if (path != null) {
            try (FileWriter writer = new FileWriter(path)) {
                writer.write(contents);
                writer.flush();
                writer.close();
            }
        }
    }

    public TemplateFile copy() throws IOException {
        TemplateFile newTemplate = new TemplateFile(this.type);
        newTemplate.fileName = this.fileName;
        newTemplate.contents = getContents();
        return newTemplate;
    }

    @Override
    public String toString() {
        return this.getFileName();
    }
}
