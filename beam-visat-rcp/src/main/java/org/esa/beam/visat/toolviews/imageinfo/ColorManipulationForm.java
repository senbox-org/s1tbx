/*
 * $Id: ContrastStretchToolView.java,v 1.2 2007/04/20 08:49:14 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponentDescriptor;
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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private AbstractButton applyButton;
    private AbstractButton resetButton;
    private AbstractButton multiApplyButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private SuppressibleOptionPane suppressibleOptionPane;

    private ProductSceneView productSceneView;
    private Band[] bandsToBeModified;
    private BeamFileFilter beamFileFilter;
    private final ProductNodeListener productNodeListener;
    private boolean defaultColorPalettesInstalled;
    private JPanel contentPanel;
    private final ColorManipulationToolView toolView;
    private ColorManipulationChildForm childForm;
    private ColorManipulationChildForm continuous1BandSwitcherForm;
    private ColorManipulationChildForm discrete1BandTabularForm;
    private ColorManipulationChildForm continuous3BandGraphicalForm;
    private JPanel toolButtonsPanel;
    private AbstractButton helpButton;
    private File ioDir;
    private JPanel editorPanel;
    private ImageInfo imageInfo; // our model!
    private MoreOptionsPane moreOptionsPane;

    public ColorManipulationForm(ColorManipulationToolView colorManipulationToolView) {
        this.toolView = colorManipulationToolView;
        visatApp = VisatApp.getApp();
        preferences = visatApp.getPreferences();
        productNodeListener = new ColorManipulationPNL();
    }

    public ProductSceneView getProductSceneView() {
        return productSceneView;
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

    public ImageInfo getImageInfo() {
        return imageInfo;
    }


    ChangeListener createApplyEnablerChangeListener() {
        return new ApplyEnablerCL();
    }

    TableModelListener createApplyEnablerTableModelListener() {
        return new ApplyEnablerTML();
    }

    private void setProductSceneView(final ProductSceneView productSceneView) {
        ProductSceneView productSceneViewOld = this.productSceneView;
        if (productSceneViewOld != null) {
            productSceneViewOld.getProduct().removeProductNodeListener(productNodeListener);
        }
        this.productSceneView = productSceneView;
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().addProductNodeListener(productNodeListener);
        }

        if (this.productSceneView != null) {
            setImageInfoCopy(this.productSceneView.getScene().getImageInfo());
        }

        installChildForm(productSceneViewOld);

        updateTitle();
        updateToolButtons();

        setApplyEnabled(false);
    }

    private void installChildForm(ProductSceneView productSceneViewOld) {
        final ColorManipulationChildForm oldForm = childForm;
        ColorManipulationChildForm newForm = EmptyImageInfoForm.INSTANCE;
        if (productSceneView != null) {
            if (isContinuous3BandImage()) {
                if (oldForm instanceof Continuous3BandGraphicalForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous3BandGraphicalForm();
                }
            } else if (isContinuous1BandImage()) {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            } else if (isDiscrete1BandImage()) {
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
                oldForm.handleFormHidden(productSceneViewOld);
            }
            childForm.handleFormShown(productSceneView);
        } else {
            childForm.updateFormModel(productSceneView);
        }
    }

    private void updateTitle() {
        String titlePostfix = "";
        if (productSceneView != null) {
            titlePostfix = " - " + productSceneView.getScene().getName();
        }
        // todo - this doesn't work at all!!!
        toolView.setTitle(getToolViewDescriptor().getTitle() + titlePostfix);
    }

    private void updateToolButtons() {
        final boolean hasSceneView = this.productSceneView != null;
        resetButton.setEnabled(hasSceneView);
        importButton.setEnabled(hasSceneView && !isRgbMode());
        exportButton.setEnabled(hasSceneView && !isRgbMode());
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

    private boolean isContinuous3BandImage() {
        return productSceneView.isRGB();
    }

    private boolean isContinuous1BandImage() {
        return (productSceneView.getRaster() instanceof Band)
                && ((Band) productSceneView.getRaster()).getIndexCoding() == null;
    }

    private boolean isDiscrete1BandImage() {
        return (productSceneView.getRaster() instanceof Band)
                && ((Band) productSceneView.getRaster()).getIndexCoding() != null;
    }

    private PageComponentDescriptor getToolViewDescriptor() {
        return toolView.getDescriptor();
    }

    private void initContentPanel() {

        moreOptionsPane = new MoreOptionsPane(this);

        applyButton = new JButton("Apply");
        applyButton.setName("ApplyButton");
        applyButton.setMnemonic('A');
        applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                applyChanges();
            }
        });

        resetButton = createButton("icons/Undo24.gif");
        resetButton.setName("ResetButton");
        resetButton.setToolTipText("Reset to defaults"); /*I18N*/
        resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                resetToDefaults();
            }
        });

        multiApplyButton = createButton("icons/MultiAssignBands24.gif");
        multiApplyButton.setName("MultiApplyButton");
        multiApplyButton.setToolTipText("Apply to other bands"); /*I18N*/
        multiApplyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                applyMultipleColorPaletteDef();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("ImportButton");
        importButton.setToolTipText("Import settings from text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                importColorPaletteDef();
            }
        });
        importButton.setEnabled(true);

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("ExportButton");
        exportButton.setToolTipText("Export settings to text file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                exportColorPaletteDef();
            }
        });
        exportButton.setEnabled(true);

        helpButton = createButton("icons/Help24.gif");
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


    public void setApplyEnabled(final boolean enabled) {
        final boolean canApply = productSceneView != null;
        applyButton.setEnabled(canApply && enabled);
        multiApplyButton.setEnabled(canApply && (!enabled && (!isRgbMode() && visatApp != null)));
    }

    void installToolButtons() {
        toolButtonsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.insets.bottom = 3;
        gbc.gridy = 1;
        toolButtonsPanel.add(applyButton, gbc);
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


    private void applyChanges() {
        setApplyEnabled(false);
        if (productSceneView != null) {
            try {
                getToolViewPaneControl().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (!isRgbMode()) {
                    productSceneView.getRaster().setImageInfo(imageInfo);
                    productSceneView.getScene().setImageInfo(imageInfo);
                } else {
                    productSceneView.getScene().setRasters(childForm.getRasters());
                    productSceneView.getScene().setImageInfo(imageInfo);
                }
                setImageInfoCopy(imageInfo);
                childForm.updateFormModel(productSceneView);
                VisatApp.getApp().updateImage(productSceneView);
            } finally {
                getToolViewPaneControl().setCursor(Cursor.getDefaultCursor());
            }
        }
        setApplyEnabled(false);
    }

    private void setImageInfoCopy(ImageInfo imageInfo) {
        this.imageInfo = imageInfo.createDeepCopy();
    }

    private void resetToDefaults() {
        if (productSceneView != null) {
            setImageInfoCopy(createDefaultImageInfo());
            childForm.updateFormModel(getProductSceneView());
            applyButton.setEnabled(true);
        }
    }

    private void applyMultipleColorPaletteDef() {
        if (productSceneView == null) {
            return;
        }

        final Product selectedProduct = productSceneView.getProduct();
        final ProductManager productManager = selectedProduct.getProductManager();
        final RasterDataNode[] protectedRasters = productSceneView.getRasters();
        final ArrayList<Band> availableBandList = new ArrayList<Band>();
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
                                                        bandsToBeModified);
        final List<Band> modifiedRasterList = new ArrayList<Band>(availableBands.length);
        if (bandChooser.show() == BandChooser.ID_OK) {
            bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : bandsToBeModified) {
                applyColorPaletteDef(getImageInfo().getColorPaletteDef(), band, band.getImageInfo());
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

    private File getIODir() {
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
        final ImageInfo targetImageInfo = getImageInfo();
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
        if (file != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null && file.canRead()) {
                try {
                    final ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(file);
                    applyColorPaletteDef(colorPaletteDef, getProductSceneView().getRaster(), targetImageInfo);
                    setImageInfoCopy(targetImageInfo);
                    childForm.updateFormModel(getProductSceneView());
                    setApplyEnabled(true);
                } catch (IOException e) {
                    showErrorDialog("Failed to import colour palette:\n" + e.getMessage());
                }
            }
        }
    }

    private void applyColorPaletteDef(ColorPaletteDef colorPaletteDef, RasterDataNode targetRaster,
                                      ImageInfo targetImageInfo) {
        boolean colorsOnly = false;
        if (targetRaster instanceof Band) {
            colorsOnly = ((Band) targetRaster).getIndexCoding() != null;
        }
        if (colorsOnly) {
            final Color[] newColors = colorPaletteDef.getColors();
            final int pointCount = targetImageInfo.getColorPaletteDef().getNumPoints();
            for (int i = 0; i < pointCount; i++) {
                final ColorPaletteDef.Point at = targetImageInfo.getColorPaletteDef().getPointAt(i);
                at.setColor(newColors[i % newColors.length]);
            }
        } else {
            targetImageInfo.transferColorPaletteDef(colorPaletteDef, false);
        }
    }

    private void exportColorPaletteDef() {
        final ImageInfo imageInfo = getImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreacable because, the export Button
            // is disabled if the _contrastStretchPane have no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Colour Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showSaveDialog(getToolViewPaneControl());
        File file = fileChooser.getSelectedFile();
        if (file != null) {
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


    private boolean isRgbMode() {
        return productSceneView != null && isContinuous3BandImage();
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

    public ImageInfo createDefaultImageInfo() {
        try {
            return ProductUtils.createImageInfo(productSceneView.getRasters(), false, ProgressMonitor.NULL);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                                          "Failed to create default image settings:\n" + e.getMessage(),
                                          "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public RasterDataNode.Stx getStx(RasterDataNode raster) {
        try {
            return raster.getStx(ProgressMonitor.NULL); // todo - use PM
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                                          "Failed to load statistics for '" +
                                                  raster.getName() + "':\n" + e.getMessage(),
                                          "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
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
                } else if (RasterDataNode.PROPERTY_NAME_STATISTICS.equalsIgnoreCase(propertyName)) {
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.isValidMaskProperty(propertyName)) {
                    if (raster.getStx() != null) {
                        raster.getStx().setDirty(true); // force recreation of statistics
                    }
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
            final ProductSceneView view = getProductSceneViewByFrame(e);
            if (getProductSceneView() == view) {
                setProductSceneView(null);
            }
        }

        private ProductSceneView getProductSceneViewByFrame(final InternalFrameEvent e) {
            ProductSceneView view = null;
            if (e.getInternalFrame().getContentPane() instanceof ProductSceneView) {
                view = (ProductSceneView) e.getInternalFrame().getContentPane();
            }
            return view;
        }
    }

    class ApplyEnablerCL implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            setApplyEnabled(true);
        }
    }


    class ApplyEnablerTML implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            setApplyEnabled(true);
        }
    }

}