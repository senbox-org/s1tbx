/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.graphbuilder.rcp.dialogs.ProductSetPanel;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.ModelessDialog;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Find Optimal Reference product for InSAR
 */
public class InSARStackOverviewDialog extends ModelessDialog {

    DecimalFormat df = new DecimalFormat("0.00");

    private boolean ok = false;

    private final InSARFileModel outputFileModel = new InSARFileModel();
    private final AppContext appContext = SnapApp.getDefault().getAppContext();
    private final ProductSetPanel inputProductListPanel = new ProductSetPanel(appContext, "Input stack");
    private final ProductSetPanel outputProductListPanel = new ProductSetPanel(appContext, "Overview", outputFileModel);

    private final Map<SLCImage, File> slcFileMap = new HashMap<>(10);

    private JButton openBtn;
    private final JCheckBox searchDBCheckBox = new JCheckBox("Search Product Library");

    public InSARStackOverviewDialog() {
        super(SnapApp.getDefault().getMainFrame(), "Stack Overview and Optimal InSAR Reference Selection", ModalDialog.ID_OK_CANCEL_HELP, "InSARStackOverview");

        getButton(ID_OK).setText("Overview");
        getButton(ID_CANCEL).setText("Close");

        initContent();
    }

    public void setInputProductList(Product[] products) {
        final List<File> fileList = new ArrayList<>(products.length);
        for (Product prod : products) {
            final File file = prod.getFileLocation();
            if (file != null && file.exists()) {
                fileList.add(file);
            }
        }
        inputProductListPanel.setProductFileList(fileList.toArray(new File[fileList.size()]));
    }

