package org.esa.beam.visat.toolviews.layermanager;

public class LayerSource {

    private String name;
    private LayerPage page;

    public LayerSource(String name) {
        this.name = name;
    }

    public LayerSource(String name, LayerPage page) {
        this.name = name;
        this.page = page;
    }

    @Override
    public String toString() {
        return name;
    }

    public LayerPage getPage() {
        return page;
    }
}
