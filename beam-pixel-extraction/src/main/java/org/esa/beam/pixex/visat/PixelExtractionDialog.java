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

package org.esa.beam.pixex.visat;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.pixex.PixExOp;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class PixelExtractionDialog extends ModelessDialog {

    private final Map<String, Object> parameterMap;
    private final AppContext appContext;
    private final PixelExtractionIOForm ioForm;
    private final PixelExtractionParametersForm parametersForm;

    PixelExtractionDialog(AppContext appContext, String title) {
        super(appContext.getApplicationWindow(), title, ID_OK | ID_CLOSE | ID_HELP, "pixelExtraction");

        this.appContext = appContext;

        AbstractButton button = getButton(ID_OK);
        button.setText("Extract");
        button.setMnemonic('E');

        parameterMap = new HashMap<String, Object>();
        final PropertyContainer propertyContainer = createParameterMap(parameterMap);

        ioForm = new PixelExtractionIOForm(appContext, propertyContainer);
        ioForm.setInputChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Product[] sourceProducts = ioForm.getSourceProducts();
                if (sourceProducts.length > 0) {
                    parametersForm.setActiveProduct(sourceProducts[0]);
                    return;
                } else {
                    if(parameterMap.containsKey("inputPaths")) {
                        final File[] inputPaths = (File[]) parameterMap.get("inputPaths");
                        if(inputPaths.length > 0) {
                            try {
                                Product firstProduct = null;
                                File file = PixExOp.getParsedInputPaths(inputPaths)[0];
                                if (file.isDirectory()) {
                                    for (File subFile : file.listFiles()) {
                                        firstProduct = ProductIO.readProduct(subFile);
                                        if(firstProduct != null) {
                                            break;
                                        }
                                    }
                                } else {
                                    firstProduct = ProductIO.readProduct(file);
                                }
                                parametersForm.setActiveProduct(firstProduct);
                                return;
                            } catch (IOException ignore) {
                            }
                        }
                    }
                }
                parametersForm.setActiveProduct(null);
            }
        });

        parametersForm = new PixelExtractionParametersForm(appContext, propertyContainer);

        final JPanel ioPanel = ioForm.getPanel();
        ioPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        final JPanel parametersPanel = parametersForm.getPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JTabbedPane tabbedPanel = new JTabbedPane();
        tabbedPanel.addTab("Input/Output", ioPanel);
        tabbedPanel.addTab("Parameters", parametersPanel);

        setContent(tabbedPanel);
    }

    @Override
    protected void onOK() {
        parameterMap.put("coordinates", parametersForm.getCoordinates());
        parameterMap.put("expression", parametersForm.getExpression());
        parameterMap.put("timeDifference", parametersForm.getAllowedTimeDifference());
        parameterMap.put("exportExpressionResult", parametersForm.isExportExpressionResultSelected());
        ProgressMonitorSwingWorker worker = new MyProgressMonitorSwingWorker(getParent(), "Creating output file(s)...");
        worker.executeWithBlocking();
    }

    @Override
    public void close() {
        super.close();
        ioForm.clear();
    }

    @Override
    public int show() {
        ioForm.setSelectedProduct(appContext.getSelectedProduct());
        return super.show();
    }

    private static PropertyContainer createParameterMap(Map<String, Object> map) {
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer container = PropertyContainer.createMapBacked(map, PixExOp.class,
                                                                              parameterDescriptorFactory);
        container.setDefaultValues();
        return container;
    }

    private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Void, Void> {

        protected MyProgressMonitorSwingWorker(Component parentComponent, String title) {
            super(parentComponent, title);
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Computing pixel values...", -1);
            AbstractButton runButton = getButton(ID_OK);
            runButton.setEnabled(false);
            try {
                GPF.createProduct("PixEx", parameterMap, ioForm.getSourceProducts());
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
                Object outputDir = parameterMap.get("outputDir");
                String message;
                if (outputDir != null) {
                    message = String.format(
                            "The pixel extraction tool has run successfully and written the result file(s) to %s.",
                            outputDir.toString());
                } else {
                    message = "The pixel extraction tool has run successfully and written the result file to to std.out.";
                }

                JOptionPane.showMessageDialog(getJDialog(), message);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException e) {
                appContext.handleError(e.getMessage(), e);
            } finally {
                AbstractButton runButton = getButton(ID_OK);
                runButton.setEnabled(true);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final DefaultAppContext context = new DefaultAppContext("dev0");
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(new PixExOp.Spi());

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

}
