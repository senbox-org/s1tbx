package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.combobox.ColorComboBox;
import org.esa.beam.framework.datamodel.ImageInfo;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class MoreOptionsForm {
    static final String NO_DATA_COLOR_PROPERTY = "noDataColor";
    static final String HISTOGRAM_MATCHING_PROPERTY = "histogramMatching";

    private JPanel contentPanel;
    private GridBagConstraints constraints;
    private BindingContext bindingContext;

    private ColorManipulationForm parentForm;

    MoreOptionsForm(ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        ValueContainer valueContainer = new ValueContainer();
        valueContainer.addModel(ValueModel.createModel(NO_DATA_COLOR_PROPERTY, ImageInfo.NO_COLOR));
        valueContainer.addModel(ValueModel.createModel(HISTOGRAM_MATCHING_PROPERTY, ImageInfo.HistogramMatching.None));

        valueContainer.getModel(HISTOGRAM_MATCHING_PROPERTY).getDescriptor().setNotNull(true);
        valueContainer.getModel(HISTOGRAM_MATCHING_PROPERTY).getDescriptor().setValueSet(new ValueSet(
                new ImageInfo.HistogramMatching[]{
                        ImageInfo.HistogramMatching.None,
                        ImageInfo.HistogramMatching.Equalize,
                        ImageInfo.HistogramMatching.Normalize,
                })
        );


        JLabel noDataColorLabel = new JLabel("No-data colour: ");
        ColorComboBox noDataColorComboBox = new ColorComboBox();
        noDataColorComboBox.setColorValueVisible(false);
        noDataColorComboBox.setAllowDefaultColor(true);

        JLabel histogramMatchingLabel = new JLabel("Histogram matching: ");
        JComboBox histogramMatchingBox = new JComboBox();

        bindingContext = new BindingContext(valueContainer);

        Binding noDataColorBinding = new ColorComboBoxBinding(bindingContext, NO_DATA_COLOR_PROPERTY, noDataColorComboBox);
        bindingContext.addBinding(noDataColorBinding);
        noDataColorBinding.attachSecondaryComponent(noDataColorLabel);
        noDataColorBinding.adjustComponents();

        bindingContext.bind(HISTOGRAM_MATCHING_PROPERTY, histogramMatchingBox);
        bindingContext.getBinding(HISTOGRAM_MATCHING_PROPERTY).attachSecondaryComponent(histogramMatchingLabel);
        contentPanel = new JPanel(new GridBagLayout());

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.BASELINE;
        constraints.weightx = 0.5;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(1, 0, 1, 0);

        addRow(noDataColorLabel, noDataColorComboBox);
        addRow(histogramMatchingLabel, histogramMatchingBox);

        final PropertyChangeListener pcl = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                getParentForm().getImageInfo().setNoDataColor(getNoDataColor());
                getParentForm().getImageInfo().setHistogramMatching(getHistogramMatching());
                getParentForm().setApplyEnabled(true);
            }
        };

        bindingContext.addPropertyChangeListener(NO_DATA_COLOR_PROPERTY, pcl);
        bindingContext.addPropertyChangeListener(HISTOGRAM_MATCHING_PROPERTY, pcl);
    }

    public ColorManipulationForm getParentForm() {
        return parentForm;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public void addRow(JLabel label, JComponent editor) {
        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.gridx = 0;
        contentPanel.add(label, constraints);
        constraints.gridx = 1;
        contentPanel.add(editor, constraints);
    }

    public void addRow(JComponent editor) {
        constraints.gridwidth = 2;
        constraints.gridy++;
        constraints.gridx = 0;
        contentPanel.add(editor, constraints);
    }

    public Color getNoDataColor() {
        return (Color) getBindingContext().getBinding(NO_DATA_COLOR_PROPERTY).getValue();
    }

    public void setNoDataColor(Color color) {
        getBindingContext().getBinding(NO_DATA_COLOR_PROPERTY).setValue(color);
    }

    public ImageInfo.HistogramMatching getHistogramMatching() {
        return (ImageInfo.HistogramMatching) getBindingContext().getBinding(HISTOGRAM_MATCHING_PROPERTY).getValue();
    }

    public void setHistogramMatching(ImageInfo.HistogramMatching histogramMatching) {
        getBindingContext().getBinding(HISTOGRAM_MATCHING_PROPERTY).setValue(histogramMatching);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        bindingContext.addPropertyChangeListener(propertyChangeListener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        bindingContext.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

}
