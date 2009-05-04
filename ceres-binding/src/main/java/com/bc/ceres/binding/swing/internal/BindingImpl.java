package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BindingImpl implements Binding, PropertyChangeListener {

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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        adjustComponents();
    }

    @Override
    public ComponentAdapter getComponentAdapter() {
        return componentAdapter;
    }

    @Override
    public final BindingContext getContext() {
        return context;
    }

    @Override
    public final String getPropertyName() {
        return name;
    }

    @Override
    public Object getPropertyValue() {
        return context.getValueContainer().getValue(getPropertyName());
    }

    @Override
    public void setPropertyValue(Object value) {
        try {
            context.getValueContainer().setValue(getPropertyName(), value);
        } catch (Exception e) {
            componentAdapter.handleError(e);
        }
    }

    @Override
    public boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    @Override
    public void adjustComponents() {
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
     * Gets the secondary Swing components attached to the binding, e.g. some {@link javax.swing.JLabel}s.
     *
     * @return the secondary Swing components. The returned array may be empty.
     *
     * @see #addComponent(javax.swing.JComponent)
     */
    @Override
    public JComponent[] getComponents() {
        if (secondaryComponents == null) {
            return componentAdapter.getComponents();
        } else {
            JComponent[] primaryComponents = componentAdapter.getComponents();
            JComponent[] allComponents = new JComponent[primaryComponents.length + secondaryComponents.size()];
            for (int i = 0; i < primaryComponents.length; i++) {
                allComponents[i] = primaryComponents[i];
            }
            int j = primaryComponents.length;
            for (JComponent component : secondaryComponents) {
                allComponents[j] = component;
                j++;
            }
            return allComponents;
        }
    }

    /**
     * Attaches a secondary Swing component to this binding.
     *
     * @param component The secondary component.
     *
     * @see #removeComponent(javax.swing.JComponent)
     */
    @Override
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
     *
     * @see #addComponent(javax.swing.JComponent)
     */
    @Override
    public void removeComponent(JComponent component) {
        if (secondaryComponents != null) {
            secondaryComponents.remove(component);
        }
    }
}
