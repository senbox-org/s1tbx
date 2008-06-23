package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * A bi-directional binding between obne or more Swing components and a property in a value container.
 * <p/>
 * Clients are asked to add an appropriate change listener to their Swing component
 * in order to set the corresponding property value in the {@link ValueContainer}.
 * <p/>
 * Whenever the value of the named property changes in the {@link ValueContainer},
 * the template method {@link #adjustComponents()} will be called.
 * Clients implement the {@link #adjustComponentsImpl()} method
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

    public Binding(BindingContext context, String name) {
        this.context = context;
        this.name = name;
        context.getValueContainer().addPropertyChangeListener(name, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adjustComponents();
            }
        });
    }

    public final BindingContext getContext() {
        return context;
    }

    public final ValueContainer getValueContainer() {
        return context.getValueContainer();
    }

    public final String getName() {
        return name;
    }

    public Object getValue() {
        return context.getValueContainer().getValue(getName());
    }

    public void setValue(Object propertyValue) {
        try {
            context.getValueContainer().setValue(getName(), propertyValue);
        } catch (Exception e) {
            handleError(e);
        }
    }

    public final boolean isAdjustingComponents() {
        return adjustingComponents;
    }

    public void adjustComponents() {
        if (!adjustingComponents) {
            try {
                adjustingComponents = true;
                adjustComponentsImpl();
            } catch (Exception e) {
                handleError(e);
            } finally {
                adjustingComponents = false;
            }
        }
    }

    public void handleError(Exception exception) {
        context.handleError(exception, getPrimaryComponent());
    }

    protected abstract void adjustComponentsImpl();

    public void enableComponents() {
        setComponentsEnabled(true);
    }

    public void disableComponents() {
        setComponentsEnabled(false);
    }

    private synchronized void setComponentsEnabled(boolean enabled) {
        getPrimaryComponent().setEnabled(enabled);
        if (secondaryComponents != null) {
            for (JComponent component : secondaryComponents) {
                component.setEnabled(enabled);
            }
        }
    }

    public abstract JComponent getPrimaryComponent();

    public JComponent[] getSecondaryComponents() {
        if (secondaryComponents == null) {
            return new JComponent[0];
        }
        return secondaryComponents.toArray(new JComponent[secondaryComponents.size()]);
    }

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

    public void detachSecondaryComponent(JComponent component) {
        if (secondaryComponents != null) {
            secondaryComponents.remove(component);
        }
    }
}
