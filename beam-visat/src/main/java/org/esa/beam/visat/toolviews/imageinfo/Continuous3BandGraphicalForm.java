package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.param.*;
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
import java.util.HashMap;
import java.util.Set;

class Continuous3BandGraphicalForm extends AbstractContinuousGraphicalForm {
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

    private Parameter rgbBandsParam;
    private final ImageInfo[] rgbImageInfos;
    private final RasterDataNode[] rgbBands;
    private HashMap<String, RasterDataNode> availableBandMap;

    private Unloader unloader;
    private int rgbBandsIndex;

    public Continuous3BandGraphicalForm(final ColorManipulationForm parentForm) {
        super(parentForm);

        initParameters();

        rgbImageInfos = new ImageInfo[3];
        rgbBands = new Band[3];
        availableBandMap = new HashMap<String, RasterDataNode>(31);

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
        redButton.setSelected(true);
        rgbBandsIndex = 0;

        final ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                rgbImageInfos[rgbBandsIndex] = getCurrentImageInfo();
                rgbBandsIndex = redButton.isSelected() ? 0 : greenButton.isSelected() ? 1 : 2;
                updateImageInfo();
                setRgbBandsComponentColors();
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
        rgbSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, GraphicalPaletteEditor.HOR_BORDER_SIZE, 2,
                                                                  GraphicalPaletteEditor.HOR_BORDER_SIZE));
        rgbSettingsPane.add(rgbButtonsPane, BorderLayout.NORTH);
        rgbSettingsPane.add(rgbSourcePane, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        imageEnhancementsPane = GridBagUtils.createPanel();
        imageEnhancementsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0,
                                                GraphicalPaletteEditor.HOR_BORDER_SIZE,
                                                0,
                                                GraphicalPaletteEditor.HOR_BORDER_SIZE),
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

    public void performApply(ProductSceneView productSceneView) {
        rgbImageInfos[rgbBandsIndex] = paletteEditor.getImageInfo();
        for (int i = 0; i < rgbBands.length; i++) {
            rgbBands[i].setImageInfo(rgbImageInfos[i]);
        }
        final RasterDataNode[] oldRGBRasters = productSceneView.getRasters().clone();
        productSceneView.setRasters(rgbBands.clone());
        productSceneView.setHistogramMatching(histogramMatchingParam.getValueAsText());
        for (RasterDataNode oldRGBRaster : oldRGBRasters) {
            unloader.unloadUnusedRasterData(oldRGBRaster);
        }
        setAvailableBandNames(productSceneView);
    }

    public void performReset(ProductSceneView productSceneView) {
        resetDefaultValues(rgbBands[rgbBandsIndex]);
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");

        rgbBands[0] = productSceneView.getRedRaster();
        rgbBands[1] = productSceneView.getGreenRaster();
        rgbBands[2] = productSceneView.getBlueRaster();

        final int length = rgbBands.length;
        for (int i = 0; i < length; i++) {
            final RasterDataNode node = rgbBands[i];
            rgbImageInfos[i] = node.getImageInfo();
            if (i == rgbBandsIndex) {
                setCurrentImageInfo(rgbImageInfos[i].createDeepCopy());
            }
        }

        histogramMatchingParam.setValue(productSceneView.getHistogramMatching(), new ParamExceptionHandler() {
            public boolean handleParamException(ParamException e) {
                histogramMatchingParam.setValue(ImageInfo.HISTOGRAM_MATCHING_OFF, null);
                return true;
            }
        });

        setAvailableBandNames(productSceneView);
        redButton.setSelected(true);
        gammaParam.setUIEnabled(true);


        setRgbBandsComponentColors();

    }

    @Override
    public void handleFormHidden() {
        availableBandMap.clear();
        rgbImageInfos[0] = null;
        rgbImageInfos[1] = null;
        rgbImageInfos[2] = null;
        rgbBands[0] = null;
        rgbBands[1] = null;
        rgbBands[2] = null;
    }

    private void setApplyEnabled(boolean b) {
        parentForm.setApplyEnabled(b);
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
                autoStretch95Button,
                autoStretch100Button,
                zoomInVButton,
                zoomOutVButton,
                zoomInHButton,
                zoomOutHButton,
                imageEnhancementsButton
        };
    }

    public void updateState(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        updateImageInfo();
        setImageEnhancementsPaneVisible(imageEnhancementsVisible);
        showRgbButtons();
        parentForm.revalidate();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        paletteEditor.setImageInfo(imageInfo);
    }

    private void initParameters() {
        final ParamChangeListener rgbListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                final String name = rgbBandsParam.getValueAsText();
                final RasterDataNode availableBand = getAvailableBand(name);
                Assert.notNull(availableBand, "availableBand");
                if (rgbBands[rgbBandsIndex] != availableBand) {
                    final ImageInfo imageInfo = ensureValidImageInfo(availableBand);
                    if (imageInfo != null) {
                        unloader.unloadUnusedRasterData(rgbBands[rgbBandsIndex]);
                        rgbBands[rgbBandsIndex] = availableBand;
                        rgbImageInfos[rgbBandsIndex] = availableBand.getImageInfo();
                        updateImageInfo();
                        setApplyEnabled(true);
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
                if (paletteEditor != null && paletteEditor.getImageInfo() != null) {
                    final float gamma = ((Number) gammaParam.getValue()).floatValue();
                    paletteEditor.getImageInfo().setGamma(gamma);
                    paletteEditor.updateGamma();
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


    private void updateImageInfo() {
        RasterDataNode rasterDataNode = rgbBands[rgbBandsIndex];
        setCurrentImageInfo(rgbImageInfos[rgbBandsIndex].createDeepCopy());
        paletteEditor.setRGBColor(RGB_COLORS[rgbBandsIndex]);
        paletteEditor.setUnit(rasterDataNode.getUnit());
        rgbBandsParam.setValueAsText(rasterDataNode.getName(), null);
        rgbBandsParam.getProperties().setValueSetBound(true);
        gammaParam.setValue(getCurrentImageInfo().getGamma(), null);
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

    private RasterDataNode getAvailableBand(final String name) {
        return availableBandMap.get(name);
    }

    public String getTitle(ProductSceneView productSceneView) {
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