package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Default implementation of the {@link SourceProductsDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultSourceProductsDescriptor implements SourceProductsDescriptor {

    String name;
    int count;
    String alias;
    String label;
    String description;
    String productType;
    String[] bands;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCount() {
        return count;
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
    public String getProductType() {
        return productType;
    }

    @Override
    public String[] getBands() {
        return bands;
    }

    @Override
    public Class<? extends Product[]> getDataType() {
        return Product[].class;
    }

}
