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
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.pixex.PixExOp;
import org.esa.beam.util.io.WildcardMatcher;
import org.esa.beam.util.logging.BeamLogManager;

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
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

class PixelExtractionDialog extends ModelessDialog {

    public final static String HELP_ID_JAVA_HELP = "pixelExtraction";

    private final Map<String, Object> parameterMap;
    private final AppContext appContext;
    private final PixelExtractionIOForm ioForm;
    private final PixelExtractionParametersForm parametersForm;

    PixelExtractionDialog(AppContext appContext, String title) {
        super(appContext.getApplicationWindow(), title, ID_OK | ID_CLOSE | ID_HELP, HELP_ID_JAVA_HELP);

        this.appContext = appContext;

        AbstractButton button = getButton(ID_OK);
        button.setText("Extract");
        button.setMnemonic('E');

        parameterMap = new HashMap<String, Object>();
        final PropertyContainer propertyContainer = createParameterMap(parameterMap);

        final Class<PixExOp> operatorClass = PixExOp.class;
        final OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorClass,
                                                                                       propertyContainer,
                                                                                       parameterMap,
                                                                                       null);
        final OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                           operatorClass,
                                                           parameterSupport,
                                                           HELP_ID_JAVA_HELP);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());

        ioForm = new PixelExtractionIOForm(appContext, propertyContainer);
        ioForm.setInputChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Product[] sourceProducts = ioForm.getSourceProducts();
                if (sourceProducts.length > 0) {
                    parametersForm.setActiveProduct(sourceProducts[0]);
                    return;
                } else {
                    if (parameterMap.containsKey("sourceProductPaths")) {
                        final String[] inputPaths = (String[]) parameterMap.get("sourceProductPaths");
                        if (inputPaths.length > 0) {
                            Product firstProduct = openFirstProduct(inputPaths);
                            if (firstProduct != null) {
                                parametersForm.setActiveProduct(firstProduct);
                                return;
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

    private Product openFirstProduct(String[] inputPaths) {
        if (inputPaths != null) {

            final Logger logger = BeamLogManager.getSystemLogger();

            for (String inputPath : inputPaths) {
                if (inputPath == null || inputPath.trim().length() == 0) {
                    continue;
                }
                try {
                    final TreeSet<File> fileSet = new TreeSet<File>();
                    WildcardMatcher.glob(inputPath, fileSet);
                    for (File file : fileSet) {
                        final Product product = ProductIO.readProduct(file);
                        if (product != null) {
                            return product;
                        }
                    }
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    protected void onOK() {
        parameterMap.put("coordinates", parametersForm.getCoordinates());
        parameterMap.put("expression", parametersForm.getExpression());
        parameterMap.put("timeDifference", parametersForm.getAllowedTimeDifference());
        parameterMap.put("exportExpressionResult", parametersForm.isExportExpressionResultSelected());
        parameterMap.put("aggregatorStrategyType", parametersForm.getPixelValueAggregationMethod());
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
