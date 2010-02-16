package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class SourceProductDesc extends FieldDesc{
    private final SourceProduct annotation;

    SourceProductDesc(Field field, FieldDoc fieldDoc, SourceProduct annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    public SourceProduct getAnnotation() {
        return annotation;
    }

    public String getShortDescription() {
        return annotation.description();
    }
}