    private void initContent() {
        final JPanel contentPane = new JPanel(new BorderLayout());

        final JPanel buttonPanel1 = new JPanel(new GridLayout(10, 1));
        final JButton addAllBtn = DialogUtils.createButton("addAllBtn", "Add Opened", null, buttonPanel1, DialogUtils.ButtonStyle.Text);
        addAllBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
                setInputProductList(products);
            }
        });
        buttonPanel1.add(addAllBtn);

        final JButton clearBtn = DialogUtils.createButton("clearBtn", "Clear", null, buttonPanel1, DialogUtils.ButtonStyle.Text);
        clearBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                inputProductListPanel.setProductFileList(new File[]{});
            }
        });
        buttonPanel1.add(clearBtn);

        inputProductListPanel.add(buttonPanel1, BorderLayout.EAST);
        contentPane.add(inputProductListPanel, BorderLayout.NORTH);

        // option pane
        final JPanel optionsPane = new JPanel(new GridBagLayout());
        optionsPane.setBorder(BorderFactory.createTitledBorder("Options"));
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.gridy++;
        DialogUtils.addComponent(optionsPane, gbc, "", searchDBCheckBox);
        gbc.gridy++;
        // not yet working
        //contentPane.add(optionsPane, BorderLayout.CENTER);

        final JPanel buttonPanel2 = new JPanel(new GridLayout(10, 1));
        openBtn = DialogUtils.createButton("openButton", "     Open     ", null, buttonPanel2, DialogUtils.ButtonStyle.Text);
        openBtn.setEnabled(false);
        openBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                File[] files = outputProductListPanel.getSelectedFiles();
                if (files.length == 0)                      // default to get all files
                    files = outputProductListPanel.getFileList();

                final org.esa.snap.rcp.actions.file.ProductOpener opener = new org.esa.snap.rcp.actions.file.ProductOpener();
                opener.setFiles(files);
                opener.setMultiSelectionEnabled(true);
                opener.openProduct();
            }
        });
        buttonPanel2.add(openBtn);
        outputProductListPanel.add(buttonPanel2, BorderLayout.EAST);
        contentPane.add(outputProductListPanel, BorderLayout.SOUTH);

        setContent(contentPane);
    }

    private void validate() throws Exception {
        final File[] inputFiles = inputProductListPanel.getFileList();
        if (inputFiles.length < 2) {
            throw new Exception("Please select at least two SLC products");
        }
    }

    private void processStack() {
        try {
            validate();

            final File[] inputFiles = inputProductListPanel.getFileList();

            final InSARStackOverview.IfgStack[] ifgStack = findInSARProducts(inputFiles);

            if (ifgStack == null) {
                openBtn.setEnabled(false);
                Dialogs.showWarning("Optimal reference not found");
            } else {
                final InSARStackOverview dataStack = new InSARStackOverview();
                final int referenceIndex = dataStack.findOptimalMaster(ifgStack);
                final InSARStackOverview.IfgPair[] secondaryList = ifgStack[referenceIndex].getMasterSlave();

                updateData(secondaryList, referenceIndex);

                openBtn.setEnabled(true);
                ok = true;
            }
        } catch (Exception e) {
            Dialogs.showError("Error: " + e.getMessage());
        }
    }

    protected void onOK() {
        processStack();
    }

    public boolean IsOK() {
        return ok;
    }

    private void updateData(final InSARStackOverview.IfgPair[] secondaryList, final int referenceIndex) {
        outputFileModel.clear();
        final File refFile = slcFileMap.get(secondaryList[referenceIndex].getMasterMetadata());

        try {
            final Product productRef = CommonReaders.readProduct(refFile);
            final MetadataElement absRootRef = AbstractMetadata.getAbstractedMetadata(productRef);
            final String[] refValues = new String[]{
                    productRef.getName(),
                    "Reference",
                    OperatorUtils.getAcquisitionDate(absRootRef),
                    String.valueOf(absRootRef.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                    String.valueOf(absRootRef.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                    String.valueOf(df.format(secondaryList[referenceIndex].getPerpendicularBaseline())),
                    String.valueOf(df.format(secondaryList[referenceIndex].getTemporalBaseline())),
                    String.valueOf(df.format(secondaryList[referenceIndex].getCoherence())),
                    String.valueOf(df.format(secondaryList[referenceIndex].getHeightAmb())),
                    String.valueOf(df.format(secondaryList[referenceIndex].getDopplerDifference()))
            };
            outputFileModel.addFile(refFile, refValues);
        } catch (Exception e) {
            Dialogs.showError("Unable to read " + refFile.getName() + '\n' + e.getMessage());
        }

        for (InSARStackOverview.IfgPair secondary : secondaryList) {
            final File secFile = slcFileMap.get(secondary.getSlaveMetadata());
            if (!secFile.equals(refFile)) {
                try {
                    final Product product = CommonReaders.readProduct(secFile);
                    final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

                    final String[] secValues = new String[]{
                            product.getName(),
                            "Secondary",
                            OperatorUtils.getAcquisitionDate(absRoot),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                            String.valueOf(df.format(secondary.getPerpendicularBaseline())),
                            String.valueOf(df.format(secondary.getTemporalBaseline())),
                            String.valueOf(df.format(secondary.getCoherence())),
                            String.valueOf(df.format(secondary.getHeightAmb())),
                            String.valueOf(df.format(secondary.getDopplerDifference()))
                    };
                    outputFileModel.addFile(secFile, secValues);
                } catch (Exception e) {
                    Dialogs.showError("Unable to read " + secFile.getName() + '\n' + e.getMessage());
                }
            }
        }
    }

    private InSARStackOverview.IfgStack[] findInSARProducts(final File[] inputFiles) {

        final List<SLCImage> imgList = new ArrayList<>(inputFiles.length);
        final List<Orbit> orbList = new ArrayList<>(inputFiles.length);

        for (File file : inputFiles) {
            try {
                final Product product = CommonReaders.readProduct(file);
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final SLCImage img = new SLCImage(absRoot, product);
                final Orbit orb = new Orbit(absRoot, 3);

                slcFileMap.put(img, file);

                imgList.add(img);
                orbList.add(orb);
            } catch (IOException e) {
                Dialogs.showError("Error: unable to read " + file.getPath() + '\n' + e.getMessage());
            } catch (Exception e) {
                Dialogs.showError("Error: " + file.getPath() + '\n' + e.getMessage());
            }
        }

        try {
            final InSARStackOverview dataStack = new InSARStackOverview();
            dataStack.setInput(imgList.toArray(new SLCImage[imgList.size()]), orbList.toArray(new Orbit[orbList.size()]));

            final Worker worker = new Worker(SnapApp.getDefault().getMainFrame(), "Computing Optimal InSAR Reference",
                                             dataStack);
            worker.executeWithBlocking();

            return (InSARStackOverview.IfgStack[]) worker.get();

        } catch (Throwable t) {
            Dialogs.showError("Error:" + t.getMessage());
            return null;
        }
    }

    private static class Worker extends ProgressMonitorSwingWorker {
        private final InSARStackOverview dataStack;

        Worker(final Component component, final String title,
               final InSARStackOverview optimalReference) {
            super(component, title);
            this.dataStack = optimalReference;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            return dataStack.getCoherenceScores(pm);
        }
    }

}
