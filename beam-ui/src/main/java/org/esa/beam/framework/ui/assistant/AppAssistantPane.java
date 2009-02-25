package org.esa.beam.framework.ui.assistant;

import org.esa.beam.framework.ui.AppContext;

import java.awt.Window;

public class AppAssistantPane extends AssistantPane implements AppAssistantPageContext {

    private final AppContext appContext;

    public AppAssistantPane(Window parent, String title, AppContext appContext) {
        super(parent, title);
        this.appContext = appContext;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }
}
