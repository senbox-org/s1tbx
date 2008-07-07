package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.internal.*;

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
    private Map<String, BindingImpl> bindingMap;

    public BindingContext(ValueContainer valueContainer) {
        this(valueContainer, new BindingContext.DefaultErrorHandler());
    }

    public BindingContext(ValueContainer valueContainer, BindingContext.ErrorHandler errorHandler) {
        this.valueContainer = valueContainer;
        this.errorHandler = errorHandler;
        this.bindingMap = new HashMap<String, BindingImpl>(17);
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

    public Binding bind(String propertyName, ComponentAdapter adapter) {
        BindingImpl binding = new BindingImpl(this, propertyName, adapter);
        addBinding(binding);
        adapter.setBinding(binding);
        adapter.bindComponents();
        binding.bindProperty();
        binding.adjustComponents();
        configureComponents(binding);
        return binding;
    }

    public void unbind(Binding binding) {
        removeBinding(binding.getPropertyName());
        if (binding instanceof BindingImpl) {
            ((BindingImpl)binding).unbindProperty();
        }
        binding.getComponentAdapter().unbindComponents();        
    }

    public Binding bind(final String propertyName, final JTextField textField) {
        return bind(propertyName, new TextFieldAdapter(textField));
    }

    public Binding bind(final String propertyName, final JFormattedTextField textField) {
        return bind(propertyName, new FormattedTextFieldAdapter(textField));
    }

    public Binding bind(final String propertyName, final JCheckBox checkBox) {
        return bind(propertyName, new AbstractButtonAdapter(checkBox));
    }

    public Binding bind(String propertyName, JRadioButton radioButton) {
        return bind(propertyName, new AbstractButtonAdapter(radioButton));
    }

    public Binding bind(final String propertyName, final JList list, final boolean selectionIsValue) {
        if (selectionIsValue) {
            return bind(propertyName, new ListSelectionAdapter(list));
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public Binding bind(final String propertyName, final JSpinner spinner) {
        return bind(propertyName, new SpinnerAdapter(spinner));
    }

    public Binding bind(final String propertyName, final JComboBox comboBox) {
        return bind(propertyName, new ComboBoxAdapter(comboBox));
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup) {
        return bind(propertyName, buttonGroup, ButtonGroupAdapter.createButtonToValueMap(buttonGroup, getValueContainer(), propertyName));
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup, final Map<AbstractButton, Object> valueSet) {
        ComponentAdapter adapter = new ButtonGroupAdapter(buttonGroup, valueSet);
        return bind(propertyName, adapter);
    }

    public void enable(final String propertyName, final Object propertyCondition, final JComponent component) {
        bindEnabling(propertyName, propertyCondition, true, component);
    }

    public void disable(final String propertyName, final Object propertyCondition, final JComponent component) {
        bindEnabling(propertyName, propertyCondition, false, component);
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

    private void configureComponents(Binding binding) {
        final String propertyName = binding.getPropertyName();
        final String toolTipTextStr = getToolTipText(propertyName);
        final JComponent[] components = binding.getComponents();
        JComponent primaryComponent =  components[0];
        configureComponent(primaryComponent, propertyName, toolTipTextStr);
        for (int i = 1; i < components.length; i++) {
            JComponent component = components[i];
            configureComponent(component, propertyName + "." + i, toolTipTextStr);
        }
    }

    private String getToolTipText(String propertyName) {
        final ValueModel valueModel = valueContainer.getModel(propertyName);
        StringBuilder toolTipText = new StringBuilder(32);
        final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
        if (valueDescriptor.getDescription() != null) {
            toolTipText.append(valueDescriptor.getDescription());
        }
        if (valueDescriptor.getUnit() != null && !valueDescriptor.getUnit().isEmpty()) {
            toolTipText.append(" (");
            toolTipText.append(valueDescriptor.getUnit());
            toolTipText.append(")");
        }
        return toolTipText.toString();
    }

    private void configureComponent(JComponent component, String name, String toolTipText) {
        if (component.getName() == null) {
            component.setName(name);
        }
        if (component.getToolTipText() == null && !toolTipText.isEmpty()) {
            component.setToolTipText(toolTipText);
        }
    }

    private void bindEnabling(final String propertyName,
                              final Object propertyCondition, final boolean componentEnabled, final JComponent component) {
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

    private Binding addBinding(BindingImpl binding) {
        return bindingMap.put(binding.getPropertyName(), binding);
    }

    private Binding removeBinding(String propertyName) {
        return bindingMap.remove(propertyName);
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
