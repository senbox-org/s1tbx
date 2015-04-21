/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.binding;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.internal.AbstractButtonAdapter;
import com.bc.ceres.swing.binding.internal.BindingImpl;
import com.bc.ceres.swing.binding.internal.ButtonGroupAdapter;
import com.bc.ceres.swing.binding.internal.ComboBoxAdapter;
import com.bc.ceres.swing.binding.internal.FormattedTextFieldAdapter;
import com.bc.ceres.swing.binding.internal.ListSelectionAdapter;
import com.bc.ceres.swing.binding.internal.SpinnerAdapter;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A context used to bind Swing components to properties in a value container.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.6
 */
public class BindingContext {

    private final PropertySet propertySet;
    private Map<String, BindingImpl> bindings;
    private Map<String, EnablementImpl> enablements;
    private ArrayList<BindingProblemListener> bindingProblemListeners;

    /**
     * Constructor. Uses an empty, default property set and a
     * default problem handler which will display an error dialog box on any binding errors.
     */
    public BindingContext() {
        this(new PropertyContainer());
    }

    /**
     * Constructor. Uses a default problem handler which will display an error dialog box on any binding errors.
     *
     * @param propertySet The property set.
     */
    public BindingContext(PropertySet propertySet) {
        this(propertySet, new VerbousProblemHandler());
    }

    /**
     * Constructor.
     *
     * @param propertySet    The property set.
     * @param problemHandler A problem handler, or {@code null}.
     */
    public BindingContext(PropertySet propertySet, BindingProblemListener problemHandler) {
        this.propertySet = propertySet;
        this.bindings = new HashMap<String, BindingImpl>(17);
        this.enablements = new HashMap<String, EnablementImpl>(11);
        if (problemHandler != null) {
            addProblemListener(problemHandler);
        }
    }

    /**
     * @return The property set.
     */
    public PropertySet getPropertySet() {
        return propertySet;
    }

