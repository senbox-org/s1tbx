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

import com.bc.ceres.core.Assert;

import javax.swing.JComponent;


/**
 * A component adapter provides the GUI components used to edit a bound property value.
 * <p>
 * Clients may derive their own component adapters by implementing this abstract class.
 * <p>
 * The actual binding is established by calling {@link BindingContext#bind(String, ComponentAdapter)}.
 * The returned binding may be undone later by calling {@link BindingContext#unbind(Binding)}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.6
 */
public abstract class ComponentAdapter {
    private Binding binding;

    /**
     * Gets the binding which is using this adapter.
     *
     * @return The binding.
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * Sets the binding which is using this adapter.
     * <p>
     * Clients shall never call this method directly, it is called by the framework.
     *
     * @param binding The binding.
     *
     * @throws IllegalStateException if the binding has already been set.
     */
    public final void setBinding(Binding binding) {
        Assert.state(binding == null || this.binding == null, "binding already set");
        this.binding = binding;
    }

    /**
     * Gets the components participating in the binding.
     *
     * @return The array of components. Must not be empty.
     */
    public abstract JComponent[] getComponents();

    /**
     * Called by the framework in order to bind the GUI components to the bound property.
     * <p>
     * The frameworks called this method immediately after
     * {@link #setBinding(Binding)} has been called.
     * <p>
     * Most implementations will register a change listener in the editor component which
     * convert the input value and set the bound property by calling
     * {@link Binding#setPropertyValue(Object) getBinding().setPropertyValue(value)}.
     *
     * @see #adjustComponents()
     * @see #unbindComponents()
     */
    public abstract void bindComponents();

    /**
     * Called by the framework in order to unbind the GUI components from the bound property.
     * <p>
     * Most implementations will deregister any registered change listeners from the editor component.
     *
     * @see #bindComponents()
     * @see #adjustComponents()
     */
    public abstract void unbindComponents();

    /**
     * Called by the framework either
     * <ol>
     * <li>if a binding is established by calling {@link BindingContext#bind(String, ComponentAdapter)},</li>
     * <li>if {@link BindingContext#adjustComponents()} is called, or</li>
     * <li>if a property-change event occurs in the associated {@code PropertyContainer} of the {@link BindingContext}.</li>
     * </ol>
     * <p>
     * Most implementations adjusts the editor component with the value retrieved by
     * {@link Binding#getPropertyValue() getBinding().getPropertyValue()}. Note that changes to the UI component shall be made on the EDT.
     * Consider using {@link javax.swing.SwingUtilities#invokeLater(Runnable) SwingUtilities.invokeLater()}.
     *
     * @see #bindComponents()
     * @see #unbindComponents()
     */
    public abstract void adjustComponents();
}
