package org.esa.snap.core.gpf.descriptor.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A template engine represents an abstraction over concrete scripting engines.
 * The currently supported scripting engines are Apache Velocity and Javascript.
 *
 * @author Cosmin Cara
 */
public abstract class TemplateEngine {

    private static final String LINE_SEPARATOR = "\r\n|\n";

    protected ToolAdapterOperatorDescriptor operatorDescriptor;

    protected TemplateEngine(ToolAdapterOperatorDescriptor descriptor) {
        this.operatorDescriptor = descriptor;
    }

    /**
     * Parses the given template without processing it.
     * This method should be used for verifying the syntactical correctness of the template.
     *
     * @param template  The template to parse
     * @throws TemplateException
     */
    public abstract void parse(TemplateFile template) throws TemplateException;

    /**
     * Processes the given template.
     *
     * @param template      The template to be processed
     * @param parameters    Parameters to be passed to the template.
     * @return              If everything ok, the transformed template
     * @throws TemplateException
     */
    public abstract String execute(TemplateFile template, Map<String, Object> parameters) throws TemplateException;

    /**
     * Returns the type of the template. Can be either TemplateType.VELOCITY or TemplateType.JAVASCRIPT
     */
    public abstract TemplateType getType();

    /**
     * Creates an instance of a template engine, of the given type, for the given descriptor.
     *
     * @param descriptor    The descriptor for which to create the engine
     * @param templateType  The template type
     * @return  An instance of a template engine
     */
    public static TemplateEngine createInstance(ToolAdapterOperatorDescriptor descriptor, TemplateType templateType) {
        if (templateType == null) {
            throw new IllegalArgumentException("null template");
        }
        switch (templateType) {
            case JAVASCRIPT:
                return new JavascriptEngine(descriptor);
            case VELOCITY:
            default:
                return new VelocityEngine(descriptor);
        }
    }

    /**
     * Returns the contents of the processed template as a list of strings (lines).
     *
     * @param template      The template to be processed
     * @param parameters    Parameters to be passed to the template engine
     * @return              A list of strings representing the lines of the processed template
     * @throws TemplateException
     */
    public List<String> getLines(TemplateFile template, Map<String, Object> parameters) throws TemplateException {
        if (template == null) {
            throw new IllegalArgumentException("null template");
        }
        String result = execute(template, parameters);
        return Arrays.asList(result.split(LINE_SEPARATOR));
    }

    /**
     * Returns the absolute path of the template file.
     */
    public Path getTemplateBasePath() {
        Path adapterPath = null;
        if (this.operatorDescriptor != null) {
            adapterPath = ToolAdapterIO.getAdaptersPath().resolve(this.operatorDescriptor.getAlias());
        }
        return adapterPath;
    }

    /**
     * Implementation for Apache Velocity engine.
     */
    static class VelocityEngine extends TemplateEngine {

        private String macroTemplateContents;

        VelocityEngine(ToolAdapterOperatorDescriptor descriptor) {
            super(descriptor);
            try {
                this.macroTemplateContents = new String(Files.readAllBytes(Paths.get(getClass().getResource("macros.vm").toURI())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void parse(TemplateFile template) throws TemplateException {
            org.apache.velocity.app.VelocityEngine veloEngine = new org.apache.velocity.app.VelocityEngine();
            File templateFile = template.getTemplatePath();
            veloEngine.setProperty("file.resource.loader.path", templateFile.getParent());
            List<SystemVariable> variables = operatorDescriptor.getVariables();
            for(SystemVariable variable : variables) {
                veloEngine.addProperty(variable.getKey(), variable.getValue());
            }
            veloEngine.init();
            boolean evalResult = false;
            try {
                String contents = template.getContents();
                evalResult = veloEngine.evaluate(new VelocityContext(), new StringWriter(), templateFile.getName(), contents);
            } catch (Exception inner) {
                throw new TemplateException(inner);
            }
            if (!evalResult) {
                throw new TemplateException("Template evaluation failed");
            }
        }

        @Override
        public String execute(TemplateFile template, Map<String, Object> parameters) throws TemplateException {
            try {
                org.apache.velocity.app.VelocityEngine veloEngine = new org.apache.velocity.app.VelocityEngine();
                VelocityContext veloContext = new VelocityContext();
                File templateFile = template.getTemplatePath();
                veloEngine.setProperty("file.resource.loader.path", templateFile.getParent());
                List<SystemVariable> variables = operatorDescriptor.getVariables();
                for (SystemVariable variable : variables) {
                    veloEngine.addProperty(variable.getKey(), variable.getValue());
                    veloContext.put(variable.getKey(), variable.getValue());
                }
                veloEngine.init();
                //Template veloTemplate = veloEngine.getTemplate(templateFile.getName());
                Template veloTemplate = createTemplate(veloEngine, templateFile); //veloEngine.getTemplate(templateFile.getName());
                for (String key : parameters.keySet()) {
                    veloContext.put(key, parameters.get(key));
                }
                StringWriter writer = new StringWriter();
                veloTemplate.merge(veloContext, writer);
                return writer.toString();
            } catch (Exception inner) {
                throw new TemplateException(inner);
            }
        }

        @Override
        public TemplateType getType() {
            return TemplateType.VELOCITY;
        }

        private Template createTemplate(org.apache.velocity.app.VelocityEngine engine, File templateFile) throws ParseException, IOException {
            Template template;
            if (this.macroTemplateContents != null && !this.macroTemplateContents.isEmpty()) {
                RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
                String veloTemplate = this.macroTemplateContents + "\n" +
                        new String(Files.readAllBytes(templateFile.toPath()));
                StringReader reader = new StringReader(veloTemplate);
                SimpleNode node = runtimeServices.parse(reader, templateFile.getName());
                template = new Template();
                template.setRuntimeServices(runtimeServices);
                template.setData(node);
                template.initDocument();
            } else {
                template = engine.getTemplate(templateFile.getName());
            }
            return template;
        }
    }

    /**
     * Implementation for the Nashorn Javascript engine.
     */
    static class JavascriptEngine extends TemplateEngine {

        private ScriptEngine scriptEngine;

        JavascriptEngine(ToolAdapterOperatorDescriptor descriptor) {
            super(descriptor);
            scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        }

        @Override
        public void parse(TemplateFile template) throws TemplateException {
            try {
                if (template == null) {
                    throw new ScriptException("null template");
                }
                String contents = template.getContents();
                scriptEngine.eval(contents);
            } catch (ScriptException | IOException ex) {
                throw new TemplateException(ex);
            }
        }

        @Override
        public String execute(TemplateFile template, Map<String, Object> parameters) throws TemplateException {
            String result;
            Bindings bindings = new SimpleBindings(parameters);
            scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            StringWriter writer = new StringWriter();
            scriptEngine.getContext().setWriter(writer);
            try {
                String contents = template.getContents();
                scriptEngine.eval(contents);
                result = writer.toString();
            } catch (ScriptException | IOException e) {
                throw new TemplateException(e);
            }
            return result;
        }

        @Override
        public TemplateType getType() {
            return TemplateType.JAVASCRIPT;
        }
    }

}
