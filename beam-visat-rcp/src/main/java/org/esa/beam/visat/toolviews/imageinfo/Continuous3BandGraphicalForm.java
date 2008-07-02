package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Continuous3BandGraphicalForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final ImageInfoEditor imageInfoEditor;
    private final ImageInfoEditorSupport imageInfoEditorSupport;
    private final JPanel contentPanel;
    private final AbstractButton imageEnhancementsButton;
    private final JPanel imageEnhancementsPane;

    private final ImageInfoEditorModel3B[] models;
    private final RasterDataNode[] channelSources;
    private final List<RasterDataNode> channelSourcesList;
    private final RasterDataUnloader rasterDataUnloader;
    private final ChangeListener applyEnablerCL;
    private final BindingContext bindingContext;

    private int channel;
    double gamma = 1.0;
    String channelSourceName = "";
    String histogramMatching = "";

    private boolean gammaTipShown;

    public Continuous3BandGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        imageInfoEditor = new ImageInfoEditor();

        imageInfoEditorSupport = new ImageInfoEditorSupport(imageInfoEditor);
        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
        rasterDataUnloader = new RasterDataUnloader();
        models = new ImageInfoEditorModel3B[3];
        channelSources = new RasterDataNode[3];
        channelSourcesList = new ArrayList<RasterDataNode>(31);
        channel = 0;

        final ValueContainer valueContainer = new ValueContainer();
        valueContainer.addModel(ValueModel.createClassFieldModel(this, "channel", 0));
        valueContainer.addModel(ValueModel.createClassFieldModel(this, "channelSourceName", ""));
        valueContainer.addModel(ValueModel.createClassFieldModel(this, "gamma", 1.0));
        valueContainer.addModel(ValueModel.createClassFieldModel(this, "histogramMatching", ImageInfo.HISTOGRAM_MATCHING_OFF));

        valueContainer.getModel("channel").getDescriptor().setValueSet(new ValueSet(new Integer[]{0, 1, 2}));

        valueContainer.getModel("gamma").getDescriptor().setValueRange(new ValueRange(1.0 / 10.0, 10.0));
        valueContainer.getModel("gamma").getDescriptor().setDefaultValue(1.0);
        valueContainer.addPropertyChangeListener("gamma", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                imageInfoEditor.getModel().setGamma(gamma);
            }
        });

        valueContainer.getModel("histogramMatching").getDescriptor().setNotNull(true);
        valueContainer.getModel("histogramMatching").getDescriptor().setValueSet(new ValueSet(
                new String[]{
                        ImageInfo.HISTOGRAM_MATCHING_OFF,
                        ImageInfo.HISTOGRAM_MATCHING_EQUALIZE,
                        ImageInfo.HISTOGRAM_MATCHING_NORMALIZE
                }));

        valueContainer.addPropertyChangeListener("histogramMatching", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                imageInfoEditor.getModel().setHistogramMatching(histogramMatching);
            }
        });

        bindingContext = new BindingContext(valueContainer);

        JRadioButton rChannelButton = new JRadioButton("Red");
        JRadioButton gChannelButton = new JRadioButton("Green");
        JRadioButton bChannelButton = new JRadioButton("Blue");
        rChannelButton.setName("rChannelButton");
        gChannelButton.setName("gChannelButton");
        bChannelButton.setName("bChannelButton");

        final ButtonGroup channelButtonGroup = new ButtonGroup();
        channelButtonGroup.add(rChannelButton);
        channelButtonGroup.add(gChannelButton);
        channelButtonGroup.add(bChannelButton);

        bindingContext.bind("channel", channelButtonGroup);
        bindingContext.addPropertyChangeListener("channel", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                acknowledgeChannel();
            }
        });

        JTextField gammaField = new JTextField();
        gammaField.setColumns(6);
        gammaField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind("gamma", gammaField);

        JComboBox histogramMatchingBox = new JComboBox();
        bindingContext.bind("histogramMatching", histogramMatchingBox);

        JComboBox channelSourceNameBox = new JComboBox();
        channelSourceNameBox.setEditable(false);
        bindingContext.bind("channelSourceName", channelSourceNameBox);

        bindingContext.addPropertyChangeListener("channelSourceName", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                RasterDataNode newChannelSource = null;
                for (RasterDataNode rasterDataNode : channelSourcesList) {
                    if (rasterDataNode.getName().equals(channelSourceName)) {
                        newChannelSource = rasterDataNode;
                        break;
                    }
                }
                if (newChannelSource == null) {
                    JOptionPane.showMessageDialog(null,
                                                  "newChannelSource == null!\n" +
                                                          "channelSourceName = " + channelSourceName);
                    return;
                }

                final RasterDataNode oldChannelSource = channelSources[channel];
                if (newChannelSource != oldChannelSource) {
                    final RasterDataNode.Stx stx = Continuous3BandGraphicalForm.this.parentForm.getStx(newChannelSource);
                    if (stx != null) {
                        final ImageInfo imageInfo = Continuous3BandGraphicalForm.this.parentForm.getImageInfo();
                        rasterDataUnloader.unloadUnusedRasterData(oldChannelSource);
                        channelSources[channel] = newChannelSource;
                        models[channel] = new ImageInfoEditorModel3B(imageInfo, channel);
                        models[channel].setDisplayProperties(newChannelSource);
                        imageInfo.getRgbChannelDef().setSourceName(channel, channelSourceName);
                        acknowledgeChannel();
                        imageInfoEditor.compute95Percent();
                        Continuous3BandGraphicalForm.this.parentForm.setApplyEnabled(true);
                    } else {
                        final Object value = evt.getOldValue();
                        bindingContext.getBinding("channelSourceName").setValue(value == null ? "" : value);
                    }
                }
            }
        });

        final JPanel channelButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelButtonPanel.add(rChannelButton);
        channelButtonPanel.add(gChannelButton);
        channelButtonPanel.add(bChannelButton);

        final JPanel channelSourcePanel = new JPanel(new BorderLayout());
        channelSourcePanel.add(new JLabel("Source: "), BorderLayout.WEST);
        channelSourcePanel.add(channelSourceNameBox, BorderLayout.CENTER);

        JPanel channelSettingsPane = new JPanel(new BorderLayout());
        channelSettingsPane.setBorder(BorderFactory.createEmptyBorder(0, ImageInfoEditor.HOR_BORDER_SIZE, 2,
                                                                      ImageInfoEditor.HOR_BORDER_SIZE));
        channelSettingsPane.add(channelButtonPanel, BorderLayout.NORTH);
        channelSettingsPane.add(channelSourcePanel, BorderLayout.SOUTH);

        imageEnhancementsButton = ImageInfoEditorSupport.createToggleButton("icons/ImageEnhancements24.gif");
        imageEnhancementsButton.setName("imageEnhancementsButton");
        imageEnhancementsButton.setToolTipText("Show/hide image enhancements pane"); /*I18N*/
        imageEnhancementsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                setImageEnhancementsPaneVisible(imageEnhancementsButton.isSelected());
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        imageEnhancementsPane = GridBagUtils.createPanel();
        gbc.gridwidth = 1;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        imageEnhancementsPane.add(new JLabel("Gamma:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        imageEnhancementsPane.add(gammaField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.75;
        imageEnhancementsPane.add(new JLabel("Histogram matching:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        imageEnhancementsPane.add(histogramMatchingBox, gbc);

        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(channelSettingsPane, BorderLayout.NORTH);
        contentPanel.add(imageInfoEditor, BorderLayout.CENTER);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
        setImageEnhancementsPaneVisible(imageEnhancementsButton.isSelected());
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        imageInfoEditor.getModel().removeChangeListener(applyEnablerCL);
        imageInfoEditor.setModel(null);
        channelSourcesList.clear();
        Arrays.fill(models, null);
        Arrays.fill(channelSources, null);
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        RasterDataNode[] rasters = productSceneView.getRasters();
        channelSources[0] = rasters[0];
        channelSources[1] = rasters[1];
        channelSources[2] = rasters[2];

        for (int i = 0; i < models.length; i++) {
            if (models[i] != null) {
                models[i].removeChangeListener(applyEnablerCL);
            }
            models[i] = new ImageInfoEditorModel3B(parentForm.getImageInfo(), i);
            models[i].addChangeListener(applyEnablerCL);
        }

        final Band[] availableBands = productSceneView.getProduct().getBands();
        channelSourcesList.clear();
        channelSourcesList.addAll(Arrays.asList(availableBands));
        for (int i = 0; i < channelSources.length; i++) {
            RasterDataNode channelSource = channelSources[i];
            channelSourcesList.remove(channelSource);
            channelSourcesList.add(i, channelSource);
        }

        final String[] sourceNames = new String[channelSourcesList.size()];
        for (int i = 0; i < channelSourcesList.size(); i++) {
            sourceNames[i] = channelSourcesList.get(i).getName();
        }

        bindingContext.getValueContainer().getModel("channelSourceName").getDescriptor().setValueSet(new ValueSet(sourceNames));
        bindingContext.getBinding("histogramMatching").setValue(models[channel].getHistogramMatching());

        acknowledgeChannel();
    }

    public RasterDataNode[] getRasters() {
        return channelSources.clone();
    }

    private void showGammaTip() {
        if (!gammaTipShown) {
            gammaTipShown = true;
            parentForm.showMessageDialog("gamma.rgb.tip",
                                         "Tip:\n" +
                                                 "Gamma values between 0.6 and 0.9 tend to yield best results.\n" +
                                                 "Press enter key after you have typed in a new value for gamma.",
                                         " Tip");
        }
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

    private void setImageEnhancementsPaneVisible(final boolean visible) {
        if (visible) {
            showGammaTip();
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
        RasterDataNode channelSource = channelSources[channel];
        final ImageInfoEditorModel3B model = models[channel];
        model.setDisplayProperties(channelSource);
        imageInfoEditor.setModel(model);
        bindingContext.getBinding("channelSourceName").setValue(channelSource.getName());
        bindingContext.getBinding("gamma").setValue(model.getGamma());
    }

    private static class RasterDataUnloader {


        public RasterDataUnloader() {
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