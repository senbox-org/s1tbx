package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDefinition;
import com.bc.ceres.binding.ValueDefinitionFactory;
import org.esa.beam.framework.datamodel.Product;

import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationFormModel {

    private Product[] defaultProducts;
    private Product referenceProduct;
    private Product collocateProduct;

    private final ValueContainer valueContainer;

    public CollocationFormModel(Product[] defaultProducts) {
        this.defaultProducts = defaultProducts;
        final Factory factory = new Factory(new ValueDefinitionFactory() {
            public ValueDefinition createValueDefinition(Field field) {
                return new ValueDefinition(field.getName(), field.getType());
            }
        });
        valueContainer = factory.createObjectBackedValueContainer(this);

    }


    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public Product[] getDefaultProducts() {
        return defaultProducts;
    }

    public Product getReferenceProduct() {
        return referenceProduct;
    }

    public void setReferenceProduct(Product referenceProduct) {
        this.referenceProduct = referenceProduct;
    }

    public Product getCollocateProduct() {
        return collocateProduct;
    }

    public void setCollocateProduct(Product collocateProduct) {
        this.collocateProduct = collocateProduct;
    }
}
