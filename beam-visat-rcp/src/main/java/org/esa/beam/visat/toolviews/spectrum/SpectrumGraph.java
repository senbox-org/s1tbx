package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.diagram.AbstractDiagramGraph;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.Comparator;


class SpectrumGraph extends AbstractDiagramGraph {

    private Placemark placemark;
    private Band[] bands;
    private float[] energies;
    private float[] wavelengths;
    private final Range energyRange;
    private final Range wavelengthRange;

    public SpectrumGraph(Placemark placemark, Band[] bands) {
        Debug.assertNotNull(bands);
        this.placemark = placemark;
        this.bands = bands;
        energyRange = new Range();
        wavelengthRange = new Range();
        setBands(bands);
    }

    public Placemark getPlacemark() {
        return placemark;
    }

    public String getXName() {
        return "Wavelength";
    }

    public String getYName() {
        return placemark != null ? placemark.getLabel() : "Cursor";
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

    public void readValues(int pixelX, int pixelY, int level) {
        Debug.assertNotNull(bands);
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            if (placemark != null) {
                // position of placemark is given in image (L0) coordinates
                // we have to transform them to the current level
                final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(band);
                final AffineTransform i2mTransform = multiLevelModel.getImageToModelTransform(0);
                final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
                final Point2D modelPixel = i2mTransform.transform(placemark.getPixelPos(), null);
                final Point2D imagePixel = m2iTransform.transform(modelPixel, null);
                pixelX = (int) Math.floor(imagePixel.getX());
                pixelY = (int) Math.floor(imagePixel.getY());
            }
            energies[i] = getSample(band, pixelX, pixelY, level);
        }
        Range.computeRangeFloat(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    private float getSample(Band band, int pixelX, int pixelY, int level) {
        PlanarImage image = ImageManager.getInstance().getSourceImage(band, level);
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        Raster data = image.getTile(tileX, tileY);
        float sampleFloat = data.getSampleFloat(pixelX, pixelY, 0);
        if (band.isScalingApplied()) {
            sampleFloat = (float) band.scale(sampleFloat);
        }
        return sampleFloat;
    }

    @Override
    public void dispose() {
        placemark = null;
        bands = null;
        energies = null;
        wavelengths = null;
        super.dispose();
    }
}
