package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class TargetProductDesc extends FieldDesc{
    private final TargetProduct annotation;

    TargetProductDesc(Field field, FieldDoc fieldDoc, TargetProduct annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    @Override
     public String getShortDescription() {
        return annotation.description();
    }
}