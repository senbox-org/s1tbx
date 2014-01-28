package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationTargetProductDescriptor implements OperatorSpi.TargetProductDescriptor {

    private final TargetProduct annotation;

    public AnnotationTargetProductDescriptor(TargetProduct annotation) {
        Assert.notNull(annotation, "annotation");
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return "targetProduct";
    }

    @Override
    public String getAlias() {
        return "target";
    }

    @Override
    public String getLabel() {
        return annotation.label();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }

    @Override
    public Class<?> getDataType() {
        return Product.class;
    }
}
