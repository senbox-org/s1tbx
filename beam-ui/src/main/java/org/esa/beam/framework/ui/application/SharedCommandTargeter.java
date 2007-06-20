package org.esa.beam.framework.ui.application;

import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;

public class SharedCommandTargeter extends PageComponentListenerAdapter {
    private final ApplicationWindow window;

    public SharedCommandTargeter(ApplicationWindow window) {
        this.window = window;
    }

    public ApplicationWindow getWindow() {
        return window;
    }
}
