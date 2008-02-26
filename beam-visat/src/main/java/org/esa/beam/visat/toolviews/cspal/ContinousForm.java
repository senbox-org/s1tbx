package org.esa.beam.visat.toolviews.cspal;

import com.bc.ceres.core.Assert;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;

public class ContinousForm implements SpecificForm {
    private final static Color[] RGB_COLORS = new Color[]{Color.RED, Color.GREEN, Color.BLUE};

    private final ImageInterpretationForm imageForm;
    private AbstractButton autoStretch95Button;
    private AbstractButton autoStretch100Button;
    private AbstractButton zoomInVButton;
    private AbstractButton zoomOutVButton;
    private AbstractButton zoomInHButton;
    private AbstractButton zoomOutHButton;

    private AbstractButton evenDistButton;
    private JRadioButton redButton;
    private JRadioButton greenButton;


    private JRadioButton blueButton;
    private AbstractButton imageEnhancementsButton;
    private boolean imageEnhancementsVisible;

    private JPanel imageEnhancementsPane;

    // Gamma support for RGB images
    private Parameter gammaParam;

    // Histogram equalization
    private Parameter histogramMatchingParam;
    private JPanel rgbSettingsPane;
    private JPanel contentPanel;
    private JRadioButton lastRgbButton;

    private Parameter rgbBandsParam;
    private ProductSceneView productSceneView;

    private ColourPaletteEditorPanel colorPaletteEditorPanel;
    private final ImageInfo[] rgbImageInfos;
    private final RasterDataNode[] rgbBands;

    private Unloader unloader;

