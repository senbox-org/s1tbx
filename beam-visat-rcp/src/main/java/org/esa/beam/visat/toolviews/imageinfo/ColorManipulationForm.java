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
package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponentDescriptor;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * The contrast stretch window.
 */
class ColorManipulationForm {

    private final static String PREFERENCES_KEY_IO_DIR = "visat.color_palettes.dir";

    private final static String FILE_EXTENSION = ".cpd";
    private VisatApp visatApp;
    private PropertyMap preferences;
    private AbstractButton resetButton;
    private AbstractButton multiApplyButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private SuppressibleOptionPane suppressibleOptionPane;

    private final AbstractToolView toolView;
    private final FormModel formModel;
    private Band[] bandsToBeModified;
    private BeamFileFilter beamFileFilter;
    private final ProductNodeListener productNodeListener;
    private boolean defaultColorPalettesInstalled;
    private JPanel contentPanel;
    private ColorManipulationChildForm childForm;
    private ColorManipulationChildForm continuous1BandSwitcherForm;
    private ColorManipulationChildForm discrete1BandTabularForm;
    private ColorManipulationChildForm continuous3BandGraphicalForm;
    private JPanel toolButtonsPanel;
    private AbstractButton helpButton;
    private File ioDir;
    private JPanel editorPanel;
    private MoreOptionsPane moreOptionsPane;
    private SceneViewImageInfoChangeListener sceneViewChangeListener;
    private final String titlePrefix;

    public ColorManipulationForm(AbstractToolView colorManipulationToolView, FormModel formModel) {
        Assert.notNull(colorManipulationToolView);
        Assert.notNull(formModel);
        this.toolView = colorManipulationToolView;
        this.formModel = formModel;
        visatApp = VisatApp.getApp();
        preferences = visatApp.getPreferences();
        productNodeListener = new ColorManipulationPNL();
        sceneViewChangeListener = new SceneViewImageInfoChangeListener();
        titlePrefix = getToolViewDescriptor().getTitle();
    }

    public FormModel getFormModel() {
        return formModel;
    }

    public JPanel getContentPanel() {
        if (contentPanel == null) {
            initContentPanel();
        }
        if (!defaultColorPalettesInstalled) {
            installDefaultColorPalettes();
        }
        return contentPanel;
    }

    public void revalidateToolViewPaneControl() {
        getToolViewPaneControl().invalidate();
        getToolViewPaneControl().validate();
        getToolViewPaneControl().repaint();
        updateToolButtons();
    }

    public static AbstractButton createButton(final String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }

