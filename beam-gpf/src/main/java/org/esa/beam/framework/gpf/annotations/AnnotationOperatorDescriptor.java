package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationOperatorDescriptor implements OperatorSpi.OperatorDescriptor {
    private final String name;
    private final Class<? extends Operator> dataType;
    private final OperatorMetadata annotation;

    public AnnotationOperatorDescriptor(String name, Class<? extends Operator> dataType, OperatorMetadata annotation) {
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
    public String getLabel() {
        return annotation.label();
    }

    @Override
    public String getAlias() {
        return annotation.alias();
    }

    @Override
    public String getVersion() {
        return annotation.version();
    }

    @Override
    public String getAuthors() {
        return annotation.authors();
    }

    @Override
    public String getCopyright() {
        return annotation.copyright();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }

    @Override
    public boolean isInternal() {
        return annotation.internal();
    }

    @Override
    public Class<? extends Operator> getDataType() {
        return dataType;
    }

    @Override
    public OperatorSpi.SourceProductDescriptor[] getSourceProductDescriptors() {
        // todo - implement getSourceProductDescriptors (Norman, 29.01.14)
        return new OperatorSpi.SourceProductDescriptor[0];
    }

    @Override
    public OperatorSpi.TargetProductDescriptor getTargetProductDescriptor() {
        // todo - implement getTargetProductDescriptor (Norman, 29.01.14)
        return null;
    }

    @Override
    public OperatorSpi.TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        // todo - implement getTargetPropertyDescriptors (Norman, 29.01.14)
        return new OperatorSpi.TargetPropertyDescriptor[0];
    }

    @Override
    public OperatorSpi.ParameterDescriptor[] getParameterDescriptors() {
        // todo - implement getParameterDescriptors (Norman, 29.01.14)
        return new OperatorSpi.ParameterDescriptor[0];
    }
}
