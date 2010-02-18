package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.*;

import java.util.HashMap;
import java.util.Map;

public class OperatorHandlerConsole implements OperatorHandler {
    @Override
    public void start(RootDoc root) {
    }

    @Override
    public void stop(RootDoc root) {
    }

    @Override
    public void processOperator(OperatorDesc operatorDesc) throws Exception {

        ClassDoc typeDoc = operatorDesc.getClassDoc();

        StringBuilder sb = new StringBuilder(300);
        sb.append(String.format("Operator: %s\n", typeDoc.typeName()));
        sb.append(String.format("\tClass: %s\n", typeDoc.qualifiedTypeName()));
        sb.append(String.format("\tText: %s\n", typeDoc.commentText()));

        writeClassAnnotiations(typeDoc, sb);

        FieldDoc[] fieldDocs = typeDoc.fields(false);
        for (FieldDoc fieldDoc : fieldDocs) {
            writeFieldAnnotiations(fieldDoc, sb);
        }
        System.out.println(sb.toString());
    }

    public  void writeClassAnnotiations(ClassDoc classDoc, StringBuilder sb) {
        AnnotationDesc[] annotations = classDoc.annotations();
        for (AnnotationDesc annotation : annotations) {
            sb.append("\t").append(annotation.annotationType().name()).append(" : \n");
            writeAnnotationTable(annotation, sb);
        }
    }

    public  void writeFieldAnnotiations(FieldDoc fieldDoc, StringBuilder sb) {
        AnnotationDesc[] annotations = fieldDoc.annotations();
        for (AnnotationDesc annotation : annotations) {
            String fieldName = fieldDoc.name();
            sb.append("\t").append(annotation.annotationType().name()).append(String.format(" [%s]", fieldName));
            sb.append(" : \n");
            writeAnnotationTable(annotation, sb);
        }
    }

    public  void writeAnnotationTable(AnnotationDesc annotation, StringBuilder sb) {
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

    public  Map<String, String> createElementValueMap(AnnotationDesc annotation) {
        AnnotationDesc.ElementValuePair[] elementValuePairs = annotation.elementValues();
        Map<String, String> elementValueMap = new HashMap<String, String>();
        for (AnnotationDesc.ElementValuePair elementValuePair : elementValuePairs) {
            elementValueMap.put(elementValuePair.element().name(), elementValuePair.value().toString());
        }
        return elementValueMap;
    }

}