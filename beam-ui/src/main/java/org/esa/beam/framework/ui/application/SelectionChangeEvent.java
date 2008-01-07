package org.esa.beam.framework.ui.application;

import java.awt.Component;
import java.util.EventObject;

public class SelectionChangeEvent extends EventObject {
    private final Selection selection;

    public SelectionChangeEvent(Component component, Selection selection) {
        super(component);
        this.selection = selection;
    }

    public SelectionChangeEvent(Object source, Selection selection) {
        super(source);
        this.selection = selection;
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
