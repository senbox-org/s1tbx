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
