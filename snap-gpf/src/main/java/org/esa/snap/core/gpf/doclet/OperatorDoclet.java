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

package org.esa.snap.core.gpf.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.util.io.WildcardMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

// This Main must be started with ceres launcher. Otherwise not all dependencies are on the classpath.
// Main class: org.esa.snap.runtime.Launcher
// VM options: -Dsnap.mainClass=org.esa.snap.core.gpf.doclet.OperatorDoclet
//             -Dsnap.home="<projectDir>\snap-desktop\snap-application\target\snap"
//             -Dsnap.extraClusters=<clusterPaths>
//             -Dsnap.debug=true
//             -Xmx2G
// Working dir: "<projectDir>\snap-desktop\snap-application\target\snap"
// classpath of module snap-gpf
//
// snap.extraClusters is not needed if only snap-engine shall be scanned




/**
 * A doclet which scans the classpath for GPF operators and creates
 * associated documentation from the {@link OperatorDescriptor} retrieved via the {@link OperatorSpi}.
 * <p>
 * This Doclet can be called on Windows from the command line
 * by the following instruction.
 * <b>NOTE:</b> You have to adopt the paths to your needs.
 * <p>
 * <pre>
 *  SET DocletClassName=OperatorDoclet
 *  SET SourcePath=.\beam-gpf\src\main\java
 *  SET ClassPath=.\beam-gpf\target\classes
 * </pre>
 * <p>
 * <pre>
 * javadoc -doclet "%DocletClassName%" -docletpath "%DocletPath%" ^
 *         -sourcepath "%SourcePath%" -classpath "%ClassPath%" ^
 *         org.esa.snap.gpf.operators.std
 * </pre>
 *
 * @author Norman Fomferra
 */
public class OperatorDoclet extends Doclet {

    private static String format;
    private static Path projectPath;

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: OperatorDoclet <projectPath> [ console | html ]");
            System.exit(1);
        }
        projectPath = Paths.get(args[0]);
        if (args.length == 2) {
            format = args[1];
        }

        Set<String> packageNameSet = getPackagesToConsider();
        File[] sourcePaths = WildcardMatcher.glob(projectPath.toString() + "/snap-engine/*/src/main/java");
        String sourcePathParam = concatPaths(sourcePaths);

        ArrayList<String> params = new ArrayList<>();
        params.add("-doclet");
        params.add(OperatorDoclet.class.getName());
        params.add("-sourcepath");
        params.add(sourcePathParam);
        params.addAll(packageNameSet);
        com.sun.tools.javadoc.Main.main(params.toArray(new String[0]));
    }

    private static String concatPaths(File[] sourcePaths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sourcePaths.length; i++) {
            sb.append(sourcePaths[i]);
            if (i < sourcePaths.length - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    private static Set<String> getPackagesToConsider() {
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        Set<OperatorSpi> operatorSpis = operatorSpiRegistry.getOperatorSpis();

        Set<String> packageNameSet = new TreeSet<>();
        for (OperatorSpi operatorSpi : operatorSpis) {
            String packageName = operatorSpi.getOperatorClass().getPackage().getName();
            packageNameSet.add(packageName);
        }
        return packageNameSet;
    }

    public static boolean start(RootDoc root) {
        OperatorHandler operatorHandler;
        if ("console".equalsIgnoreCase(format)) {
            operatorHandler = new OperatorHandlerConsole();
        } else if ("html".equalsIgnoreCase(format)) {
            Path outputDir = projectPath.resolve("target").resolve("operatorDoclet");
            try {
                Files.createDirectories(outputDir);
                operatorHandler = new OperatorHandlerHtml(outputDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create output directory: " + outputDir);
            }
        } else {
            throw new RuntimeException("Illegal output format: " + format);
        }

        try {
            operatorHandler.start(root);
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();

        ClassDoc[] classDocs = root.classes();
        for (ClassDoc classDoc : classDocs) {
            if (classDoc.subclassOf(root.classNamed(Operator.class.getName()))) {
                try {
                    System.out.println("Processing " + classDoc.typeName() + "...");
                    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<? extends Operator> type = (Class<? extends Operator>) contextClassLoader.loadClass(classDoc.qualifiedTypeName());
                    OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(OperatorSpi.getOperatorAlias(type));
                    if (operatorSpi != null) {
                        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
                        if (!operatorDescriptor.isInternal()) {
                            OperatorDesc operatorDesc = new OperatorDesc(type, classDoc, operatorDescriptor);
                            operatorHandler.processOperator(operatorDesc);
                        } else {
                            System.err.printf("Warning: Skipping %s because it is internal.%n", classDoc.typeName());
                        }
                    } else {
                        System.err.printf("No SPI found for operator class '%s'.%n", type.getName());
                    }
                } catch (Throwable e) {
                    System.err.println("Error: " + classDoc.typeName() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }

        try {
            operatorHandler.stop(root);
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }

        return true;
    }

    public static int optionLength(String optionName) {
        if (optionName.equals("format")) {
            return 1;
        }
        return 0;
    }

    public static boolean validOptions(String[][] options,
                                       DocErrorReporter docErrorReporter) {
        for (int i = 0; i < options.length; i++) {
            for (int j = 0; j < options[i].length; j++) {
                docErrorReporter.printWarning("options[" + i + "][" + j + "] = " + options[i][j]);
            }
        }
        return true;
    }

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }
}