    public static AbstractButton createToggleButton(final String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), true);
    }

    private Component getToolViewPaneControl() {
        return toolView.getPaneControl();
    }

    private void setProductSceneView(final ProductSceneView productSceneView) {
        ProductSceneView productSceneViewOld = getFormModel().getProductSceneView();
        if (productSceneViewOld != null) {
            productSceneViewOld.getProduct().removeProductNodeListener(productNodeListener);
            productSceneViewOld.removePropertyChangeListener(sceneViewChangeListener);
        }
        getFormModel().setProductSceneView(productSceneView);
        if (getFormModel().isValid()) {
            getFormModel().getProductSceneView().getProduct().addProductNodeListener(productNodeListener);
            getFormModel().getProductSceneView().addPropertyChangeListener(sceneViewChangeListener);
        }

        if (getFormModel().isValid()) {
            getFormModel().setModifiedImageInfo(getFormModel().getOriginalImageInfo());
        }

        installChildForm();

        updateTitle();
        updateToolButtons();

        updateMultiApplyState();
    }

    private void installChildForm() {
        final ColorManipulationChildForm oldForm = childForm;
        ColorManipulationChildForm newForm = EmptyImageInfoForm.INSTANCE;
        if (getFormModel().isValid()) {
            if (getFormModel().isContinuous3BandImage()) {
                if (oldForm instanceof Continuous3BandGraphicalForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous3BandGraphicalForm();
                }
            } else if (getFormModel().isContinuous1BandImage()) {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            } else if (getFormModel().isDiscrete1BandImage()) {
                if (oldForm instanceof Discrete1BandTabularForm) {
                    newForm = oldForm;
                } else {
                    newForm = getDiscrete1BandTabularForm();
                }
            } else {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            }
        }

        if (newForm != oldForm) {
            childForm = newForm;

            installToolButtons();
            installMoreOptions();

            editorPanel.removeAll();
            editorPanel.add(childForm.getContentPanel(), BorderLayout.CENTER);
            if (!(childForm instanceof EmptyImageInfoForm)) {
                editorPanel.add(moreOptionsPane.getContentPanel(), BorderLayout.SOUTH);
            }
            revalidateToolViewPaneControl();

            if (oldForm != null) {
                oldForm.handleFormHidden(getFormModel());
            }
            childForm.handleFormShown(getFormModel());
        } else {
            childForm.updateFormModel(getFormModel());
        }
    }

    private void updateTitle() {
        String titlePostfix = "";
        if (getFormModel().isValid()) {
            titlePostfix = " - " + getFormModel().getModelName();
        }
        toolView.setTitle(titlePrefix + titlePostfix);
    }

    private void updateToolButtons() {
        resetButton.setEnabled(getFormModel().isValid());
        importButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
        exportButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
    }

    private ColorManipulationChildForm getContinuous3BandGraphicalForm() {
        if (continuous3BandGraphicalForm == null) {
            continuous3BandGraphicalForm = new Continuous3BandGraphicalForm(this);
        }
        return continuous3BandGraphicalForm;
    }

    private ColorManipulationChildForm getContinuous1BandSwitcherForm() {
        if (continuous1BandSwitcherForm == null) {
            continuous1BandSwitcherForm = new Continuous1BandSwitcherForm(this);
        }
        return continuous1BandSwitcherForm;
    }

    private ColorManipulationChildForm getDiscrete1BandTabularForm() {
        if (discrete1BandTabularForm == null) {
            discrete1BandTabularForm = new Discrete1BandTabularForm(this);
        }
        return discrete1BandTabularForm;
    }

    private PageComponentDescriptor getToolViewDescriptor() {
        return toolView.getDescriptor();
    }

    public ActionListener wrapWithAutoApplyActionListener(final ActionListener actionListener) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionListener.actionPerformed(e);
                applyChanges();
            }
        };
    }

    private void initContentPanel() {

        moreOptionsPane = new MoreOptionsPane(this);

        resetButton = createButton("icons/Undo24.gif");
        resetButton.setName("ResetButton");
        resetButton.setToolTipText("Reset to defaults"); /*I18N*/
        resetButton.addActionListener(wrapWithAutoApplyActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                resetToDefaults();
            }
        }));

        multiApplyButton = createButton("icons/MultiAssignBands24.gif");
        multiApplyButton.setName("MultiApplyButton");
        multiApplyButton.setToolTipText("Apply to other bands"); /*I18N*/
        multiApplyButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                applyMultipleColorPaletteDef();
            }
        });

        importButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-open.png");
        importButton.setName("ImportButton");
        importButton.setToolTipText("Import colour palette from text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                importColorPaletteDef();
                applyChanges();
            }
        });
        importButton.setEnabled(true);

        exportButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-save.png");
        exportButton.setName("ExportButton");
        exportButton.setToolTipText("Save colour palette to text file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                exportColorPaletteDef();
                childForm.updateFormModel(getFormModel());
            }
        });
        exportButton.setEnabled(true);

        helpButton = createButton("icons/Help22.png");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");

        editorPanel = new JPanel(new BorderLayout(4, 4));
        toolButtonsPanel = GridBagUtils.createPanel();

        contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.setPreferredSize(new Dimension(320, 200));
        contentPanel.add(editorPanel, BorderLayout.CENTER);
        contentPanel.add(toolButtonsPanel, BorderLayout.EAST);

        installHelp();
        suppressibleOptionPane = visatApp.getSuppressibleOptionPane();

        setProductSceneView(visatApp.getSelectedProductSceneView());

        // Add an internal frame listsner to VISAT so that we can update our
        // contrast stretch dialog with the information of the currently activated
        // product scene view.
        //
        visatApp.addInternalFrameListener(new ColorManipulationIFL());
    }


    public void updateMultiApplyState() {
        multiApplyButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
    }

    void installToolButtons() {
        toolButtonsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        toolButtonsPanel.add(resetButton, gbc);
        toolButtonsPanel.add(multiApplyButton, gbc);
        gbc.gridy++;
        toolButtonsPanel.add(importButton, gbc);
        toolButtonsPanel.add(exportButton, gbc);
        gbc.gridy++;
        AbstractButton[] additionalButtons = childForm.getToolButtons();
        for (int i = 0; i < additionalButtons.length; i++) {
            AbstractButton button = additionalButtons[i];
            toolButtonsPanel.add(button, gbc);
            if (i % 2 == 1) {
                gbc.gridy++;
            }
        }

        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        toolButtonsPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 1;
        toolButtonsPanel.add(helpButton, gbc);
    }

    void installMoreOptions() {
        final MoreOptionsForm moreOptionsForm = childForm.getMoreOptionsForm();
        if (moreOptionsForm != null) {
            moreOptionsForm.updateForm();
            moreOptionsPane.setComponent(moreOptionsForm.getContentPanel());
        }
    }

    // todo - code duplication in all tool views with help support!!! (nf 200905)
    private void installHelp() {
        if (getToolViewDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getToolViewDescriptor().getHelpId());
            HelpSys.enableHelpKey(getToolViewPaneControl(), getToolViewDescriptor().getHelpId());
        }
    }

    public void showMessageDialog(String propertyName, String message, String title) {
        suppressibleOptionPane.showMessageDialog(propertyName,
                                                 getToolViewPaneControl(),
                                                 message,
                                                 getToolViewDescriptor().getTitle() + title,
                                                 JOptionPane.INFORMATION_MESSAGE);
    }


    void applyChanges() {
        updateMultiApplyState();
        if (getFormModel().isValid()) {
            try {
                getToolViewPaneControl().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (getFormModel().isContinuous3BandImage()) {
                    getFormModel().setRasters(childForm.getRasters());
                } else {
                    getFormModel().getRaster().setImageInfo(getFormModel().getModifiedImageInfo());
                }
                getFormModel().applyModifiedImageInfo();
            } finally {
                getToolViewPaneControl().setCursor(Cursor.getDefaultCursor());
            }
        }
        updateMultiApplyState();
    }

    private void resetToDefaults() {
        if (getFormModel().isValid()) {
            getFormModel().setModifiedImageInfo(createDefaultImageInfo());
            childForm.resetFormModel(getFormModel());
        }
    }

    private void applyMultipleColorPaletteDef() {
        if (!getFormModel().isValid()) {
            return;
        }

        final Product selectedProduct = getFormModel().getProduct();
        final ProductManager productManager = selectedProduct.getProductManager();
        final RasterDataNode[] protectedRasters = getFormModel().getRasters();
        final ArrayList<Band> availableBandList = new ArrayList<>();
        for (int i = 0; i < productManager.getProductCount(); i++) {
            final Product product = productManager.getProduct(i);
            final Band[] bands = product.getBands();
            for (final Band band : bands) {
                boolean validBand = false;
                if (band.getImageInfo() != null) {
                    validBand = true;
                    for (RasterDataNode protectedRaster : protectedRasters) {
                        if (band == protectedRaster) {
                            validBand = false;
                        }
                    }
                }
                if (validBand) {
                    availableBandList.add(band);
                }
            }
        }
        final Band[] availableBands = new Band[availableBandList.size()];
        availableBandList.toArray(availableBands);
        availableBandList.clear();

        if (availableBands.length == 0) {
            JOptionPane.showMessageDialog(getToolViewPaneControl(),
                                          "No other bands available.", /*I18N*/
                                          getToolViewDescriptor().getTitle(),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        final BandChooser bandChooser = new BandChooser(toolView.getPaneWindow(),
                                                        "Apply to other bands", /*I18N*/
                                                        getToolViewDescriptor().getHelpId(),
                                                        availableBands,
                                                        bandsToBeModified, false);
        final List<Band> modifiedRasterList = new ArrayList<>(availableBands.length);
        if (bandChooser.show() == BandChooser.ID_OK) {
            bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : bandsToBeModified) {
                applyColorPaletteDef(getFormModel().getModifiedImageInfo().getColorPaletteDef(), band, band.getImageInfo());
                modifiedRasterList.add(band);
            }
        }

        final Band[] rasters = new Band[modifiedRasterList.size()];
        modifiedRasterList.toArray(rasters);
        modifiedRasterList.clear();
        visatApp.updateImages(rasters);
    }

    private void setIODir(final File dir) {
        if (preferences != null) {
            preferences.setPropertyString(PREFERENCES_KEY_IO_DIR, dir.getPath());
        }
        ioDir = dir;
    }

    protected File getIODir() {
        if (ioDir == null) {
            if (preferences != null) {
                ioDir = new File(
                        preferences.getPropertyString(PREFERENCES_KEY_IO_DIR, getSystemAuxdataDir().getPath()));
            } else {
                ioDir = getSystemAuxdataDir();
            }
        }
        return ioDir;
    }

    private BeamFileFilter getOrCreateColorPaletteDefinitionFileFilter() {
        if (beamFileFilter == null) {
            final String formatName = "COLOR_PALETTE_DEFINITION_FILE";
            final String description = "Colour palette files (*" + FILE_EXTENSION + ")";  /*I18N*/
            beamFileFilter = new BeamFileFilter(formatName, FILE_EXTENSION, description);
        }
        return beamFileFilter;
    }

    private void importColorPaletteDef() {
        final ImageInfo targetImageInfo = getFormModel().getModifiedImageInfo();
        if (targetImageInfo == null) {
            // Normaly this code is unreachable because, the export Button
            // is disabled if the _contrastStretchPane has no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Colour Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showOpenDialog(getToolViewPaneControl());
        final File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null && file.canRead()) {
                try {
                    final ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(file);
                    colorPaletteDef.getFirstPoint().setLabel(file.getName());
                    applyColorPaletteDef(colorPaletteDef, getFormModel().getRaster(), targetImageInfo);
                    getFormModel().setModifiedImageInfo(targetImageInfo);
                    childForm.updateFormModel(getFormModel());
                    updateMultiApplyState();
                } catch (IOException e) {
                    showErrorDialog("Failed to import colour palette:\n" + e.getMessage());
                }
            }
        }
    }

    private void applyColorPaletteDef(ColorPaletteDef colorPaletteDef,
                                      RasterDataNode targetRaster,
                                      ImageInfo targetImageInfo) {
        if (isIndexCoded(targetRaster)) {
            targetImageInfo.setColors(colorPaletteDef.getColors());
        } else {
            Stx stx = targetRaster.getStx(false, ProgressMonitor.NULL);
            Boolean autoDistribute = getAutoDistribute(colorPaletteDef);
            if (autoDistribute == null) {
                return;
            }
            targetImageInfo.setColorPaletteDef(colorPaletteDef,
                                               stx.getMinimum(),
                                               stx.getMaximum(),
                                               autoDistribute);
        }
    }

    private Boolean getAutoDistribute(ColorPaletteDef colorPaletteDef) {
        if (colorPaletteDef.isAutoDistribute()) {
            return Boolean.TRUE;
        }
        int answer = JOptionPane.showConfirmDialog(getToolViewPaneControl(),
                                                   "Automatically distribute points of\n" +
                                                   "colour palette between min/max?",
                                                   "Import Colour Palette",
                                                   JOptionPane.YES_NO_CANCEL_OPTION
        );
        if (answer == JOptionPane.YES_OPTION) {
            return Boolean.TRUE;
        } else if (answer == JOptionPane.NO_OPTION) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    private boolean isIndexCoded(RasterDataNode targetRaster) {
        return targetRaster instanceof Band && ((Band) targetRaster).getIndexCoding() != null;
    }

    private void exportColorPaletteDef() {
        final ImageInfo imageInfo = getFormModel().getModifiedImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreacable because, the export Button should be
            // disabled if the color manipulation form have no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Colour Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showSaveDialog(getToolViewPaneControl());
        File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                file = FileUtils.ensureExtension(file, FILE_EXTENSION);
                try {
                    final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
                    ColorPaletteDef.storeColorPaletteDef(colorPaletteDef, file);
                } catch (IOException e) {
                    showErrorDialog("Failed to export colour palette:\n" + e.getMessage());  /*I18N*/
                }
            }
        }
    }

    private void showErrorDialog(final String message) {
        if (message != null && message.trim().length() > 0) {
            if (visatApp != null) {
                visatApp.showErrorDialog(message);
            } else {
                JOptionPane.showMessageDialog(getToolViewPaneControl(),
                                              message,
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void installDefaultColorPalettes() {
        final URL codeSourceUrl = BeamUiActivator.class.getProtectionDomain().getCodeSource().getLocation();
        final File auxdataDir = getSystemAuxdataDir();
        final ResourceInstaller resourceInstaller = new ResourceInstaller(codeSourceUrl, "auxdata/color_palettes/",
                                                                          auxdataDir);
        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(toolView.getPaneControl(),
                                                                                "Installing Auxdata...") {
            @Override
            protected Object doInBackground(ProgressMonitor progressMonitor) throws Exception {
                resourceInstaller.install(".*.cpd", progressMonitor);
                defaultColorPalettesInstalled = true;
                return Boolean.TRUE;
            }

            /**
             * Executed on the <i>Event Dispatch Thread</i> after the {@code doInBackground}
             * method is finished. The default
             * implementation does nothing. Subclasses may override this method to
             * perform completion actions on the <i>Event Dispatch Thread</i>. Note
             * that you can query status inside the implementation of this method to
             * determine the result of this task or whether this task has been cancelled.
             *
             * @see #doInBackground
             * @see #isCancelled()
             * @see #get
             */
            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    visatApp.getLogger().log(Level.SEVERE, "Could not install auxdata", e);
                }
            }
        };
        swingWorker.executeWithBlocking();
    }

    private File getSystemAuxdataDir() {
        return new File(SystemUtils.getApplicationDataDir(), "beam-ui/auxdata/color-palettes");
    }

    private ImageInfo createDefaultImageInfo() {
        try {
            return ProductUtils.createImageInfo(getFormModel().getRasters(), false, ProgressMonitor.NULL);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                                          "Failed to create default image settings:\n" + e.getMessage(),
                                          "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    Stx getStx(RasterDataNode raster) {
        return raster.getStx(false, ProgressMonitor.NULL);
    }

    private class ColorManipulationPNL extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            final RasterDataNode[] rasters = childForm.getRasters();
            RasterDataNode raster = null;
            for (RasterDataNode dataNode : rasters) {
                if (event.getSourceNode() == dataNode) {
                    raster = (RasterDataNode) event.getSourceNode();
                }
            }
            if (raster != null) {
                final String propertyName = event.getPropertyName();
                if (ProductNode.PROPERTY_NAME_NAME.equalsIgnoreCase(propertyName)) {
                    updateTitle();
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_UNIT.equalsIgnoreCase(propertyName)) {
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_STX.equalsIgnoreCase(propertyName)) {
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.isValidMaskProperty(propertyName)) {
                    getStx(raster);
                }
            }
        }
    }

    private class ColorManipulationIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneViewByFrame(e);
            setProductSceneView(view);
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            if (getFormModel().getProductSceneView() == getProductSceneViewByFrame(e)) {
                setProductSceneView(null);
            }
        }

        private ProductSceneView getProductSceneViewByFrame(final InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                return (ProductSceneView) content;
            } else {
                return null;
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class SceneViewImageInfoChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProductSceneView.PROPERTY_NAME_IMAGE_INFO.equals(evt.getPropertyName())) {
                boolean correctFormForRaster = getFormModel().getRaster() == getFormModel().getProductSceneView().getRaster();
                if (correctFormForRaster) {
                    ImageInfo modifiedImageInfo = (ImageInfo) evt.getNewValue();
                    getFormModel().setModifiedImageInfo(modifiedImageInfo);
                    childForm.updateFormModel(getFormModel());
                }
            }
        }
    }
}
