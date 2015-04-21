package com.bc.ceres.swing.binding;

import java.beans.PropertyChangeListener;

/**
 * An enablement.
 *
 * @author Thomas
 * @author Sabine
 * @author Olaf
 * @author Norman
 * @since Ceres 0.13
 */
public interface Enablement extends PropertyChangeListener {
    void apply();

    /**
     * A condition used to determine the new {@code enabled} state of components.
     *
     * @see BindingContext#bindEnabledState(String, boolean, com.bc.ceres.swing.binding.Enablement.Condition)
     */
    abstract class Condition {
        /**
         * @param bindingContext The binding context.
         * @return {@code true}, if the condition is met.
         */
        public abstract boolean evaluate(BindingContext bindingContext);

        /**
         * Adds the given property change enablement to any dependent bindings or components in the binding context.
         * The default implementation does nothing.
         *
         * @param bindingContext The binding context.
         * @param enablement     A property change enablement.
         */
        public void install(BindingContext bindingContext, Enablement enablement) {
        }

        /**
         * Notifies this condition that the enablement has been uninstalled.
         * The default implementation does nothing.
         * <p>
         * The default implementation does nothing.
         *
         * @param bindingContext The binding context.
         * @param enablement     A property change enablement.
         */
        public void uninstall(BindingContext bindingContext, Enablement enablement) {
        }
    }
}