    /**
     * @return {@code true} if this context has problems.
     * @since Ceres 0.10
     */
    public boolean hasProblems() {
        for (Map.Entry<String, BindingImpl> entry : bindings.entrySet()) {
            if (entry.getValue().getProblem() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The array of problems this context might have.
     * @since Ceres 0.10
     */
    public BindingProblem[] getProblems() {
        ArrayList<BindingProblem> list = new ArrayList<BindingProblem>();
        for (Map.Entry<String, BindingImpl> entry : bindings.entrySet()) {
            final BindingProblem problem = entry.getValue().getProblem();
            if (problem != null) {
                list.add(problem);
            }
        }
        return list.toArray(new BindingProblem[list.size()]);
    }

    /**
     * Adds a problem listener to this context.
     *
     * @param listener The listener.
     * @since Ceres 0.10
     */
    public void addProblemListener(BindingProblemListener listener) {
        Assert.notNull(listener, "listener");
        if (bindingProblemListeners == null) {
            bindingProblemListeners = new ArrayList<BindingProblemListener>();
        }
        bindingProblemListeners.add(listener);
    }

    /**
     * Removes a problem listener from this context.
     *
     * @param listener The listener.
     * @since Ceres 0.10
     */
    public void removeProblemListener(BindingProblemListener listener) {
        Assert.notNull(listener, "listener");
        if (bindingProblemListeners != null) {
            bindingProblemListeners.remove(listener);
        }
    }

    /**
     * @return The array of problem listeners.
     * @since Ceres 0.10
     */
    public BindingProblemListener[] getProblemListeners() {
        return bindingProblemListeners != null
                ? bindingProblemListeners.toArray(new BindingProblemListener[bindingProblemListeners.size()])
                : new BindingProblemListener[0];
    }

    /**
     * Adjusts all associated GUI components so that they reflect the
     * values of the associated value container.
     *
     * @see ComponentAdapter#adjustComponents()
     */
    public void adjustComponents() {
        for (Map.Entry<String, BindingImpl> entry : bindings.entrySet()) {
            entry.getValue().adjustComponents();
        }
    }

    /**
     * Gets the binding for the given property name.
     *
     * @param propertyName The property name.
     * @return The binding, or {@code null} if no such exists.
     */
    public Binding getBinding(String propertyName) {
        Assert.notNull(propertyName, "propertyName");
        return bindings.get(propertyName);
    }

    /**
     * Binds the property given by its name to the given component adapter.
     * <p>
     * The method creates a new binding, adds it to this context and calls the follwing methods on
     * the given component adapter:
     * <ol>
     * <li>{@link ComponentAdapter#setBinding(Binding) componentAdapter.setBinding(binding)}</li>
     * <li>{@link ComponentAdapter#bindComponents() componentAdapter.bindComponents()}</li>
     * <li>{@link ComponentAdapter#adjustComponents() componentAdapter.adjustComponents(}</li>
     * </ol>
     *
     * @param propertyName     The property name.
     * @param componentAdapter The component adapter.
     * @return The resulting binding.
     * @see #unbind(Binding)
     */
    public Binding bind(String propertyName, ComponentAdapter componentAdapter) {
        Assert.notNull(propertyName, "propertyName");
        Assert.notNull(componentAdapter, "componentAdapter");
        BindingImpl binding = new BindingImpl(this, propertyName, componentAdapter);
        addBinding(binding);
        componentAdapter.setBinding(binding);
        componentAdapter.bindComponents();
        binding.bindProperty();
        binding.adjustComponents();
        configureComponents(binding);
        return binding;
    }

    /**
     * Cancels the given binding by calling removing it from this context and finally calling
     * <ol>
     * <li>{@link ComponentAdapter#unbindComponents() componentAdapter.unbindComponents()}</li>
     * <li>{@link ComponentAdapter#setBinding(Binding) componentAdapter.setBinding(null)}</li>
     * </ol>
     *
     * @param binding The binding.
     * @see #bind(String, ComponentAdapter)
     */
    public void unbind(Binding binding) {
        Assert.notNull(binding, "binding");
        removeBinding(binding.getPropertyName());
        if (binding instanceof BindingImpl) {
            ((BindingImpl) binding).unbindProperty();
        }
        binding.getComponentAdapter().unbindComponents();
        binding.getComponentAdapter().setBinding(null);
    }

    /**
     * Binds a property in the value container to a Swing {@code JTextComponent} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JTextComponent component) {
        return bind(propertyName, new TextComponentAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JTextField} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JTextField component) {
        return bind(propertyName, new TextComponentAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JFormattedTextField} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JFormattedTextField component) {
        return bind(propertyName, new FormattedTextFieldAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JCheckBox} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JCheckBox component) {
        return bind(propertyName, new AbstractButtonAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JRadioButton} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(String propertyName, JRadioButton component) {
        return bind(propertyName, new AbstractButtonAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JList} component.
     *
     * @param propertyName     The property name.
     * @param component        The Swing component.
     * @param selectionIsValue if {@code true}, the current list selection provides the value,
     *                         if {@code false}, the list content is the value.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JList component, final boolean selectionIsValue) {
        if (selectionIsValue) {
            return bind(propertyName, new ListSelectionAdapter(component));
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    /**
     * Binds a property in the value container to a Swing {@code JSpinner} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JSpinner component) {
        return bind(propertyName, new SpinnerAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code JComboBox} component.
     *
     * @param propertyName The property name.
     * @param component    The Swing component.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final JComboBox component) {
        return bind(propertyName, new ComboBoxAdapter(component));
    }

    /**
     * Binds a property in the value container to a Swing {@code ButtonGroup}.
     *
     * @param propertyName The property name.
     * @param buttonGroup  The button group.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final ButtonGroup buttonGroup) {
        return bind(propertyName, buttonGroup,
                    ButtonGroupAdapter.createButtonToValueMap(buttonGroup, getPropertySet(), propertyName));
    }

    /**
     * Binds a property in the value container to a Swing {@code ButtonGroup}.
     *
     * @param propertyName The property name.
     * @param buttonGroup  The button group.
     * @param valueSet     The mapping from a button to the actual property value.
     * @return The resulting binding.
     */
    public Binding bind(final String propertyName, final ButtonGroup buttonGroup,
                        final Map<AbstractButton, Object> valueSet) {
        ComponentAdapter adapter = new ButtonGroupAdapter(buttonGroup, valueSet);
        return bind(propertyName, adapter);
    }

    /**
     * Shortcut for {@link com.bc.ceres.binding.PropertyContainer#addPropertyChangeListener(java.beans.PropertyChangeListener) getPropertyContainer().addPropertyChangeListener(l}.
     *
     * @param l The property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertySet.addPropertyChangeListener(l);
    }

    /**
     * Shortcut for {@link com.bc.ceres.binding.PropertyContainer#addPropertyChangeListener(String, java.beans.PropertyChangeListener) getPropertyContainer().addPropertyChangeListener(name, l}.
     *
     * @param name The property name.
     * @param l    The property change listener.
     */
    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        propertySet.addPropertyChangeListener(name, l);
    }

    /**
     * Shortcut for {@link com.bc.ceres.binding.PropertyContainer#removePropertyChangeListener(java.beans.PropertyChangeListener) getPropertyContainer().removePropertyChangeListener(l}.
     *
     * @param l The property change listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertySet.removePropertyChangeListener(l);
    }

    /**
     * Shortcut for {@link com.bc.ceres.binding.PropertyContainer#removePropertyChangeListener(String, java.beans.PropertyChangeListener) getPropertyContainer().removePropertyChangeListener(name, l}.
     *
     * @param name The property name.
     * @param l    The property change listener.
     */
    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        propertySet.removePropertyChangeListener(name, l);
    }

    /**
     * Permits component validation and property changes of the value container
     * triggered by the given component.
     *
     * @param component The component.
     * @see #preventPropertyChanges(javax.swing.JComponent)
     * @see #getPropertySet()
     * @since Ceres 0.10
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    public void permitPropertyChanges(JComponent component) {
        component.setVerifyInputWhenFocusTarget(true);
    }

    /**
     * Prevents component validation and property changes of the value container
     * triggered by the given component.
     * <p>
     * For example, if a text component loses keyboard focus because another component requests it,
     * its text value will be converted, validated and the value container's property will be changed.
     * For some focus targets, like a dialog's "Cancel" button, this is not desired.
     * <p>
     * By default, component validation and property changes are permitted for most Swing components.
     *
     * @param component The component.
     * @see #permitPropertyChanges(javax.swing.JComponent)
     * @see #getPropertySet()
     * @since Ceres 0.10
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    public void preventPropertyChanges(JComponent component) {
        component.setVerifyInputWhenFocusTarget(false);
    }

    private void configureComponents(Binding binding) {
        final String propertyName = binding.getPropertyName();
        final String toolTipTextStr = getToolTipText(propertyName);
        final boolean enabled = isEnabled(propertyName);
        final JComponent[] components = binding.getComponents();
        JComponent primaryComponent = components[0];
        configureComponent(primaryComponent, propertyName, toolTipTextStr, enabled);
        for (int i = 1; i < components.length; i++) {
            JComponent component = components[i];
            configureComponent(component, propertyName + "." + i, toolTipTextStr, enabled);
        }
    }

    private String getToolTipText(String propertyName) {
        final Property property = propertySet.getProperty(propertyName);
        StringBuilder toolTipText = new StringBuilder(32);
        final PropertyDescriptor propertyDescriptor = property.getDescriptor();
        if (propertyDescriptor.getDescription() != null) {
            toolTipText.append(propertyDescriptor.getDescription());
        }
        if (propertyDescriptor.getUnit() != null && !propertyDescriptor.getUnit().isEmpty()) {
            toolTipText.append(" (");
            toolTipText.append(propertyDescriptor.getUnit());
            toolTipText.append(")");
        }
        return toolTipText.toString();
    }

    private boolean isEnabled(String propertyName) {
        final Property property = propertySet.getProperty(propertyName);
        final PropertyDescriptor propertyDescriptor = property.getDescriptor();
        final Object enabled = propertyDescriptor.getAttribute("enabled");
        return enabled == null || !Boolean.FALSE.equals(enabled);
    }

    private static void configureComponent(JComponent component, String name, String toolTipText, boolean enabled) {
        if (component.getName() == null) {
            component.setName(name);
        }
        if (component.getToolTipText() == null && !toolTipText.isEmpty()) {
            component.setToolTipText(toolTipText);
        }
        component.setEnabled(enabled);
    }

    /**
     * Sets the <i>enabled</i> state of the components associated with {@code targetProperty}.
     * If the current value of {@code sourceProperty} equals {@code sourcePropertyValue} then
     * the enabled state will be set to the value of {@code enabled}, otherwise it is the negated value
     * of {@code enabled}. Neither the source property nor the target property need to have an active binding.
     *
     * @param targetPropertyName  The name of the target property.
     * @param targetState         The enabled state.
     * @param sourcePropertyName  The name of the source property.
     * @param sourcePropertyValue The value of the source property.
     */
    public Enablement bindEnabledState(final String targetPropertyName,
                                       final boolean targetState,
                                       final String sourcePropertyName,
                                       final Object sourcePropertyValue) {
        return bindEnabledState(targetPropertyName, targetState,
                                new EqualValuesCondition(sourcePropertyName, sourcePropertyValue));
    }

    /**
     * Sets the <i>enabled</i> state of the components associated with {@code targetProperty} to {@code targetState}
     * if a certain {@code condition} is met.
     *
     * @param targetPropertyName The name of the target property.
     * @param targetState        The target enabled state.
     * @param condition          The condition.
     */
    public Enablement bindEnabledState(final String targetPropertyName,
                                       final boolean targetState,
                                       final Enablement.Condition condition) {
        final EnablementImpl enablement = new EnablementImpl(targetPropertyName, targetState, condition);
        final Binding binding = getBinding(targetPropertyName);
        if (binding != null) {
            activateEnablement(enablement);
        }
        enablements.put(targetPropertyName, enablement);
        return enablement;
    }

    public void setComponentsEnabled(String propertyName, boolean enabled) {
        final JComponent[] components = getBinding(propertyName).getComponents();
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
    }

    private void setComponentsEnabled(final String targetPropertyName,
                                      final boolean targetState,
                                      Enablement.Condition condition) {
        boolean conditionIsTrue = condition.evaluate(this);
        final JComponent[] components = getBinding(targetPropertyName).getComponents();
        for (JComponent component : components) {
            component.setEnabled(conditionIsTrue ? targetState : !targetState);
        }
    }

    private void addBinding(BindingImpl binding) {
        bindings.put(binding.getPropertyName(), binding);
        EnablementImpl enablement = enablements.get(binding.getPropertyName());
        if (enablement != null && !enablement.isActive()) {
            activateEnablement(enablement);
        }
    }

    private void removeBinding(String propertyName) {
        bindings.remove(propertyName);
        EnablementImpl enablement = enablements.get(propertyName);
        if (enablement != null) {
            deactivateEnablement(enablement);
        }
    }

    private void activateEnablement(EnablementImpl enablement) {
        enablement.setActive(true);
        enablement.apply();
        enablement.getCondition().install(this, enablement);
    }

    private void deactivateEnablement(EnablementImpl enablement) {
        enablement.setActive(false);
        enablement.getCondition().uninstall(this, enablement);
    }

    public static class VerbousProblemHandler implements BindingProblemListener {
        @Override
        public void problemReported(BindingProblem newProblem, BindingProblem oldProblem) {
            final Binding binding = newProblem.getBinding();
            final ComponentAdapter adapter = binding.getComponentAdapter();
            final JComponent component = adapter.getComponents()[0];
            final Window window = SwingUtilities.windowForComponent(component);
            JOptionPane.showMessageDialog(window,
                                          newProblem.getCause().getMessage(),
                                          "Invalid Input",
                                          JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public void problemCleared(BindingProblem oldProblem) {
        }
    }

    public static class SilentProblemHandler implements BindingProblemListener {
        @Override
        public void problemReported(BindingProblem newProblem, BindingProblem oldProblem) {
            newProblem.getBinding().adjustComponents();
        }

        @Override
        public void problemCleared(BindingProblem oldProblem) {
        }
    }

    private class EnablementImpl implements Enablement {

        private final String targetPropertyName;
        private final boolean targetState;
        private final Condition condition;
        private boolean active;

        private EnablementImpl(String targetPropertyName,
                               boolean targetState,
                               Condition condition) {
            this.targetPropertyName = targetPropertyName;
            this.targetState = targetState;
            this.condition = condition;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public Condition getCondition() {
            return condition;
        }

        @Override
        public final void propertyChange(PropertyChangeEvent evt) {
            apply();
        }

        public void apply() {
            setComponentsEnabled(targetPropertyName,
                                 targetState,
                                 condition);
        }
    }

    private static class EqualValuesCondition extends Enablement.Condition {
        private final String sourcePropertyName;
        private final Object sourcePropertyValue;

        private EqualValuesCondition(String sourcePropertyName, Object sourcePropertyValue) {
            this.sourcePropertyName = sourcePropertyName;
            this.sourcePropertyValue = sourcePropertyValue;
        }

        @Override
        public boolean evaluate(BindingContext bindingContext) {
            Object propertyValue = bindingContext.getPropertySet().getValue(sourcePropertyName);
            return propertyValue == sourcePropertyValue
                    || (propertyValue != null && propertyValue.equals(sourcePropertyValue));
        }

        @Override
        public void install(BindingContext bindingContext, Enablement enablement) {
            bindingContext.addPropertyChangeListener(sourcePropertyName, enablement);
        }

        @Override
        public void uninstall(BindingContext bindingContext, Enablement enablement) {
            bindingContext.removePropertyChangeListener(sourcePropertyName, enablement);
        }
    }
}
