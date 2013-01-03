package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

public class Spectrum {

    private String name;
    private String description;
    private String namePattern;
    private Band[] spectralBands;

    public Spectrum(String name, String description, Band[] spectralBands) {
        this(name, description, "", spectralBands);
    }

    public Spectrum(String name, String description, String namePattern, Band[] spectralBands) {
        this.name = name;
        this.description = description;
        this.namePattern = namePattern;
        this.spectralBands = spectralBands;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public Band[] getSpectralBands() {
        return spectralBands;
    }

    public void setSpectralBands(Band[] spectralBands) {
        this.spectralBands = spectralBands;
    }
}
