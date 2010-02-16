package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class ParameterDesc extends FieldDesc{
    private final Parameter annotation;

    ParameterDesc(Field field, FieldDoc fieldDoc, Parameter annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    public String getShortDescription() {
        return annotation.description();
    }
}