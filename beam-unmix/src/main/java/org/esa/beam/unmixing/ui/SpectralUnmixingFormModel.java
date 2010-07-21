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

package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import java.util.HashMap;
import java.util.Map;

class SpectralUnmixingFormModel {
    private Product sourceProduct;
    private Map<String, Object> operatorParameters;
    private PropertyContainer operatorParameterContainer;

    public SpectralUnmixingFormModel(Product sourceProduct) {
        this.sourceProduct = sourceProduct;

        this.operatorParameters = new HashMap<String, Object>();
        this.operatorParameterContainer = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(SpectralUnmixingOp.Spi.class.getName(), operatorParameters);
        try {
            this.operatorParameterContainer.setDefaultValues();
            Property model = this.operatorParameterContainer.getProperty("sourceBandNames");
            model.setValue(model.getDescriptor().getValueSet().getItems());
        } catch (ValidationException e) {
            // ignore, validation will be performed again later
        }
    }

    public PropertyContainer getOperatorValueContainer() {
        return operatorParameterContainer;
    }

    public Map<String, Object> getOperatorParameters() {
        return operatorParameters;
    }

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public void setSourceProduct(Product product) {
        sourceProduct = product;
    }
}
