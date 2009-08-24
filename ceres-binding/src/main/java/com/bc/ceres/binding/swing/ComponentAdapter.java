package com.bc.ceres.binding.swing;

import javax.swing.JComponent;


/**
 * A component adapter provides the GUI components used to edit a bound property value.
 * <p/>
 * Clients may derive their own component adapters by implementing this abstract class.
 * <p/>
 * The actual binding is established by calling {@link BindingContext#bind(String, ComponentAdapter)}.
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
     * <p/>
     * Clients shall never call this method directly, it is called by the framework.
     *
     * @param binding The binding.
     *
     * @throws IllegalStateException if the binding has already been set.
     */
    public final void setBinding(Binding binding) {
        if (this.binding != null) {
            throw new IllegalStateException("this.binding != null");
        }
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
     * <p/>
     * The frameworks called this method immediately after
     * {@link #setBinding(Binding)} has been called.
     * <p/>
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
     * <p/>
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
     * <li>if a property-change event occurs in the associated {@code ValueContainer} of the {@link BindingContext}.</li>
     * </ol>
     * <p/>
     * Most implementations adjusts the editor component with the value retrieved by
     * {@link Binding#getPropertyValue() getBinding().getPropertyValue()}.
     *
     * @see #bindComponents()
     * @see #unbindComponents()
     */
    public abstract void adjustComponents();

    /**
     * Handles an error occured while transferring data from the bound property to the
     * Swing component or vice versa.
     * Delegates the call to {@link BindingContext#handleError(Exception, javax.swing.JComponent)}  handleError()}
     * of the binding context using this adapters's first component:
     * <pre>
     * getBinding().getContext().handleError(exception, getComponents()[0]);
     * </pre>
     *
     * @param error The error.
     *
     * @see #getComponents()
     * @deprecated Since 0.10, for error handling use {@link BindingContext#addProblemListener(javax.swing.event.ChangeListener)}
     *             and {@link BindingContext#getProblems()} instead
     */
    @Deprecated
    public void handleError(Exception error) {
        getBinding().getContext().handleError(error, getComponents()[0]);
    }
}
