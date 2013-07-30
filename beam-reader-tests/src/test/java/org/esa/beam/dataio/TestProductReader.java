package org.esa.beam.dataio;

import java.util.ArrayList;

class TestProductReader {

    private String pluginClassName;
    private ArrayList<String> intendedProductNames;
    private ArrayList<String> suitableProductNames;

    TestProductReader() {
        intendedProductNames = new ArrayList<String>();
        suitableProductNames = new ArrayList<String>();
    }

    String getPluginClassName() {
        return pluginClassName;
    }

    void setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
    }

    ArrayList<String> getIntendedProductNames() {
        return intendedProductNames;
    }

    void setIntendedProductNames(ArrayList<String> intendedProductNames) {
        this.intendedProductNames = intendedProductNames;
    }

    ArrayList<String> getSuitableProductNames() {
        return suitableProductNames;
    }

    void setSuitableProductNames(ArrayList<String> suitableProductNames) {
        this.suitableProductNames = suitableProductNames;
    }
}
