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
package org.esa.beam.visat.toolviews.cspal;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The contrast stretch window.
 */
public class ContrastStretchToolView extends AbstractToolView {

    public static final String ID = ContrastStretchToolView.class.getName();
    private final static String _PREFERENCES_KEY_IO_DIR = "visat.color_palettes.dir";
    private final static String _FILE_EXTENSION = ".cpd";

    private final ImageInfo[] _rgbImageInfos;
    private final RasterDataNode[] _rgbBands;
    private final Color[] _rgbColors;

    private VisatApp _visatApp;
    private PropertyMap _preferences;
    private ProductSceneView _productSceneView;
    private ContrastStretchPane _contrastStretchPane;
    private AbstractButton _applyButton;
    private AbstractButton _resetButton;
    private AbstractButton _multiApplyButton;
    private AbstractButton _importButton;
    private AbstractButton _exportButton;
    private AbstractButton _autoStretch95Button;
    private AbstractButton _autoStretch100Button;
    private AbstractButton _zoomInVButton;
    private AbstractButton _zoomOutVButton;
    private AbstractButton _zoomInHButton;
    private AbstractButton _zoomOutHButton;
    private AbstractButton _evenDistButton;
    private JRadioButton _redButton;
    private JRadioButton _greenButton;
    private JRadioButton _blueButton;
    private JPanel _rgbSettingsPane;
    private JPanel _centerPane;
    private Parameter _rgbBandsParam;
    private JRadioButton _lastRGBButton;
    private Unloader _unloader;
    private SuppressibleOptionPane _suppressibleOptionPane;
    private ProductNodeListener _rasterDataChangedListener;


    private AbstractButton _imageEnhancementsButton;
    private boolean _imageEnhancementsVisible;
    private JPanel _imageEnhancementsPane;

    // Gamma support for RGB images
    private Parameter _gammaParam;

    // Histogram equalization
    private Parameter _histogramMatchingParam;
    private Band[] _bandsToBeModified;
    private BeamFileFilter _beamFileFilter;
    private final ProductNodeListener _productNodeListener;
    private boolean auxDataInstalled;


    public ContrastStretchToolView() {
        _visatApp = VisatApp.getApp();
        _preferences = VisatApp.getApp().getPreferences();
        _rgbColors = new Color[]{Color.red, Color.green, Color.blue};
        _rgbImageInfos = new ImageInfo[3];
        _rgbBands = new Band[3];
        _productNodeListener = createProductNodeListener();

        initParameters();
    }

    public ProductSceneView getProductSceneView() {
        return _productSceneView;
    }

    public void setProductSceneView(final ProductSceneView productSceneView) {
        setApplyEnabled(false);
        _lastRGBButton = null;

        if (_productSceneView != null) {
            _productSceneView.getProduct().removeProductNodeListener(_productNodeListener);
        }
        _productSceneView = productSceneView;
        if (_productSceneView != null) {
            _productSceneView.getProduct().addProductNodeListener(_productNodeListener);
            if (isRgbMode()) {
                _rgbBands[0] = _productSceneView.getRedRaster();
                _rgbBands[1] = _productSceneView.getGreenRaster();
                _rgbBands[2] = _productSceneView.getBlueRaster();

                final int length = _rgbBands.length;
                final int selectedRgbIndex = getSelectedRgbIndex();
                for (int i = 0; i < length; i++) {
                    final RasterDataNode node = _rgbBands[i];
                    if (node != null) {
                        setImageInfoAt(i, node.getImageInfo(), i == selectedRgbIndex);
                    }
                }

                if (_productSceneView.getHistogramMatching() != null) {
                    _histogramMatchingParam.setValue(_productSceneView.getHistogramMatching(), null);
                } else {
                    _histogramMatchingParam.setValue(ImageInfo.HISTOGRAM_MATCHING_OFF, null);
                }

                updateBandNames();
                _redButton.setSelected(true);
                _gammaParam.setUIEnabled(true);
                setRgbBandsComponentColors();
            } else {
                final RasterDataNode raster = _productSceneView.getRaster();
                setImageInfo(raster.getImageInfo());
                _gammaParam.setUIEnabled(false);
            }
            updateTitle();
        } else {
            clearBasicDisplayInfos();
            _gammaParam.setUIEnabled(false);
            setTitle(getDescriptor().getTitle());
        }
        updateUIState();
    }

