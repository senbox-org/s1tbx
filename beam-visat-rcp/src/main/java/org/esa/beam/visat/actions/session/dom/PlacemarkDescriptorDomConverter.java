/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
