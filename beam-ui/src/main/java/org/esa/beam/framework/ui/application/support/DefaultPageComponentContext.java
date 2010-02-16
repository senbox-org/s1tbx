package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.PageComponentContext;
import org.esa.beam.framework.ui.application.PageComponentPane;

public class DefaultPageComponentContext implements PageComponentContext {
    private final PageComponentPane pane;

    private final ApplicationPage page;

    public DefaultPageComponentContext(ApplicationPage page, PageComponentPane pane) {
        Assert.notNull(page, "page");
        this.page = page;
        this.pane = pane;
    }

    @Override
    public ApplicationPage getPage() {
        return page;
    }

    @Override
    public PageComponentPane getPane() {
        return pane;
    }
}