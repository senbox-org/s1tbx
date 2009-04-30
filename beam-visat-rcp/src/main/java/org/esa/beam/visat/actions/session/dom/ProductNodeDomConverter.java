package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;

abstract class ProductNodeDomConverter<T extends ProductNode> implements DomConverter {

    private final Class<T> type;
    private final ProductManager productManager;

    protected ProductNodeDomConverter(Class<T> type, ProductManager productManager) {
        this.type = type;
        this.productManager = productManager;
    }

    @Override
    public final Class<T> getValueType() {
        return type;
    }

    @Override
    public final T convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                    ValidationException {
        final DomElement refNoElement = parentElement.getChild("refNo");

        if (refNoElement == null) {
            throw new ConversionException(String.format(
                    "In parent element '%s': no child element 'refNo'", parentElement.getName()));
        }
        final Integer refNo;
        try {
            refNo = Integer.valueOf(refNoElement.getValue());
        } catch (NumberFormatException e) {
            throw new ConversionException(String.format(
                    "In parent element '%s': %s", parentElement.getName(), e.getMessage()));
        }
        final Product product = productManager.getProductByRefNo(refNo);
        if (product == null) {
            throw new ConversionException(String.format(
                    "In parent element '%s': no product with refNo = %d", parentElement.getName(), refNo));
        }

        return getProductNode(parentElement, product);
    }

    @Override
    public final void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        @SuppressWarnings({"unchecked"})
        final T productNode = (T) value;
        final Product product = productNode.getProduct();
        if (product == null) {
            throw new ConversionException("Product node does not belong to a product");
        }
        final DomElement refNo = parentElement.createChild("refNo");
        refNo.setValue(String.valueOf(product.getRefNo()));

        convertProductNodeToDom(productNode, parentElement);
    }

    protected abstract T getProductNode(DomElement parentElement, Product product);

    protected abstract void convertProductNodeToDom(T productNode, DomElement parentElement);
}
