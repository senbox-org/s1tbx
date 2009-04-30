package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.AbstractFigure;

import java.util.HashMap;
import java.util.Map;

public class SessionDomConverter extends DefaultDomConverter {

    private final Map<Class<?>, DomConverter> domConverterMap = new HashMap<Class<?>, DomConverter>(33);

    public SessionDomConverter(ProductManager productManager) {
        super(ValueContainer.class);

        setDomConverter(Product.class, new ProductDomConverter(productManager));
        setDomConverter(RasterDataNode.class, new RasterDataNodeDomConverter(productManager));
        setDomConverter(BitmaskDef.class, new BitmaskDefDomConverter(productManager));
        setDomConverter(AbstractFigure.class, new FigureDomConverter());
        setDomConverter(PlacemarkDescriptor.class, new PlacemarkDescriptorDomConverter());
    }

    final void setDomConverter(Class<?> type, DomConverter domConverter) {
        domConverterMap.put(type, domConverter);
    }

    @Override
    protected DomConverter getDomConverter(ValueDescriptor descriptor) {
        DomConverter domConverter = getDomConverter(descriptor.getType());
        if (domConverter == null) {
            domConverter = super.getDomConverter(descriptor);
        }
        return domConverter;
    }

    private DomConverter getDomConverter(Class<?> type) {
        DomConverter domConverter = domConverterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = domConverterMap.get(type);
        }
        return domConverter;
    }
}
