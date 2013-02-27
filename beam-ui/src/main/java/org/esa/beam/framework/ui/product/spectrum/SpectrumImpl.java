package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpectrumImpl implements Spectrum {

    private String name;
    private String namePattern;
    private String description;
    private List<Band> spectralBands;

    public SpectrumImpl(String name) {
        this(name, "", "", new Band[]{});
    }

    public SpectrumImpl(String name, String description, Band[] spectralBands) {
        this(name, description, "", spectralBands);
    }

    public SpectrumImpl(String name, String description, String namePattern, Band[] spectralBands) {
        this.name = name;
        this.description = description;
        this.namePattern = namePattern;
        this.spectralBands = new ArrayList<Band>();
        Collections.addAll(this.spectralBands, spectralBands);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Band[] getSpectralBands() {
        return spectralBands.toArray(new Band[spectralBands.size()]);
    }

    public void addBand(Band spectralBand) {
        spectralBands.add(spectralBand);
    }

    public boolean hasBands() {
        return !spectralBands.isEmpty();
    }

}
