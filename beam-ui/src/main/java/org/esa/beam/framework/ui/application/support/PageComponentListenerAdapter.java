package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentListener;

public class PageComponentListenerAdapter implements PageComponentListener {
    private PageComponent activeComponent;

    /**
     * @return the active component
     */
    public PageComponent getActiveComponent() {
        return activeComponent;
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentOpened(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentClosed(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentShown(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentHidden(PageComponent component) {
    }

    /**
     * The default implementation sets the active component.
     * <p>Subclasses may override but shall call the super method first.</p>
     * @param component the component
     */
    public void componentFocusGained(PageComponent component) {
        this.activeComponent = component;
    }

    /**
     * The default implementation nulls the active component.
     * <p>Subclasses may override but shall call the super method first.</p>
     * @param component the component
     */
    public void componentFocusLost(PageComponent component) {
        this.activeComponent = null;
    }
}