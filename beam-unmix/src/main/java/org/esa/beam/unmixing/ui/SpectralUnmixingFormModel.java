package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class SpectralUnmixingFormModel {
    private Product sourceProduct;
    private DefaultListModel bandListModel;
    private Map<String, Object> operatorParameters;
    private ValueContainer operatorValueContainer;

    public SpectralUnmixingFormModel(Product sourceProduct) {
        this.sourceProduct = sourceProduct;

        this.operatorParameters = new HashMap<String, Object>();
        this.operatorValueContainer = ParameterDescriptorFactory.createMapBackedOperatorValueContainer(SpectralUnmixingOp.Spi.class.getName(), operatorParameters);

        try {
            this.operatorValueContainer.getModel("sourceBandNames").setValue(getInitialBandNames());
        } catch (ValidationException e) {
            // ignore
        }
        bandListModel = new DefaultListModel();
        updateBandListModel();
    }

    private void updateBandListModel() {
        bandListModel.clear();
        if (sourceProduct != null) {
            final Band[] bands = sourceProduct.getBands();
            for (Band band : bands) {
                if (band.getSpectralWavelength() > 0) {
                    bandListModel.addElement(band.getName());
                }
            }
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
        updateBandListModel();
    }

    public ListModel getBandListModel() {
        return bandListModel;
    }

    public int[] getInitialBandIndices() {
        if (sourceProduct != null) {
            String[] bandNames = sourceProduct.getBandNames();
            int[] temp = new int[bandNames.length];
            int n = 0;
            for (int i = 0; i < bandNames.length; i++) {
                String bandName = bandNames[i];
                if (sourceProduct.getBand(bandName).getSpectralWavelength() > 0) {
                    temp[n++] = i;
                }
            }
            int[] selectedIndices = new int[n];
            System.arraycopy(temp, 0, selectedIndices, 0, n);
            return selectedIndices;
        } else {
            return new int[0];
        }
    }

    public String[] getInitialBandNames() {
        if (sourceProduct != null) {
            String[] bandNames = sourceProduct.getBandNames();
            ArrayList<String> names = new ArrayList<String>(bandNames.length);
            for (String bandName : bandNames) {
                if (sourceProduct.getBand(bandName).getSpectralWavelength() > 0.0) {
                    names.add(bandName);
                }
            }
            return names.toArray(new String[0]);
        } else {
            return new String[0];
        }
    }
}
