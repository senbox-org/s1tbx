package org.esa.beam.scripting.visat;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

public class ScriptManager {
    private final PrintWriter output;
    private final ClassLoader classLoader;
    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;

    public ScriptManager(PrintWriter output, ClassLoader classLoader) {
        this.output = output;
        this.classLoader = classLoader;
    }

    public ScriptManager(PrintWriter output, ClassLoader classLoader, ScriptEngineManager scriptEngineManager) {
        this.output = output;
        this.classLoader = classLoader;
        this.scriptEngineManager = scriptEngineManager;
    }

    public PrintWriter getOutput() {
        return output;
    }

    public ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {
            ClassLoader oldClassLoader = setContextClassLoader();
            try {
                scriptEngineManager = new ScriptEngineManager(classLoader);
            } finally {
                setContextClassLoader(oldClassLoader);
            }
        }
        return scriptEngineManager;
    }

    public ScriptEngineFactory[] getAvailableScriptEngineFactories() {
        List<ScriptEngineFactory> scriptEngineFactoryList = getScriptEngineManager().getEngineFactories();
        return scriptEngineFactoryList.toArray(new ScriptEngineFactory[scriptEngineFactoryList.size()]);
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        if (this.scriptEngine != scriptEngine) {
            this.scriptEngine = scriptEngine;

            final ClassLoader oldClassLoader = setContextClassLoader();
            try {
                initScriptEngine();
            } finally {
                setContextClassLoader(oldClassLoader);
            }
        }
    }

    public Object evalScriptCode(String code) throws ScriptException {
        checkState();
        final ClassLoader oldClassLoader = setContextClassLoader();
        try {
            return scriptEngine.eval(code);
        } finally {
            setContextClassLoader(oldClassLoader);
        }
    }

    public Object evalScriptCode(Reader reader) throws ScriptException {
        checkState();
        final ClassLoader oldClassLoader = setContextClassLoader();
        try {
            return scriptEngine.eval(reader);
        } finally {
            setContextClassLoader(oldClassLoader);
        }
    }

    public void evalScript(URL initScriptURL) throws IOException, ScriptException {
        final Reader reader = new InputStreamReader(initScriptURL.openStream());
        try {
            evalScriptCode(reader);
        } finally {
            reader.close();
        }
    }

    private void initScriptEngine() {

        ScriptContext context = new SimpleScriptContext();
        context.setWriter(output);
        context.setErrorWriter(output);

        scriptEngine.setContext(context);
        scriptEngine.put("out", output);
        scriptEngine.put("err", output);

        output.println(MessageFormat.format("Script language set to {0}.", scriptEngine.getFactory().getLanguageName()));

        final URL initScriptURL = findInitScript();
        if (initScriptURL != null) {
            try {
                output.println(MessageFormat.format("Loading initialisation script ''{0}''...", initScriptURL));
                evalScript(initScriptURL);
                output.println("Initialisation script loaded. BEAM-specific language extensions are enabled.");
            } catch (Throwable t) {
                output.println("Failed to load initialisation script. " +
                        "BEAM-specific language extensions are disabled.");
                t.printStackTrace(output);
            }
        } else {
            output.println("No initialisation script found. " +
                    "BEAM-specific language extensions are disabled.");
        }
    }

    private URL findInitScript() {
        String cl = getClass().getSimpleName();
        String ln = scriptEngine.getFactory().getLanguageName();
        URL url = findInitScript(cl + "_" + ln + ".%s");
        if (url == null) {
            return findInitScript(cl + ".%s");
        }
        return null;
    }

    private URL findInitScript(String pattern) {
        for (String extension : scriptEngine.getFactory().getExtensions()) {
            URL resource = getClass().getResource(String.format(pattern, extension));
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private void checkState() {
        if (scriptEngine == null) {
            throw new IllegalStateException("scriptEngine == null");
        }
    }

    private ClassLoader setContextClassLoader() {
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        setContextClassLoader(classLoader);
        return oldClassLoader;
    }

    private void setContextClassLoader(ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
    }

}
