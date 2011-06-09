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
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptorRegistry;

class PlacemarkDescriptorDomConverter implements DomConverter {

    @Override
    public Class<?> getValueType() {
        return PlacemarkDescriptor.class;
    }

    @Override
    public PlacemarkDescriptor convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
            ValidationException {
        final String type = parentElement.getAttribute("class");
        PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(type);
        if (placemarkDescriptor == null) {
            throw new ConversionException(String.format("Unknown placemark descriptor class '%s'", type));
        }
        return placemarkDescriptor;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        parentElement.setAttribute("class", value.getClass().getName());
    }
}
