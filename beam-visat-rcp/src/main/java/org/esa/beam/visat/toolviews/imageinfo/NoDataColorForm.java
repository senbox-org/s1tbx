package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.combobox.ColorComboBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;

class NoDataColorForm {
    static final String NO_DATA_COLOR_PROPERTY = "noDataColor";

    private ValueContainer valueContainer;
    private JPanel contentPanel;
    private ColorComboBoxBinding noDataColorBinding;
    @SuppressWarnings({"UnusedDeclaration"})
    private Color noDataColor;

    NoDataColorForm() {
        JLabel label = new JLabel("No-data color:");
        ColorComboBox noDataColorComboBox = new ColorComboBox();
        noDataColorComboBox.setColorValueVisible(false);
        noDataColorComboBox.setAllowDefaultColor(true);

        valueContainer = new ValueContainer();
        valueContainer.addModel(ValueModel.create(this, NO_DATA_COLOR_PROPERTY));

        BindingContext context = new BindingContext(valueContainer);
        noDataColorBinding = new ColorComboBoxBinding(context, noDataColorComboBox, NO_DATA_COLOR_PROPERTY);
        noDataColorBinding.adjustComponents();
        noDataColorBinding.attachSecondaryComponent(label);

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

    public boolean isNoDataColorUsed() {
        return getNoDataColor() != null;
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void enable(boolean enabled) {
        noDataColorBinding.setComponentsEnabledState(enabled);
    }
}
