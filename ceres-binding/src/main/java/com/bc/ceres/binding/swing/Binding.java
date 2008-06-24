package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A bi-directional binding between one or more Swing components and a property in a value container.
 * <p/>
 * Classes derived from {@code Binding} are asked to add an appropriate change listener to their Swing component
 * which will transfer the value from the component into the associated {@link ValueContainer}
 * using the {@link #setValue(Object)} method.
 * <p/>
 * Whenever the value of the named property changes in the {@link ValueContainer},
 * the template method {@link #adjustComponents()} will be called.
 * Clients implement the {@link #doAdjustComponents()} method
 * in order to adjust their Swing component according the new property value.
 * Note that {@code adjustComponentImpl()} will <i>not</i> be recursively called again, if the
 * the property value changes within the {@code adjustComponentImpl()} code. In this case,
 * the value of {@link #isAdjustingComponents() adjustingComponent} will be {@code true}.
 * <p/>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public abstract class Binding {

    private final BindingContext context;
    private final String name;
    private List<JComponent> secondaryComponents;
    private boolean adjustingComponents;

    /**
     * Constructs a binding for the given binding context and the name of the property to be bound.
     *
     * @param context The binding context.
     * @param name    The name of the property to be bound.
     */
    public Binding(BindingContext context, String name) {
        this.context = context;
        this.name = name;
        context.getValueContainer().addPropertyChangeListener(name, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adjustComponents();
            }
        });
    }

    /**
     * @return The binding context.
     */
    public final BindingContext getContext() {
        return context;
    }

    /**
     * @return The associated value container.
     */
    public final ValueContainer getValueContainer() {
        return context.getValueContainer();
    }

    /**
     * @return The name of the bound property.
     */
    public final String getName() {
        return name;
    }

    /**
     * @return The value of the bound property.
     */
    public Object getValue() {
        return context.getValueContainer().getValue(getName());
    }

    /**
     * Sets the value of the bound property.
     * This may trigger a property change event in the associated {@code ValueContainer}.
     *
     * @param value The new value of the bound property.
     */
    public void setValue(Object value) {
        try {
            context.getValueContainer().setValue(getName(), value);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Tests if this binding is currently adjusting the bound Swing components.
     *
     * @return {@code true} if so.
     */
    public final boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    /**
     * Adjusts the bound Swing components in reaction to a property change event in the
     * associated {@code ValueContainer}. The method calls {@link #doAdjustComponents()}
     * only if this binding is not already adjusting the bound Swing components. It will also
     * handle any kind of exceptions thrown in the {@code doAdjustComponents()} method
     * by calling {@link #handleError(Exception)}.
     */
    public final void adjustComponents() {
        if (!adjustingComponents) {
            try {
                adjustingComponents = true;
                doAdjustComponents();
            } catch (Exception e) {
                handleError(e);
            } finally {
                adjustingComponents = false;
            }
        }
    }

    /**
     * Transfers the value of the bound property into the Swing component(s).
     */
    protected abstract void doAdjustComponents();

    /**
     * Handles an error occured while transferring data from the bound property to the
     * Swing component or vice versa.
     * Delegates the call to the binding context using the binding's primary Swing component.
     *
     * @param exception The error.
     */
    protected void handleError(Exception exception) {
        context.handleError(exception, getPrimaryComponent());
    }

    /**
     * Gets the primary Swing component used for the binding, e.g. a {@link javax.swing.JTextField}.
     *
     * @return the primary Swing component.
     * @see #getSecondaryComponents()
     */
    public abstract JComponent getPrimaryComponent();

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
                secondaryComponents = new ArrayList<JComponent>();
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
