package org.esa.beam.framework.ui.application;

/**
 * A <code>PageComponentPane</code> is a container that holds the
 * <code>PageComponent</code>'s control, and can add extra decorations (add a toolbar,
 * a border, ...).
 * <p/>
 * This allows for adding extra behaviour to <code>PageComponent</code>s that have to
 * be applied to all <code>PageComponent</code>.
 *
 * @author Norman Fomferra (original by Peter De Bruycker of Spring RCP project)
 */
public interface PageComponentPane extends ControlFactory {

    /**
     * Gets the contained page component.
     *
     * @return The page component.
     */
    PageComponent getPageComponent();
}