    private void updateTitle() {
        final String titleAddition;
        if (isRgbMode()) {
            titleAddition = " - " + _productSceneView.getProduct().getProductRefString() + " RGB";     /*I18N*/
        } else {
            titleAddition = " - " + _productSceneView.getRaster().getDisplayName();
        }
        setTitle(getDescriptor().getTitle() + titleAddition);
    }

    public void setUnloader(final Unloader unloader) {
        _unloader = unloader;
    }

    private void updateUIState() {
        final boolean enabled = _productSceneView != null;
        _resetButton.setEnabled(enabled);
        _importButton.setEnabled(enabled);
        _exportButton.setEnabled(enabled);
        _autoStretch95Button.setEnabled(enabled);
        _autoStretch100Button.setEnabled(enabled);
        _zoomInVButton.setEnabled(enabled);
        _zoomOutVButton.setEnabled(enabled);
        _zoomInHButton.setEnabled(enabled);
        _zoomOutHButton.setEnabled(enabled);
        _redButton.setEnabled(enabled);
        _evenDistButton.setEnabled(enabled);
        _greenButton.setEnabled(enabled);
        _blueButton.setEnabled(enabled);
        _imageEnhancementsButton.setEnabled(enabled && isRgbMode());
        _rgbBandsParam.setUIEnabled(enabled);
        if (enabled) {
            if (isRgbMode()) {
                final int index = getSelectedRgbIndex();
                if (index != -1) {
                    updateImageInfo(index);
                }
                updateGamma(index);
                setImageEnhancementsPaneVisible(_imageEnhancementsVisible);
                showRgbButtons();
            } else {
                _contrastStretchPane.setUnit(_productSceneView.getRaster().getUnit());
                _contrastStretchPane.setRGBColor(null);
                setImageEnhancementsPaneVisible(false);
                hideRgbButtons();
            }
            revalidate();
        } else {
            setImageEnhancementsPaneVisible(false);
            _contrastStretchPane.setImageInfo(null);
        }
        if (!auxDataInstalled) {
            installAuxData();
        }
    }

    private void updateGamma(final int index) {
        final RasterDataNode raster = _productSceneView.getRasterAt(index);
        if (raster.getImageInfo() != null) {
            String text = "Gamma: "; /*I18N*/
            if (isRgbMode()) {
                text = index == 0 ? "Red gamma: " : index == 1 ? "Green gamma: " : "Blue gamma: "; /*I18N*/
            }
            _gammaParam.getEditor().getLabelComponent().setText(text);
            _gammaParam.setValue(raster.getImageInfo().getGamma(), null);
        }
    }

    private void setImageInfo(final ImageInfo imageInfo) {
        setImageInfoAt(0, imageInfo, true);
    }

    private void setImageInfoAt(final int index, final ImageInfo imageInfo, final boolean deliver) {
        _rgbImageInfos[index] = imageInfo;
        if (deliver) {
            setImageInfoCopyToContrastStretchPaneAt(index);
        }
    }

    private void setImageInfoCopyToContrastStretchPaneAt(final int index) {
        if (_rgbImageInfos[index] != null) {
            _contrastStretchPane.setImageInfo(_rgbImageInfos[index].createDeepCopy());
        } else {
            _contrastStretchPane.setImageInfo(null);
        }
        if (!isRgbMode()) {
            setApplyEnabled(false);
        }
    }

    private void setImageInfoOfContrastStretchPaneAt(final int index) {
        if (index >= 0 && index <= 2) {
            _rgbImageInfos[index] = _contrastStretchPane.getImageInfo();
        }
    }

