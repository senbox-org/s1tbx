/*
 * Copyright (C) 2014-2016 CS ROMANIA
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
package org.esa.snap.core.gpf.descriptor.template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A template engine represents an abstraction over concrete scripting engines.
 * The currently supported scripting engines are Apache Velocity and Javascript.
 *
 * @author Cosmin Cara
 */
public abstract class TemplateEngine<C> {

    private static final String LINE_SEPARATOR = "\r\n|\n";

    protected ToolAdapterOperatorDescriptor operatorDescriptor;
    protected TemplateContext<C> context;
    protected TemplateContext<C> lastContext;

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
    public abstract void parse(Template template) throws TemplateException;

    /**
     * Processes the given template.
     *
     * @param template      The template to be processed
     * @param parameters    Parameters to be passed to the template.
     * @return              If everything ok, the transformed template
     * @throws TemplateException
     */
    public abstract String execute(Template template, Map<String, Object> parameters) throws TemplateException;

    /**
     * Returns the type of the template. Can be either TemplateType.VELOCITY or TemplateType.JAVASCRIPT
     */
    public abstract TemplateType getType();

    /**
     * Returns the last execution context. The execution context would contain transformed parameter values.
     */
    public TemplateContext<C> getContext() {
        return this.context != null ? this.context : this.lastContext;
    }

    /**
     * Creates an instance of a template engine, of the given type, for the given descriptor.
     *
     * @param descriptor    The descriptor for which to create the engine
     * @param templateType  The template type
     * @return  An instance of a template engine
     */
    public static TemplateEngine createInstance(ToolAdapterOperatorDescriptor descriptor, TemplateType templateType) {
        return createInstance(descriptor, templateType, false);
    }

