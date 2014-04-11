/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.ParameterUpdater;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        if (appContext instanceof VisatApp) {
            ((VisatApp) appContext).getLogger().warning("");
        }
        formModel = new BinningFormModelImpl();
        form = new BinningForm(appContext, formModel, getTargetProductSelector());

        // TODO menu entries for binning op, still a work in progress by nf 2013-11-05
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
        TargetVariableSpec[] targetVariableSpecs = formModel.getTargetVariableSpecs();
        if (targetVariableSpecs.length == 0) {
            showErrorDialog("No target variable set.");
            return;
        }
        for (TargetVariableSpec spec : targetVariableSpecs) {
            boolean specValid = spec.isValid();
            if (!specValid) {
                showErrorDialog("Aggregation " + spec.toString() + " is invalid.");
                return;
            }
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

    private static String getVarName(TargetVariableSpec spec) {
        return spec.source.type == TargetVariableSpec.Source.RASTER_SOURCE_TYPE ? spec.source.bandName : spec.targetName;
    }

    private BinningConfig createBinningConfig() {
        final List<VariableConfig> variableConfigs = new ArrayList<>();
        final List<AggregatorConfig> aggregatorConfigs = new ArrayList<>();
        final TargetVariableSpec[] targetVariableSpecs = formModel.getTargetVariableSpecs();
        for (final TargetVariableSpec spec : targetVariableSpecs) {
            variableConfigs.add(new VariableConfig(getVarName(spec), spec.source.expression));
            aggregatorConfigs.add(createAggregatorConfig(spec.aggregatorDescriptor.getName(),
                                                         getVarName(spec),
                                                         spec.targetName,
                                                         spec.aggregatorProperties));
        }
        return createBinningConfig(variableConfigs, aggregatorConfigs);
    }

    private BinningConfig createBinningConfig(List<VariableConfig> variableConfigs, List<AggregatorConfig> aggregatorConfigs) {
        final BinningConfig binningConfig = new BinningConfig();
        binningConfig.setAggregatorConfigs(aggregatorConfigs.toArray(new AggregatorConfig[aggregatorConfigs.size()]));
        binningConfig.setVariableConfigs(variableConfigs.toArray(new VariableConfig[variableConfigs.size()]));
        binningConfig.setMaskExpr(formModel.getMaskExpr());
        binningConfig.setNumRows(formModel.getNumRows());
        binningConfig.setSupersampling(formModel.getSupersampling());
        return binningConfig;
    }

    private AggregatorConfig createAggregatorConfig(String aggregatorName, String varName, String targetName, PropertyContainer aggregatorProperties) {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        AggregatorDescriptor aggregatorDescriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorName);
        final AggregatorConfig aggregatorConfig = aggregatorDescriptor.createConfig();
        PropertyContainer pc = PropertyContainer.createObjectBacked(aggregatorConfig);
        if (pc.isPropertyDefined("varName")) {
            pc.setValue("varName", varName);
        }
        if (pc.isPropertyDefined("targetName") && StringUtils.isNotNullAndNotEmpty(targetName)) {
            pc.setValue("targetName", targetName);
        }
        for (Property property : aggregatorProperties.getProperties()) {
            pc.setValue(property.getName(), property.getValue());
        }
        return aggregatorConfig;
    }

    @Override
    public int show() {
        setContent(form);
        return super.show();
    }

    private class TargetProductCreator extends ProgressMonitorSwingWorker<Product, Void> {

        protected TargetProductCreator() {
            super(BinningDialog.this.getJDialog(), "Creating target product");
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Binning...", 100);

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("region", formModel.getRegion());
            setTimeFiltering(parameters);

            BinningConfig binningConfig = createBinningConfig();
            parameters.put("variableConfigs", binningConfig.getVariableConfigs());
            parameters.put("aggregatorConfigs", binningConfig.getAggregatorConfigs());

            parameters.put("outputFormat", "BEAM-DIMAP");
            parameters.put("outputFile", getTargetProductSelector().getModel().getProductFile().getPath());
            parameters.put("outputType", "Product");

            parameters.put("maskExpr", binningConfig.getMaskExpr());
            parameters.put("numRows", binningConfig.getNumRows());
            parameters.put("superSampling", binningConfig.getSupersampling());

            final Product targetProduct = GPF.createProduct("Binning", parameters, formModel.getSourceProducts());

            pm.done();

            return targetProduct;
        }

        private void setTimeFiltering(Map<String, Object> parameters) {
            BinningOp.TimeFilterMethod method = formModel.getTimeFilterMethod();
            parameters.put("timeFilterMethod", method);
            switch (method) {
                case NONE:
                    return;
                case SPATIOTEMPORAL_DATA_DAY: {
                    parameters.put("minDataHour", formModel.getMinDataHour());
                }
                case TIME_RANGE: {
                    parameters.put("startDateTime", formModel.getStartDateTime());
                    parameters.put("periodDuration", formModel.getPeriodDuration());
                }
            }
        }
    }

    private class BinningParameterUpdater implements ParameterUpdater {
        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            formModel.getBindingContext().adjustComponents();
            final PropertySet propertySet = formModel.getBindingContext().getPropertySet();
            final Property[] properties = propertySet.getProperties();
            for (Property property : properties) {
                parameterMap.put(property.getName(), property.getValue());
            }
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            final PropertySet propertySet = formModel.getBindingContext().getPropertySet();
            final Set<Map.Entry<String, Object>> entries = parameterMap.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                Property property = propertySet.getProperty(entry.getKey());
                if (property != null) {
                    property.setValue(entry.getValue());
                }
            }
            formModel.getBindingContext().adjustComponents();
        }
    }
}
