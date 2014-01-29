package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationTargetPropertyDescriptor implements OperatorSpi.TargetPropertyDescriptor {

    private final String name;
    private final Class<?> dataType;
    private final TargetProperty annotation;

    public AnnotationTargetPropertyDescriptor(String name, Class<?> dataType, TargetProperty annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(dataType, "dataType");
        Assert.notNull(annotation, "annotation");
        this.name = name;
        this.dataType = dataType;
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getDataType() {
        return dataType;
    }

    public TargetProperty getAnnotation() {
        return annotation;
    }

    @Override
    public String getAlias() {
        return annotation.alias();
    }

    @Override
    public String getLabel() {
        return annotation.label();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }
}