    /**
     * Creates an instance of a template engine.
     *
     * @param descriptor        The operator descriptor
     * @param templateType      The template type
     * @param stateful          If <code>true</code>, the scripting engine will be singleton-like.
     * @return
     */
    public static TemplateEngine createInstance(ToolAdapterOperatorDescriptor descriptor, TemplateType templateType, boolean stateful) {
        if (templateType == null) {
            throw new IllegalArgumentException("null template");
        }
        switch (templateType) {
            case JAVASCRIPT:
                return new JavascriptEngine(descriptor, stateful);
            case XSLT:
                return new XsltEngine(descriptor, stateful);
            case VELOCITY:
            default:
                return new ApacheVelocityEngine(descriptor, stateful);
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
    public List<String> getLines(Template template, Map<String, Object> parameters) throws TemplateException {
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
    static class ApacheVelocityEngine extends TemplateEngine<VelocityContext> {

        /**
         * Implementation wrapper for org.apache.velocity.VelocityContext
         */
        static class VelocityCtx extends TemplateContext<VelocityContext> {

            VelocityCtx(VelocityContext wrappedContext) {
                super(wrappedContext);
            }

            @Override
            public Object getValue(String name) {
                return this.context != null ? this.context.get(name) : null;
            }
        }

        private String macroTemplateContents;

        ApacheVelocityEngine(ToolAdapterOperatorDescriptor descriptor, boolean stateful) {
            super(descriptor);
            if (stateful) {
                context = new VelocityCtx(new VelocityContext());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("macros.vm")))) {
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                if (builder.length() > 0) {
                    builder.setLength(builder.length() - 1);
                    this.macroTemplateContents = builder.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void parse(Template template) throws TemplateException {
            VelocityEngine veloEngine = new VelocityEngine();
            if (!template.isInMemory()) {
                File templateFile = template.getPath();
                veloEngine.setProperty("file.resource.loader.path", templateFile.getParent());
            }
            List<SystemVariable> variables = operatorDescriptor.getVariables();
            for(SystemVariable variable : variables) {
                String variableValue = variable.getValue();
                if (variableValue == null) {
                    variableValue = "";
                }
                veloEngine.addProperty(variable.getKey(), variableValue);
            }
            veloEngine.init();
            boolean evalResult;
            try {
                String contents = template.getContents();
                evalResult = veloEngine.evaluate(new VelocityContext(), new StringWriter(), template.getName(), contents);
            } catch (Exception inner) {
                throw new TemplateException(inner);
            }
            if (!evalResult) {
                throw new TemplateException("Template evaluation failed");
            }
        }

        @Override
        public String execute(Template template, Map<String, Object> parameters) throws TemplateException {
            try {
                VelocityEngine veloEngine = new VelocityEngine();
                VelocityContext veloContext = context != null ? context.getContext() : new VelocityContext();
                if (!template.isInMemory()) {
                    File templateFile = template.getPath();
                    if (templateFile.getParent() != null) {
                        veloEngine.setProperty("file.resource.loader.path", templateFile.getParent());
                    }
                }
                List<SystemVariable> variables = operatorDescriptor.getVariables();
                for (SystemVariable variable : variables) {
                    String variableValue = variable.getValue();
                    if (variableValue == null) {
                        variableValue = "";
                    }
                    veloEngine.addProperty(variable.getKey(), variableValue);
                    veloContext.put(variable.getKey(), variableValue);
                }
                veloEngine.init();
                //Template veloTemplate = veloEngine.getTemplate(templateFile.getName());
                org.apache.velocity.Template veloTemplate = createTemplate(veloEngine, template);
                        //veloEngine.getTemplate(templateFile.getName());
                for (String key : parameters.keySet()) {
                    veloContext.put(key, parameters.get(key));
                }
                StringWriter writer = new StringWriter();
                veloTemplate.merge(veloContext, writer);
                lastContext = new VelocityCtx(veloContext);
                return writer.toString();
            } catch (Exception inner) {
                throw new TemplateException(inner);
            }
        }

        @Override
        public TemplateType getType() {
            return TemplateType.VELOCITY;
        }

        private org.apache.velocity.Template createTemplate(org.apache.velocity.app.VelocityEngine engine, Template internalTemplate) throws ParseException, IOException {
            org.apache.velocity.Template template;
            if (this.macroTemplateContents != null && !this.macroTemplateContents.isEmpty()) {
                RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
                String veloTemplate = this.macroTemplateContents + "\n" +
                        (internalTemplate.isInMemory() ?
                            internalTemplate.getContents() :
                            new String(Files.readAllBytes(internalTemplate.getPath().toPath())));
                StringReader reader = new StringReader(veloTemplate);
                SimpleNode node = runtimeServices.parse(reader, internalTemplate.getName());
                template = new org.apache.velocity.Template();
                template.setRuntimeServices(runtimeServices);
                template.setData(node);
                template.initDocument();
            } else {
                template = engine.getTemplate(internalTemplate.getName());
            }
            return template;
        }

    }

    /**
     * Implementation for the Nashorn Javascript engine.
     */
    static class JavascriptEngine extends TemplateEngine<ScriptContext> {

        /**
         * Implementation wrapper for javax.script.ScriptContext
         */
        static class JavaScriptCtx extends TemplateContext<ScriptContext> {

            JavaScriptCtx(ScriptContext wrappedContext) {
                super(wrappedContext);
            }

            @Override
            public Object getValue(String name) {
                return this.context != null ? this.context.getAttribute(name) : null;
            }
        }

        private ScriptEngine scriptEngine;

        JavascriptEngine(ToolAdapterOperatorDescriptor descriptor, boolean stateful) {
            super(descriptor);
            scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        }

        @Override
        public void parse(org.esa.snap.core.gpf.descriptor.template.Template template) throws TemplateException {
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
        public String execute(org.esa.snap.core.gpf.descriptor.template.Template template, Map<String, Object> parameters) throws TemplateException {
            String result;
            Bindings bindings = new SimpleBindings(parameters);
            scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            StringWriter writer = new StringWriter();
            ScriptContext context = scriptEngine.getContext();
            context.setWriter(writer);
            try {
                String contents = template.getContents();
                scriptEngine.eval(contents);
                lastContext = new JavaScriptCtx(scriptEngine.getContext());
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

    static class XsltEngine extends TemplateEngine<Transformer> {

        /**
         * Implementation wrapper for javax.script.ScriptContext
         */
        static class XsltContext extends TemplateContext<Transformer> {

            public XsltContext(Transformer transformer) {
                super(transformer);
            }

            @Override
            public Object getValue(String name) {
                return this.context != null ? this.context.getParameter(name) : null;
            }
        }

        XsltEngine(ToolAdapterOperatorDescriptor descriptor, boolean stateful) {
            super(descriptor);
        }

        @Override
        public void parse(org.esa.snap.core.gpf.descriptor.template.Template template) throws TemplateException {
            if (template == null) {
                throw new TemplateException("null template");
            }
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            try {
                SAXParser saxParser = saxParserFactory.newSAXParser();
                String templateContents = template.getContents();
                saxParser.parse(new InputSource(new StringReader(templateContents)), new DefaultHandler());
            } catch (Exception ex) {
                throw new TemplateException(ex);
            }
        }

        @Override
        public String execute(org.esa.snap.core.gpf.descriptor.template.Template template, Map<String, Object> parameters) throws TemplateException {
            String result;
            try {
                Source stringSource = new StreamSource(new StringReader(template.getContents()));
                this.context = new XsltContext(TransformerFactory.newInstance().newTransformer(stringSource));
                Transformer transformContext = this.context.getContext();
                if (parameters != null) {
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        if (entry.getValue() != null) {
                            transformContext.setParameter(entry.getKey(), entry.getValue());
                        }
                    }
                }
                transformContext.setOutputProperty("method", "xml");
                transformContext.setOutputProperty("indent", "yes");
                StringWriter writer = new StringWriter();
                transformContext.transform(new DOMSource(), new StreamResult(writer));
                result = writer.toString();
                this.lastContext = this.context;
            } catch (Exception ex) {
                throw new TemplateException(ex);
            }
            return result;
        }

        @Override
        public TemplateType getType() {
            return TemplateType.XSLT;
        }
    }
}
