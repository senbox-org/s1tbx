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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.pet.PetOp;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class PixelExtractionDialog extends ModelessDialog {

    private Map<String, Object> parameterMap;
    private final AppContext appContext;

    PixelExtractionDialog(AppContext appContext, String title) {
        super(appContext.getApplicationWindow(), title, ID_APPLY_CLOSE_HELP, "pixelExtraction");

        this.appContext = appContext;

        AbstractButton button = getButton(ID_APPLY);
        button.setText("Extract");
        button.setMnemonic('E');

        parameterMap = new HashMap<String, Object>();
        final PropertyContainer propertyContainer = createParameterMap(parameterMap);
        propertyContainer.addProperty(Property.create("sourceProducts", Product[].class));
        final PixelExtractionIOForm ioForm = new PixelExtractionIOForm(appContext, propertyContainer);
        final PixelExtractionProcessingForm processingForm = new PixelExtractionProcessingForm(appContext,
                                                                                               propertyContainer);
        JTabbedPane tabbedPanel = new JTabbedPane();
        tabbedPanel.addTab("I/O Parameters", ioForm.getPanel());
        tabbedPanel.addTab("Processing Parameters", processingForm.getPanel());

        setContent(tabbedPanel);
    }

    @Override
    protected void onApply() {
        ProgressMonitorSwingWorker worker = new MyProgressMonitorSwingWorker(getParent(), "Creating output file(s)...");
        AbstractButton runButton = getButton(ID_APPLY);
        runButton.setEnabled(false);
        worker.execute();
    }

    private PropertyContainer createParameterMap(Map<String, Object> map) {
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer propertyContainer = PropertyContainer.createMapBacked(map,
                                                                                      PetOp.class,
                                                                                      parameterDescriptorFactory);
        try {
            propertyContainer.setDefaultValues();
        } catch (ValidationException e) {
            e.printStackTrace();
            showErrorDialog(e.getMessage());
        }
        return propertyContainer;
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final DefaultAppContext context = new DefaultAppContext("dev0");
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(new PetOp.Spi());
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final PixelExtractionDialog dialog = new PixelExtractionDialog(context, "Pixel Extraction") {
                    @Override
                    protected void onClose() {
                        System.exit(0);
                    }
                };
                dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.show();
            }
        });

    }

    private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Void, Void> {

        protected MyProgressMonitorSwingWorker(Component parentComponent, String title) {
            super(parentComponent, title);
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Computing pixel values...", 1);
            try {
                GPF.createProduct("Pet", parameterMap);
                pm.worked(1);
            } finally {
                pm.done();
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                String message = String.format(
                        "The pixel extraction tool has run successfully and written the result file(s) to %s.",
                        parameterMap.get("outputDir").toString());
                JOptionPane.showMessageDialog(getJDialog(), message);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException e) {
                appContext.handleError(e.getMessage(), e);
            }finally {
                AbstractButton runButton = getButton(ID_APPLY);
                runButton.setEnabled(true);
            }
        }
    }
}
