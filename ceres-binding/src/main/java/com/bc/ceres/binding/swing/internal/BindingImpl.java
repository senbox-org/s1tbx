package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

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

    public final String getPropertyName() {
        return name;
    }

    public Object getPropertyValue() {
        return context.getValueContainer().getValue(getPropertyName());
    }

    public void setPropertyValue(Object value) {
        try {
            context.getValueContainer().setValue(getPropertyName(), value);
        } catch (Exception e) {
            componentAdapter.handleError(e);
        }
    }

    public final boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    public final void adjustComponents() {
        if (!adjustingComponents) {
            Exception error = null;
            try {
                adjustingComponents = true;
                componentAdapter.adjustComponents();
            } catch (Exception e) {
                error = e;
            } finally {
                adjustingComponents = false;
            }
            if (error != null) {
                componentAdapter.handleError(error);
            }
        }
    }

    /**
     * Gets the primary Swing component used for the binding, e.g. a {@link javax.swing.JTextField}.
     *
     * @return the primary Swing component.
     * @see #getComponents()
     */
    public JComponent getPrimaryComponent() {
        return componentAdapter.getComponents()[0];
    }

    /**
     * Gets the secondary Swing components attached to the binding, e.g. some {@link javax.swing.JLabel}s.
     *
     * @return the secondary Swing components. The returned array may be empty.
     * @see #addComponent(javax.swing.JComponent)
     */
    public JComponent[] getComponents() {
        if (secondaryComponents == null) {
            return componentAdapter.getComponents();
        } else {
            final List<JComponent> list = Arrays.asList(componentAdapter.getComponents());
            list.addAll(secondaryComponents);
            return list.toArray(new JComponent[list.size()]);
        }
    }

    /**
     * Attaches a secondary Swing component to this binding.
     *
     * @param component The secondary component.
     * @see #removeComponent(javax.swing.JComponent)
     */
    public void addComponent(JComponent component) {
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
     * @see #addComponent(javax.swing.JComponent)
     */
    public void removeComponent(JComponent component) {
        if (secondaryComponents != null) {
            secondaryComponents.remove(component);
        }
    }

    /**
     * Sets the <i>enabled</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    public void setComponentsEnabledState(boolean state) {
        for (JComponent component : getComponents()) {
            component.setEnabled(state);
        }
    }

    /**
     * Sets the <i>visible</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    public void setComponentsVisibleState(boolean state) {
        for (JComponent component : getComponents()) {
            component.setVisible(state);
        }
    }
}
