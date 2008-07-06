package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public final class BindingImpl implements Binding,PropertyChangeListener {

    private ComponentAdapter componentAdapter;
    private final BindingContext context;
    private final String name;
    private List<JComponent> secondaryComponents;
    private boolean adjustingComponents;
    private PropertyChangeListener pcl;

    public BindingImpl(BindingContext context, String name, ComponentAdapter componentAdapter) {
        this.context = context;
        this.name = name;
        this.componentAdapter = componentAdapter;
    }

    public void bindProperty() {
        context.addPropertyChangeListener(name, this);
    }

    public void unbindProperty() {
        context.removePropertyChangeListener(name, this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        adjustComponents();
    }

    public ComponentAdapter getComponentAdapter() {
        return componentAdapter;
    }

    public final BindingContext getContext() {
        return context;
    }

    public final String getName() {
        return name;
    }

    public Object getValue() {
        return context.getValueContainer().getValue(getName());
    }

    public void setValue(Object value) {
        try {
            context.getValueContainer().setValue(getName(), value);
        } catch (Exception e) {
            handleError(e);
        }
    }

    public final boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    public final void adjustComponents() {
        if (!adjustingComponents) {
            try {
                adjustingComponents = true;
                componentAdapter.adjustComponents();
            } catch (Exception e) {
                handleError(e);
            } finally {
                adjustingComponents = false;
            }
        }
    }

    /**
     * Handles an error occured while transferring data from the bound property to the
     * Swing component or vice versa.
     * Delegates the call to the binding context using the binding's primary Swing component.
     *
     * @param exception The error.
     */
    public void handleError(Exception exception) {
        context.handleError(exception, getPrimaryComponent());
    }

    /**
     * Gets the primary Swing component used for the binding, e.g. a {@link javax.swing.JTextField}.
     *
     * @return the primary Swing component.
     * @see #getSecondaryComponents()
     */
    public JComponent getPrimaryComponent() {
        return componentAdapter.getPrimaryComponent();
    }

    /**
     * Gets the secondary Swing components attached to the binding, e.g. some {@link javax.swing.JLabel}s.
     *
     * @return the secondary Swing components. The returned array may be empty.
     * @see #getPrimaryComponent()
     * @see #attachSecondaryComponent(javax.swing.JComponent)
     */
    public JComponent[] getSecondaryComponents() {
        if (secondaryComponents == null) {
            return new JComponent[0];
        }
        return secondaryComponents.toArray(new JComponent[secondaryComponents.size()]);
    }

    /**
     * Attaches a secondary Swing component to this binding.
     *
     * @param component The secondary component.
     * @see #detachSecondaryComponent(javax.swing.JComponent)
     */
    public void attachSecondaryComponent(JComponent component) {
        synchronized (this) {
            if (secondaryComponents == null) {
                secondaryComponents = new ArrayList<JComponent>(3);
            }
            if (!secondaryComponents.contains(component)) {
                secondaryComponents.add(component);
            }
        }
    }

    /**
     * Detaches a secondary Swing component from this binding.
     *
     * @param component The secondary component.
     * @see #attachSecondaryComponent(javax.swing.JComponent)
     */
    public void detachSecondaryComponent(JComponent component) {
        if (secondaryComponents != null) {
            secondaryComponents.remove(component);
        }
    }

    /**
     * Sets the <i>enabled</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    public void setComponentsEnabledState(boolean state) {
        getPrimaryComponent().setEnabled(state);
        for (JComponent component : getSecondaryComponents()) {
            component.setEnabled(state);
        }
    }

    /**
     * Sets the <i>visible</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    public void setComponentsVisibleState(boolean state) {
        getPrimaryComponent().setVisible(state);
        for (JComponent component : getSecondaryComponents()) {
            component.setVisible(state);
        }
    }
}
