package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A bi-directional binding between a Swing component and a property in a value container.
 * <p/>
 * Clients are asked to add an appropriate change listener to their Swing component
 * in order to set the corresponding property value in the {@link ValueContainer}.
 * <p/>
 * Whenever the value of the named property changes in the {@link ValueContainer},
 * the template method {@link #adjustComponent()} will be called.
 * Clients implement the {@link #adjustComponentImpl()} method
 * in order to adjust their Swing component according the new property value.
 * Note that {@code adjustComponentImpl()} will <i>not</i> be recursively called again, if the
 * the property value changes within the {@code adjustComponentImpl()} code. In this case,
 * the value of {@link #isAdjustingComponent() adjustingComponent} will be {@code true}.
 * <p/>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public abstract class Binding {

    private final BindingContext context;
    private final String propertyName;
    private boolean adjustingComponent;

    public Binding(BindingContext context, String propertyName) {
        this.context = context;
        this.propertyName = propertyName;
        context.getValueContainer().addPropertyChangeListener(propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adjustComponent();
            }
        });
    }

    public final BindingContext getContext() {
        return context;
    }

    public final ValueContainer getValueContainer() {
        return context.getValueContainer();
    }

    public final String getPropertyName() {
        return propertyName;
    }

    public Object getPropertyValue() {
        return context.getValueContainer().getValue(getPropertyName());
    }

    public void setPropertyValue(Object propertyValue) {
        try {
            context.getValueContainer().setValue(getPropertyName(), propertyValue);
        } catch (Exception e) {
            handleError(e);
        }
    }

    public final boolean isAdjustingComponent() {
        return adjustingComponent;
    }

    public void adjustComponent() {
        if (!adjustingComponent) {
            try {
                adjustingComponent = true;
                adjustComponentImpl();
            } finally {
                adjustingComponent = false;
            }
        }
    }

    public void handleError(Exception e) {
        context.handleError(getPrimaryComponent(), e);
    }

    protected abstract void adjustComponentImpl();

    protected abstract JComponent getPrimaryComponent();

}
