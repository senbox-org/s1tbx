package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.MessageFormat;

class Continuous3BandGraphicalForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final ImageInfoEditor imageInfoEditor;
    private final ImageInfoEditorSupport imageInfoEditorSupport;
    private final JPanel contentPanel;

    private final ImageInfoEditorModel3B[] models;
    private final RasterDataNode[] initialChannelSources;
    private final RasterDataNode[] currentChannelSources;
    private final List<RasterDataNode> channelSourcesList;
    private final RasterDataUnloader rasterDataUnloader;
    private final ChangeListener applyEnablerCL;
    private final BindingContext bindingContext;
    private final MoreOptionsForm moreOptionsForm;

    private int channel;

    private static final String GAMMA_PROPERTY = "gamma";
    double gamma = 1.0;

    private static final String CHANNEL_SOURCE_NAME_PROPERTY = "channelSourceName";
    String channelSourceName = "";

    public Continuous3BandGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        imageInfoEditor = new ImageInfoEditor();
        imageInfoEditorSupport = new ImageInfoEditorSupport(imageInfoEditor);
        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
        moreOptionsForm = new MoreOptionsForm(parentForm, true);
        rasterDataUnloader = new RasterDataUnloader();
        models = new ImageInfoEditorModel3B[3];
        initialChannelSources = new RasterDataNode[3];
        currentChannelSources = new RasterDataNode[3];
        channelSourcesList = new ArrayList<RasterDataNode>(32);
        channel = 0;

        final ValueModel channelSourceNameModel = ValueModel.createClassFieldModel(this, CHANNEL_SOURCE_NAME_PROPERTY, "");
        JComboBox channelSourceNameBox = new JComboBox();
        channelSourceNameBox.setEditable(false);

        final ValueModel gammaModel = ValueModel.createClassFieldModel(this, GAMMA_PROPERTY, 1.0);
        gammaModel.getDescriptor().setValueRange(new ValueRange(1.0 / 10.0, 10.0));
        gammaModel.getDescriptor().setDefaultValue(1.0);
        JTextField gammaField = new JTextField();
        gammaField.setColumns(6);
        gammaField.setHorizontalAlignment(JTextField.RIGHT);

        moreOptionsForm.getBindingContext().getValueContainer().addModel(channelSourceNameModel);
        moreOptionsForm.getBindingContext().bind(CHANNEL_SOURCE_NAME_PROPERTY, channelSourceNameBox);

        moreOptionsForm.getBindingContext().getValueContainer().addModel(gammaModel);
        moreOptionsForm.getBindingContext().bind(GAMMA_PROPERTY, gammaField);

        moreOptionsForm.addRow(new JLabel("Source band: "), channelSourceNameBox);
        moreOptionsForm.addRow(new JLabel("Gamma non-linearity: "), gammaField);

        final ValueContainer valueContainer = new ValueContainer();
        valueContainer.addModel(ValueModel.createClassFieldModel(this, "channel", 0));
        valueContainer.getModel("channel").getDescriptor().setValueSet(new ValueSet(new Integer[]{0, 1, 2}));

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

        final JPanel channelButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelButtonPanel.add(rChannelButton);
        channelButtonPanel.add(gChannelButton);
        channelButtonPanel.add(bChannelButton);

        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(channelButtonPanel, BorderLayout.NORTH);
        contentPanel.add(imageInfoEditor, BorderLayout.CENTER);

        moreOptionsForm.getBindingContext().addPropertyChangeListener(GAMMA_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                handleGammaChanged(evt);
            }
        });
        moreOptionsForm.getBindingContext().addPropertyChangeListener(CHANNEL_SOURCE_NAME_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                handleChannelSourceNameChanged(evt);
            }
        });
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        RasterDataNode[] rasters = productSceneView.getRasters();
        initialChannelSources[0] = rasters[0];
        initialChannelSources[1] = rasters[1];
        initialChannelSources[2] = rasters[2];
        updateFormModel(productSceneView);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        imageInfoEditor.getModel().removeChangeListener(applyEnablerCL);
        imageInfoEditor.setModel(null);
        channelSourcesList.clear();
        Arrays.fill(models, null);
        Arrays.fill(initialChannelSources, null);
        Arrays.fill(currentChannelSources, null);
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        RasterDataNode[] rasters = productSceneView.getRasters();
        currentChannelSources[0] = rasters[0];
        currentChannelSources[1] = rasters[1];
        currentChannelSources[2] = rasters[2];

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
        for (int i = 0; i < initialChannelSources.length; i++) {
            RasterDataNode channelSource = initialChannelSources[i];
            channelSourcesList.remove(channelSource);
            channelSourcesList.add(i, channelSource);
        }
        for (int i = 0; i < currentChannelSources.length; i++) {
            RasterDataNode channelSource = currentChannelSources[i];
            channelSourcesList.remove(channelSource);
            channelSourcesList.add(i, channelSource);
        }

        final String[] sourceNames = new String[channelSourcesList.size()];
        for (int i = 0; i < channelSourcesList.size(); i++) {
            sourceNames[i] = channelSourcesList.get(i).getName();
        }

        moreOptionsForm.getBindingContext().getValueContainer().getModel(CHANNEL_SOURCE_NAME_PROPERTY).getDescriptor().setValueSet(new ValueSet(sourceNames));

        acknowledgeChannel();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        imageInfoEditor.getModel().setDisplayProperties(raster);
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STATISTICS)) {
            imageInfoEditor.compute100Percent();
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return currentChannelSources.clone();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                imageInfoEditorSupport.autoStretch95Button,
                imageInfoEditorSupport.autoStretch100Button,
                imageInfoEditorSupport.zoomInVButton,
                imageInfoEditorSupport.zoomOutVButton,
                imageInfoEditorSupport.zoomInHButton,
                imageInfoEditorSupport.zoomOutHButton,
        };
    }

    private void acknowledgeChannel() {
        RasterDataNode channelSource = currentChannelSources[channel];
        final ImageInfoEditorModel3B model = models[channel];
        model.setDisplayProperties(channelSource);
        imageInfoEditor.setModel(model);
        moreOptionsForm.getBindingContext().getBinding(CHANNEL_SOURCE_NAME_PROPERTY).setPropertyValue(channelSource.getName());
        moreOptionsForm.getBindingContext().getBinding(GAMMA_PROPERTY).setPropertyValue(model.getGamma());
    }

    private void handleGammaChanged(PropertyChangeEvent evt) {
        imageInfoEditor.getModel().setGamma(gamma);
    }

    private void handleChannelSourceNameChanged(PropertyChangeEvent evt) {
        RasterDataNode newChannelSource = null;
        for (RasterDataNode rasterDataNode : channelSourcesList) {
            if (rasterDataNode.getName().equals(channelSourceName)) {
                newChannelSource = rasterDataNode;
                break;
            }
        }
        if (newChannelSource == null) {
            JOptionPane.showMessageDialog(contentPanel,
                                          MessageFormat.format("Unknown band: ''{0}''", channelSourceName),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final RasterDataNode oldChannelSource = currentChannelSources[channel];
        if (newChannelSource != oldChannelSource) {
            final RasterDataNode.Stx stx = this.parentForm.getStx(newChannelSource);
            if (stx != null) {
                final ImageInfo imageInfo = this.parentForm.getImageInfo();
                rasterDataUnloader.unloadUnusedRasterData(oldChannelSource);
                currentChannelSources[channel] = newChannelSource;
                models[channel] = new ImageInfoEditorModel3B(imageInfo, channel);
                models[channel].setDisplayProperties(newChannelSource);
                imageInfo.getRgbChannelDef().setSourceName(channel, channelSourceName);
                acknowledgeChannel();
                imageInfoEditor.compute95Percent();
                this.parentForm.setApplyEnabled(true);
            } else {
                final Object value = evt.getOldValue();
                moreOptionsForm.getBindingContext().getBinding(CHANNEL_SOURCE_NAME_PROPERTY).setPropertyValue(value == null ? "" : value);
            }
        }
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