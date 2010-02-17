package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

/**
 * A doclet which scans the classpath for GPF operators and creates
 * associated documentation derived from an operator's annotations
 * <ol>
 * <li>{@link org.esa.beam.framework.gpf.annotations.OperatorMetadata OperatorMetadata}</li>
 * <li>{@link org.esa.beam.framework.gpf.annotations.SourceProduct SourceProduct}</li>
 * <li>{@link org.esa.beam.framework.gpf.annotations.SourceProducts SourceProducts}</li>
 * <li>{@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct}</li>
 * <li>{@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}</li>
 * </ol>
 * <p/>
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
 *         org.esa.beam.framework.gpf.operators.common
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
            System.out.println("Usage: OperatorDoclet [<format>]");
            System.exit(1);
        }
        com.sun.tools.javadoc.Main.main(new String[] {
                 "-doclet", OperatorDoclet.class.getName(),
                "-sourcepath", "./beam-gpf/src/main/java;./beam-gpf-common/src/main/java",
//                "-classpath", "./beam-gpf/target/classes"
                "org.esa.beam.framework.gpf.operators.common",
                "org.esa.beam.gpf.common.reproject",
                "org.esa.beam.gpf.common.mosaic",
                "org.esa.beam.framework.gpf.operators.meris",
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

        ClassDoc[] classDocs = root.classes();
        for (ClassDoc classDoc : classDocs) {
            if (classDoc.subclassOf(root.classNamed(Operator.class.getName()))) {
                try {
                    System.out.println("Processing " + classDoc.typeName() + "...");
                    Class<? extends Operator> type = (Class<? extends Operator>) Class.forName(classDoc.qualifiedTypeName());
                    OperatorMetadata annotation = type.getAnnotation(OperatorMetadata.class);
                    if (annotation != null) {
                        if (!annotation.internal()) {
                            OperatorDesc operatorDesc = new OperatorDesc(type, classDoc, annotation);
                            operatorHandler.processOperator(operatorDesc);
                        }
                    } else {
                        System.err.println("Warning: Skipping " + classDoc.typeName() + " because it has no metadata.");
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
                docErrorReporter.printWarning("options["+i+"]["+j+"] = " + options[i][j]);
            }
        }
        return true;
    }

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }
}
