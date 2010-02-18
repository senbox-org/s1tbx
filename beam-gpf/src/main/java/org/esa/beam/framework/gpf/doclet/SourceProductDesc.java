package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.lang.reflect.Field;

public class SourceProductDesc extends FieldDesc{
    private final SourceProduct annotation;

    SourceProductDesc(Field field, FieldDoc fieldDoc, SourceProduct annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    public SourceProduct getAnnotation() {
        return annotation;
    }

    @Override
    public String getShortDescription() {
        return annotation.description();
    }
}