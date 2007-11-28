package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.*;
import com.bc.ceres.core.Assert;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;

// todo  - this class needs a refactoring
// todo - extract AbstractBinding classes
// todo - check if we can generalize bind() method

public class SwingBindingContext {

    private ValueContainer valueContainer;
    private SwingBindingContext.ErrorHandler errorHandler;

    public SwingBindingContext(ValueContainer valueContainer) {
        this(valueContainer, new SwingBindingContext.DefaultErrorHandler());
    }

    public SwingBindingContext(ValueContainer valueContainer, SwingBindingContext.ErrorHandler errorHandler) {
        this.valueContainer = valueContainer;
        this.errorHandler = errorHandler;
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void enable(final JComponent component, final String propertyName, final boolean value) {
        valueContainer.addPropertyChangeListener(propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setEnabledImpl(component, propertyName, value);
            }
        });
        setEnabledImpl(component, propertyName, value);
    }

    private void setEnabledImpl(JComponent component, String propertyName, boolean value) {
        Boolean propertyValue = (Boolean) valueContainer.getValue(propertyName);
        component.setEnabled(value == (propertyValue != null && propertyValue));
    }

    public void bind(final JTextField textField, final String propertyName) {
        configureComponent(textField, propertyName);
        TextFieldBinding textFieldBinding = new SwingBindingContext.TextFieldBinding(textField, propertyName);
        textField.addActionListener(textFieldBinding);
        textField.setInputVerifier(textFieldBinding.createInputVerifier());
        textFieldBinding.adjustWidget();
    }

    public void bind(final JFormattedTextField textField, final String propertyName) {
        configureComponent(textField, propertyName);
        FormattedTextFieldBinding textFieldBinding = new FormattedTextFieldBinding(textField, propertyName);
        textField.addPropertyChangeListener("value", textFieldBinding);
        textFieldBinding.adjustWidget();
    }

    public void bind(final JCheckBox checkBox, final String propertyName) {
        configureComponent(checkBox, propertyName);
        CheckBoxBinding checkBoxBinding = new CheckBoxBinding(propertyName, checkBox);
        checkBox.addActionListener(checkBoxBinding);
        checkBoxBinding.adjustWidget();
    }

    public void bind(JRadioButton radioButton, String propertyName) {
        configureComponent(radioButton, propertyName);
        RadioButtonBinding radioButtonBinding = new RadioButtonBinding(propertyName, radioButton);
        radioButton.addChangeListener(radioButtonBinding);
        radioButtonBinding.adjustWidget();
    }

    public void bind(final JList list, final String propertyName, final boolean selectionIsValue) {
        configureComponent(list, propertyName);
        if (selectionIsValue) {
            ListSelectionBinding binding = new ListSelectionBinding(list, propertyName);
            list.addListSelectionListener(binding);
            binding.adjustWidget();
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public void bind(final JSpinner spinner, final String propertyName) {
        configureComponent(spinner, propertyName);
        SpinnerBinding binding = new SpinnerBinding(propertyName, spinner);
        spinner.addChangeListener(binding);
        binding.adjustWidget();
    }

    public void bind(final JComboBox comboBox, final String propertyName) {
        configureComponent(comboBox, propertyName);
        ComboBoxBinding comboBoxBinding = new ComboBoxBinding(propertyName, comboBox);
        comboBox.addActionListener(comboBoxBinding);
        comboBoxBinding.adjustWidget();
    }

    private void handleError(JComponent component, Exception e) {
        System.out.println("e = " + e);
        errorHandler.handleError(component, e);
    }

    private void configureComponent(JComponent component, String propertyName) {
        Assert.notNull(component, "component");
        Assert.notNull(propertyName, "propertyName");
        final ValueModel valueModel = valueContainer.getModel(propertyName);
        Assert.argument(valueModel != null, "Undefined property '" + propertyName + "'");
        if (component.getName() == null) {
            component.setName(propertyName);
        }
        if (valueModel != null) {
            if (component.getToolTipText() == null) {
                StringBuilder toolTipText = new StringBuilder();
                final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
                if (valueDescriptor.getDescription() != null) {
                    toolTipText.append(valueDescriptor.getDescription());
                }
                if (valueDescriptor.getUnit() != null && !valueDescriptor.getUnit().isEmpty()) {
                    toolTipText.append(" [");
                    toolTipText.append(valueDescriptor.getUnit());
                    toolTipText.append("]");
                }
                if (toolTipText.length() > 0) {
                    component.setToolTipText(toolTipText.toString());
                }
            }
        }
    }

    public interface ErrorHandler {

        void handleError(JComponent component, Exception e);
    }

    public static class DefaultErrorHandler implements SwingBindingContext.ErrorHandler {

        public void handleError(JComponent component, Exception e) {
            JOptionPane.showMessageDialog(component, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            component.requestFocus();
        }
    }


    abstract class AbstractBinding {

        private String propertyName;
        private boolean adjustingWidget;
//        private boolean adjustingProperty;

        public AbstractBinding(String propertyName) {
            this.propertyName = propertyName;
            valueContainer.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    adjustWidget();
                }
            });
        }

//        public boolean isAdjustingProperty() {
//            return adjustingProperty;
//        }

        public boolean isAdjustingWidget() {
            return adjustingWidget;
        }

        public String getPropertyName() {
            return propertyName;
        }

//        public final void adjustProperty() {
//            if (!adjustingProperty) {
//                try {
//                    adjustingProperty = true;
//                    adjustPropertyImpl();
//                } finally {
//                    adjustingProperty = false;
//                }
//            }
//        }
//
//        protected void adjustPropertyImpl() {
//            adjustWidget();
//        }

        public void adjustWidget() {
            if (!adjustingWidget) {
                try {
                    adjustingWidget = true;
                    adjustWidgetImpl();
                } finally {
                    adjustingWidget = false;
                }
            }
        }

        protected abstract void adjustWidgetImpl();
    }

    class SpinnerBinding extends SwingBindingContext.AbstractBinding implements ChangeListener {

        final JSpinner spinner;

        public SpinnerBinding(String propertyName, JSpinner spinner) {
            super(propertyName);
            this.spinner = spinner;

            ValueDescriptor valueDescriptor = valueContainer.getValueDescriptor(propertyName);
            if (valueDescriptor.getValueRange() != null) {
                Class<?> type = valueDescriptor.getType();

                if (Number.class.isAssignableFrom(type)) {
                    Number defaultValue = (Number) valueDescriptor.getDefaultValue();
                    double min = valueDescriptor.getValueRange().getMin();
                    double max = valueDescriptor.getValueRange().getMax();
                    // todo - get step size from interval

                    if (type == Byte.class) {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, (byte) min, (byte) max, 1));
                    } else if (type == Short.class) {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, (short) min, (short) max, 1));
                    } else if (type == Integer.class) {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, (int) min, (int) max, 1));
                    } else if (type == Long.class) {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, (long) min, (long) max, 1));
                    } else if (type == Float.class) {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, (float) min, (float) max, 1));
                    } else {
                        spinner.setModel(new SpinnerNumberModel(defaultValue, min, max, 1));
                    }
                }
            } else if (valueDescriptor.getValueSet() != null) {
                spinner.setModel(new SpinnerListModel(valueDescriptor.getValueSet().getItems()));
            }
        }

        public void stateChanged(ChangeEvent evt) {
            try {
                valueContainer.setValue(getPropertyName(), spinner.getValue());
            } catch (Exception e) {
                handleError(spinner, e);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            try {
                Object value = valueContainer.getValue(getPropertyName());
                spinner.setValue(value);
            } catch (Exception e) {
                handleError(spinner, e);
            }
        }
    }

    class ListSelectionBinding extends SwingBindingContext.AbstractBinding implements ListSelectionListener {

        private final JList list;

        public ListSelectionBinding(JList list, String propertyName) {
            super(propertyName);
            this.list = list;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (isAdjustingWidget()) {
                return;
            }
            final ValueModel model = valueContainer.getModel(getPropertyName());
            Object[] selectedValues = list.getSelectedValues();
            Object array = Array.newInstance(model.getDescriptor().getType().getComponentType(), selectedValues.length);
            for (int i = 0; i < selectedValues.length; i++) {
                Array.set(array, i, selectedValues[i]);
            }
            try {
                model.setValue(array);
            } catch (ValidationException e1) {
                handleError(list, e1);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            Object array = valueContainer.getValue(getPropertyName());
            if (array != null) {
                ListModel model = list.getModel();
                int size = model.getSize();
                int[] temp = new int[size];
                int numSelectedElements = 0;
                int arrayLength = Array.getLength(array);
                for (int i = 0; i < size; i++) {
                    Object element = model.getElementAt(i);
                    for (int j = 0; j < arrayLength; j++) {
                        if (element.equals(Array.get(array, j))) {
                            temp[numSelectedElements++] = i;
                        }
                    }
                }
                int[] indices = new int[numSelectedElements];
                System.arraycopy(temp, 0, indices, 0, numSelectedElements);
                list.setSelectedIndices(indices);
            }
        }
    }

    class TextFieldBinding extends SwingBindingContext.AbstractBinding implements ActionListener {

        private final JTextField textField;

        public TextFieldBinding(JTextField textField, String propertyName) {
            super(propertyName);
            this.textField = textField;
        }

        public void actionPerformed(ActionEvent e) {
            adjustValueContainer();
        }

        @Override
        protected void adjustWidgetImpl() {
            String text = valueContainer.getAsText(getPropertyName());
            textField.setText(text);
        }

        private boolean adjustValueContainer() {
            try {
                valueContainer.setFromText(getPropertyName(), textField.getText());
                return true;
            } catch (Exception e) {
                handleError(textField, e);
                return false;
            }
        }

        public InputVerifier createInputVerifier() {
            return new TextFieldVerifier(this);
        }
    }

    class FormattedTextFieldBinding extends SwingBindingContext.AbstractBinding implements PropertyChangeListener {

        private final JFormattedTextField textField;

        public FormattedTextFieldBinding(JFormattedTextField textField, String propertyName) {
            super(propertyName);
            this.textField = textField;
        }

        public void propertyChange(PropertyChangeEvent e) {
            try {
                valueContainer.setValue(getPropertyName(), textField.getValue());
            } catch (Exception e1) {
                handleError(textField, e1);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            try {
                Object value = valueContainer.getValue(getPropertyName());
                textField.setValue(value);
            } catch (Exception e) {
                handleError(textField, e);
            }
        }
    }

    class ComboBoxBinding extends SwingBindingContext.AbstractBinding implements ActionListener {

        final JComboBox comboBox;

        public ComboBoxBinding(String propertyName, JComboBox comboBox) {
            super(propertyName);
            this.comboBox = comboBox;

            ValueDescriptor valueDescriptor = valueContainer.getValueDescriptor(propertyName);
            ValueSet valueSet = valueDescriptor.getValueSet();
            if (valueSet != null) {
                comboBox.setModel(new DefaultComboBoxModel(valueSet.getItems()));
            }
        }

        public void actionPerformed(ActionEvent event) {
            try {
                valueContainer.setValue(getPropertyName(), comboBox.getSelectedItem());
            } catch (Exception e) {
                handleError(comboBox, e);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            try {
                Object value = valueContainer.getValue(getPropertyName());
                comboBox.setSelectedItem(value);
            } catch (Exception e) {
                handleError(comboBox, e);
            }
        }
    }

    class CheckBoxBinding extends SwingBindingContext.AbstractBinding implements ActionListener {

        private final JCheckBox checkBox;

        public CheckBoxBinding(String propertyName, JCheckBox checkBox) {
            super(propertyName);
            this.checkBox = checkBox;
        }

        public void actionPerformed(ActionEvent event) {
            try {
                valueContainer.setValue(getPropertyName(), checkBox.isSelected());
            } catch (Exception e) {
                handleError(checkBox, e);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            try {
                boolean selected = (Boolean) valueContainer.getValue(getPropertyName());
                checkBox.setSelected(selected);
            } catch (Exception e) {
                handleError(checkBox, e);
            }
        }
    }

    class RadioButtonBinding extends SwingBindingContext.AbstractBinding implements ChangeListener {

        private final JRadioButton radioButton;

        public RadioButtonBinding(String propertyName, JRadioButton radioButton) {
            super(propertyName);
            this.radioButton = radioButton;
        }

        public void stateChanged(ChangeEvent event) {
            try {
                valueContainer.setValue(getPropertyName(), radioButton.isSelected());
            } catch (Exception e) {
                handleError(radioButton, e);
            }
        }

        @Override
        protected void adjustWidgetImpl() {
            try {
                boolean selected = (Boolean) valueContainer.getValue(getPropertyName());
                radioButton.setSelected(selected);
            } catch (Exception e) {
                handleError(radioButton, e);
            }
        }
    }

    private class TextFieldVerifier extends InputVerifier {
        private TextFieldBinding binding;

        private TextFieldVerifier(TextFieldBinding binding) {
            this.binding = binding;
        }

        /**
         * Checks whether the JComponent's input is valid. This method should have no side effects. It returns a boolean
         * indicating the status of the argument's input.
         *
         * @param input the JComponent to verify
         * @return <code>true</code> when valid, <code>false</code> when invalid
         * @see JComponent#setInputVerifier
         * @see JComponent#getInputVerifier
         */
        @Override
        public boolean verify(JComponent input) {
            try {
                final String text = ((JTextField) input).getText();
                final String name = binding.getPropertyName();
                final ValueDescriptor descriptor = valueContainer.getValueDescriptor(name);
                final Converter converter = descriptor.getConverter();
                Assert.notNull(converter);
                final Object value = converter.parse(text);
                final Validator validator = descriptor.getValidator();
                if (validator != null) {
                    validator.validateValue(valueContainer.getModel(name), value);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        /**
         * Calls <code>verify(input)</code> to ensure that the input is valid. This method can have side effects. In
         * particular, this method is called when the user attempts to advance focus out of the argument component into
         * another Swing component in this window. If this method returns <code>true</code>, then the focus is
         * transfered normally; if it returns <code>false</code>, then the focus remains in the argument component.
         *
         * @param input the JComponent to verify
         * @return <code>true</code> when valid, <code>false</code> when invalid
         * @see JComponent#setInputVerifier
         * @see JComponent#getInputVerifier
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            return binding.adjustValueContainer();
        }
    }
}
