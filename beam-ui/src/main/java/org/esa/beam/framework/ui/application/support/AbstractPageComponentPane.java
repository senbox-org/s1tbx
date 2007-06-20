package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentPane;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A <code>PageComponentPane</code> is a container that holds the
 * <code>PageComponent</code>'s control, and can add extra decorations (add a toolbar,
 * a border, docking capabilities ...)
 * <p/>
 * This allows for adding extra behaviour to <code>PageComponent</code>s that have to
 * be applied to all <code>PageComponent</code>.
 */
public abstract class AbstractPageComponentPane extends AbstractControlFactory implements PageComponentPane {

    private final PageComponent pageComponent;

    protected AbstractPageComponentPane(PageComponent pageComponent) {
        this.pageComponent = pageComponent;
        this.pageComponent.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                pageComponentChanged(evt);
            }
        });
    }

    public PageComponent getPageComponent() {
        return pageComponent;
    }

    /**
     * Handle the change of a property of this pane's page component.
     * @param evt
     */
    protected abstract void pageComponentChanged(PropertyChangeEvent evt);
}
