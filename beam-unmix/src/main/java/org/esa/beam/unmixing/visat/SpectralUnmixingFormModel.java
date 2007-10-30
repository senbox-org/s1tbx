package org.esa.beam.unmixing.visat;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class SpectralUnmixingFormModel {
    private Product inputProduct;
    private DefaultListModel bandListModel;
    private Map<String, Object> operatorParameters;
    private ValueContainer operatorValueContainer;

    public SpectralUnmixingFormModel(Product inputProduct) {
        this.inputProduct = inputProduct;

        this.operatorParameters = new HashMap<String, Object>();
        this.operatorValueContainer = ParameterDescriptorFactory.createMapBackedOperatorValueContainer("SpectralUnmixing", operatorParameters);

        try {
            this.operatorValueContainer.getModel("sourceBandNames").setValue(getInitialBandNames());
        } catch (ValidationException e) {
            // ignore
        }
        bandListModel = new DefaultListModel();
        updateBandListModel(inputProduct);
    }

    private void updateBandListModel(Product inputProduct) {
        String[] bandNames = inputProduct.getBandNames();
        bandListModel.clear();
        for (String bandName : bandNames) {
            bandListModel.addElement(bandName);
        }
    }

    public ValueContainer getOperatorValueContainer() {
        return operatorValueContainer;
    }

    public Map<String, Object> getOperatorParameters() {
        return operatorParameters;
    }

    public Product getInputProduct() {
        return inputProduct;
    }

    public void setInputProduct(Product product) {
        inputProduct = product;
        updateBandListModel(inputProduct);
    }

    public ListModel getBandListModel() {
        return bandListModel;
    }

    public int[] getInitialBandIndices() {
        String[] bandNames = inputProduct.getBandNames();
        int[] temp = new int[bandNames.length];
        int n = 0;
        for (int i = 0; i < bandNames.length; i++) {
            String bandName = bandNames[i];
            if (inputProduct.getBand(bandName).getSpectralWavelength() > 0) {
                temp[n++] = i;
            }
        }
        int[] selectedIndices = new int[n];
        System.arraycopy(temp, 0, selectedIndices, 0, n);
        return selectedIndices;
    }

    public String[] getInitialBandNames() {
        String[] bandNames = inputProduct.getBandNames();
        ArrayList<String> names = new ArrayList<String>(bandNames.length);
        for (String bandName : bandNames) {
            if (inputProduct.getBand(bandName).getSpectralWavelength() > 0) {
                names.add(bandName);
            }
        }
        return names.toArray(new String[0]);
    }
}
