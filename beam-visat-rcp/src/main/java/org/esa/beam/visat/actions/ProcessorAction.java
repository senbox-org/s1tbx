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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorStatusEvent;
import org.esa.beam.framework.processor.ProcessorStatusListener;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

/**
 * This action starts the associated processor.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ProcessorAction extends ExecCommand {

    private Processor _processor;
    private ProcessorApp _processorApp;
    private Class<?> processorClass;


    @Override
    public void actionPerformed(CommandEvent event) {
        try {
            showProcessorFrame(VisatApp.getApp(), getHelpId());
        } catch (CoreException e) {
            String msg = "Not able to create Processor";
            VisatApp.getApp().getLogger().log(Level.SEVERE, msg, e);
            VisatApp.getApp().showErrorDialog(msg + "\n" + e.getMessage());
        }
    }

    @Override
    public void updateComponentTreeUI() {
        if (_processorApp != null && _processorApp.getMainFrame() != null) {
            SwingUtilities.updateComponentTreeUI(_processorApp.getMainFrame());
        }
    }

    private Processor createProcessor() throws CoreException {
        try {
            return (Processor) processorClass.newInstance();
        } catch (Exception e) {
            throw new CoreException("Not able to create an instance of " + processorClass.getName());
        }
    }

    private void setProcessorClass(Class<?> processorClass) {
        this.processorClass = processorClass;
    }


    private void showProcessorFrame(final VisatApp visatApp, String helpId) throws CoreException {
        // if processor hasn't been created yet - do it now
        // ------------------------------------------------
        if (_processor == null) {
            _processor = createProcessor();
            _processor.addProcessorStatusListener(new StandardPSL());
            _processorApp = new ProcessorApp(_processor);
            _processorApp.setStandAlone(false);
            try {
                _processorApp.startUp(ProgressMonitor.NULL);
            } catch (Exception e) {
                visatApp.showErrorDialog("Failed to initialise processor application:\n" + e.getMessage());
                BeamLogManager.getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                _processor = null;
                _processorApp = null;
                return;
            }
            _processorApp.addRequestValidator(new VisatRequestValidator());
            if (helpId != null) {
                if (HelpSys.isValidID(helpId)) {
                    final JFrame mainFrame = _processorApp.getMainFrame();
                    HelpSys.enableHelpKey(mainFrame.getContentPane(), helpId);
                    HelpSys.enableHelpKey(mainFrame.getJMenuBar(), helpId);
                    _processorApp.setHelpID(helpId);
                }
            }
        }
        final boolean productSet = setDefaultInputProduct();
        if (productSet) {
            Debug.trace("ProcessorAction.showProcessorFrame: input product has been set");
        } else {
            Debug.trace("ProcessorAction.showProcessorFrame: input product has NOT been set");
        }
        _processorApp.getMainFrame().setVisible(true);
    }

    private boolean setDefaultInputProduct() {

        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        if (selectedProduct == null) {
            return false; // don't set input product, nothing selected
        }

        final Vector requestList;
        try {
            requestList = _processorApp.getRequests();
        } catch (ProcessorException e) {
            return false; // don't set input product, failed to obtain request list
        }

        if (requestList == null || requestList.size() == 0) {
            return false; // don't set input product, no requests defined
        }

        final ProductReader reader = selectedProduct.getProductReader();
        if ((reader == null) ||
            (reader.getSubsetDef() != null) ||
            (reader instanceof DimapProductReader && selectedProduct.isModified())) {
            return false; // don't set input product, product cannot be processed
        }


        final File currentDir1 = new File("").getAbsoluteFile();
        final File currentDir2 = new File(".").getAbsoluteFile();
        final File selectedProductFile = selectedProduct.getFileLocation();
        if (selectedProductFile == null) {
            return false;
        }

        final Request request = (Request) requestList.elementAt(0);
        if (request.getNumInputProducts() == 0) {
            request.addInputProduct(new ProductRef(selectedProductFile));
        } else if (request.getNumInputProducts() == 1) {
            final ProductRef inputProductRef = request.getInputProductAt(0);
            File inputProductFile = inputProductRef.getFile();
            inputProductFile = inputProductFile.getAbsoluteFile();
            if (inputProductFile.equals(currentDir1) || inputProductFile.equals(currentDir2)) {
                inputProductRef.setFile(selectedProductFile);
            }
        }

        // reset modified request list
        try {
            _processorApp.setRequests(requestList);
        } catch (ProcessorException e) {
            return false; // input product not set, invalid request list
        }

        return true;
    }

    private class VisatRequestValidator implements RequestValidator {

        public boolean validateRequest(Processor processor, Request request) {
            final int numOutputProducts = request.getNumOutputProducts();
            for (int i = 0; i < numOutputProducts; i++) {

                final ProductRef outputProductRef = request.getOutputProductAt(i);
                final File outputFile = new File(outputProductRef.getFilePath()).getAbsoluteFile();
                final int numOpenProducts = VisatApp.getApp().getProductManager().getProductCount();
                for (int j = 0; j < numOpenProducts; j++) {
                    final Product openProduct = VisatApp.getApp().getProductManager().getProduct(j);
                    File openFile = openProduct.getFileLocation();
                    if (openFile != null) {
                        openFile = openFile.getAbsoluteFile();
                        if (openFile.equals(outputFile)) {
                            _processorApp.showErrorDialog(
                                    "The output product\n" +
                                    "  " + outputFile.getPath() + "\n" +
                                    "already exists and is currently opened in " + VisatApp.getApp().getAppName() + ".\n" +
                                    "Close this product in " + VisatApp.getApp().getAppName() +
                                    " first or select a different name for it.");
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    private class StandardPSL implements ProcessorStatusListener {

        /**
         * Called if a new processing request is started.
         *
         * @param event processor event
         */
        public void handleProcessingStarted(ProcessorStatusEvent event) {
            Debug.trace("ProcessorAction.handleProcessingStarted");
        }

        /**
         * Called if the current processing request has been successfully completed.
         *
         * @param event processor event
         */
        public void handleProcessingCompleted(ProcessorStatusEvent event) {
            Debug.trace("ProcessorAction.handleProcessingCompleted");

            int status = event.getNewStatus();

            if (status == ProcessorConstants.STATUS_COMPLETED_WITH_WARNING) {
                String warningMessage = "There were errors during processing!\n" +
                                        "Please view log file for detailed information!";
                String[] warningDetails = _processor.getWarningMessages();
                VisatApp.getApp().showWarningsDialog(warningMessage, warningDetails);
            }

            final Request request = event.getRequest();

            final int numOutputProducts = request.getNumOutputProducts();

            if (numOutputProducts == 0) {
                final String finalMessage = _processor.getCompletionMessage();
                if (finalMessage != null) {
                    VisatApp.getApp().showInfoDialog(finalMessage, null);
                }
                return;
            }

            final List<File> filesToOpenInVisat = new ArrayList<File>();
            for (int i = 0; i < numOutputProducts; i++) {
                final ProductRef productRef = request.getOutputProductAt(i);
                final File productFile = new File(productRef.getFilePath());
                if (productFile.isFile()
                    && ProductIO.getProductReaderForFile(productFile) != null) {
                    filesToOpenInVisat.add(productFile);
                }
            }
            final int numFilesToOpen = filesToOpenInVisat.size();

            if (numFilesToOpen == 0) {
                VisatApp.getApp().showInfoDialog("The request has successfully been processed.", null); /*I18N*/
                return;
            }

            final StringBuffer msg = new StringBuffer();
            if (numFilesToOpen == 1) {
                msg.append("The output product has been written to\n"); /*I18N*/
                msg.append((filesToOpenInVisat.get(0)).getPath());
                msg.append("\n\n");
                msg.append("Do you want to open it in ");
                msg.append(VisatApp.getApp().getAppName());
                msg.append(" now?\n");/*I18N*/
            } else {
                msg.append(numFilesToOpen);
                msg.append(" output products have been written to\n"); /*I18N*/
                for (int i = 0; i < numFilesToOpen; i++) {
                    msg.append((filesToOpenInVisat.get(i)).getPath());
                    msg.append("\n");
                }
                msg.append("\n");
                msg.append("Do you want to open them in ");
                msg.append(VisatApp.getApp().getAppName());
                msg.append(" now?\n");/*I18N*/
            }

            final int answer = VisatApp.getApp().showQuestionDialog(_processor.getUITitle(), msg.toString(), null);

            if (answer == JOptionPane.NO_OPTION) {
                return;
            }

            for (final File productFile : filesToOpenInVisat) {
                if (!productFile.isFile()) {
                    final StringBuffer notFoundMsg = new StringBuffer();
                    notFoundMsg.append("For some reason the output product\n"); /*I18N*/
                    notFoundMsg.append("  ").append(productFile.getPath());
                    notFoundMsg.append("\ncould not be found.\n");
                    notFoundMsg.append("Please use ");
                    msg.append(VisatApp.getApp().getAppName());
                    msg.append(" to open it manually.\n");
                    VisatApp.getApp().showWarningDialog(notFoundMsg.toString());
                }

                try {
                    Product product = ProductIO.readProduct(productFile);
                    if (product != null) {
                        VisatApp.getApp().addProduct(product);
                    } else {
                        final StringBuffer notReadMsg = new StringBuffer();
                        notReadMsg.append("For some reason the output product\n"); /*I18N*/
                        notReadMsg.append("  ").append(productFile.getPath());
                        notReadMsg.append("\ncould not be read.\n");
                        notReadMsg.append("Please use ");
                        notReadMsg.append(VisatApp.getApp().getAppName());
                        notReadMsg.append(" to open it manually.\n");
                        VisatApp.getApp().showWarningDialog(notReadMsg.toString());
                    }
                } catch (IOException e) {
                    Debug.trace(e);
                    final StringBuffer readExceptionMsg = new StringBuffer();
                    readExceptionMsg.append("For some reason the output product\n"); /*I18N*/
                    readExceptionMsg.append("  ").append(productFile.getPath());
                    readExceptionMsg.append("\ncould not be read.\n");
                    readExceptionMsg.append("The following exception occurred:\n");
                    readExceptionMsg.append(e.getMessage()).append("\n");
                    readExceptionMsg.append("Please use ");
                    readExceptionMsg.append(VisatApp.getApp().getAppName());
                    readExceptionMsg.append(" to open it manually.\n");
                    VisatApp.getApp().showWarningDialog(readExceptionMsg.toString());
                }
            }
        }

        /**
         * Called if the current processing request has been aborted.
         *
         * @param event processor event
         */
        public void handleProcessingAborted(ProcessorStatusEvent event) {
            Debug.trace("ProcessorAction.handleProcessingAborted");
        }

        /**
         * Called if a processing error occurred.
         *
         * @param event processor event
         */
        public void handleProcessingFailed(ProcessorStatusEvent event) {
            Debug.trace("ProcessorAction.handleProcessingFailed");
        }

        /**
         * Called if a processing state changed.
         *
         * @param event the processor event
         */
        public void handleProcessingStateChanged(ProcessorStatusEvent event) {
            Debug.trace("ProcessorAction.handleProcessingStateChanged: status = " + event.getNewStatus());
        }

    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        String processorClassName = getConfigString(config, "processor");
        if (processorClassName != null) {
            Class<?> aClass;
            try {
                aClass = config.getDeclaringExtension().getDeclaringModule().loadClass(processorClassName);
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  processorClassName);
                throw new CoreException(msg, e);
            }
            if (Processor.class.isAssignableFrom(aClass)) {
                Class<Processor> processorClass = (Class<Processor>) aClass;
                setProcessorClass(processorClass);
            } else {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derieved from [{2}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  processorClassName,
                                                  Processor.class.getName());
                throw new CoreException(msg);
            }

        }
    }

}
