package org.esa.beam.framework.ui.product.spectrum;

import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import org.esa.beam.framework.datamodel.Band;

public class DisplayableSpectrum implements Spectrum {

    public final static String MIXED_UNITS = "mixed units";
    public final static String DEFAULT_SPECTRUM_NAME = "Available spectral bands";
    public final static String ALTERNATIVE_DEFAULT_SPECTRUM_NAME = "Further spectral bands";

    private List<Band> bands;
    private List<Boolean> areBandsSelected;
    private String name;
    private Stroke lineStyle;
    private int symbolIndex;
    private int symbolSize;
    private boolean isSelected;
    private String unit;

    public DisplayableSpectrum(String spectrumName) {
        this(spectrumName, new Band[]{});
    }

    public DisplayableSpectrum(String spectrumName, Band[] spectralBands) {
        this.name = spectrumName;
        bands = new ArrayList<Band>(spectralBands.length);
        areBandsSelected = new ArrayList<Boolean>();
        symbolIndex = -1;
        symbolSize = SpectrumShapeProvider.DEFAULT_SCALE_GRADE;
        for (Band spectralBand : spectralBands) {
            addBand(spectralBand, true);
        }
        setSelected(true);
    }

    public void addBand(Band band, boolean selected) {
        bands.add(band);
        areBandsSelected.add(selected);
        updateUnit();
    }

    public Shape getScaledShape() {
        int usedSymbolIndex = getSymbolIndex();
        Shape symbol;
        if (symbolIndex == -1) {
            symbol = SpectrumShapeProvider.shapes[1];
            usedSymbolIndex = 1;
        } else {
            symbol = SpectrumShapeProvider.shapes[getSymbolIndex()];
        }
        if (getSymbolSize() != 3) {
            symbol = SpectrumShapeProvider.getScaledShape(usedSymbolIndex, getSymbolSize());
        }
        return symbol;
    }

    public boolean isDefaultSpectrum() {
        return name.equals(DEFAULT_SPECTRUM_NAME) || name.equals(ALTERNATIVE_DEFAULT_SPECTRUM_NAME);
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
        if (isDefaultSpectrum()) {
            return SpectrumStrokeProvider.EMPTY_STROKE;
        }
        return lineStyle;
    }

    public void setLineStyle(Stroke lineStyle) {
        this.lineStyle = lineStyle;
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

    public int getSymbolSize() {
        return symbolSize;
    }

    public void setSymbolSize(int symbolSize) {
        this.symbolSize = symbolSize;
    }

    public int getSymbolIndex() {
        return symbolIndex;
    }

    public void setSymbolIndex(int symbolIndex) {
        this.symbolIndex = symbolIndex;
    }

    public void updateUnit() {
        if (bands.size() > 0) {
            unit = bands.get(0).getUnit();
        }
        if (bands.size() > 1) {
            for (int i = 1; i < bands.size(); i++) {
                if (!unit.equals(bands.get(i).getUnit())) {
                    unit = MIXED_UNITS;
                    return;
                }
            }
        }
    }
}
