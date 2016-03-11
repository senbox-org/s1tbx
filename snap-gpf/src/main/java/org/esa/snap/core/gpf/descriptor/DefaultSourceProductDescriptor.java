package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Default implementation of the {@link SourceProductDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultSourceProductDescriptor implements SourceProductDescriptor {

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
        return bands != null ? bands : new String[0];
    }

    @Override
    public Class<? extends Product> getDataType() {
        return Product.class;
    }

}
