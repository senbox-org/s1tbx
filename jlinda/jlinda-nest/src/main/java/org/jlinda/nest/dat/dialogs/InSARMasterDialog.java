package org.jlinda.nest.dat.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.ProductSetPanel;
import org.esa.nest.dat.util.ProductOpener;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.util.DialogUtils;
import org.esa.nest.gpf.OperatorUtils;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.stacks.MasterSelection;
import org.jlinda.core.stacks.OptimalMaster;

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
 * Find Optimal Master product for InSAR
 */
public class InSARMasterDialog extends ModelessDialog {

    DecimalFormat df = new DecimalFormat("0.00");

    private boolean ok = false;

    private final InSARFileModel outputFileModel = new InSARFileModel();
    private final ProductSetPanel inputProductListPanel = new ProductSetPanel(VisatApp.getApp(), "Input stack");
    private final ProductSetPanel outputProductListPanel = new ProductSetPanel(VisatApp.getApp(), "Overview", outputFileModel);

    private final Map<SLCImage, File> slcFileMap = new HashMap<SLCImage, File>(10);

    private JButton openBtn;
    private final JCheckBox searchDBCheckBox = new JCheckBox("Search Product Library");

    public InSARMasterDialog(String helpId) {
        super(VisatApp.getApp().getMainFrame(), "Stack Overview and Optimal InSAR Master Selection", ModalDialog.ID_OK_CANCEL_HELP, helpId);

        getButton(ID_OK).setText("Overview");
        getButton(ID_CANCEL).setText("Close");

        initContent();
    }

    private void initContent() {
        final JPanel contentPane = new JPanel(new BorderLayout());

        final JPanel buttonPanel1 = new JPanel(new GridLayout(10, 1));
        final JButton addAllBtn = DialogUtils.CreateButton("addAllBtn", "Add Opened", null, buttonPanel1);
        addAllBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final Product[] products = VisatApp.getApp().getProductManager().getProducts();
                final List<File> fileList = new ArrayList<File>(products.length);
                for (Product prod : products) {
                    final File file = prod.getFileLocation();
                    if (file != null && file.exists()) {
                        fileList.add(file);
                    }
                }
                inputProductListPanel.setProductFileList(fileList.toArray(new File[fileList.size()]));
            }
        });
        buttonPanel1.add(addAllBtn);

        final JButton clearBtn = DialogUtils.CreateButton("clearBtn", "Clear", null, buttonPanel1);
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
        openBtn = DialogUtils.CreateButton("openButton", "     Open     ", null, buttonPanel2);
        openBtn.setEnabled(false);
        openBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                File[] files = outputProductListPanel.getSelectedFiles();
                if (files.length == 0)                      // default to get all files
                    files = outputProductListPanel.getFileList();
                final ProductOpener opener = new ProductOpener(VisatApp.getApp());
                opener.openProducts(files);
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

    protected void onOK() {
        try {
            validate();

            final File[] inputFiles = inputProductListPanel.getFileList();

            final MasterSelection.IfgStack[] ifgStack = findInSARProducts(inputFiles);

            if (ifgStack == null) {
                openBtn.setEnabled(false);
                VisatApp.getApp().showWarningDialog("Optimal master not found");
            } else {
                final OptimalMaster dataStack = new MasterSelection();
                final int masterIndex = dataStack.findOptimalMaster(ifgStack);
                final MasterSelection.IfgPair[] slaveList = ifgStack[masterIndex].getMasterSlave();

                updateData(slaveList, masterIndex);

                openBtn.setEnabled(true);
                ok = true;
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog("Error: " + e.getMessage());
        }
    }

    public boolean IsOK() {
        return ok;
    }

    private void updateData(final MasterSelection.IfgPair[] slaveList, final int masterIndex) {
        outputFileModel.clear();
        final File mstFile = slcFileMap.get(slaveList[masterIndex].getMasterMetadata());
//        String test = df.format(slaveList[masterIndex].getCoherence());
        try {
            final Product productMst = ProductIO.readProduct(mstFile);
            final MetadataElement absRootMst = AbstractMetadata.getAbstractedMetadata(productMst);
            final String[] mstValues = new String[]{
                    productMst.getName(),
                    "Master",
                    OperatorUtils.getAcquisitionDate(absRootMst),
                    String.valueOf(absRootMst.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                    String.valueOf(absRootMst.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                    String.valueOf(df.format(slaveList[masterIndex].getPerpendicularBaseline())),
                    String.valueOf(df.format(slaveList[masterIndex].getTemporalBaseline())),
                    String.valueOf(df.format(slaveList[masterIndex].getCoherence())),
                    String.valueOf(df.format(slaveList[masterIndex].getHeightAmb())),
                    String.valueOf(df.format(slaveList[masterIndex].getDopplerDifference()))
            };
            outputFileModel.addFile(mstFile, mstValues);
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog("Unable to read " + mstFile.getName() + '\n' + e.getMessage());
        }

        for (MasterSelection.IfgPair slave : slaveList) {
            final File slvFile = slcFileMap.get(slave.getSlaveMetadata());
            if (!slvFile.equals(mstFile)) {
                try {
                    final Product product = ProductIO.readProduct(slvFile);
                    final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

                    final String[] slvValues = new String[]{
                            product.getName(),
                            "Slave",
                            OperatorUtils.getAcquisitionDate(absRoot),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                            String.valueOf(df.format(slave.getPerpendicularBaseline())),
                            String.valueOf(df.format(slave.getTemporalBaseline())),
                            String.valueOf(df.format(slave.getCoherence())),
                            String.valueOf(df.format(slave.getHeightAmb())),
                            String.valueOf(df.format(slave.getDopplerDifference()))
                    };
                    outputFileModel.addFile(slvFile, slvValues);
                } catch (Exception e) {
                    VisatApp.getApp().showErrorDialog("Unable to read " + slvFile.getName() + '\n' + e.getMessage());
                }
            }
        }
    }

    private MasterSelection.IfgStack[] findInSARProducts(final File[] inputFiles) {
        final int size = inputFiles.length;
        final List<SLCImage> imgList = new ArrayList<SLCImage>(size);
        final List<Orbit> orbList = new ArrayList<Orbit>(size);

        for (File file : inputFiles) {
            try {
                final Product product = ProductIO.readProduct(file);
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final SLCImage img = new SLCImage(absRoot);
                final Orbit orb = new Orbit(absRoot, 3);

                slcFileMap.put(img, file);

                imgList.add(img);
                orbList.add(orb);
            } catch (IOException e) {
                VisatApp.getApp().showErrorDialog("Error: unable to read " + file.getPath() + '\n' + e.getMessage());
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Error: " + file.getPath() + '\n' + e.getMessage());
            }
        }

        try {
            final OptimalMaster dataStack = new MasterSelection();
            dataStack.setInput(imgList.toArray(new SLCImage[size]), orbList.toArray(new Orbit[size]));

            final Worker worker = new Worker(VisatApp.getApp().getMainFrame(), "Computing Optimal InSAR Master",
                    dataStack);
            worker.executeWithBlocking();

            return (MasterSelection.IfgStack[]) worker.get();

        } catch (Throwable t) {
            VisatApp.getApp().showErrorDialog("Error:" + t.getMessage());
            return null;
        }
    }

    private static class Worker extends ProgressMonitorSwingWorker {
        private final OptimalMaster dataStack;

        Worker(final Component component, final String title,
               final OptimalMaster optimalMaster) {
            super(component, title);
            this.dataStack = optimalMaster;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            return dataStack.getCoherenceScores(pm);
        }
    }

}