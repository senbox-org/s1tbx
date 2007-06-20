package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.ControlFactory;

import javax.swing.*;

/**
 * A control factory that only creates it's control when requested.
 *
 * @author Keith Donald
 */
public abstract class AbstractControlFactory implements ControlFactory {

    private boolean singleton = true;

    private JComponent control;

    protected final boolean isSingleton() {
        return singleton;
    }

    protected final void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public final JComponent getControl() {
        if (isSingleton()) {
            if (control == null) {
                this.control = createControl();
            }
            return control;
        }

        return createControl();
    }

    public final boolean isControlCreated() {
        if (isSingleton()) {
            return control != null;
        }

        return false;
    }

    protected void createControlIfNecessary() {
        if (isSingleton() && control == null) {
            getControl();
        }
    }

    protected abstract JComponent createControl();
}