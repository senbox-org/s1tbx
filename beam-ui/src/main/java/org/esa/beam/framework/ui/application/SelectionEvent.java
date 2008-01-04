package org.esa.beam.framework.ui.application;

import java.util.EventObject;
import java.awt.Component;

public class SelectionEvent extends EventObject {
    private final Selection selection;

    public SelectionEvent(Component component, Selection selection) {
        super(component);
        this.selection = selection;
    }

    public SelectionEvent(PageComponent pageComponent, Selection selection) {
        super(pageComponent);
        this.selection = selection;
    }

    public PageComponent getPageComponent() {
        final Object source = getSource();
        if (source instanceof PageComponent) {
            return (PageComponent) source;
        }
        return null;
    }

    public Component getComponent() {
        final Object source = getSource();
        if (source instanceof Component) {
            return (Component) source;
        }
        return null;
    }

    public Selection getSelection() {
        return selection;
    }
}
