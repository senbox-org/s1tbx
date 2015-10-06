package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.gpf.annotations.TargetProperty;

/**
 * A {@link TargetPropertyDescriptor} implementation for the
 * {@link TargetProperty TargetProperty} annotation.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationTargetPropertyDescriptor implements TargetPropertyDescriptor {

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
        return getNonEmptyStringOrNull(annotation.alias());
    }

    @Override
    public String getLabel() {
        return getNonEmptyStringOrNull(annotation.label());
    }

    @Override
    public String getDescription() {
        return getNonEmptyStringOrNull(annotation.description());
    }

    private static String getNonEmptyStringOrNull(String label) {
        return label == null || label.isEmpty() ? null : label;
    }
}
