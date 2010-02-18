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

public abstract class FieldDesc implements ElementDesc {
    private final Field field;
    private final FieldDoc fieldDoc;

    FieldDesc(Field field, FieldDoc fieldDoc) {
        this.field = field;
        this.fieldDoc = fieldDoc;
    }

    public String getName() {
        return field.getName();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return field;
    }

    public FieldDoc getFieldDoc() {
        return fieldDoc;
    }

    public String getLongDescription() {
        return fieldDoc != null ? fieldDoc.commentText() : "";
    }
}