package org.esa.beam.framework.ui.application;

/**
 * A listener which can be added to an application page.
 * <p>Clients may implement this interface. However, the preferred way to implement this interface is via the
 * {@link org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter}, since this interface may handle more
 * events in the future.</p>
 */
public interface PageComponentListener {
    /**
     * Notifies this listener that the given component has been created.
     *
     * @param component the component that was created
     */
    public void componentOpened(PageComponent component);

    /**
     * Notifies this listener that the given component has been closed.
     *
     * @param component the component that was closed
     */
    public void componentClosed(PageComponent component);

    /**
     * Notifies this listener that the given component has been given focus
     *
     * @param component the component that was given focus
     */
    public void componentFocusGained(PageComponent component);

    /**
     * Notifies this listener that the given component has lost focus.
     *
     * @param component the component that lost focus
     */
    public void componentFocusLost(PageComponent component);

    /**
     * Notifies this listener that the given component was shown.
     *
     * @param component the component that was shown
     */
    public void componentShown(PageComponent component);

    /**
     * Notifies this listener that the given component was hidden.
     *
     * @param component the component that was hidden
     */
    public void componentHidden(PageComponent component);
}
