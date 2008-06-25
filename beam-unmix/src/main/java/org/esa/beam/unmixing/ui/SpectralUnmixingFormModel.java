package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import java.util.HashMap;
import java.util.Map;

class SpectralUnmixingFormModel {
    private Product sourceProduct;
    private Map<String, Object> operatorParameters;
    private ValueContainer operatorValueContainer;

    public SpectralUnmixingFormModel(Product sourceProduct) {
        this.sourceProduct = sourceProduct;

        this.operatorParameters = new HashMap<String, Object>();
        this.operatorValueContainer = ParameterDescriptorFactory.createMapBackedOperatorValueContainer(SpectralUnmixingOp.Spi.class.getName(), operatorParameters);
        try {
            this.operatorValueContainer.setDefaultValues();
            ValueModel model = this.operatorValueContainer.getModel("sourceBandNames");
            model.setValue(model.getDescriptor().getValueSet().getItems());
        } catch (ValidationException e) {
            // ignore, validation will be performed again later
        }
    }

    public ValueContainer getOperatorValueContainer() {
        return operatorValueContainer;
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
