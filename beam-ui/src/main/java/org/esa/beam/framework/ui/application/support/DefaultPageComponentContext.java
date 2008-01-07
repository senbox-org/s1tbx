package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.ui.application.*;

import java.util.HashMap;
import java.util.Map;

public class DefaultPageComponentContext implements PageComponentContext {
    private final PageComponentPane pane;

    private final ApplicationPage page;

    private Map<String, ActionCommandExecutor> sharedCommandExecutors;

    public DefaultPageComponentContext(ApplicationPage page, PageComponentPane pane) {
        Assert.notNull(page, "page");
        this.page = page;
        this.pane = pane;
    }

    public ApplicationWindow getWindow() {
        return page.getWindow();
    }

    public ApplicationPage getPage() {
        return page;
    }

    public PageComponentPane getPane() {
        return pane;
    }

    public synchronized ActionCommandExecutor getLocalCommandExecutor(String commandId) {
        Assert.notNull(commandId, "commandId");
        if (sharedCommandExecutors == null) {
            return null;
        }
        return sharedCommandExecutors.get(commandId);
    }

    public synchronized void setLocalCommandExecutor(String commandId, ActionCommandExecutor executor) {
        Assert.notNull(commandId, "commandId");
        if (sharedCommandExecutors == null) {
            sharedCommandExecutors = new HashMap<String, ActionCommandExecutor>(5);
        }
        if (executor == null) {
            sharedCommandExecutors.remove(commandId);
        } else {
            sharedCommandExecutors.put(commandId, executor);
        }
    }

    public SelectionProvider getSelectionProvider() {
        final DefaultSelectionService service = (DefaultSelectionService) page.getWindow().getSelectionService();
        return service.getSelectionProvider(pane.getPageComponent());
    }

    public void setSelectionProvider(SelectionProvider selectionProvider) {
        final DefaultSelectionService service = (DefaultSelectionService) page.getWindow().getSelectionService();
        service.setSelectionProvider(pane.getPageComponent(), selectionProvider);
    }
}