    public ContinousForm(final ImageInterpretationForm imageForm) {
        this.imageForm = imageForm;

        initParameters();

        rgbImageInfos = new ImageInfo[3];
        rgbBands = new Band[3];

        colorPaletteEditorPanel = new ColourPaletteEditorPanel();
        colorPaletteEditorPanel.addPropertyChangeListener(RasterDataNode.PROPERTY_NAME_IMAGE_INFO,
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

        autoStretch95Button = createButton("icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels"); /*I18N*/
        autoStretch95Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                auto95();
            }
        });

        autoStretch100Button = createButton("icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels"); /*I18N*/
        autoStretch100Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                auto100();
            }
        });

        zoomInVButton = createButton("icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically"); /*I18N*/
        zoomInVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                stretchVertically();
            }
        });

        zoomOutVButton = createButton("icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically"); /*I18N*/
        zoomOutVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                shrinkVertically();
            }
        });

        zoomInHButton = createButton("icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally"); /*I18N*/
        zoomInHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                stretchHorizontally();
            }
        });

        zoomOutHButton = createButton("icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally"); /*I18N*/
        zoomOutHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                shrinkHorizontally();
            }
        });

        evenDistButton = createButton("icons/EvenDistribution24.gif");
        evenDistButton.setName("evenDistButton");
        evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        evenDistButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        });

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
        rgbSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, ContrastStretchPane.HOR_BORDER_SIZE, 2,
                                                                  ContrastStretchPane.HOR_BORDER_SIZE));
        rgbSettingsPane.add(rgbButtonsPane, BorderLayout.NORTH);
        rgbSettingsPane.add(rgbSourcePane, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        imageEnhancementsPane = GridBagUtils.createPanel();
        imageEnhancementsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0,
                                                ContrastStretchPane.HOR_BORDER_SIZE,
                                                0,
                                                ContrastStretchPane.HOR_BORDER_SIZE),
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

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(colorPaletteEditorPanel);

        unloader = new Unloader(VisatApp.getApp());
    }

    public void reset() {
        if (isRgbMode()) {
            final int index = getSelectedRgbIndex();
            if (index != -1) {
                final RasterDataNode raster = rgbBands[index];
                if (raster != null) {
                    colorPaletteEditorPanel.resetDefaultValues(raster);
                } else {
                    colorPaletteEditorPanel.setImageInfo(null);
                }
            }
        } else {
            final RasterDataNode raster = productSceneView.getRaster();
            colorPaletteEditorPanel.resetDefaultValues(raster);
        }
    }

    public ImageInfo getCurrentImageInfo() {
        return colorPaletteEditorPanel.getImageInfo();
    }


    private ProductSceneView getProductSceneView() {
        return productSceneView;
    }

    public void initProductSceneView(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        this.productSceneView = productSceneView;
        if (isRgbMode()) {
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
        } else {
            final RasterDataNode raster = this.productSceneView.getRaster();
            setImageInfo(raster.getImageInfo());
            gammaParam.setUIEnabled(false);
        }
    }

    private void setApplyEnabled(boolean b) {
        imageForm.setApplyEnabled(b);
    }

    private void auto95() {
        colorPaletteEditorPanel.compute95Percent();
    }

    private void auto100() {
        colorPaletteEditorPanel.compute100Percent();
    }

    private void shrinkHorizontally() {
        colorPaletteEditorPanel.computeZoomOutToFullHistogramm();
    }

    private void stretchHorizontally() {
        colorPaletteEditorPanel.computeZoomInToSliderLimits();
    }

    private void shrinkVertically() {
        colorPaletteEditorPanel.computeZoomOutVertical();
    }

    private void stretchVertically() {
        colorPaletteEditorPanel.computeZoomInVertical();
    }

    private void distributeSlidersEvenly() {
        colorPaletteEditorPanel.distributeSlidersEvenly();
    }

    private void showGammaTip() {
        if (isRgbMode()) {
            imageForm.showMessageDialog("gamma.rgb.tip",
                                        "Tip:\n" +
                                                "Gamma values between 0.6 and 0.9 tend to yield best results.\n" +
                                                "Press enter key after you have typed in a new value for gamma.",
                                        " Tip");
        } else {
            imageForm.showMessageDialog("gamma.mono.tip",
                                        "Tip:\nGamma correction can only be used with RGB images.",
                                        " Tip"
            );
        }
    }

    private boolean isRgbMode() {
        return getProductSceneView().isRGB();
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{
                autoStretch95Button,
                autoStretch95Button,
                zoomInVButton,
                zoomOutVButton,
                zoomInHButton,
                zoomOutHButton,
                evenDistButton,
                imageEnhancementsButton
        };


    }

    public void updateState() {
        imageEnhancementsButton.setEnabled(isRgbMode());
        if (isRgbMode()) {
            final int index = getSelectedRgbIndex();
            if (index != -1) {
                updateImageInfo(index);
            }
            updateGamma(index);
            setImageEnhancementsPaneVisible(imageEnhancementsVisible);
            showRgbButtons();
        } else {
            colorPaletteEditorPanel.setUnit(productSceneView.getRaster().getUnit());
            colorPaletteEditorPanel.setRGBColor(null);
            setImageEnhancementsPaneVisible(false);
            hideRgbButtons();
        }
        imageForm.revalidate();
    }

    private void updateGamma(final int index) {
        final RasterDataNode raster = productSceneView.getRasterAt(index);
        if (raster.getImageInfo() != null) {
            String text = "Gamma: "; /*I18N*/
            if (isRgbMode()) {
                text = index == 0 ? "Red gamma: " : index == 1 ? "Green gamma: " : "Blue gamma: "; /*I18N*/
            }
            gammaParam.getEditor().getLabelComponent().setText(text);
            gammaParam.setValue(raster.getImageInfo().getGamma(), null);
        }
    }

    private void setImageInfo(final ImageInfo imageInfo) {
        setImageInfoAt(0, imageInfo, true);
    }

    private void setImageInfoAt(final int index, final ImageInfo imageInfo, final boolean deliver) {
        rgbImageInfos[index] = imageInfo;
        if (deliver) {
            setImageInfoCopyToContrastStretchPaneAt(index);
        }
    }

    private void setImageInfoCopyToContrastStretchPaneAt(final int index) {
        if (rgbImageInfos[index] != null) {
            colorPaletteEditorPanel.setImageInfo(rgbImageInfos[index].createDeepCopy());
        } else {
            colorPaletteEditorPanel.setImageInfo(null);
        }
        if (!isRgbMode()) {
            setApplyEnabled(false);
        }
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

    public Component getContentPanel() {
        return contentPanel;
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

        if (isRgbMode()) {
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
        } else {
            productSceneView.getRaster().setImageInfo(colorPaletteEditorPanel.getImageInfo());
            VisatApp.getApp().updateImage(productSceneView);
        }
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

    private void hideRgbButtons() {
        contentPanel.remove(rgbSettingsPane);
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

    private AbstractButton createToggleButton(String s) {
        return ImageInterpretationForm.createToggleButton(s);
    }

    private AbstractButton createButton(String s) {
        return ImageInterpretationForm.createButton(s);
    }

    private void setRgbBandsComponentColors() {
        if (isRgbMode()) {
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

    public void releaseProductSceneView() {
        productSceneView = null;
        rgbImageInfos[0] = null;
        rgbImageInfos[1] = null;
        rgbImageInfos[2] = null;
        rgbBands[0] = null;
        rgbBands[1] = null;
        rgbBands[2] = null;
    }

    public String getTitle() {
        final String titleAddition;
        if (isRgbMode()) {
            titleAddition = productSceneView.getProduct().getProductRefString() + " RGB";     /*I18N*/
        } else {
            titleAddition = productSceneView.getRaster().getDisplayName();
        }
        return titleAddition;
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
