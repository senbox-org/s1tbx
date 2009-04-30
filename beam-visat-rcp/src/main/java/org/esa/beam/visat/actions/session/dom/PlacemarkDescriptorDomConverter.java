package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;

class PlacemarkDescriptorDomConverter implements DomConverter {

    @Override
    public Class<?> getValueType() {
        return PlacemarkDescriptor.class;
    }

    @Override
    public PlacemarkDescriptor convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                                ValidationException {
        final String type = parentElement.getAttribute("class");
        if (PinDescriptor.class.getName().equals(type)) {
            return PinDescriptor.INSTANCE;
        }
        if (GcpDescriptor.class.getName().equals(type)) {
            return GcpDescriptor.INSTANCE;
        }
        throw new ConversionException(String.format("illegal placemark descriptor '%s", type));
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        parentElement.setAttribute("class", value.getClass().getName());
    }
}
