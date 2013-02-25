package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

public class SpectrumInDisplay {

    private final Spectrum spectrum;
    private List<Boolean> areBandsSelected;
    //    private List<Band> selectedBands;
    private Stroke lineStyle;
    private Shape Symbol;

    public SpectrumInDisplay(String spectrumName) {
        this.spectrum = new Spectrum(spectrumName);
        areBandsSelected = new ArrayList<Boolean>();
    }

    public SpectrumInDisplay(String spectrumName, String description, Band[] spectralBands) {
        this.spectrum = new Spectrum(spectrumName, description, spectralBands);
        areBandsSelected = new ArrayList<Boolean>();
        for (Band spectralBand : spectralBands) {
            areBandsSelected.add(true);
        }
    }

    public SpectrumInDisplay(String name, String description, String namePattern, Band[] spectralBands) {
        this.spectrum = new Spectrum(name, description, namePattern, spectralBands);
        areBandsSelected = new ArrayList<Boolean>();
        for (Band spectralBand : spectralBands) {
            areBandsSelected.add(true);
        }
    }

    public void addBand(Band band) {
        spectrum.addBand(band);
        areBandsSelected.add(true);
    }

    public boolean hasBands() {
        return spectrum.hasBands();
    }

    public String getName() {
        return spectrum.getName();
    }

    public Band[] getSpectralBands() {
        return spectrum.getSpectralBands();
    }

    public String getDescription() {
        return spectrum.getDescription();
    }

    public String getNamePattern() {
        return spectrum.getNamePattern();
    }

    public Band[] getSelectedBands() {
        List<Band> selectedBands = new ArrayList<Band>();
        Band[] spectralBands = spectrum.getSpectralBands();
        for (int i = 0; i < spectralBands.length; i++) {
            Band band = spectralBands[i];
            if (areBandsSelected.get(i)) {
                selectedBands.add(band);
            }
        }
        return selectedBands.toArray(new Band[selectedBands.size()]);
    }

    public void setBandSelected(int index, boolean selected) {
        areBandsSelected.set(index, selected);
    }

    public boolean isBandSelected(int index) {
        return areBandsSelected.get(index);
    }

    public Stroke getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(Stroke lineStyle) {
        this.lineStyle = lineStyle;
    }

    public Shape getSymbol() {
        return Symbol;
    }

    public void setSymbol(Shape symbol) {
        Symbol = symbol;
    }
}
