package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.combobox.ColorComboBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.beans.PropertyChangeListener;

class NoDataColorForm {
    static final String NO_DATA_COLOR_PROPERTY = "noDataColor";

    private JPanel contentPanel;
    private ColorComboBoxBinding noDataColorBinding;

    NoDataColorForm() {
        JLabel label = new JLabel("No-data color:");
        ColorComboBox noDataColorComboBox = new ColorComboBox();
        noDataColorComboBox.setColorValueVisible(false);
        noDataColorComboBox.setAllowDefaultColor(true);

        ValueContainer valueContainer = new ValueContainer();
        valueContainer.addModel(ValueModel.createModel(NO_DATA_COLOR_PROPERTY, Color.class));

        BindingContext context = new BindingContext(valueContainer);
        noDataColorBinding = new ColorComboBoxBinding(context, noDataColorComboBox, NO_DATA_COLOR_PROPERTY);
        noDataColorBinding.attachSecondaryComponent(label);
        noDataColorBinding.adjustComponents();

        contentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        contentPanel.add(label);
        contentPanel.add(noDataColorComboBox);
    }

    public Color getNoDataColor() {
        return (Color) noDataColorBinding.getValue();
    }

    public void setNoDataColor(Color color) {
        noDataColorBinding.setValue(color);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void setEnabled(boolean enabled) {
        noDataColorBinding.setComponentsEnabledState(enabled);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        noDataColorBinding.getContext().getValueContainer().addPropertyChangeListener(propertyChangeListener);
    }
}
