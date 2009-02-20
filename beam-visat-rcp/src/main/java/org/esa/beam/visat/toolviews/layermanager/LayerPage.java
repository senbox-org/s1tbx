package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.mpage.AbstractPage;
import org.esa.beam.framework.ui.mpage.Page;
import org.esa.beam.framework.ui.mpage.PageContext;

import java.awt.Component;

public abstract class LayerPage extends AbstractPage {

    protected LayerPage(String pageTitle) {
        super(pageTitle);
    }

    public final LayerPageContext getLayerPageContext() {
        return (LayerPageContext) getPageContext();
    }

    @Override
    public final Page getNextPage() {
        return getNextLayerPage();
    }

    public LayerPage getNextLayerPage() {
        return null;
    }

    @Override
    protected final Component createPageComponent(PageContext context) {
        return createLayerPageComponent((LayerPageContext) context);
    }

    protected abstract Component createLayerPageComponent(LayerPageContext context);
}
