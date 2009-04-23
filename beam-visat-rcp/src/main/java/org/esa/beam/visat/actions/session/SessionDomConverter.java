package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class SessionDomConverter extends DefaultDomConverter implements SessionAccessorHolder {

    private Session.SessionAccessor sessionAccesor;
    private HashMap<Class<?>, DomConverter> converterHashMap;

    public SessionDomConverter() {
        super(ValueContainer.class, new InternalClassFieldDescriptorFactory());
        converterHashMap = new HashMap<Class<?>, DomConverter>();   // todo - replace with registry
        final RasterDataNodeDomConverter domConverter = new RasterDataNodeDomConverter();
        converterHashMap.put(domConverter.getValueType(), domConverter);
    }

    @Override
    public Session.SessionAccessor getSessionAccessor() {
        return sessionAccesor;
    }

    @Override
    public void setSessionAccessor(Session.SessionAccessor session) {
        this.sessionAccesor = session;
    }

    @Override
    protected DomConverter getDomConverter(ValueDescriptor descriptor) {
        DomConverter domConverter = super.getDomConverter(descriptor);
        if (domConverter == null) {
            domConverter = getConverterFromMap(descriptor.getType());
            if (domConverter != null) {
                ((SessionAccessorHolder) domConverter).setSessionAccessor(getSessionAccessor());
            }
        }
        return domConverter;

    }

    private DomConverter getConverterFromMap(Class<?> type) {
        DomConverter domConverter = converterHashMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = converterHashMap.get(type);
        }
        return domConverter;
    }

    public <T> void registerDomConverter(Class<? extends T> type, AbstractSingleTypeDomConverter<T> domConverter) {
        // TODO: implement
    }

    private static class RasterDataNodeDomConverter extends AbstractSingleTypeDomConverter<RasterDataNode> {

        private RasterDataNodeDomConverter() {
            super(RasterDataNode.class);
        }

        @Override
        public RasterDataNode convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                               ValidationException {

            final Integer refNo = Integer.valueOf(parentElement.getChild("refNo").getValue());
            final String rasterName = parentElement.getChild("rasterName").getValue();

            value = getSessionAccessor().getRasterDataNode(refNo, rasterName);
            return (RasterDataNode) value;
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) {
            RasterDataNode node = (RasterDataNode) value;
            final DomElement refNo = parentElement.createChild("refNo");
            final DomElement rasterName = parentElement.createChild("rasterName");
            refNo.setValue(String.valueOf(node.getProduct().getRefNo()));
            rasterName.setValue(node.getName());
        }

    }

    private static class InternalClassFieldDescriptorFactory implements ClassFieldDescriptorFactory {

        @Override
        public ValueDescriptor createValueDescriptor(Field field) {
            return new ValueDescriptor(field.getName(), field.getType());
        }
    }
}
