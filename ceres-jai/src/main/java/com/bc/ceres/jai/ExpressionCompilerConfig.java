/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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