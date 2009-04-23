package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.ProductManager;

// todo - find better name (mp, rq - 22.04.2009)
public abstract class SessionElementDomConverter<T> implements DomConverter {

    private final Class<? extends T> type;
    private transient ProductManager productManager;

    protected SessionElementDomConverter(Class<? extends T> type) {
        this.type = type;
    }

    @Override
    public final Class<? extends T> getValueType() {
        return type;
    }

    @Override
    public abstract T convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                       ValidationException;

    protected final ProductManager getProductManager() {
        return productManager;
    }

    public final void setProductManager(ProductManager productManager) {
        this.productManager = productManager;
    }
}
