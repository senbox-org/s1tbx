package com.bc.ceres.binding.swing;

import javax.swing.JComponent;

/**
 * A bi-directional binding between one or more Swing components and a property in a value container.
 * <p/>
 * Classes derived from {@code Binding} are asked to add an appropriate change listener to their Swing component
 * which will transfer the value from the component into the associated {@link com.bc.ceres.binding.ValueContainer}
 * using the {@link #setValue(Object)} method.
 * <p/>
 * Whenever the value of the named property changes in the {@link com.bc.ceres.binding.ValueContainer},
 * the template method {@link #adjustComponents()} will be called.
 * Clients implement the {@link ComponentAdapter#adjustComponents()} method
 * in order to adjust their Swing component according the new property value.
 * Note that {@code adjustComponents()} will <i>not</i> be recursively called again, if the
 * the property value changes while {@code adjustComponents()} is executed. In this case,
 * the value of the {@link #isAdjustingComponents() adjustingComponent} property will be {@code true}.
 * <p/>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface Binding {
    /**
     * @return The binding context.
     */
    BindingContext getContext();

    /**
     * @return The component adapter.
     */
    ComponentAdapter getComponentAdapter();

    /**
     * @return The name of the bound property.
     */
    String getName();

    /**
     * @return The value of the bound property.
     */
    Object getValue();

    /**
     * Sets the value of the bound property.
     * This may trigger a property change event in the associated {@code ValueContainer}.
     *
     * @param value The new value of the bound property.
     */
    void setValue(Object value);

    /**
     * Adjusts the bound Swing components in reaction to a property change event in the
     * associated {@code ValueContainer}. The method calls {@link ComponentAdapter#adjustComponents()}
     * only if this binding is not already adjusting the bound Swing components. It will also
     * handle any kind of exceptions thrown in the {@code doAdjustComponents()} method
     * by calling {@link #handleError(Exception)}.
     */
    public void adjustComponents();

    /**
     * Tests if this binding is currently adjusting the bound Swing components.
     *
     * @return {@code true} if so.
     */
    boolean isAdjustingComponents();

/**
     * Handles an error occured while transferring data from the bound property to the
     * Swing component or vice versa.
     * Delegates the call to the binding context using the binding's primary Swing component.
     *
     * @param exception The error.
     */
    public void handleError(Exception exception);

    /**
     * Gets the primary Swing component used for the binding, e.g. a {@link javax.swing.JTextField}.
     *
     * @return the primary Swing component.
     * @see #getSecondaryComponents()
     */
    JComponent getPrimaryComponent();

    /**
     * Gets the secondary Swing components attached to the binding, e.g. some {@link javax.swing.JLabel}s.
     *
     * @return the secondary Swing components. The returned array may be empty.
     * @see #getPrimaryComponent()
     * @see #attachSecondaryComponent(javax.swing.JComponent)
     */
    JComponent[] getSecondaryComponents();

    /**
     * Attaches a secondary Swing component to this binding.
     *
     * @param component The secondary component.
     * @see #detachSecondaryComponent(javax.swing.JComponent)
     */
    void attachSecondaryComponent(JComponent component);

    /**
     * Detaches a secondary Swing component from this binding.
     *
     * @param component The secondary component.
     * @see #attachSecondaryComponent(javax.swing.JComponent)
     */
    void detachSecondaryComponent(JComponent component);

    /**
     * Sets the <i>enabled</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    void setComponentsEnabledState(boolean state);

    /**
     * Sets the <i>visible</i> state of the primary Swing component and all secondary components, if any.
     * @param state The state to be propagated.
     */
    void setComponentsVisibleState(boolean state);
}
