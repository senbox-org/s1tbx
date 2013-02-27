package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Spectrum {

    private String name;
    //Not used at the moment
//    private String namePattern;
//    private String description;
    private List<Band> spectralBands;

    public Spectrum(String name) {
        this(name, "", "", new Band[]{});
    }

    public Spectrum(String name, String description, Band[] spectralBands) {
        this(name, description, "", spectralBands);
    }

    public Spectrum(String name, String description, String namePattern, Band[] spectralBands) {
        this.name = name;
    //Not used this time
//        this.description = description;
//        this.namePattern = namePattern;
        this.spectralBands = new ArrayList<Band>();
        Collections.addAll(this.spectralBands, spectralBands);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



        //Not used at the moment
/*

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

*/

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
