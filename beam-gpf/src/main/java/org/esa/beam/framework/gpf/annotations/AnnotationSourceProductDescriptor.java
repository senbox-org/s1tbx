package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationSourceProductDescriptor implements OperatorSpi.SourceProductDescriptor {
    private final SourceProduct annotation;
    private final String name;

    public AnnotationSourceProductDescriptor(String name, SourceProduct annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(annotation, "annotation");
        this.name = name;
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOptional() {
        return annotation.optional();
    }

    @Override
    public String getProductType() {
        return annotation.type();
    }

    @Override
    public String[] getBands() {
        return annotation.bands();
    }

    @Override
    public String getAlias() {
        return annotation.alias();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }

    @Override
    public String getLabel() {
        return annotation.label();
    }

    @Override
    public Class<? extends Product> getDataType() {
        return Product.class;
    }
}
