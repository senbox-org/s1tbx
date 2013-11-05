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
import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.ParameterUpdater;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
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

/*
        TODO menu entries for binning op, still a work in progress by nf 2013-11-05
        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);

        ParameterUpdater parameterUpdater = new BinningParameterUpdater();


        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorClass(),
                                                                                 null,
                                                                                 null,
                                                                                 parameterUpdater);
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorClass(),
                                                     parameterSupport,
                                                     helpID);

        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());

*/
    }

    static Property createProperty(String name, Class type) {
        final DefaultPropertyAccessor defaultAccessor = new DefaultPropertyAccessor();
        final PropertyDescriptor descriptor = new PropertyDescriptor(name, type);
        descriptor.setDefaultConverter();
        return new Property(descriptor, defaultAccessor);
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new BinningOp.Spi());
        final TargetProductCreator targetProductCreator = new TargetProductCreator();
        targetProductCreator.executeWithBlocking();
        return targetProductCreator.get();
    }

    private FormatterConfig createFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFormat("BEAM-DIMAP");
        formatterConfig.setOutputFile(getTargetProductSelector().getModel().getProductFile().getPath());
        formatterConfig.setOutputType("Product");
        return formatterConfig;
    }

    private BinningConfig createBinningConfig() {
        final List<VariableConfig> variableConfigs = new ArrayList<VariableConfig>();
        final List<AggregatorConfig> aggregatorConfigs = new ArrayList<AggregatorConfig>();
        for (TableRow tableRow : formModel.getTableRows()) {
            variableConfigs.add(new VariableConfig(tableRow.name, tableRow.expression));
            aggregatorConfigs.add(createAggregatorConfig(tableRow.aggregator.getName(),
                                                         tableRow.name,
                                                         tableRow.weight,
                                                         tableRow.percentile));
        }
        return createBinningConfig(variableConfigs, aggregatorConfigs);
    }

    private AggregatorConfig createAggregatorConfig(String aggregatorName, String varName, Double weightCoeff, int percentile) {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        AggregatorDescriptor aggregatorDescriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorName);
        final AggregatorConfig aggregatorConfig = aggregatorDescriptor.createConfig();
        PropertyContainer pc = PropertyContainer.createObjectBacked(aggregatorConfig);
        if (pc.isPropertyDefined("varName")) {
            pc.setValue("varName", varName);
        }
        if (pc.isPropertyDefined("weightCoeff")) {
            pc.setValue("weightCoeff", weightCoeff);
        }
        if (pc.isPropertyDefined("percentage")) {
            pc.setValue("percentage", percentile);
        }
        return aggregatorConfig;
    }

    private BinningConfig createBinningConfig(List<VariableConfig> variableConfigs, List<AggregatorConfig> aggregatorConfigs) {
        final BinningConfig binningConfig = new BinningConfig();
        binningConfig.setAggregatorConfigs(aggregatorConfigs.toArray(new AggregatorConfig[aggregatorConfigs.size()]));
        binningConfig.setVariableConfigs(variableConfigs.toArray(new VariableConfig[variableConfigs.size()]));
        binningConfig.setMaskExpr(formModel.getValidExpression());
        binningConfig.setNumRows(formModel.getNumRows());
        binningConfig.setSuperSampling(formModel.getSuperSampling());
        return binningConfig;
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

            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("region", formModel.getRegion());
            parameters.put("startDate", formModel.getStartDate());
            parameters.put("endDate", formModel.getEndDate());
            parameters.put("outputBinnedData", formModel.shallOutputBinnedData());
            parameters.put("binningConfig", createBinningConfig());
            parameters.put("formatterConfig", createFormatterConfig());

            pm.worked(1);

            final Product targetProduct = GPF.createProduct("Binning", parameters, formModel.getSourceProducts());

            pm.worked(99);
            pm.done();

            return targetProduct;
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
            final Set<Map.Entry<String,Object>> entries = parameterMap.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                try {
                    propertySet.setValue(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    // todo - handle exception (Norman, 14.05.13)
                    e.printStackTrace();
                }
            }
            formModel.getBindingContext().adjustComponents();
        }
    }
}
