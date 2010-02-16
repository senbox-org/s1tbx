package com.bc.ceres.jai;

import java.io.File;

/**
 * The configuration for the Java expression compiler.
 * This class is used as parameter for the
 * {@link com.bc.ceres.jai.operator.ExpressionDescriptor expression} operation.
 */
public class ExpressionCompilerConfig {
    private File outputDir;
    private File[] classPath;

    public ExpressionCompilerConfig() {
        this(new File("."), new File[]{new File(".")});
    }

    public ExpressionCompilerConfig(File outputDir, File[] classPath) {
        this.outputDir = outputDir;
        this.classPath = classPath;
    }

    public File[] getClassPath() {
        return classPath;
    }

    public void setClassPath(File[] classPath) {
        this.classPath = classPath;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
}