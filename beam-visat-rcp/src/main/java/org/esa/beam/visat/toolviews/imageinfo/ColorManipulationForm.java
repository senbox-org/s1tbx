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
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


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

    private ProductNodeListener rasterDataChangedListener;

    private ProductSceneView productSceneView;
    private Band[] bandsToBeModified;
    private BeamFileFilter beamFileFilter;
    private final ProductNodeListener productNodeListener;
    private boolean auxDataInstalled;
    private JPanel contentPanel;
    private final ColorManipulationToolView toolView;
    private ImageInfoEditor imageInfoEditor;
    private ImageInfoEditor continuous1BandSwitcherForm;
    private ImageInfoEditor discrete1BandTabularForm;
    private ImageInfoEditor continuous3BandGraphicalForm;
    private JPanel buttonsPanel;
    private AbstractButton helpButton;
    private File ioDir;
    private NoDataColorForm noDataColorForm;
    private JPanel editorPanel;

    public ColorManipulationForm(ColorManipulationToolView colorManipulationToolView) {
        this.toolView = colorManipulationToolView;
        visatApp = VisatApp.getApp();
        preferences = visatApp.getPreferences();
        productNodeListener = createProductNodeListener();
        noDataColorForm = new NoDataColorForm();
    }

    public ProductSceneView getProductSceneView() {
        return productSceneView;
    }

    public void setProductSceneView(final ProductSceneView productSceneView) {
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().removeProductNodeListener(productNodeListener);
        }
        this.productSceneView = productSceneView;
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().addProductNodeListener(productNodeListener);
        }
        installSpecificPaletteEditorForm();
        updateNoDataForm();
        updateState();
    }

    private void updateNoDataForm() {
        if (this.productSceneView != null) {
            ImageInfo imageInfo = this.productSceneView.getRaster().getImageInfo();
            if (imageInfo != null) {
                noDataColorForm.setNoDataColor(imageInfo.getNoDataColor());
            }
        }
    }

    private void installSpecificPaletteEditorForm() {
        final ImageInfoEditor oldForm = imageInfoEditor;
        ImageInfoEditor newForm = EmptyPaletteEditorForm.INSTANCE;
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
            imageInfoEditor = newForm;
            installSpecificFormUI();
            oldForm.handleFormHidden();
            imageInfoEditor.handleFormShown(productSceneView);
        }
    }

    private ImageInfoEditor getContinuous3BandGraphicalForm() {
        if (continuous3BandGraphicalForm == null) {
            continuous3BandGraphicalForm = new Continuous3BandGraphicalForm(this);
        }
        return continuous3BandGraphicalForm;
    }

    private ImageInfoEditor getContinuous1BandSwitcherForm() {
        if (continuous1BandSwitcherForm == null) {
            continuous1BandSwitcherForm = new Continuous1BandSwitcherForm(this);
        }
        return continuous1BandSwitcherForm;
    }

    private ImageInfoEditor getDiscrete1BandTabularForm() {
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
                && ((Band)productSceneView.getRaster()).getIndexCoding() == null;
    }

    public boolean isDiscrete1BandImage() {
        return (productSceneView.getRaster() instanceof Band)
                && ((Band)productSceneView.getRaster()).getIndexCoding() != null;
    }

    public PageComponentDescriptor getPageComponentDescriptor() {
        return toolView.getDescriptor();
    }

    private void updateTitle() {
        toolView.setTitle(getPageComponentDescriptor().getTitle() + " - " + imageInfoEditor.getTitle(productSceneView));
    }

    private void updateState() {
        updateTitle();

        setApplyEnabled(false);
        final boolean enabled = productSceneView != null;
        resetButton.setEnabled(enabled);
        importButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
        imageInfoEditor.updateState(productSceneView);
        noDataColorForm.enable(enabled);

        if (!auxDataInstalled) {
            installAuxData();
        }
    }

    public JPanel getContentPanel() {
        if (contentPanel == null) {
            initContentPanel();
        }
        return contentPanel;
    }

    public void initContentPanel() {

        imageInfoEditor = EmptyPaletteEditorForm.INSTANCE;

        applyButton = new JButton("Apply");
        applyButton.setName("ApplyButton");
        applyButton.setMnemonic('A');
        applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                apply();
            }
        });

        resetButton = createButton("icons/Undo24.gif");
        resetButton.setName("ResetButton");
        resetButton.setToolTipText("Reset to default values"); /*I18N*/
        resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                reset();
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
        buttonsPanel = GridBagUtils.createPanel();

        contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.setPreferredSize(new Dimension(320, 200));
        contentPanel.add(editorPanel, BorderLayout.CENTER);
        contentPanel.add(buttonsPanel, BorderLayout.EAST);

        installSpecificFormUI();

        if (getPageComponentDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getPageComponentDescriptor().getHelpId());
            HelpSys.enableHelpKey(getToolViewPaneControl(), getPageComponentDescriptor().getHelpId());
        }

        final ProductSceneView productSceneView = visatApp.getSelectedProductSceneView();
        addDataChangedListener(productSceneView);
        setProductSceneView(productSceneView);
        setSuppressibleOptionPane(visatApp.getSuppressibleOptionPane());

        rasterDataChangedListener = createRasterDataChangedListner();

        // Add an internal frame listsner to VISAT so that we can update our
        // contrast stretch dialog with the information of the currently activated
        // product scene view.
        //
        visatApp.addInternalFrameListener(new ContrastStretchIFL());

        noDataColorForm.getValueContainer().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                setNoDataColor();
            }
        });

        updateState();
    }

    public void installSpecificFormUI() {
        installButtons();

        editorPanel.removeAll();
        editorPanel.add(imageInfoEditor.getContentPanel(), BorderLayout.CENTER);
        editorPanel.add(noDataColorForm.getContentPanel(), BorderLayout.SOUTH);

        revalidate();
    }

    private void installButtons() {
        buttonsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.insets.bottom = 3;
        gbc.gridy = 1;
        buttonsPanel.add(applyButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        buttonsPanel.add(resetButton, gbc);
        buttonsPanel.add(multiApplyButton, gbc);
        gbc.gridy++;
        buttonsPanel.add(importButton, gbc);
        buttonsPanel.add(exportButton, gbc);
        gbc.gridy++;
        AbstractButton[] additionalButtons = imageInfoEditor.getButtons();
        for (int i = 0; i < additionalButtons.length; i++) {
            AbstractButton button = additionalButtons[i];
            buttonsPanel.add(button, gbc);
            if (i % 2 == 1) {
                gbc.gridy++;
            }
        }
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonsPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 1;
        buttonsPanel.add(helpButton, gbc);
    }

    public SuppressibleOptionPane getSuppressibleOptionPane() {
        return suppressibleOptionPane;
    }

    public void setSuppressibleOptionPane(final SuppressibleOptionPane suppressibleOptionPane) {
        this.suppressibleOptionPane = suppressibleOptionPane;
    }

    public void showMessageDialog(String propertyName, String message, String title) {
        getSuppressibleOptionPane().showMessageDialog(propertyName,
                                                      getToolViewPaneControl(),
                                                      message,
                                                      getPageComponentDescriptor().getTitle() + title,
                                                      JOptionPane.INFORMATION_MESSAGE);
    }


    private void apply() {
        setApplyEnabled(false);
        if (productSceneView != null) {
            try {
                getToolViewPaneControl().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                imageInfoEditor.performApply(productSceneView);
                VisatApp.getApp().updateImage(productSceneView);
            } finally {
                getToolViewPaneControl().setCursor(Cursor.getDefaultCursor());
            }
        }
    }


    public void setApplyEnabled(final boolean enabled) {
        final boolean canApply = productSceneView != null;
        applyButton.setEnabled(canApply && enabled);
        multiApplyButton.setEnabled(canApply && (!enabled && (!isRgbMode() && visatApp != null)));
    }

    public void reset() {
        if (productSceneView != null) {
            imageInfoEditor.performReset(productSceneView);
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
                                          getPageComponentDescriptor().getTitle(),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        final BandChooser bandChooser = new BandChooser(toolView.getPaneWindow(),
                                                        "Apply to other bands", /*I18N*/
                                                        getPageComponentDescriptor().getHelpId(),
                                                        availableBands,
                                                        bandsToBeModified);
        final ArrayList<Band> modifiedRasterList = new ArrayList<Band>();
        if (bandChooser.show() == BandChooser.ID_OK) {
            final ImageInfo imageInfo = imageInfoEditor.getCurrentImageInfo();
            bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : bandsToBeModified) {
                band.getImageInfo().transferColorPaletteDef(imageInfo, false);
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
        this.ioDir = dir; 
    }

    private File getIODir() {
        if (ioDir == null) {
            if (preferences != null) {
                ioDir = new File(preferences.getPropertyString(PREFERENCES_KEY_IO_DIR, getSystemAuxdataDir().getPath()));
            } else {
                ioDir = getSystemAuxdataDir();
            }
        }
        return ioDir;
    }

    private BeamFileFilter getOrCreateColorPaletteDefinitionFileFilter() {
        if (beamFileFilter == null) {
            final String formatName = "COLOR_PALETTE_DEFINITION_FILE";
            final String description = "Color palette definition files (*" + FILE_EXTENSION + ")";  /*I18N*/
            beamFileFilter = new BeamFileFilter(formatName, FILE_EXTENSION, description);
        }
        return beamFileFilter;
    }

    private void importColorPaletteDef() {
        final ImageInfo imageInfo = imageInfoEditor.getCurrentImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreachable because, the export Button
            // is disabled if the _contrastStretchPane has no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Color Palette Definition"); /*I18N*/
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
                    imageInfo.transferColorPaletteDef(colorPaletteDef, false);
                    apply();
                } catch (IOException e) {
                    showErrorDialog("Failed to import color palette definition.\n" + e.getMessage());
                }
            }
        }
    }

    private void exportColorPaletteDef() {
        final ImageInfo imageInfo = imageInfoEditor.getCurrentImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreacable because, the export Button
            // is disabled if the _contrastStretchPane have no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Color Palette Definition"); /*I18N*/
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
                    showErrorDialog("Failed to export color palette definition.\n" + e.getMessage());  /*I18N*/
                }
            }
        }
    }


    public static AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    public static AbstractButton createToggleButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), true);
    }

    public boolean isRgbMode() {
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

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                final String propertyName = event.getPropertyName();
                if (Product.PROPERTY_NAME_NAME.equalsIgnoreCase(propertyName)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((isContinuous3BandImage() && sourceNode == productSceneView.getProduct())
                            || sourceNode == productSceneView.getRaster()) {
                        updateTitle();
                    }
                } else if (DataNode.PROPERTY_NAME_UNIT.equalsIgnoreCase(propertyName)) {
                    // todo - ???
                    // final DataNode dataNode = (DataNode) event.getSourceNode();
                    // _colorPaletteEditorPanel.setUnit(dataNode.getUnit());
                    // revalidate();
                }
            }
        };
    }


    private void addDataChangedListener(final ProductSceneView productSceneView) {
        if (productSceneView != null) {
            final Product product = productSceneView.getProduct();
            if (product != null) {
                product.addProductNodeListener(rasterDataChangedListener);
            }
        }
    }

    private void removeDataChangedListener(final ProductSceneView productSceneView) {
        if (productSceneView != null) {
            final Product product = productSceneView.getProduct();
            if (product != null) {
                product.removeProductNodeListener(rasterDataChangedListener);
            }
        }
    }

    private ProductNodeListener createRasterDataChangedListner() {
        return new ProductNodeListenerAdapter() {

            @Override
            public void nodeDataChanged(final ProductNodeEvent event) {
                recomputeDisplayInformation(event);
            }
        };
    }

    private void recomputeDisplayInformation(final ProductNodeEvent event) {
        if (event.getSourceNode() instanceof RasterDataNode) {
            final RasterDataNode rasterDataNode = (RasterDataNode) event.getSourceNode();
            final RasterDataNode[] rasters = getProductSceneView().getRasters();
            for (RasterDataNode raster : rasters) {
                if (raster == rasterDataNode && raster.getRasterData() != null) {
                    reset();
                }
            }
        }
    }

    public Component getToolViewPaneControl() {
        return toolView.getPaneControl();
    }

    public void revalidate() {
        getToolViewPaneControl().invalidate();
        getToolViewPaneControl().validate();
        getToolViewPaneControl().repaint();
    }

    private class ContrastStretchIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e);
            removeDataChangedListener(ColorManipulationForm.this.getProductSceneView());
            if (view != null) {
                addDataChangedListener(view);
                setProductSceneView(view);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e);
            if (ColorManipulationForm.this.getProductSceneView() == view) {
                removeDataChangedListener(ColorManipulationForm.this.getProductSceneView());
                ColorManipulationForm.this.setProductSceneView(null);
            }
        }

        private ProductSceneView getProductSceneView(final InternalFrameEvent e) {
            ProductSceneView view = null;
            if (e.getInternalFrame().getContentPane() instanceof ProductSceneView) {
                view = (ProductSceneView) e.getInternalFrame().getContentPane();
            }
            return view;
        }
    }

    private void installAuxData() {
        final URL codeSourceUrl = BeamUiActivator.class.getProtectionDomain().getCodeSource().getLocation();
        final File auxdataDir = getSystemAuxdataDir();
        final ResourceInstaller resourceInstaller = new ResourceInstaller(codeSourceUrl, "auxdata/color_palettes/", auxdataDir);
        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(toolView.getPaneControl(), "Installing Auxdata...") {
            @Override
            protected Object doInBackground(com.bc.ceres.core.ProgressMonitor progressMonitor) throws Exception {
                resourceInstaller.install(".*.cpd", progressMonitor);
                auxDataInstalled = true;
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

    private void setNoDataColor() {
        Color color = noDataColorForm.getNoDataColor();
        ImageInfo imageInfo = imageInfoEditor.getCurrentImageInfo();
        if (imageInfo != null) {
            imageInfo.setNoDataColor(color);
            imageInfoEditor.getContentPanel().repaint();
            setApplyEnabled(true);
        }
    }

}