    private void initParameters() {
        final ParamChangeListener rgbListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                final String name = _rgbBandsParam.getValueAsText();
                final RasterDataNode band = getBand(name);
                final int index = getSelectedRgbIndex();
                if (index == -1) {
                    return;
                }
                updateGamma(index);
                if (_rgbBands[index] != band) {
                    if (_unloader != null) {
                        _unloader.unloadUnusedRasterData(_rgbBands[index]);
                    }
                    _rgbBands[index] = band;
                    if (band != null) {
                        _contrastStretchPane.ensureValidImageInfo(band);
                        setImageInfoAt(index, band.getImageInfo(), true);
                    } else {
                        setImageInfoAt(index, null, true);
                    }
                    setApplyEnabled(true);
                }
            }
        };

        _rgbBandsParam = new Parameter("rgbBands");
        _rgbBandsParam.getProperties().setValueSet(new String[]{""});
        _rgbBandsParam.getProperties().setValueSetBound(true);
        _rgbBandsParam.addParamChangeListener(rgbListener);

        final ParamChangeListener imageEnhancementsListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                if (_contrastStretchPane != null && _contrastStretchPane.getImageInfo() != null) {
                    final float gamma = ((Number) _gammaParam.getValue()).floatValue();
                    _contrastStretchPane.getImageInfo().setGamma(gamma);
                    _contrastStretchPane.updateGamma();
                    apply();
                }
            }
        };
        _gammaParam = new Parameter("gamma", 1.0f);
        _gammaParam.getProperties().setDescription("Gamma");
        _gammaParam.getProperties().setDefaultValue(1.0f);
        _gammaParam.getProperties().setMinValue(1.0f / 10.0f);
        _gammaParam.getProperties().setMaxValue(10.0f);
        _gammaParam.getProperties().setNumCols(6);
        _gammaParam.addParamChangeListener(imageEnhancementsListener);

        _histogramMatchingParam = new Parameter("histogramMatching", ImageInfo.HISTOGRAM_MATCHING_OFF);
        _histogramMatchingParam.getProperties().setValueSet(new String[]{
                ImageInfo.HISTOGRAM_MATCHING_OFF,
                ImageInfo.HISTOGRAM_MATCHING_EQUALIZE,
                ImageInfo.HISTOGRAM_MATCHING_NORMALIZE
        });
        _histogramMatchingParam.getProperties().setNullValueAllowed(false);
        _histogramMatchingParam.getProperties().setValueSetBound(true);
        _histogramMatchingParam.getProperties().setLabel("Histogram matching");
        _histogramMatchingParam.getProperties().setDescription("Apply histogram matching");
        _histogramMatchingParam.addParamChangeListener(imageEnhancementsListener);
    }

    /*
     * IMPORTANT: We cannot directly  use _productSceneView.getProduct().getBand(name)
     * because _productSceneView can use temporary RGB bands not contained in the
     * product's band list.
     */
    private RasterDataNode getBand(final String name) {
        final RasterDataNode[] rasters = _productSceneView.getRasters();
        for (final RasterDataNode raster : rasters) {
            if (raster.getName().equalsIgnoreCase(name)) {
                return raster;
            }
        }
        return _productSceneView.getProduct().getBand(name);
    }

    @Override
    public JComponent createControl() {

        _contrastStretchPane = new ContrastStretchPane();
        _contrastStretchPane.addPropertyChangeListener(RasterDataNode.PROPERTY_NAME_IMAGE_INFO,
                                                       new PropertyChangeListener() {

                                                           /**
                                                            * This method gets called when a bound property is changed.
                                                            *
                                                            * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
                                                            */
                                                           public void propertyChange(final PropertyChangeEvent evt) {
                                                               setApplyEnabled(true);
                                                           }
                                                       });

        _applyButton = new JButton("Apply");
        _applyButton.setName("ApplyButton");
        _applyButton.setMnemonic('A');
        _applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                apply();
            }
        });

        _resetButton = createButton("icons/Undo24.gif");
        _resetButton.setName("ResetButton");
        _resetButton.setToolTipText("Reset to default values"); /*I18N*/
        _resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                reset();
            }
        });

        _multiApplyButton = createButton("icons/MultiAssignBands24.gif");
        _multiApplyButton.setName("MultiApplyButton");
        _multiApplyButton.setToolTipText("Apply to other bands"); /*I18N*/
        _multiApplyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                applyMultipleColorPaletteDef();
            }
        });

        _importButton = createButton("icons/Import24.gif");
        _importButton.setName("ImportButton");
        _importButton.setToolTipText("Import settings from text file."); /*I18N*/
        _importButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                importColorPaletteDef();
            }
        });
        _importButton.setEnabled(true);

        _exportButton = createButton("icons/Export24.gif");
        _exportButton.setName("ExportButton");
        _exportButton.setToolTipText("Export settings to text file."); /*I18N*/
        _exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                exportColorPaletteDef();
            }
        });
        _exportButton.setEnabled(true);

        _autoStretch95Button = createButton("icons/Auto95Percent24.gif");
        _autoStretch95Button.setName("AutoStretch95Button");
        _autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels"); /*I18N*/
        _autoStretch95Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                auto95();
            }
        });

        _autoStretch100Button = createButton("icons/Auto100Percent24.gif");
        _autoStretch100Button.setName("AutoStretch100Button");
        _autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels"); /*I18N*/
        _autoStretch100Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                auto100();
            }
        });

        _zoomInVButton = createButton("icons/ZoomIn24V.gif");
        _zoomInVButton.setName("zoomInVButton");
        _zoomInVButton.setToolTipText("Stretch histogram vertically"); /*I18N*/
        _zoomInVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                stretchVertically();
            }
        });

        _zoomOutVButton = createButton("icons/ZoomOut24V.gif");
        _zoomOutVButton.setName("zoomOutVButton");
        _zoomOutVButton.setToolTipText("Shrink histogram vertically"); /*I18N*/
        _zoomOutVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                shrinkVertically();
            }
        });

        _zoomInHButton = createButton("icons/ZoomIn24H.gif");
        _zoomInHButton.setName("zoomInHButton");
        _zoomInHButton.setToolTipText("Stretch histogram horizontally"); /*I18N*/
        _zoomInHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                stretchHorizontally();
            }
        });

        _zoomOutHButton = createButton("icons/ZoomOut24H.gif");
        _zoomOutHButton.setName("zoomOutHButton");
        _zoomOutHButton.setToolTipText("Shrink histogram horizontally"); /*I18N*/
        _zoomOutHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                shrinkHorizontally();
            }
        });

        _evenDistButton = createButton("icons/EvenDistribution24.gif");
        _evenDistButton.setName("evenDistButton");
        _evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        _evenDistButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        });

        _imageEnhancementsButton = createToggleButton("icons/ImageEnhancements24.gif");
        _imageEnhancementsButton.setName("imageEnhancementsButton");
        _imageEnhancementsButton.setToolTipText("Show/hide image enhancements pane"); /*I18N*/
        _imageEnhancementsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                _imageEnhancementsVisible = _imageEnhancementsButton.isSelected();
                if (_imageEnhancementsVisible) {
                    showGammaTip();
                }
                setImageEnhancementsPaneVisible(_imageEnhancementsVisible);
            }
        });

        AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");

        final JPanel buttonPane = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.insets.bottom = 3;
        gbc.gridy = 1;
        buttonPane.add(_applyButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        buttonPane.add(_resetButton, gbc);
        buttonPane.add(_multiApplyButton, gbc);
        gbc.gridy++;
        buttonPane.add(_importButton, gbc);
        buttonPane.add(_exportButton, gbc);
        gbc.gridy++;
        buttonPane.add(_autoStretch95Button, gbc);
        buttonPane.add(_autoStretch100Button, gbc);
        gbc.gridy++;
        buttonPane.add(_zoomInVButton, gbc);
        buttonPane.add(_zoomOutVButton, gbc);
        gbc.gridy++;
        buttonPane.add(_zoomInHButton, gbc);
        buttonPane.add(_zoomOutHButton, gbc);
        gbc.gridy++;
        buttonPane.add(_evenDistButton, gbc);
        buttonPane.add(_imageEnhancementsButton, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 1;
        buttonPane.add(helpButton, gbc);

        _redButton = new JRadioButton("Red ");
        _greenButton = new JRadioButton("Green ");
        _blueButton = new JRadioButton("Blue ");
        _redButton.setName("redButton");
        _greenButton.setName("greenButton");
        _blueButton.setName("blueButton");

        final ButtonGroup rgbButtonGroup = new ButtonGroup();
        rgbButtonGroup.add(_redButton);
        rgbButtonGroup.add(_greenButton);
        rgbButtonGroup.add(_blueButton);
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final JRadioButton source = (JRadioButton) e.getSource();
                setImageInfoOfContrastStretchPaneAt(getIndex(_lastRGBButton));
                updateImageInfo(getIndex(source));
                setRgbBandsComponentColors();
                _lastRGBButton = source;
            }
        };
        _redButton.addActionListener(listener);
        _greenButton.addActionListener(listener);
        _blueButton.addActionListener(listener);

        final JPanel rgbButtonsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rgbButtonsPane.add(_redButton);
        rgbButtonsPane.add(_greenButton);
        rgbButtonsPane.add(_blueButton);

        final JPanel rgbSourcePane = new JPanel(new BorderLayout());
        rgbSourcePane.add(new JLabel("Source: "), BorderLayout.WEST);
        rgbSourcePane.add(_rgbBandsParam.getEditor().getEditorComponent(), BorderLayout.CENTER);

        _rgbSettingsPane = new JPanel(new BorderLayout());
        _rgbSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, ContrastStretchPane.HOR_BORDER_SIZE, 2,
                                                                   ContrastStretchPane.HOR_BORDER_SIZE));
        _rgbSettingsPane.add(rgbButtonsPane, BorderLayout.NORTH);
        _rgbSettingsPane.add(rgbSourcePane, BorderLayout.SOUTH);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        _imageEnhancementsPane = GridBagUtils.createPanel();
        _imageEnhancementsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0,
                                                ContrastStretchPane.HOR_BORDER_SIZE,
                                                0,
                                                ContrastStretchPane.HOR_BORDER_SIZE),
                BorderFactory.createTitledBorder("Image Enhancements"))); /*I18N*/
        gbc.gridwidth = 1;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        _imageEnhancementsPane.add(_gammaParam.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        _imageEnhancementsPane.add(_gammaParam.getEditor().getEditorComponent(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        _imageEnhancementsPane.add(_histogramMatchingParam.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        _imageEnhancementsPane.add(_histogramMatchingParam.getEditor().getEditorComponent(), gbc);

        _centerPane = new JPanel(new BorderLayout());
        _centerPane.add(_contrastStretchPane);

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        mainPane.add(BorderLayout.CENTER, _centerPane);
        mainPane.add(BorderLayout.EAST, buttonPane);

        mainPane.setPreferredSize(new Dimension(320, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(getContentPane(), getDescriptor().getHelpId());
        }

        setApplyEnabled(false);


        setUnloader(new Unloader(_visatApp));
        final ProductSceneView productSceneView = _visatApp.getSelectedProductSceneView();
        addDataChangedListener(productSceneView);
        setProductSceneView(productSceneView);
        setSuppressibleOptionPane(_visatApp.getSuppressibleOptionPane());

        _rasterDataChangedListener = createRasterDataChangedListner();

        // Add an internal frame listsner to VISAT so that we can update our
        // contrast stretch dialog with the information of the currently activated
        // product scene view.
        //
        _visatApp.addInternalFrameListener(new ContrastStretchIFL());

        return mainPane;
    }

    public SuppressibleOptionPane getSuppressibleOptionPane() {
        return _suppressibleOptionPane;
    }

    public void setSuppressibleOptionPane(final SuppressibleOptionPane suppressibleOptionPane) {
        _suppressibleOptionPane = suppressibleOptionPane;
    }

    private void showGammaTip() {
        if (_suppressibleOptionPane != null) {
            if (isRgbMode()) {
                _suppressibleOptionPane.showMessageDialog("gamma.rgb.tip",
                                                          getContentPane(),
                                                          "Tip:\n" +
                                                                  "Gamma values between 0.6 and 0.9 tend to yield best results.\n" +
                                                                  "Press enter key after you have typed in a new value for gamma.",
                                                          /*I18N*/
                                                          getDescriptor().getTitle() + " Tip",
                                                          JOptionPane.INFORMATION_MESSAGE);
            } else {
                _suppressibleOptionPane.showMessageDialog("gamma.mono.tip",
                                                          getContentPane(),
                                                          "Tip:\n" +
                                                                  "Gamma correction can only be used with RGB images.", /*I18N*/
                                                                                                                        getDescriptor().getTitle() + " Tip",
                                                                                                                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void setImageEnhancementsPaneVisible(final boolean visible) {
        if (visible) {
            if (_imageEnhancementsPane.getParent() != _centerPane) {
                _centerPane.add(BorderLayout.SOUTH, _imageEnhancementsPane);
                _centerPane.revalidate();
            }
        } else {
            if (_imageEnhancementsPane.getParent() == _centerPane) {
                _centerPane.remove(_imageEnhancementsPane);
                _centerPane.revalidate();
            }
        }
        if (_imageEnhancementsButton.isSelected() != visible) {
            _imageEnhancementsButton.setSelected(visible);
        }
    }

    private void apply() {
        if (_productSceneView == null) {
            return;
        }
        setApplyEnabled(false);
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyImpl();
        getContentPane().setCursor(Cursor.getDefaultCursor());
    }

    private void applyImpl() {
        if (isRgbMode()) {
            final int index = getSelectedRgbIndex();
            if (index >= 0 && index <= 2) {
                _rgbImageInfos[index] = _contrastStretchPane.getImageInfo();
            }
            for (int i = 0; i < _rgbBands.length; i++) {
                final RasterDataNode raster = _rgbBands[i];
                if (raster != null) {
                    raster.setImageInfo(_rgbImageInfos[i]);
                }
            }
            final RasterDataNode[] oldRGBRasters = new RasterDataNode[3];
            for (int i = 0; i < oldRGBRasters.length; i++) {
                oldRGBRasters[i] = _productSceneView.getRasterAt(i);
            }
            _productSceneView.setRasters(_rgbBands);
            _productSceneView.setHistogramMatching(_histogramMatchingParam.getValueAsText());
            _visatApp.updateImage(_productSceneView);
            if (_unloader != null) {
                for (RasterDataNode oldRGBRaster : oldRGBRasters) {
                    _unloader.unloadUnusedRasterData(oldRGBRaster);
                }
            }
            updateBandNames();
        } else {
            _productSceneView.getRaster().setImageInfo(_contrastStretchPane.getImageInfo());
            _visatApp.updateImage(_productSceneView);
        }
    }

    private void setApplyEnabled(final boolean enabled) {
        final boolean canApply = _productSceneView != null;
        _applyButton.setEnabled(canApply && enabled);
        _multiApplyButton.setEnabled(canApply && (!enabled && (!isRgbMode() && _visatApp != null)));
    }

    public void reset() {
        if (_productSceneView != null) {
            if (isRgbMode()) {
                final int index = getSelectedRgbIndex();
                if (index != -1) {
                    final RasterDataNode raster = _rgbBands[index];
                    if (raster != null) {
                        _contrastStretchPane.resetDefaultValues(raster);
                    } else {
                        _contrastStretchPane.setImageInfo(null);
                    }
                }
            } else {
                final RasterDataNode raster = _productSceneView.getRaster();
                _contrastStretchPane.resetDefaultValues(raster);
            }
        }
    }

    private void applyMultipleColorPaletteDef() {
        if (_productSceneView == null) {
            return;
        }

        final Product selectedProduct = _productSceneView.getProduct();
        final ProductManager productManager = selectedProduct.getProductManager();
        final RasterDataNode[] protectedRasters = _productSceneView.getRasters();
        final ArrayList<Band> availableBandList = new ArrayList<Band>();
        for (int i = 0; i < productManager.getNumProducts(); i++) {
            final Product product = productManager.getProductAt(i);
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
            JOptionPane.showMessageDialog(getContentPane(),
                                          "No other bands available.", /*I18N*/
                                          getDescriptor().getTitle(),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        final BandChooser bandChooser = new BandChooser(getWindowAncestor(),
                                                        "Apply to other bands", /*I18N*/
                                                        getDescriptor().getHelpId(),
                                                        availableBands,
                                                        _bandsToBeModified);
        final ArrayList<Band> modifiedRasterList = new ArrayList<Band>();
        if (bandChooser.show() == BandChooser.ID_OK) {
            final ImageInfo imageInfo = _contrastStretchPane.getImageInfo();
            _bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : _bandsToBeModified) {
                band.getImageInfo().transferColorPaletteDef(imageInfo, false);
                modifiedRasterList.add(band);
            }
        }

        final Band[] rasters = new Band[modifiedRasterList.size()];
        modifiedRasterList.toArray(rasters);
        modifiedRasterList.clear();
        _visatApp.updateImages(rasters);
    }

    private void setIODir(final File dir) {
        if (_preferences != null) {
            _preferences.setPropertyString(_PREFERENCES_KEY_IO_DIR, dir.getPath());
        }
    }

    private File getIODir() {
        File dir = new File(SystemUtils.getUserHomeDir(), ".beam/beam-ui/auxdata/color-palettes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (_preferences != null) {
            dir = new File(_preferences.getPropertyString(_PREFERENCES_KEY_IO_DIR, dir.getPath()));
        }
        return dir;
    }

    private BeamFileFilter getOrCreateColorPaletteDefinitionFileFilter() {
        if (_beamFileFilter == null) {
            final String formatName = "COLOR_PALETTE_DEFINITION_FILE";
            final String description = "Color palette definition files (*" + _FILE_EXTENSION + ")";  /*I18N*/
            _beamFileFilter = new BeamFileFilter(formatName, _FILE_EXTENSION, description);
        }
        return _beamFileFilter;
    }

    private void importColorPaletteDef() {
        final ImageInfo imageInfo = _contrastStretchPane.getImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreachable because, the export Button
            // is disabled if the _contrastStretchPane has no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Color Palette Definition"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showOpenDialog(getContentPane());
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
        final ImageInfo imageInfo = _contrastStretchPane.getImageInfo();
        if (imageInfo == null) {
            // Normaly this code is unreacable because, the export Button
            // is disabled if the _contrastStretchPane have no ImageInfo.
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Color Palette Definition"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showSaveDialog(getContentPane());
        File file = fileChooser.getSelectedFile();
        if (file != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null) {
                if (!_visatApp.promptForOverwrite(file)) {
                    return;
                }
                file = FileUtils.ensureExtension(file, _FILE_EXTENSION);
                try {
                    final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
                    ColorPaletteDef.storeColorPaletteDef(colorPaletteDef, file);
                } catch (IOException e) {
                    showErrorDialog("Failed to export color palette definition.\n" + e.getMessage());  /*I18N*/
                }
            }
        }
    }

    private void auto95() {
        if (_productSceneView != null) {
            _contrastStretchPane.compute95Percent();
        }
    }

    private void auto100() {
        if (_productSceneView != null) {
            _contrastStretchPane.compute100Percent();
        }
    }

    private void shrinkHorizontally() {
        _contrastStretchPane.computeZoomOutToFullHistogramm();
    }

    private void stretchHorizontally() {
        _contrastStretchPane.computeZoomInToSliderLimits();
    }

    private void shrinkVertically() {
        _contrastStretchPane.computeZoomOutVertical();
    }

    private void stretchVertically() {
        _contrastStretchPane.computeZoomInVertical();
    }

    private void distributeSlidersEvenly() {
        _contrastStretchPane.distributeSlidersEvenly();
    }

    private static AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    private static AbstractButton createToggleButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), true);
    }

    private void clearBasicDisplayInfos() {
        _rgbImageInfos[0] = null;
        _rgbImageInfos[1] = null;
        _rgbImageInfos[2] = null;
        _rgbBands[0] = null;
        _rgbBands[1] = null;
        _rgbBands[2] = null;
    }

    private void updateImageInfo(final int index) {
        RasterDataNode rasterDataNode = null;
        if (index < 0 || index > 2) {
            _redButton.setSelected(true);
        } else {
//            setScalingToPane(index);
            setImageInfoCopyToContrastStretchPaneAt(index);
            _contrastStretchPane.setRGBColor(_rgbColors[index]);
            rasterDataNode = _rgbBands[index];
        }
        if (rasterDataNode != null) {
            _rgbBandsParam.setValueAsText(rasterDataNode.getName(), null);
            _rgbBandsParam.getProperties().setValueSetBound(true);
            _contrastStretchPane.setUnit(rasterDataNode.getUnit());
        } else {
            _rgbBandsParam.getProperties().setValueSetBound(false);
            _rgbBandsParam.setValueAsText("no band selected", null);
            _contrastStretchPane.setUnit("");
        }
    }

    private boolean isRgbMode() {
        return _productSceneView != null && _productSceneView.isRGB();
    }

    private int getSelectedRgbIndex() {
        if (_redButton.isSelected()) {
            return 0;
        }
        if (_greenButton.isSelected()) {
            return 1;
        }
        if (_blueButton.isSelected()) {
            return 2;
        }
        return -1;
    }

    private void hideRgbButtons() {
        _centerPane.remove(_rgbSettingsPane);
    }

    private void showRgbButtons() {
        if (!_centerPane.isAncestorOf(_rgbSettingsPane)) {
            _centerPane.add(_rgbSettingsPane, BorderLayout.NORTH);
        }
    }

    private void revalidate() {
        getContentPane().invalidate();
        getContentPane().validate();
        getContentPane().repaint();
    }

    private int getIndex(final JRadioButton source) {
        int index = -1;
        if (source == _redButton) {
            index = 0;
        }
        if (source == _greenButton) {
            index = 1;
        }
        if (source == _blueButton) {
            index = 2;
        }
        return index;
    }

    private void updateBandNames() {
        final Product product = _productSceneView.getProduct();
        if (product != null) {
            final List<String> nameList = new ArrayList<String>();
            final List<String> bandNameList = Arrays.asList(product.getBandNames());
            nameList.addAll(bandNameList);
            final RasterDataNode[] rasters = _productSceneView.getRasters();
            for (final RasterDataNode raster : rasters) {
                if (!nameList.contains(raster.getName())) {
                    nameList.add(raster.getName());
                }
            }
            final String[] valueSet = nameList.toArray(new String[nameList.size()]);
            _rgbBandsParam.setValueSet(valueSet);
        }
    }

    private void setRgbBandsComponentColors() {
        if (isRgbMode()) {
            Color foreground = null;
            if (_redButton.isSelected()) {
                foreground = new Color(128, 0, 0); // Color.darkred;
            } else if (_greenButton.isSelected()) {
                foreground = new Color(0, 128, 0); // Color.darkgreen;
            } else if (_blueButton.isSelected()) {
                foreground = new Color(0, 0, 128); // Color.darkblue;
            }
            final JComponent component = _rgbBandsParam.getEditor().getComponent();
            component.setForeground(foreground);
        }
    }

    private void showErrorDialog(final String message) {
        if (message != null && message.trim().length() > 0) {
            if (_visatApp != null) {
                _visatApp.showErrorDialog(message);
            } else {
                JOptionPane.showMessageDialog(getContentPane(),
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
                    if ((_productSceneView.isRGB() && sourceNode == _productSceneView.getProduct())
                            || sourceNode == _productSceneView.getRaster()) {
                        updateTitle();
                    }
                } else if (DataNode.PROPERTY_NAME_UNIT.equalsIgnoreCase(propertyName)) {
                    final DataNode dataNode = (DataNode) event.getSourceNode();
                    _contrastStretchPane.setUnit(dataNode.getUnit());
                    revalidate();
                }
            }
        };
    }


    private void addDataChangedListener(final ProductSceneView productSceneView) {
        if (productSceneView != null) {
            final Product product = productSceneView.getProduct();
            if (product != null) {
                product.addProductNodeListener(_rasterDataChangedListener);
            }
        }
    }

    private void removeDataChangedListener(final ProductSceneView productSceneView) {
        if (productSceneView != null) {
            final Product product = productSceneView.getProduct();
            if (product != null) {
                product.removeProductNodeListener(_rasterDataChangedListener);
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

    public static class Unloader {

        private VisatApp _app;

        public Unloader(final VisatApp app) {
            _app = app;
        }

        public void unloadUnusedRasterData(final RasterDataNode raster) {
            if (raster == null) {
                return;
            }
            if (!_app.hasRasterProductSceneView(raster) && !raster.isSynthetic()) {
                raster.unloadRasterData();
            }
        }
    }

    private class ContrastStretchIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e);
            removeDataChangedListener(ContrastStretchToolView.this.getProductSceneView());
            if (view != null) {
                addDataChangedListener(view);
                ContrastStretchToolView.this.setProductSceneView(view);
            } else {
                ContrastStretchToolView.this.setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e);
            if (ContrastStretchToolView.this.getProductSceneView() == view) {
                removeDataChangedListener(ContrastStretchToolView.this.getProductSceneView());
                ContrastStretchToolView.this.setProductSceneView(null);
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
        URL codeSourceUrl = BeamUiActivator.class.getProtectionDomain().getCodeSource().getLocation();
        final ResourceInstaller resourceInstaller = new ResourceInstaller(codeSourceUrl, "auxdata/color_palettes/",
                                                                          getIODir());
        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(_contrastStretchPane, "Installing Auxdata") {
            @Override
            protected Object doInBackground(com.bc.ceres.core.ProgressMonitor progressMonitor) throws Exception {
                resourceInstaller.install(".*.cpd", progressMonitor);
                auxDataInstalled = true;
                return Boolean.TRUE;
            }
        };
        swingWorker.executeWithBlocking();
    }

}


