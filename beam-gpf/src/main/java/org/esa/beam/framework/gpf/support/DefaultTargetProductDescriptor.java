package org.esa.beam.framework.gpf.support;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 */
public class DefaultTargetProductDescriptor implements OperatorSpi.TargetProductDescriptor {

    String name;
    String alias;
    String label;
    String description;

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
    public Class<? extends Product> getDataType() {
        return Product.class;
    }
}
