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

package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.BindingProblem;
import com.bc.ceres.swing.binding.BindingProblemListener;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public final class BindingImpl implements Binding, PropertyChangeListener {

    private final BindingContext context;
    private final String name;
    private final ComponentAdapter componentAdapter;

    private List<JComponent> secondaryComponents;
    private boolean adjustingComponents;
    private BindingProblem problem;

    public BindingImpl(BindingContext context, String name, ComponentAdapter componentAdapter) {
        Assert.notNull(context, "context");
        Assert.notNull(name, "name");
        Assert.notNull(componentAdapter, "componentAdapter");
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
    public BindingProblem getProblem() {
        return problem;
    }

    @Override
    public void clearProblem() {
        BindingProblem oldProblem = this.problem;
        if (oldProblem != null) {
            this.problem = null;
            fireProblemCleared(oldProblem);
        }
    }

    @Override
    public BindingProblem reportProblem(BindingException cause) {
        Assert.notNull(cause, "cause");
        final BindingProblem newProblem = new BindingProblemImpl(this, cause);
        BindingProblem oldProblem = this.problem;
        if (oldProblem == null || !newProblem.equals(oldProblem)) {
            this.problem = newProblem;
            fireProblemReported(newProblem, oldProblem);
        }
        return newProblem;
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
        return context.getPropertySet().getValue(getPropertyName());
    }

    @Override
    public void setPropertyValue(Object value) {
        try {
            context.getPropertySet().setValue(getPropertyName(), value);
            clearProblem();
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof BindingException) {
                reportProblem((BindingException) e.getCause());
            }
        }
    }

    @Override
    public boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    @Override
    public void adjustComponents() {
        if (!adjustingComponents) {
            // Make sure, componentAdapter is not already unbound
            if (componentAdapter.getBinding() != null) {
                try {
                    adjustingComponents = true;
                    componentAdapter.adjustComponents();
                    // Now property is in sync with UI
                    clearProblem();
                } finally {
                    adjustingComponents = false;
                }
            }
        }
    }

    /**
     * Gets all Swing components this binding is associated with.
     *
     * @return The component array.
     * @see #addComponent(javax.swing.JComponent)
     */
    @Override
    public JComponent[] getComponents() {
        if (secondaryComponents == null) {
            return componentAdapter.getComponents();
        } else {
            JComponent[] primaryComponents = componentAdapter.getComponents();
            JComponent[] allComponents = new JComponent[primaryComponents.length + secondaryComponents.size()];
            System.arraycopy(primaryComponents, 0, allComponents, 0, primaryComponents.length);
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
     * @see #addComponent(javax.swing.JComponent)
     */
    @Override
    public void removeComponent(JComponent component) {
        if (secondaryComponents != null) {
            secondaryComponents.remove(component);
        }
    }


    void fireProblemReported(BindingProblem newProblem, BindingProblem oldProblem) {
        for (BindingProblemListener listener : context.getProblemListeners()) {
            listener.problemReported(newProblem, oldProblem);
        }
    }

    void fireProblemCleared(BindingProblem oldProblem) {
        for (BindingProblemListener listener : context.getProblemListeners()) {
            listener.problemCleared(oldProblem);
        }
    }


}
