package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.BindingException;

import javax.swing.JComponent;

/**
 * A bi-directional binding between one or more Swing components and a property in a value container.
 * <p/>
 * Classes derived from {@code Binding} are asked to add an appropriate change listener to their Swing component
 * which will transfer the value from the component into the associated {@link com.bc.ceres.binding.ValueContainer}
 * using the {@link #setPropertyValue(Object)} method.
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
 * @since Ceres 0.6
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
    String getPropertyName();

    /**
     * @return The value of the bound property.
     */
    Object getPropertyValue();

    /**
     * Sets the value of the bound property.
     * This may trigger a property change event in the associated {@code ValueContainer}.
     * Whether or not setting the value was successful can be retrieved by {@link #getProblem()}.
     *
     * @param value The new value of the bound property.
     */
    void setPropertyValue(Object value);

    /**
     * Gets the current problem. If the GUI is in sync with the associated value model,
     * the method will return {@code null}.
     *
     * @return The current problem, or {@code null}.
     *
     * @since Ceres 0.10
     */
    BindingProblem getProblem();

    /**
     * Clears the current problem.
     *
     * @since Ceres 0.10
     */
    void clearProblem();

    /**
     * Reports a problem.
     * The method sets the current problem and will cause the {@link #getContext() context}
     * to fire a problem-occurred event.
     *
     * @param cause The cause.
     *
     * @return The resulting problem.
     *
     * @since Ceres 0.10
     */
    BindingProblem reportProblem(BindingException cause);

    /**
     * Adjusts the Swing components in reaction to a bound property change event in the
     * associated {@link com.bc.ceres.binding.ValueContainer ValueContainer}.
     * <p/>
     * The method delegates to {@link ComponentAdapter#adjustComponents()},
     * but only if this binding is not already adjusting its GUI components.
     * <p/>
     * After calling this method the UI is in sync with the value model, so that {@link #getProblem()}
     * will return {@code null}.
     *
     * @see #isAdjustingComponents()
     */
    void adjustComponents();

    /**
     * Tests if this binding is currently adjusting the bound Swing components.
     *
     * @return {@code true} if so.
     *
     * @see #adjustComponents()
     */
    boolean isAdjustingComponents();

    /**
     * Gets all components participating in this binding: the one returned by the associated {@link ComponentAdapter}
     * plus the ones added to this binding using the {@link #addComponent(javax.swing.JComponent)} method.
     *
     * @return All components participating in this binding.
     */
    JComponent[] getComponents();

    /**
     * Adds a secondary Swing component to this binding, e.g. a {@link javax.swing.JLabel}.
     *
     * @param component The secondary component.
     *
     * @see #removeComponent(javax.swing.JComponent)
     */
    void addComponent(JComponent component);

    /**
     * Removes a secondary Swing component from this binding.
     *
     * @param component The secondary component.
     *
     * @see #addComponent(javax.swing.JComponent)
     */
    void removeComponent(JComponent component);
}
