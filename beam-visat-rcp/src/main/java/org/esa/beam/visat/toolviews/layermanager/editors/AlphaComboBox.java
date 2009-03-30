package org.esa.beam.visat.toolviews.layermanager.editors;

import com.jidesoft.combobox.AbstractComboBox;
import com.jidesoft.combobox.PopupPanel;

import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.ParseException;


class AlphaComboBox extends AbstractComboBox {

    private DecimalFormat decimalFormat;
    private final double factor;
    private final double minimum;
    private final double maximum;
    private double value;

    AlphaComboBox(double minimum, double maximum, double value, double sliderFactor) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = value;
        this.factor = sliderFactor;

        decimalFormat = new DecimalFormat("0.0#");
        listenerList = new EventListenerList();

        initComponent();
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        setSelectedItem(Double.toString(value));
    }

    @Override
    public void setSelectedItem(Object o) {
        if (o instanceof String) {
            String text = (String) o;
            value = Double.valueOf(text);
            super.setSelectedItem(decimalFormat.format(value));
        }
    }

    @Override
    public String getSelectedItem() {
        return decimalFormat.format(value);
    }

    @Override
    public EditorComponent createEditorComponent() {
        DefaultTextFieldEditorComponent editorComponent = new DefaultTextFieldEditorComponent(Number.class);
        Component editor = editorComponent.getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField textField = (JTextField) editor;
            textField.setColumns(
                    Math.max(decimalFormat.format(minimum).length() + 1, decimalFormat.format(minimum).length() + 1));
            textField.setHorizontalAlignment(JTextField.RIGHT);
        }
        return editorComponent;
    }

    @Override
    public PopupPanel createPopupComponent() {
        final JSlider slider = new JSlider(JSlider.HORIZONTAL,
                                           scaleValue(minimum), scaleValue(maximum),
                                           scaleValue(value));
        slider.setPreferredSize(new Dimension(150, slider.getPreferredSize().height));
        slider.addChangeListener(new SliderChangeListener(slider));

        final PopupPanel panel = new MyPopupPanel(slider);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private int scaleValue(double value) {
        return (int) Math.ceil(value * factor);
    }

    private double scaleValueInverse(int value) {
        return value / factor;
    }

    private class MyPopupPanel extends PopupPanel {

        private final JSlider slider;

        private MyPopupPanel(JSlider slider) {
            super(new BorderLayout());
            this.slider = slider;
        }

        @Override
        public Object getSelectedObject() {
            int sliderValue = slider.getValue();
            double scaledValue = scaleValueInverse(sliderValue);
            return String.valueOf(scaledValue);
        }

        @Override
        public void setSelectedObject(Object o) {
            if (o instanceof String) {
                String text = (String) o;
                try {
                    slider.setValue(scaleValue(decimalFormat.parse(text).doubleValue()));
                } catch (ParseException ignored) {
                }
            }
            super.setSelectedObject(o);
        }
    }

    private class SliderChangeListener implements ChangeListener {

        private final JSlider slider;

        private SliderChangeListener(JSlider slider) {
            this.slider = slider;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof JSlider) {
                int sliderValue = slider.getValue();
                double scaledValue = scaleValueInverse(sliderValue);
                String valueAsString = String.valueOf(scaledValue);
                setSelectedItem(valueAsString);
                if (!slider.getValueIsAdjusting()) {
                    fireActionEvent();
                    AlphaComboBox.this.requestFocus();
                    hidePopup();
                }
            }
        }
    }
}
