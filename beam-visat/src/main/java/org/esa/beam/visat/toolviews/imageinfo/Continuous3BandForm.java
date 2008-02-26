package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

class Continuous3BandForm extends ContinuousForm {
    private final static Color[] RGB_COLORS = new Color[]{Color.RED, Color.GREEN, Color.BLUE};

    private JRadioButton redButton;
    private JRadioButton greenButton;
    private JRadioButton blueButton;
    private AbstractButton imageEnhancementsButton;
    private boolean imageEnhancementsVisible;
    private JPanel imageEnhancementsPane;
    private Parameter gammaParam;

    private Parameter histogramMatchingParam;

    private JPanel rgbSettingsPane;
    private JRadioButton lastRgbButton;

    private Parameter rgbBandsParam;
    private final ImageInfo[] rgbImageInfos;
    private final RasterDataNode[] rgbBands;

    private Unloader unloader;

    public Continuous3BandForm(final ImageInterpretationForm imageForm) {
        super(imageForm);

        initParameters();

        rgbImageInfos = new ImageInfo[3];
        rgbBands = new Band[3];

        imageEnhancementsButton = createToggleButton("icons/ImageEnhancements24.gif");
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
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final JRadioButton source = (JRadioButton) e.getSource();
                setImageInfoOfContrastStretchPaneAt(getIndex(lastRgbButton));
                updateImageInfo(getIndex(source));
                setRgbBandsComponentColors();
                lastRgbButton = source;
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
        rgbSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, ColourPaletteEditorPanel.HOR_BORDER_SIZE, 2,
                                                                  ColourPaletteEditorPanel.HOR_BORDER_SIZE));
        rgbSettingsPane.add(rgbButtonsPane, BorderLayout.NORTH);
        rgbSettingsPane.add(rgbSourcePane, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        imageEnhancementsPane = GridBagUtils.createPanel();
        imageEnhancementsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0,
                                                ColourPaletteEditorPanel.HOR_BORDER_SIZE,
                                                0,
                                                ColourPaletteEditorPanel.HOR_BORDER_SIZE),
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

        unloader = new Unloader(VisatApp.getApp());
    }

    public void reset() {
        final int index = getSelectedRgbIndex();
        if (index != -1) {
            final RasterDataNode raster = rgbBands[index];
            if (raster != null) {
                colorPaletteEditorPanel.resetDefaultValues(raster);
            } else {
                colorPaletteEditorPanel.setImageInfo(null);
            }
        }
    }

    @Override
    public void initProductSceneView(ProductSceneView productSceneView) {
        super.initProductSceneView(productSceneView);
        rgbBands[0] = this.productSceneView.getRedRaster();
        rgbBands[1] = this.productSceneView.getGreenRaster();
        rgbBands[2] = this.productSceneView.getBlueRaster();

        final int length = rgbBands.length;
        final int selectedRgbIndex = getSelectedRgbIndex();
        for (int i = 0; i < length; i++) {
            final RasterDataNode node = rgbBands[i];
            if (node != null) {
                setImageInfoAt(i, node.getImageInfo(), i == selectedRgbIndex);
            }
        }

        if (this.productSceneView.getHistogramMatching() != null) {
            histogramMatchingParam.setValue(this.productSceneView.getHistogramMatching(), null);
        } else {
            histogramMatchingParam.setValue(ImageInfo.HISTOGRAM_MATCHING_OFF, null);
        }

        updateBandNames();
        redButton.setSelected(true);
        gammaParam.setUIEnabled(true);
        setRgbBandsComponentColors();
    }

    private void setApplyEnabled(boolean b) {
        imageForm.setApplyEnabled(b);
    }

    private void showGammaTip() {
        imageForm.showMessageDialog("gamma.rgb.tip",
                                    "Tip:\n" +
                                            "Gamma values between 0.6 and 0.9 tend to yield best results.\n" +
                                            "Press enter key after you have typed in a new value for gamma.",
                                    " Tip");
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{
                autoStretch95Button,
                autoStretch100Button,
                zoomInVButton,
                zoomOutVButton,
                zoomInHButton,
                zoomOutHButton,
                imageEnhancementsButton
        };
    }

    public void updateState() {
        final int index = getSelectedRgbIndex();
        if (index != -1) {
            updateImageInfo(index);
        }
        updateGamma(index);
        setImageEnhancementsPaneVisible(imageEnhancementsVisible);
        showRgbButtons();
        imageForm.revalidate();
    }

    private void updateGamma(final int index) {
        final RasterDataNode raster = productSceneView.getRasterAt(index);
        if (raster.getImageInfo() != null) {
            String text = index == 0 ? "Red gamma: " : index == 1 ? "Green gamma: " : "Blue gamma: ";
            gammaParam.getEditor().getLabelComponent().setText(text);
            gammaParam.setValue(raster.getImageInfo().getGamma(), null);
        }
    }

    private void setImageInfoAt(final int index, final ImageInfo imageInfo, final boolean deliver) {
        rgbImageInfos[index] = imageInfo;
        if (deliver) {
            setImageInfoCopyToContrastStretchPaneAt(index);
        }
    }

    private void setImageInfoCopyToContrastStretchPaneAt(final int index) {
        if (rgbImageInfos[index] != null) {
            setCurrentImageInfo(rgbImageInfos[index].createDeepCopy());
        } else {
            setCurrentImageInfo(null);
        }
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        colorPaletteEditorPanel.setImageInfo(imageInfo);
    }

    private void setImageInfoOfContrastStretchPaneAt(final int index) {
        if (index >= 0 && index <= 2) {
            rgbImageInfos[index] = colorPaletteEditorPanel.getImageInfo();
        }
    }


    private void initParameters() {
        final ParamChangeListener rgbListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                final String name = rgbBandsParam.getValueAsText();
                final RasterDataNode band = getBand(name);
                final int index = getSelectedRgbIndex();
                if (index == -1) {
                    return;
                }
                updateGamma(index);
                if (rgbBands[index] != band) {
                    if (unloader != null) {
                        unloader.unloadUnusedRasterData(rgbBands[index]);
                    }
                    rgbBands[index] = band;
                    if (band != null) {
                        colorPaletteEditorPanel.ensureValidImageInfo(band);
                        setImageInfoAt(index, band.getImageInfo(), true);
                    } else {
                        setImageInfoAt(index, null, true);
                    }
                    setApplyEnabled(true);
                }
            }
        };

        rgbBandsParam = new Parameter("rgbBands");
        rgbBandsParam.getProperties().setValueSet(new String[]{""});
        rgbBandsParam.getProperties().setValueSetBound(true);
        rgbBandsParam.addParamChangeListener(rgbListener);

        final ParamChangeListener imageEnhancementsListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                if (colorPaletteEditorPanel != null && colorPaletteEditorPanel.getImageInfo() != null) {
                    final float gamma = ((Number) gammaParam.getValue()).floatValue();
                    colorPaletteEditorPanel.getImageInfo().setGamma(gamma);
                    colorPaletteEditorPanel.updateGamma();
                    apply();
                }
            }
        };
        gammaParam = new Parameter("gamma", 1.0f);
        gammaParam.getProperties().setDescription("Gamma");
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

    public void apply() {
        final int index = getSelectedRgbIndex();
        if (index >= 0 && index <= 2) {
            rgbImageInfos[index] = colorPaletteEditorPanel.getImageInfo();
        }
        for (int i = 0; i < rgbBands.length; i++) {
            final RasterDataNode raster = rgbBands[i];
            if (raster != null) {
                raster.setImageInfo(rgbImageInfos[i]);
            }
        }
        final RasterDataNode[] oldRGBRasters = new RasterDataNode[3];
        for (int i = 0; i < oldRGBRasters.length; i++) {
            oldRGBRasters[i] = productSceneView.getRasterAt(i);
        }
        productSceneView.setRasters(rgbBands);
        productSceneView.setHistogramMatching(histogramMatchingParam.getValueAsText());
        VisatApp.getApp().updateImage(productSceneView);
        if (unloader != null) {
            for (RasterDataNode oldRGBRaster : oldRGBRasters) {
                unloader.unloadUnusedRasterData(oldRGBRaster);
            }
        }
        updateBandNames();
    }

    private void updateImageInfo(final int index) {
        RasterDataNode rasterDataNode = null;
        if (index < 0 || index > 2) {
            redButton.setSelected(true);
        } else {
//            setScalingToPane(index);
            setImageInfoCopyToContrastStretchPaneAt(index);
            colorPaletteEditorPanel.setRGBColor(RGB_COLORS[index]);
            rasterDataNode = rgbBands[index];
        }
        if (rasterDataNode != null) {
            rgbBandsParam.setValueAsText(rasterDataNode.getName(), null);
            rgbBandsParam.getProperties().setValueSetBound(true);
            colorPaletteEditorPanel.setUnit(rasterDataNode.getUnit());
        } else {
            rgbBandsParam.getProperties().setValueSetBound(false);
            rgbBandsParam.setValueAsText("no band selected", null);
            colorPaletteEditorPanel.setUnit("");
        }
    }


    private int getSelectedRgbIndex() {
        if (redButton.isSelected()) {
            return 0;
        }
        if (greenButton.isSelected()) {
            return 1;
        }
        if (blueButton.isSelected()) {
            return 2;
        }
        return -1;
    }

    private void showRgbButtons() {
        if (!contentPanel.isAncestorOf(rgbSettingsPane)) {
            contentPanel.add(rgbSettingsPane, BorderLayout.NORTH);
        }
    }

    private int getIndex(final JRadioButton source) {
        int index = -1;
        if (source == redButton) {
            index = 0;
        }
        if (source == greenButton) {
            index = 1;
        }
        if (source == blueButton) {
            index = 2;
        }
        return index;
    }

    private void updateBandNames() {
        final Product product = productSceneView.getProduct();
        if (product != null) {
            final java.util.List<String> nameList = new ArrayList<String>();
            final java.util.List<String> bandNameList = Arrays.asList(product.getBandNames());
            nameList.addAll(bandNameList);
            final RasterDataNode[] rasters = productSceneView.getRasters();
            for (final RasterDataNode raster : rasters) {
                if (!nameList.contains(raster.getName())) {
                    nameList.add(raster.getName());
                }
            }
            final String[] valueSet = nameList.toArray(new String[nameList.size()]);
            rgbBandsParam.setValueSet(valueSet);
        }
    }

    private void setRgbBandsComponentColors() {
        Color foreground = null;
        if (redButton.isSelected()) {
            foreground = new Color(128, 0, 0); // Color.darkred;
        } else if (greenButton.isSelected()) {
            foreground = new Color(0, 128, 0); // Color.darkgreen;
        } else if (blueButton.isSelected()) {
            foreground = new Color(0, 0, 128); // Color.darkblue;
        }
        final JComponent component = rgbBandsParam.getEditor().getComponent();
        component.setForeground(foreground);
    }

    /*
     * IMPORTANT: We cannot directly  use _productSceneView.getProduct().getBand(name)
     * because _productSceneView can use temporary RGB bands not contained in the
     * product's band list.
     */
    private RasterDataNode getBand(final String name) {
        final RasterDataNode[] rasters = productSceneView.getRasters();
        for (final RasterDataNode raster : rasters) {
            if (raster.getName().equalsIgnoreCase(name)) {
                return raster;
            }
        }
        return productSceneView.getProduct().getBand(name);
    }

    @Override
    public void releaseProductSceneView() {
        super.releaseProductSceneView();
        rgbImageInfos[0] = null;
        rgbImageInfos[1] = null;
        rgbImageInfos[2] = null;
        rgbBands[0] = null;
        rgbBands[1] = null;
        rgbBands[2] = null;
    }

    public String getTitle() {
        return productSceneView.getProduct().getProductRefString() + " RGB";
    }

    private static class Unloader {

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

}