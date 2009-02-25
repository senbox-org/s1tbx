package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;

public class LayerSource {

    private String name;
    private AbstractAppAssistantPage page;

    public LayerSource(String name) {
        this.name = name;
    }

    public LayerSource(String name, AbstractAppAssistantPage page) {
        this.name = name;
        this.page = page;
    }

    @Override
    public String toString() {
        return name;
    }

    public AbstractAppAssistantPage getPage() {
        return page;
    }
}
