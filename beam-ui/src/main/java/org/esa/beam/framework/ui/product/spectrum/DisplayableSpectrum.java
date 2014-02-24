package org.esa.beam.framework.ui.product.spectrum;

import com.bc.ceres.core.Assert;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import org.esa.beam.framework.datamodel.Band;

public class DisplayableSpectrum implements Spectrum {

    public final static String NO_UNIT = "";
    public final static String MIXED_UNITS = "mixed units";
    public final static String DEFAULT_SPECTRUM_NAME = "Available spectral bands";
    public final static String ALTERNATIVE_DEFAULT_SPECTRUM_NAME = "Further spectral bands";

    private List<SpectrumBand> bands;
    private String name;
    private Stroke lineStyle;
    private int symbolIndex;
    private int symbolSize;
    private boolean isSelected;
    private String unit;

    public DisplayableSpectrum(String spectrumName) {
        this(spectrumName, new SpectrumBand[]{});
    }

    public DisplayableSpectrum(String spectrumName, SpectrumBand[] spectralBands) {
        this.name = spectrumName;
        bands = new ArrayList<SpectrumBand>(spectralBands.length);
        symbolIndex = -1;
        symbolSize = SpectrumShapeProvider.DEFAULT_SCALE_GRADE;
        unit = NO_UNIT;
        for (SpectrumBand spectralBand : spectralBands) {
            addBand(spectralBand);
        }
        setSelected(true);
    }

    public void addBand(SpectrumBand band) {
        Assert.notNull(band);
        bands.add(band);
        if(band.isSelected()) {
            setSelected(true);
        }
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
        Band[] spectralBands = new Band[bands.size()];
        for (int i = 0; i < bands.size(); i++) {
            spectralBands[i] = bands.get(i).getOriginalBand();
        }
        return spectralBands;
    }

    public Band[] getSelectedBands() {
        List<Band> selectedBands = new ArrayList<Band>();
        for (SpectrumBand band : bands) {
            if (band.isSelected()) {
                selectedBands.add(band.getOriginalBand());
            }
        }
        return selectedBands.toArray(new Band[selectedBands.size()]);
    }

    public void setBandSelected(int index, boolean selected) {
        bands.get(index).setSelected(selected);
    }

    public boolean isBandSelected(int index) {
        return bands.get(index).isSelected();
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
            unit = getUnit(bands.get(0));
        }
        if (bands.size() > 1) {
            for (int i = 1; i < bands.size(); i++) {
                if (!unit.equals(getUnit(bands.get(i)))) {
                    unit = MIXED_UNITS;
                    return;
                }
            }
        }
    }

    private String getUnit(SpectrumBand band) {
        String bandUnit = band.getUnit();
        if(bandUnit == null) {
            bandUnit = "";
        }
        return bandUnit;
    }

    public void remove(int j) {
        bands.remove(j);
    }
}
