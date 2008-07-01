package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.internal.*;
import com.bc.ceres.core.Assert;

import javax.swing.*;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A context used to bind Swing components to properties in a value container.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.1
 */
public class BindingContext {

    private final ValueContainer valueContainer;
    private BindingContext.ErrorHandler errorHandler;
    private Map<String, Binding> bindingMap;

    public BindingContext(ValueContainer valueContainer) {
        this(valueContainer, new BindingContext.DefaultErrorHandler());
    }

    public BindingContext(ValueContainer valueContainer, BindingContext.ErrorHandler errorHandler) {
        this.valueContainer = valueContainer;
        this.errorHandler = errorHandler;
        this.bindingMap = new HashMap<String, Binding>(17);
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Binding getBinding(String propertyName) {
        return bindingMap.get(propertyName);
    }

    public Binding addBinding(Binding binding) {
        return bindingMap.put(binding.getName(), binding);
    }

    public Binding removeBinding(String propertyName) {
        return bindingMap.remove(propertyName);
    }

    public Binding bind(final String propertyName, final JTextField textField) {
        configureComponent(propertyName, textField);
        Binding binding = new TextFieldBinding(this, textField, propertyName);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final String propertyName, final JFormattedTextField textField) {
        configureComponent(propertyName, textField);
        Binding binding = new FormattedTextFieldBinding(this, textField, propertyName);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final String propertyName, final JCheckBox checkBox) {
        configureComponent(propertyName, checkBox);
        Binding binding = new AbstractButtonBinding(this, propertyName, checkBox);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(String propertyName, JRadioButton radioButton) {
        configureComponent(propertyName, radioButton);
        Binding binding = new AbstractButtonBinding(this, propertyName, radioButton);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final String propertyName, final JList list, final boolean selectionIsValue) {
        if (selectionIsValue) {
            configureComponent(propertyName, list);
            Binding binding = new ListSelectionBinding(this, list, propertyName);
            addBinding(binding);
            binding.adjustComponents();
            return binding;
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public Binding bind(final String propertyName, final JSpinner spinner) {
        configureComponent(propertyName, spinner);
        Binding binding = new SpinnerBinding(this, propertyName, spinner);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final String propertyName, final JComboBox comboBox) {
        configureComponent(propertyName, comboBox);
        Binding binding = new ComboBoxBinding(this, propertyName, comboBox);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup) {
        return bind(propertyName, buttonGroup, ButtonGroupBinding.createButtonToValueMap(buttonGroup, getValueContainer(), propertyName));
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup, final Map<AbstractButton, Object> valueSet) {
        Binding binding = new ButtonGroupBinding(this, buttonGroup, propertyName, valueSet);
        addBinding(binding);
        binding.adjustComponents();
        return binding;
    }

    public void enable(final String propertyName, final JComponent component, final Object propertyCondition) {
        bindEnabling(propertyName, component, propertyCondition, true);
    }

    public void disable(final String propertyName, final JComponent component, final Object propertyCondition) {
        bindEnabling(propertyName, component, propertyCondition, false);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        valueContainer.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        valueContainer.addPropertyChangeListener(name, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        valueContainer.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        valueContainer.removePropertyChangeListener(name, l);
    }

    /**
     * Delegates the call to the error handler.
     *
     * @param exception The error.
     * @param component The Swing component in which the error occured.
     * @see #getErrorHandler()
     * @see #setErrorHandler(ErrorHandler)
     */
    public void handleError(Exception exception, JComponent component) {
        errorHandler.handleError(exception, component);
    }

    private void configureComponent(String propertyName, JComponent component) {
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


    private void bindEnabling(final String propertyName,
                              final JComponent component,
                              final Object propertyCondition,
                              final boolean componentEnabled) {
        valueContainer.addPropertyChangeListener(propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setEnabled(propertyName, component, propertyCondition, componentEnabled);
            }
        });
        setEnabled(propertyName, component, propertyCondition, componentEnabled);
    }

    private void setEnabled(final String propertyName,
                            final JComponent component,
                            final Object propertyCondition,
                            final boolean componentEnabled) {
        Object propertyValue = valueContainer.getValue(propertyName);
        boolean conditionIsTrue = propertyValue == propertyCondition
                || (propertyValue != null && propertyValue.equals(propertyCondition));
        component.setEnabled(conditionIsTrue ? componentEnabled : !componentEnabled);
    }

    public interface ErrorHandler {

        void handleError(Exception exception, JComponent component);
    }

    private static class DefaultErrorHandler implements BindingContext.ErrorHandler {

        public void handleError(Exception exception, JComponent component) {
            Window window = component != null ? SwingUtilities.windowForComponent(component) : null;
            if (exception instanceof ValidationException) {
                JOptionPane.showMessageDialog(window, exception.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } else {
                String message = MessageFormat.format("An internal error occured:\nType: {0}\nMessage: {1}\n", exception.getClass(), exception.getMessage());
                JOptionPane.showMessageDialog(window, message, "Internal Error", JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
            if (component != null) {
                component.requestFocus();
            }
        }
    }
}
