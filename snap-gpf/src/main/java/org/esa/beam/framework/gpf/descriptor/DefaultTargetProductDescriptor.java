package org.esa.beam.framework.gpf.descriptor;

import org.esa.beam.framework.datamodel.Product;

/**
 * Default implementation of the {@link TargetProductDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultTargetProductDescriptor implements TargetProductDescriptor {

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
