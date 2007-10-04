package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.*;
import org.esa.beam.framework.gpf.Operator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marco Peters.
 * <p/>
 * This Doclet can be called on Windows from the command line
 * by the following instruction.
 * <b>NOTE:</b> You have to adopt the pathes to your needs.
 * <p/>
 * SET DocletClassName=org.esa.beam.util.OperatorDoclet
 * SET SourcePath=C:\Projects\beam-4.1-contrib\beam-gpf\src\main\java
 * SET ClassPath=C:\Projects\beam-4.1-contrib\beam-gpf\target\classes
 * <p/>
 * javadoc -doclet "%DocletClassName%" -docletpath "%DocletPath%" ^
 * -sourcepath "%SourcePath%" -classpath "%ClassPath%" ^
 * org.esa.beam.framework.gpf.operators.common
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class OperatorDoclet extends Doclet {

    public static boolean start(RootDoc root) {
        ClassDoc[] classDocs = root.classes();
        for (ClassDoc classDoc : classDocs) {
            if (classDoc.subclassOf(root.classNamed(Operator.class.getName()))) {
                process(classDoc);
            }
        }
        return true;
    }

    private static void process(ClassDoc classDoc) {

        StringBuilder sb = new StringBuilder(300);
        sb.append(String.format("Operator: %s\n", classDoc.typeName()));
        sb.append(String.format("\tClass: %s\n", classDoc.qualifiedTypeName()));
        sb.append(String.format("\tText: %s\n", classDoc.commentText()));

        writeClassAnnotiations(classDoc, sb);

        FieldDoc[] fieldDocs = classDoc.fields(false);
        for (FieldDoc fieldDoc : fieldDocs) {
            writeFieldAnnotiations(fieldDoc, sb);
        }
        System.out.println(sb.toString());
    }

    private static void writeClassAnnotiations(ClassDoc classDoc, StringBuilder sb) {
        AnnotationDesc[] annotations = classDoc.annotations();
        for (AnnotationDesc annotation : annotations) {
            sb.append("\t").append(annotation.annotationType().name()).append(" : \n");
            writeAnnotationTable(annotation, sb);
        }
    }

    private static void writeFieldAnnotiations(FieldDoc fieldDoc, StringBuilder sb) {
        AnnotationDesc[] annotations = fieldDoc.annotations();
        for (AnnotationDesc annotation : annotations) {
            String fieldName = fieldDoc.name();
            sb.append("\t").append(annotation.annotationType().name()).append(String.format(" [%s]", fieldName));
            sb.append(" : \n");
            writeAnnotationTable(annotation, sb);
        }
    }

    private static void writeAnnotationTable(AnnotationDesc annotation, StringBuilder sb) {
        AnnotationTypeElementDoc[] annotationElements = annotation.annotationType().elements();
        if (annotationElements.length > 0) {
            Map<String, String> elementValueMap = createElementValueMap(annotation);
            sb.append(String.format("\t\t%1$20s %2$20s %3$20s\n", "NAME", "VALUE", "DEFAULT"));
            for (AnnotationTypeElementDoc annotationElement : annotationElements) {
                sb.append(String.format("\t\t%1$20s %2$20s %3$20s\n", annotationElement.name(),
                                        elementValueMap.get(annotationElement.name()),
                                        annotationElement.defaultValue()));
            }
        }
    }

    private static Map<String, String> createElementValueMap(AnnotationDesc annotation) {
        AnnotationDesc.ElementValuePair[] elementValuePairs = annotation.elementValues();
        Map<String, String> elementValueMap = new HashMap<String, String>();
        for (AnnotationDesc.ElementValuePair elementValuePair : elementValuePairs) {
            elementValueMap.put(elementValuePair.element().name(), elementValuePair.value().toString());
        }
        return elementValueMap;
    }

}
