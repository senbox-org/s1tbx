package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentPane;

import javax.swing.JComponent;
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
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                pageComponentChanged(evt);
            }
        });
    }

    @Override
    public PageComponent getPageComponent() {
        return pageComponent;
    }

    /**
     * Handle the change of a property of this pane's page component.
     * @param evt The change event.
     */
    protected abstract void pageComponentChanged(PropertyChangeEvent evt);


    /**
     * Used to uniquely name components for UI testing. Format of the new name is  "id.suffix".
     * @param component the component to be named
     * @param suffix the name suffix
     */
    protected void nameComponent(JComponent component, String suffix) {
        component.setName(getComponentName(suffix));
    }

    private String getComponentName(String suffix) {
        String base = getPageComponent().getId();
        int i = base.lastIndexOf('.');
        if (i >= 0) {
            base = base.substring(i + 1);
        }
        return base + "." + suffix;
    }
}
