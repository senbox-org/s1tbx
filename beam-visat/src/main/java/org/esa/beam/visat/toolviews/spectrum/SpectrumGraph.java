package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.AbstractDiagramGraph;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.util.math.Range;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.Debug;

import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;


class SpectrumGraph extends AbstractDiagramGraph {

    private Pin pin;
    private Band[] bands;
    private float[] energies;
    private float[] wavelengths;
    private final Range energyRange;
    private final Range wavelengthRange;

    public SpectrumGraph(Pin pin, Band[] bands) {
        Debug.assertNotNull(bands);
        this.pin = pin;
        this.bands = bands;
        energyRange = new Range();
        wavelengthRange = new Range();
        setBands(bands);
    }

    public Pin getPin() {
        return pin;
    }

    public String getXName() {
        return "Wavelength";
    }

    public String getYName() {
        return pin != null ? pin.getLabel() : "Cursor";
    }

    public int getNumValues() {
        return bands.length;
    }

    public double getXValueAt(int index) {
        return wavelengths[index];
    }

    public double getYValueAt(int index) {
        return energies[index];
    }

    public double getXMin() {
        return wavelengthRange.getMin();
    }

    public double getXMax() {
        return wavelengthRange.getMax();
    }

    public double getYMin() {
        return energyRange.getMin();
    }

    public double getYMax() {
        return energyRange.getMax();
    }

    public Band[] getBands() {
        return bands;
    }

    public void setBands(Band[] bands) {
        Debug.assertNotNull(bands);
        this.bands = bands.clone();
        Arrays.sort(this.bands, new Comparator<Band>() {
            public int compare(Band band1, Band band2) {
                final float v = band1.getSpectralWavelength() - band2.getSpectralWavelength();
                return v < 0.0F ? -1 : v > 0.0F ? 1 : 0;
            }
        });
        if (wavelengths == null || wavelengths.length != this.bands.length) {
            wavelengths = new float[this.bands.length];
        }
        if (energies == null || energies.length != this.bands.length) {
            energies = new float[this.bands.length];
        }
        for (int i = 0; i < wavelengths.length; i++) {
            wavelengths[i] = this.bands[i].getSpectralWavelength();
            energies[i] = 0.0f;
        }
        Range.computeRangeFloat(wavelengths, IndexValidator.TRUE, wavelengthRange, ProgressMonitor.NULL);
        Range.computeRangeFloat(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
    }

    public void readValues(int pixelX, int pixelY) throws IOException {
        Debug.assertNotNull(bands);
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            if (pin != null) {
                pixelX = MathUtils.floorInt(pin.getPixelPos().x);
                pixelY = MathUtils.floorInt(pin.getPixelPos().y);
            }
            energies[i] = band.readPixels(pixelX, pixelY, 1, 1, new float[1], ProgressMonitor.NULL)[0];
        }
        Range.computeRangeFloat(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    @Override
    public void dispose() {
        pin = null;
        bands = null;
        energies = null;
        wavelengths = null;
        super.dispose();
    }
}
