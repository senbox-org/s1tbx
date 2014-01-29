package org.esa.beam.framework.gpf.support;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 */
public class DefaultSourceProductDescriptor implements OperatorSpi.SourceProductDescriptor {

    String name;
    String alias;
    String label;
    String description;
    Boolean optional;
    String productType;
    String[] bands;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isOptional() {
        return optional != null ? optional : false;
    }

    @Override
    public String getProductType() {
        return productType;
    }

    @Override
    public String[] getBands() {
        return bands;
    }

    @Override
    public Class<? extends Product> getDataType() {
        return Product.class;
    }
}
