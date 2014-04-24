/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.ParameterUpdater;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * UI for binning operator.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class BinningDialog extends SingleTargetProductDialog {

    private static final String OPERATOR_NAME = "Binning";

    private final BinningForm form;
    private final BinningFormModel formModel;

    protected BinningDialog(AppContext appContext, String title, String helpID) {
        super(appContext, title, ID_APPLY_CLOSE_HELP, helpID, new TargetProductSelectorModel(), true);

        formModel = new BinningFormModel();
        form = new BinningForm(appContext, formModel, getTargetProductSelector());

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);

        ParameterUpdater parameterUpdater = new BinningParameterUpdater();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 null,
                                                                                 null,
                                                                                 parameterUpdater);
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    static Property createProperty(String name, Class type) {
        final DefaultPropertyAccessor defaultAccessor = new DefaultPropertyAccessor();
        final PropertyDescriptor descriptor = new PropertyDescriptor(name, type);
        descriptor.setDefaultConverter();
        return new Property(descriptor, defaultAccessor);
    }

    @Override
    protected void onApply() {
        AggregatorConfig[] aggregatorConfigs = formModel.getAggregatorConfigs();
        if (aggregatorConfigs.length == 0) {
            showErrorDialog("Please configure at least a single aggregator.");
            return;
        }

        if (formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY ||
            formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.TIME_RANGE) {
            if (formModel.getStartDateTime() == null) {
                showErrorDialog("Start date/time must be provided when time filter method 'spatiotemporal data day' or 'time range' is chosen.");
                return;
            }
            if (formModel.getPeriodDuration() == null) {
                showErrorDialog("Period duration must be provided when time filter method 'spatiotemporal data day' or 'time range' is chosen.");
                return;
            }
        }
        if (formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
            if (formModel.getMinDataHour() == null) {
                showErrorDialog("Min data hour must be provided when time filter method 'spatiotemporal data day' is chosen.");
                return;
            }
        }
        super.onApply();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new BinningOp.Spi());
        final TargetProductCreator targetProductCreator = new TargetProductCreator();
        targetProductCreator.executeWithBlocking();
        return targetProductCreator.get();
    }

    @Override
    public int show() {
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareClose();
        formModel.closeContextProduct();
        super.hide();
    }

    private void updateParameterMap(Map<String, Object> parameters) {
        parameters.put("variableConfigs", formModel.getVariableConfigs());
        parameters.put("aggregatorConfigs", formModel.getAggregatorConfigs());

        parameters.put("outputFormat", "BEAM-DIMAP");
        parameters.put("outputType", "Product");
        parameters.put("outputFile", getTargetProductSelector().getModel().getProductFile().getPath());

        parameters.put("maskExpr", formModel.getMaskExpr());
        parameters.put("region", formModel.getRegion());
        parameters.put("numRows", formModel.getNumRows());
        parameters.put("supersampling", formModel.getSupersampling());
        parameters.put("sourceProductPaths", formModel.getSourceProductPath());

        BinningOp.TimeFilterMethod method = formModel.getTimeFilterMethod();
        parameters.put("timeFilterMethod", method);
        if (method == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
            parameters.put("minDataHour", formModel.getMinDataHour());
            parameters.put("startDateTime", formModel.getStartDateTime());
            parameters.put("periodDuration", formModel.getPeriodDuration());
        } else if (method == BinningOp.TimeFilterMethod.TIME_RANGE) {
            parameters.put("startDateTime", formModel.getStartDateTime());
            parameters.put("periodDuration", formModel.getPeriodDuration());
        }
    }

    private void updateFormModel(Map<String, Object> parameterMap) throws ValidationException {
        final PropertySet propertySet = formModel.getBindingContext().getPropertySet();
        final Set<Map.Entry<String, Object>> entries = parameterMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Property property = propertySet.getProperty(entry.getKey());
            if (property != null) {
                property.setValue(entry.getValue());
            }
        }
        // todo - update aggregator + variable config tables
    }

    private class TargetProductCreator extends ProgressMonitorSwingWorker<Product, Void> {

        protected TargetProductCreator() {
            super(BinningDialog.this.getJDialog(), "Creating target product");
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Binning...", 100);
            final Map<String, Object> parameters = new HashMap<>();
            updateParameterMap(parameters);
            final Product targetProduct = GPF.createProduct("Binning", parameters, formModel.getSourceProducts());
            pm.done();
            return targetProduct;
        }
    }

    private class BinningParameterUpdater implements ParameterUpdater {
        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            formModel.getBindingContext().adjustComponents();
            updateParameterMap(parameterMap);
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            updateFormModel(parameterMap);
            formModel.getBindingContext().adjustComponents();
        }
    }
}
