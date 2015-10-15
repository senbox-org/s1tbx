package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * A {@link OperatorDescriptor} implementation for the {@link OperatorMetadata} annotation.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationOperatorDescriptor implements OperatorDescriptor {
    private OperatorMetadata annotation;
    private AnnotationOperatorDescriptorBody body;

    public AnnotationOperatorDescriptor(Class<? extends Operator> operatorType, OperatorMetadata annotation) {
        Assert.notNull(operatorType, "operatorType");
        Assert.notNull(annotation, "annotation");
        this.body = new AnnotationOperatorDescriptorBody(operatorType);
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return body.getOperatorClass().getName();
    }

    public OperatorMetadata getAnnotation() {
        return annotation;
    }

    @Override
    public String getLabel() {
        return getNonEmptyStringOrNull(annotation.label());
    }

    @Override
    public String getAlias() {
        return getNonEmptyStringOrNull(annotation.alias());
    }

    @Override
    public String getVersion() {
        return getNonEmptyStringOrNull(annotation.version());
    }

    @Override
    public String getAuthors() {
        return getNonEmptyStringOrNull(annotation.authors());
    }

    @Override
    public String getCopyright() {
        return getNonEmptyStringOrNull(annotation.copyright());
    }

    @Override
    public String getDescription() {
        return getNonEmptyStringOrNull(annotation.description());
    }

    @Override
    public boolean isInternal() {
        return annotation.internal();
    }

    @Override
    public boolean isAutoWriteDisabled() {
        return annotation.autoWriteDisabled();
    }

    @Override
    public Class<? extends Operator> getOperatorClass() {
        return body.getOperatorClass();
    }

    @Override
    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return body.getSourceProductDescriptors();
    }

    @Override
    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return body.getSourceProductsDescriptor();
    }

    @Override
    public TargetProductDescriptor getTargetProductDescriptor() {
        return body.getTargetProductDescriptor();
    }

    @Override
    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return body.getTargetPropertyDescriptors();
    }

    @Override
    public ParameterDescriptor[] getParameterDescriptors() {
        return body.getParameterDescriptors();
    }

    private static String getNonEmptyStringOrNull(String label) {
        return label == null || label.isEmpty() ? null : label;
    }
}
