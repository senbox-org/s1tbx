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

package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;

// This Main must be started with ceres launcher. Otherwise not all dependencies are on the classpath.
// Main class: com.bc.ceres.launcher.Launcher
// VM options: -Dceres.context=beam -Dbeam.mainClass=org.esa.beam.framework.gpf.doclet.OperatorDoclet


/**
 * A doclet which scans the classpath for GPF operators and creates
 * associated documentation from the {@link OperatorDescriptor} retrieved via the {@link OperatorSpi}.
 * <p/>
 * This Doclet can be called on Windows from the command line
 * by the following instruction.
 * <b>NOTE:</b> You have to adopt the pathes to your needs.
 * <p/>
 * <pre>
 *  SET DocletClassName=org.esa.beam.framework.gpf.doclet.OperatorDoclet
 *  SET SourcePath=.\beam-gpf\src\main\java
 *  SET ClassPath=.\beam-gpf\target\classes
 * </pre>
 * <p/>
 * <pre>
 * javadoc -doclet "%DocletClassName%" -docletpath "%DocletPath%" ^
 *         -sourcepath "%SourcePath%" -classpath "%ClassPath%" ^
 *         org.esa.beam.gpf.operators.std
 * </pre>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class OperatorDoclet extends Doclet {

    static String format;

    public static void main(String[] args) {
        if (args.length == 0) {
            format = "console";
        } else if (args.length == 1) {
            format = args[0];
        } else {
            System.out.println("Usage: OperatorDoclet [ console | html ]");
            System.exit(1);
        }

        com.sun.tools.javadoc.Main.main(new String[]{
                "-doclet", OperatorDoclet.class.getName(),
                "-sourcepath", "" +
                               "./beam-gpf/src/main/java;" +
                               "./beam-aatsr-sst/src/main/java;" +
                               "./beam-binning/src/main/java;" +
                               "./beam-cluster-analysis/src/main/java;" +
                               "./beam-collocation/src/main/java;" +
                               "./beam-flhmci/src/main/java;" +
                               "./beam-meris-radiometry/src/main/java;" +
                               "./beam-meris-smac/src/main/java;" +
                               "./beam-meris-cloud/src/main/java;" +
                               "./beam-pixel-extraction/src/main/java;" +
                               "./beam-statistics-op/src/main/java;" +
                               "./beam-temporal-percentile-op/src/main/java;" +
                               "./beam-ndvi/src/main/java;" +
                               "./beam-unmix/src/main/java",

                "-classpath", "" +
                              "./modules/beam-core-5.0.1;" +
                              "./modules/beam-gpf-5.0;" +
                              "./modules/beam-aatsr-sst-5.0;" +
                              "./modules/beam-binning-5.0.1;" +
                              "./modules/beam-cluster-analysis-5.0;" +
                              "./modules/beam-collocation-5.0;" +
                              "./modules/beam-flhmci-5.0;" +
                              "./modules/beam-meris-radiometry-5.0;" +
                              "./modules/beam-meris-smac-5.0;" +
                              "./modules/beam-meris-cloud-5.0;" +
                              "./modules/beam-pixel-extraction-5.0;" +
                              "./modules/beam-statistics-op-5.0;" +
                              "./modules/beam-temporal-percentile-op-5.0;" +
                              "./modules/beam-ndvi-5.0;" +
                              "./modules/beam-unmix-5.0",

                "org.esa.beam.gpf.operators.standard",
                "org.esa.beam.gpf.operators.standard.reproject",
                "org.esa.beam.gpf.operators.meris",
                "org.esa.beam.aatsr.sst",
                "org.esa.beam.binning.operator",
                "org.esa.beam.cluster",
                "org.esa.beam.collocation",
                "org.esa.beam.processor.flh_mci",
                "org.esa.beam.meris.radiometry",
                "org.esa.beam.smac",
                "org.esa.beam.operator.cloud",
                "org.esa.beam.pixex",
                "org.esa.beam.statistics",
                "org.esa.beam.statistics.percentile.interpolated",
                "org.esa.beam.ndvi",
                "org.esa.beam.unmixing",
        });
    }

    public static boolean start(RootDoc root) {
        OperatorHandler operatorHandler;
        if ("console".equalsIgnoreCase(format)) {
            operatorHandler = new OperatorHandlerConsole();
        } else if ("html".equalsIgnoreCase(format)) {
            operatorHandler = new OperatorHandlerHtml();
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
        operatorSpiRegistry.loadOperatorSpis();

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
