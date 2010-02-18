package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;

import java.lang.reflect.Field;

public class SourceProductsDesc extends FieldDesc{
    private final SourceProducts annotation;

    SourceProductsDesc(Field field, FieldDoc fieldDoc, SourceProducts annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    public SourceProducts getAnnotation() {
        return annotation;
    }

    public String getShortDescription() {
        return annotation.description();
    }
}