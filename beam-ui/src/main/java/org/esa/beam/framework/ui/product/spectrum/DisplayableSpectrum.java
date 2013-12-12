package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

public class DisplayableSpectrum implements Spectrum {

    public final static String mixed_units = "mixed units";

    private List<Band> bands;
    private List<Boolean> areBandsSelected;
    private String name;
    private Stroke lineStyle;
    private Shape Symbol;
    private boolean isSelected;
    private String unit;

    public DisplayableSpectrum(String spectrumName) {
        this(spectrumName, new Band[]{});
    }

    public DisplayableSpectrum(String spectrumName, Band[] spectralBands) {
        this.name = spectrumName;
        bands = new ArrayList<Band>(spectralBands.length);
        areBandsSelected = new ArrayList<Boolean>();
        for (Band spectralBand : spectralBands) {
            addBand(spectralBand);
        }
        setSelected(true);
    }

    public void addBand(Band band) {
        bands.add(band);
        areBandsSelected.add(true);
        if (unit == null) {
            unit = band.getUnit();
        } else if (!unit.equals(band.getUnit())) {
            unit = mixed_units;
        }
    }

    public boolean hasBands() {
        return !bands.isEmpty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Band[] getSpectralBands() {
        return bands.toArray(new Band[bands.size()]);
    }

    public Band[] getSelectedBands() {
        List<Band> selectedBands = new ArrayList<Band>();
        for (int i = 0; i < bands.size(); i++) {
            Band band = bands.get(i);
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

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getUnit() {
        return unit;
    }
}
