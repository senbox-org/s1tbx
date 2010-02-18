package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.ControlFactory;

import javax.swing.JComponent;

/**
 * A control factory that only creates it's control when requested.
 *
 * @author Keith Donald
 */
public abstract class AbstractControlFactory implements ControlFactory {

    private boolean singleton;

    private JComponent control;

    protected AbstractControlFactory() {
        singleton = true;
    }

    protected final synchronized boolean isSingleton() {
        return singleton;
    }

    protected final synchronized void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public final synchronized JComponent getControl() {
        if (isSingleton()) {
            if (control == null) {
                this.control = createControl();
            }
            return control;
        }
        return createControl();
    }

    public final synchronized boolean isControlCreated() {
        return isSingleton() && control != null;
    }

    protected synchronized void createControlIfNecessary() {
        if (isSingleton() && control == null) {
            getControl();
        }
    }

    protected abstract JComponent createControl();
}