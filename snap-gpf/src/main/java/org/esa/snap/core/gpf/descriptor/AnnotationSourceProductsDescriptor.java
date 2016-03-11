package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.SourceProducts;

/**
 * A {@link SourceProductsDescriptor} implementation for the
 * {@link SourceProducts SourceProducts} annotation.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationSourceProductsDescriptor implements SourceProductsDescriptor {
    private final SourceProducts annotation;
    private final String name;

    public AnnotationSourceProductsDescriptor(String name, SourceProducts annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(annotation, "annotation");
        this.name = name;
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return name;
    }

    public SourceProducts getAnnotation() {
        return annotation;
    }

    @Override
    public int getCount() {
        return annotation.count();
    }

    @Override
    public String getProductType() {
        return getNonEmptyStringOrNull(annotation.type());
    }

    @Override
    public String[] getBands() {
        return annotation.bands();
    }

    @Override
    public String getAlias() {
        return getNonEmptyStringOrNull(annotation.alias());
    }

    @Override
    public String getDescription() {
        return getNonEmptyStringOrNull(annotation.description());
    }

    @Override
    public String getLabel() {
        return getNonEmptyStringOrNull(annotation.label());
    }

    @Override
    public Class<? extends Product[]> getDataType() {
        return Product[].class;
    }


    private static String getNonEmptyStringOrNull(String label) {
        return label == null || label.isEmpty() ? null : label;
    }
}
