package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.param.*;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;

class Continuous3BandGraphicalForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final ImageInfoEditor imageInfoEditor;
    private final ImageInfoEditorSupport imageInfoEditorSupport;
    private final JPanel contentPanel;

    private JRadioButton redButton;
    private JRadioButton greenButton;
    private JRadioButton blueButton;
    private JPanel rgbSettingsPane;
    private AbstractButton imageEnhancementsButton;
    private JPanel imageEnhancementsPane;
    private boolean imageEnhancementsVisible;
    private int channel;
    private final RgbChannelEditorModel[] models;
    private final RasterDataNode[] channelRasters;
    private HashMap<String, RasterDataNode> availableBandMap;
    private Unloader unloader;

    private Parameter gammaParam;
    private Parameter histogramMatchingParam;
    private Parameter rgbBandsParam;

    private final ChangeListener applyEnablerCL;


    public Continuous3BandGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        imageInfoEditor = new ImageInfoEditor();

        this.imageInfoEditorSupport = new ImageInfoEditorSupport(imageInfoEditor);

        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(imageInfoEditor, BorderLayout.CENTER);

        initParameters();

        models = new RgbChannelEditorModel[3];
        channelRasters = new Band[3];
        availableBandMap = new HashMap<String, RasterDataNode>(31);

        imageEnhancementsButton = ImageInfoEditorSupport.createToggleButton("icons/ImageEnhancements24.gif");
        imageEnhancementsButton.setName("imageEnhancementsButton");
        imageEnhancementsButton.setToolTipText("Show/hide image enhancements pane"); /*I18N*/
        imageEnhancementsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageEnhancementsVisible = imageEnhancementsButton.isSelected();
                if (imageEnhancementsVisible) {
                    showGammaTip();
                }
                setImageEnhancementsPaneVisible(imageEnhancementsVisible);
            }
        });

        redButton = new JRadioButton("Red ");
        greenButton = new JRadioButton("Green ");
        blueButton = new JRadioButton("Blue ");
        redButton.setName("redButton");
        greenButton.setName("greenButton");
        blueButton.setName("blueButton");

        final ButtonGroup rgbButtonGroup = new ButtonGroup();
        rgbButtonGroup.add(redButton);
        rgbButtonGroup.add(greenButton);
        rgbButtonGroup.add(blueButton);
        redButton.setSelected(true);
        channel = 0;

        final ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                channel = redButton.isSelected() ? 0 : greenButton.isSelected() ? 1 : 2;
                acknowledgeChannel();
            }
        };
        redButton.addActionListener(listener);
        greenButton.addActionListener(listener);
        blueButton.addActionListener(listener);

        final JPanel rgbButtonsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rgbButtonsPane.add(redButton);
        rgbButtonsPane.add(greenButton);
        rgbButtonsPane.add(blueButton);

        final JPanel rgbSourcePane = new JPanel(new BorderLayout());
        rgbSourcePane.add(new JLabel("Source: "), BorderLayout.WEST);
        rgbSourcePane.add(rgbBandsParam.getEditor().getEditorComponent(), BorderLayout.CENTER);

        rgbSettingsPane = new JPanel(new BorderLayout());
        rgbSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, ImageInfoEditor.HOR_BORDER_SIZE, 2,
                                                                  ImageInfoEditor.HOR_BORDER_SIZE));
        rgbSettingsPane.add(rgbButtonsPane, BorderLayout.NORTH);
        rgbSettingsPane.add(rgbSourcePane, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        imageEnhancementsPane = GridBagUtils.createPanel();
        imageEnhancementsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0,
                                                ImageInfoEditor.HOR_BORDER_SIZE,
                                                0,
                                                ImageInfoEditor.HOR_BORDER_SIZE),
                BorderFactory.createTitledBorder("Image Enhancements"))); /*I18N*/
        gbc.gridwidth = 1;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        imageEnhancementsPane.add(gammaParam.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        imageEnhancementsPane.add(gammaParam.getEditor().getEditorComponent(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        imageEnhancementsPane.add(histogramMatchingParam.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        imageEnhancementsPane.add(histogramMatchingParam.getEditor().getEditorComponent(), gbc);

        unloader = new Unloader();

        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
        setAvailableBandNames(productSceneView);
        setImageEnhancementsPaneVisible(imageEnhancementsVisible);
        showRgbButtons();
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        imageInfoEditor.getModel().removeChangeListener(applyEnablerCL);
        imageInfoEditor.setModel(null);
        availableBandMap.clear();
        models[0] = null;
        models[1] = null;
        models[2] = null;
        channelRasters[0] = null;
        channelRasters[1] = null;
        channelRasters[2] = null;
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        RasterDataNode[] rasters = productSceneView.getRasters();
        this.channelRasters[0] = rasters[0];
        this.channelRasters[1] = rasters[1];
        this.channelRasters[2] = rasters[2];

        final int length = this.channelRasters.length;
        for (int i = 0; i < length; i++) {
            models[i] = new RgbChannelEditorModel(parentForm.getImageInfo(), i);
            models[i].addChangeListener(applyEnablerCL);
        }

        acknowledgeChannel();
        
        gammaParam.setValue(parentForm.getImageInfo().getRgbProfile().getSampleDisplayGamma(channel), null);
        histogramMatchingParam.setValue(parentForm.getImageInfo().getHistogramMatching(), new ParamExceptionHandler() {
            public boolean handleParamException(ParamException e) {
                histogramMatchingParam.setValue(ImageInfo.HISTOGRAM_MATCHING_OFF, null);
                return true;
            }
        });

    }

    private void showGammaTip() {
        parentForm.showMessageDialog("gamma.rgb.tip",
                                     "Tip:\n" +
                                             "Gamma values between 0.6 and 0.9 tend to yield best results.\n" +
                                             "Press enter key after you have typed in a new value for gamma.",
                                     " Tip");
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{
                imageInfoEditorSupport.autoStretch95Button,
                imageInfoEditorSupport.autoStretch100Button,
                imageInfoEditorSupport.zoomInVButton,
                imageInfoEditorSupport.zoomOutVButton,
                imageInfoEditorSupport.zoomInHButton,
                imageInfoEditorSupport.zoomOutHButton,
                imageEnhancementsButton
        };
    }

    private void initParameters() {
        final ParamChangeListener rgbListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                final String name = rgbBandsParam.getValueAsText();
                final RasterDataNode availableBand = getAvailableBand(name);
                Assert.notNull(availableBand, "availableBand");
                if (channelRasters[channel] != availableBand) {
                    final ImageInfo imageInfo = parentForm.createDefaultImageInfo(availableBand);
                    if (imageInfo != null) {
                        unloader.unloadUnusedRasterData(channelRasters[channel]);
                        channelRasters[channel] = availableBand;
                        models[channel] = new RgbChannelEditorModel(parentForm.getImageInfo(), channel);
                        models[channel].setDisplayProperties(availableBand);
                        acknowledgeChannel();
                        parentForm.setApplyEnabled(true);
                    } else {
                        rgbBandsParam.setValue(event.getOldValue(), null);
                    }
                }
            }
        };

        rgbBandsParam = new Parameter("rgbBands");
        rgbBandsParam.getProperties().setValueSet(new String[]{""});
        rgbBandsParam.getProperties().setValueSetBound(true);
        rgbBandsParam.addParamChangeListener(rgbListener);

        final ParamChangeListener imageEnhancementsListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                if (imageInfoEditor != null && parentForm.getImageInfo() != null) {
                    final float gamma = ((Number) gammaParam.getValue()).floatValue();
                    imageInfoEditor.getModel().setGamma(gamma);
                    imageInfoEditor.getModel().fireStateChanged();
                }
            }
        };
        gammaParam = new Parameter("gamma", 1.0f);
        gammaParam.getProperties().setLabel("Gamma");
        gammaParam.getProperties().setDescription("Gamma value");
        gammaParam.getProperties().setDefaultValue(1.0f);
        gammaParam.getProperties().setMinValue(1.0f / 10.0f);
        gammaParam.getProperties().setMaxValue(10.0f);
        gammaParam.getProperties().setNumCols(6);
        gammaParam.addParamChangeListener(imageEnhancementsListener);

        histogramMatchingParam = new Parameter("histogramMatching", ImageInfo.HISTOGRAM_MATCHING_OFF);
        histogramMatchingParam.getProperties().setValueSet(new String[]{
                ImageInfo.HISTOGRAM_MATCHING_OFF,
                ImageInfo.HISTOGRAM_MATCHING_EQUALIZE,
                ImageInfo.HISTOGRAM_MATCHING_NORMALIZE
        });
        histogramMatchingParam.getProperties().setNullValueAllowed(false);
        histogramMatchingParam.getProperties().setValueSetBound(true);
        histogramMatchingParam.getProperties().setLabel("Histogram matching");
        histogramMatchingParam.getProperties().setDescription("Apply histogram matching");
        histogramMatchingParam.addParamChangeListener(imageEnhancementsListener);
    }

    private void setImageEnhancementsPaneVisible(final boolean visible) {
        if (visible) {
            if (imageEnhancementsPane.getParent() != contentPanel) {
                contentPanel.add(BorderLayout.SOUTH, imageEnhancementsPane);
                contentPanel.revalidate();
            }
        } else {
            if (imageEnhancementsPane.getParent() == contentPanel) {
                contentPanel.remove(imageEnhancementsPane);
                contentPanel.revalidate();
            }
        }
        if (imageEnhancementsButton.isSelected() != visible) {
            imageEnhancementsButton.setSelected(visible);
        }
    }


    private void acknowledgeChannel() {
        RasterDataNode channelRaster = channelRasters[channel];
        final RgbChannelEditorModel model = models[channel];
        model.setDisplayProperties(channelRaster);
        imageInfoEditor.setModel(model);
        rgbBandsParam.setValueAsText(channelRaster.getName(), null);
    }


    private void showRgbButtons() {
        if (!contentPanel.isAncestorOf(rgbSettingsPane)) {
            contentPanel.add(rgbSettingsPane, BorderLayout.NORTH);
        }
    }

    private void setAvailableBandNames(ProductSceneView productSceneView) {
        availableBandMap.clear();
        final Band[] bands = productSceneView.getProduct().getBands();
        for (Band band : bands) {
            if (band.getSampleCoding() == null) {
                availableBandMap.put(band.getName(), band);
            }
        }
        final RasterDataNode[] rasters = productSceneView.getRasters();
        for (final RasterDataNode raster : rasters) {
            availableBandMap.put(raster.getName(), raster);
        }
        final Set<String> set = availableBandMap.keySet();
        final String[] valueSet = set.toArray(new String[set.size()]);
        rgbBandsParam.setValueSet(valueSet);
    }

    private RasterDataNode getAvailableBand(final String name) {
        return availableBandMap.get(name);
    }

    private static class Unloader {


        public Unloader() {
        }

        public void unloadUnusedRasterData(final RasterDataNode raster) {
            if (raster == null) {
                return;
            }
            if (!VisatApp.getApp().hasRasterProductSceneView(raster) && !raster.isSynthetic()) {
                raster.unloadRasterData();
            }
        }
    }

}