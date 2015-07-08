package org.esa.snap.framework.gpf.descriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that tries to resolve (i.e. expand) the system variables values.
 *
 * @author Cosmin Cara
 */
public abstract class VariableResolver {

    private static Map<ToolAdapterOperatorDescriptor, VariableResolver> resolverMap = new HashMap<>();

    protected VariableResolver nextResolver;
    protected ToolAdapterOperatorDescriptor descriptor;

    public static VariableResolver newInstance(ToolAdapterOperatorDescriptor descriptor) {
        if (!resolverMap.containsKey(descriptor)) {
            VariableResolver simpleResolver = new SimpleVariableResolver(descriptor);
            VariableResolver systemPathResolver = new SystemPathVariableResolver(descriptor);
            simpleResolver.setNextResolver(systemPathResolver);
            resolverMap.put(descriptor, simpleResolver);
        }
        return resolverMap.get(descriptor);
    }

    VariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
        this.descriptor = operatorDescriptor;
    }

    public abstract String resolve(String input);
    public abstract File resolve(File input);
    protected void setNextResolver(VariableResolver resolver) {
        this.nextResolver = resolver;
    }
    protected boolean isValidFileName(String fileName) {
        File f = new File(fileName);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Resolver class for substituting variables with their values.
     */
    static class SimpleVariableResolver extends VariableResolver {

        SimpleVariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
            super(operatorDescriptor);
        }

        @Override
        public String resolve(String input) {
            String resolved = input;
            if (input != null) {
                Map<String, String> lookupVars = this.descriptor.getVariables().stream().collect(Collectors.toMap(SystemVariable::getKey, SystemVariable::getValue));
                for (String key : lookupVars.keySet()) {
                    resolved = resolved.replace("$" + key, lookupVars.get(key));
                }
                if (nextResolver != null && isValidFileName(resolved)) {
                    resolved = nextResolver.resolve(resolved);
                }
            }
            return resolved;
        }

        @Override
        public File resolve(File input) {
            File resolved = input;
            if (input != null && !input.exists()) {
                String expandedValue = input.getPath();
                Map<String, String> lookupVars = this.descriptor.getVariables().stream().collect(Collectors.toMap(SystemVariable::getKey, SystemVariable::getValue));
                for (String key : lookupVars.keySet()) {
                    expandedValue = expandedValue.replace("$" + key, lookupVars.get(key));
                }
                resolved = new File(expandedValue);
                if (nextResolver != null && !resolved.exists()) {
                    resolved = nextResolver.resolve(resolved);
                }
            }
            return resolved == null ? input : resolved;
        }
    }

    /**
     * Resolver class for expanding possible system paths.
     */
    static class SystemPathVariableResolver extends VariableResolver {

        private final String[] systemPath;

        SystemPathVariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
            super(operatorDescriptor);
            String sysPath = System.getenv("PATH");
            systemPath = sysPath.split(File.pathSeparator);
        }

        @Override
        public String resolve(String input) {
            String resolved = input;
            if (resolved != null) {
                File resolvedFile = resolve(new File(input));
                if (resolvedFile != null) {
                    resolved = resolvedFile.getAbsolutePath();
                }
            }
            return resolved;
        }

        @Override
        public File resolve(File input) {
            File resolved = null;
            if (input != null && !input.exists()) {
                for (String sysPath : systemPath) {
                    File current = new File(sysPath, input.getPath());
                    if (current.exists()) {
                        resolved = current;
                        break;
                    }
                }
                if (resolved == null && nextResolver != null) {
                    resolved = nextResolver.resolve(input);
                }
            } else {
                resolved = input;
            }
            return resolved == null ? input : resolved;
        }
    }
}
