package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.beam.framework.datamodel.ProductManager;

public class SessionDomConverter extends DefaultDomConverter {

    private final ProductManager productManager;

    public SessionDomConverter(ProductManager productManager) {
        super(ValueContainer.class);
        this.productManager = productManager;
    }

    @Override
    protected DomConverter getDomConverter(ValueDescriptor descriptor) {
        DomConverter domConverter = super.getDomConverter(descriptor);
        if (domConverter == null) {
            SessionElementDomConverter<?> elemDomConverter = SessionElementDomConverterRegistry.getInstance().getConverter(
                    (Class<?>) descriptor.getType());
            if (elemDomConverter != null) {
                elemDomConverter.setProductManager(productManager);
                domConverter = elemDomConverter;
            }
        }
        return domConverter;

    }

}
