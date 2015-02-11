/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;

class SpectralUnmixingFormModel {
    private Product sourceProduct;
    private final PropertySet propertySet;

    public SpectralUnmixingFormModel(Product sourceProduct, PropertySet propertySet) {
        this.sourceProduct = sourceProduct;
        this.propertySet = propertySet;

        try {
            Property model = this.propertySet.getProperty("sourceBandNames");
            model.setValue(model.getDescriptor().getValueSet().getItems());
        } catch (ValidationException e) {
            // ignore, validation will be performed again later
        }
    }

    public PropertySet getOperatorValueContainer() {
        return propertySet;
    }

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public void setSourceProduct(Product product) {
        sourceProduct = product;
    }
}
