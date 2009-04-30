package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.Product;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class ProductDomConverter extends SessionElementDomConverter<Product> {

    public ProductDomConverter() {
        super(Product.class);
    }

    @Override
    public Product convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                    ValidationException {
        final Integer refNo = Integer.valueOf(parentElement.getChild("refNo").getValue());
        return getProductManager().getProductByRefNo(refNo);
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        final DomElement refNo = parentElement.createChild("refNo");
        refNo.setValue(String.valueOf(((Product) value).getRefNo()));
    }
}
