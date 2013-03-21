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

package org.esa.beam.timeseries.ui.assistant;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesFactory;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;

abstract class AbstractTimeSeriesAssistantPage extends AbstractAssistantPage {

    private final TimeSeriesAssistantModel assistantModel;
    private final MyChangeListener changeListener;

    AbstractTimeSeriesAssistantPage(String pageTitle, TimeSeriesAssistantModel model) {
        super(pageTitle);
        assistantModel = model;
        changeListener = new MyChangeListener();
        assistantModel.addChangeListener(changeListener);
    }

    protected TimeSeriesAssistantModel getAssistantModel() {
        return assistantModel;
    }

    @Override
    public boolean performFinish() {
        TimeSeriesAssistantModel model = getAssistantModel();
        final TimeSeriesCreator creator = new TimeSeriesCreator(model, this.getPageComponent());
        creator.executeWithBlocking();
        removeModeListener();
        return true;
    }

    @Override
    public void performCancel() {
        removeModeListener();
    }

    protected void removeModeListener() {
        assistantModel.removeChangeListener(changeListener);
    }

    private void addTimeSeriesProductToVisat(TimeSeriesAssistantModel assistantModel, ProgressMonitor pm) {
        pm.beginTask("Creating Time Series", 50);
        final ProductLocationsPaneModel locationsModel = assistantModel.getProductLocationsModel();
        pm.worked(1);
        final VariableSelectionPaneModel variablesModel = assistantModel.getVariableSelectionModel();
        pm.worked(1);
        final AbstractTimeSeries timeSeries = TimeSeriesFactory.create(assistantModel.getTimeSeriesName(),
                locationsModel.getProductLocations(),
                variablesModel.getSelectedVariableNames());
        pm.worked(42);
        ProductManager productManager = VisatApp.getApp().getProductManager();
        Product tsProduct = timeSeries.getTsProduct();
        productManager.addProduct(tsProduct);
        productManager.addListener(new CloseListener(tsProduct));
        pm.worked(6);
    }

    private static class CloseListener implements ProductManager.Listener {
        //todo ... se refactoring
        // this class is not necessary because the TimeSeriesManager.remove() do the same
        // job automatically. Only one instance of a CloseListener without tsProduct
        // field is necessary, which directly delegates to the TimeSeriesMapper.
        // Or the TimeSeriesMapper implements the interface ProductManager.Listener.
        private Product tsProduct;

        private CloseListener(Product tsProduct) {
            this.tsProduct = tsProduct;
        }

        @Override
        public void productAdded(ProductManager.Event event) {
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (event.getProduct() == tsProduct) {
                org.esa.beam.timeseries.core.TimeSeriesMapper.getInstance().remove(tsProduct);
                tsProduct = null;
            }
        }
    }

    private class MyChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            getContext().updateState();
        }
    }

    private class TimeSeriesCreator extends ProgressMonitorSwingWorker<Void, TimeSeriesAssistantModel> {

        private final TimeSeriesAssistantModel model;

        private TimeSeriesCreator(TimeSeriesAssistantModel model, Component parentComponent) {
            super(parentComponent, "Creating Time Series...");
            this.model = model;
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            addTimeSeriesProductToVisat(model, pm);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (Exception e) {
                Debug.trace(e);
                getContext().showErrorDialog(e.getMessage());
            }
        }
    }
}
