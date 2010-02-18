package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.StringUtils;

import java.lang.reflect.Field;

public class ParameterDesc extends FieldDesc{
    private final Parameter annotation;

    ParameterDesc(Field field, FieldDoc fieldDoc, Parameter annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        if(StringUtils.isNotNullAndNotEmpty(annotation.alias())) {
            return annotation.alias();
        }
        return super.getName();
    }

    @Override
    public String getShortDescription() {
        return annotation.description();
    }

    public String getDefaultValue() {
        return annotation.defaultValue();
    }
}