package com.bc.ceres.compiler;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * A Java code compiler.
 */
public class CodeCompiler {
    private final JavaCompiler compiler;
    private final File outputDir;
    private final File[] classPath;

    public CodeCompiler(File outputDir, File[] classPath) {
        this(ToolProvider.getSystemJavaCompiler(), outputDir, classPath);
    }

    public CodeCompiler(JavaCompiler compiler, File outputDir, File[] classPath) {
        this.compiler = compiler;
        this.outputDir = outputDir;
        this.classPath = classPath.clone();
    }

    public Class<?> compile(String packageName, String className, String code) throws IOException, ClassNotFoundException {
        return compile(new Code(packageName + '.' + className, code));
    }

    public Class<?> compile(Code code) throws IOException, ClassNotFoundException {
        final boolean status = performCompilerTask(code);
        if (!status) {
            // todo - include compiler error info (nf, 01.10.2008)
            throw new RuntimeException("Code compilation failed.");
        }
        URL[] urls = new URL[classPath.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = classPath[i].toURI().toURL();
        }
        URLClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        return loader.loadClass(code.getClassName());
    }

    private boolean performCompilerTask(JavaFileObject... source) throws IOException {
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        outputDir.mkdirs();
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputDir));
        fileManager.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(classPath));
        final JavaCompiler.CompilationTask task = compiler.getTask(null, // todo - specify writer
                                                                   fileManager,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   Arrays.asList(source));
        return task.call();
    }
}
