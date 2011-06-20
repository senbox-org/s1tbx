package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for the magicstick stick tool (interactor).
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicStickModel {

    public enum Mode {
        SINGLE,
        PLUS,
        MINUS,
    }

    public enum Method {
        DISTANCE,
        SHAPE,
        LIMITS,
    }

    static final String MAGIC_STICK_MASK_NAME = "magic_stick";

    private Mode mode;
    private Method method;
    private ArrayList<double[]> plusSpectra;
    private ArrayList<double[]> minusSpectra;
    private double tolerance;

    MagicStickModel() {
        mode = Mode.SINGLE;
        method = Method.DISTANCE;
        plusSpectra = new ArrayList<double[]>();
        minusSpectra = new ArrayList<double[]>();
        tolerance = 0.1;
    }

    void addSpectrum(double... spectrum) {
        if (mode == Mode.SINGLE) {
            plusSpectra.clear();
            minusSpectra.clear();
            plusSpectra.add(spectrum);
        } else if (mode == Mode.PLUS) {
            plusSpectra.add(spectrum);
        } else if (mode == Mode.MINUS) {
            minusSpectra.add(spectrum);
        }
    }

    void clearSpectra() {
        plusSpectra.clear();
        minusSpectra.clear();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    static Band[] getSpectralBands(Product product) {
        final Band[] bands = product.getBands();
        final ArrayList<Band> spectralBands = new ArrayList<Band>(bands.length);
        for (Band band : bands) {
            if (band.getSpectralWavelength() > 0.0) {
                spectralBands.add(band);
            }
        }
        return spectralBands.toArray(new Band[spectralBands.size()]);
    }

    static void setMagicStickMask(Product product, String expression) {
        final Mask magicStickMask = product.getMaskGroup().get(MAGIC_STICK_MASK_NAME);
        if (magicStickMask != null) {
            magicStickMask.getImageConfig().setValue("expression", expression);
        } else {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            product.getMaskGroup().add(Mask.BandMathsType.create(MAGIC_STICK_MASK_NAME,
                                                                 "Magic stick mask",
                                                                 width, height,
                                                                 expression,
                                                                 Color.RED, 0.5));
        }
    }

    String createExpression(Band... spectralBands) {
        final String plusPart;
        final String minusPart;
        if (getMethod() == Method.LIMITS) {
            plusPart = getLimitsPart(spectralBands, plusSpectra, tolerance);
            minusPart = getLimitsPart(spectralBands, minusSpectra, tolerance);
        } else {
            plusPart = getDistancePart(spectralBands, method, plusSpectra, tolerance);
            minusPart = getDistancePart(spectralBands, method, minusSpectra, tolerance);
        }
        if (plusPart != null && minusPart != null) {
            return String.format("(%s) && !(%s)", plusPart, minusPart);
        } else if (plusPart != null) {
            return plusPart;
        } else if (minusPart != null) {
            return String.format("!(%s)", minusPart);
        } else {
            return "0";
        }
    }

    private static String getLimitsPart(Band[] spectralBands, List<double[]> spectra, double tolerance) {
        if (spectra.isEmpty()) {
            return null;
        }
        double[] plusSpectrumMin = getMinSpectrum(spectralBands, spectra, tolerance);
        double[] plusSpectrumMax = getMaxSpectrum(spectralBands, spectra, tolerance);
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < spectralBands.length; i++) {
            Band b = spectralBands[i];
            if (i > 0) {
                part.append(" && ");
            }
            part.append(String.format("%s >= %s && %s <= %s",
                                      b.getName(), plusSpectrumMin[i],
                                      b.getName(), plusSpectrumMax[i]));
        }
        return part.toString();
    }

    private static double[] getMinSpectrum(Band[] spectralBands, List<double[]> spectra, double tolerance) {
        double[] minSpectrum = new double[spectralBands.length];
        Arrays.fill(minSpectrum, +Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                double v = spectrum[i];
                minSpectrum[i] = Math.min(minSpectrum[i], v);
            }
        }
        for (int i = 0; i < minSpectrum.length; i++) {
            minSpectrum[i] -= tolerance;
        }
        return minSpectrum;
    }

    private static double[] getMaxSpectrum(Band[] spectralBands, List<double[]> spectra, double tolerance) {
        double[] maxSpectrum = new double[spectralBands.length];
        Arrays.fill(maxSpectrum, -Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                maxSpectrum[i] = Math.max(maxSpectrum[i], spectrum[i]);
            }
        }
        for (int i = 0; i < maxSpectrum.length; i++) {
            maxSpectrum[i] += tolerance;
        }
        return maxSpectrum;
    }

    private static String getDistancePart(Band[] bands, Method method, List<double[]> spectra, double tolerance) {
        if (spectra.isEmpty()) {
            return null;
        }
        final StringBuilder part = new StringBuilder();
        for (int i = 0; i < spectra.size(); i++) {
            double[] spectrum = spectra.get(i);
            if (i > 0) {
                part.append(" || ");
            }
            part.append(createExpression(bands, method, spectrum, tolerance));
        }
        return part.toString();
    }

    private static String createExpression(Band[] bands, Method method, double[] spectrum, double tolerance) {
        if (bands.length == 0) {
            return "0";
        }
        final StringBuilder arguments = new StringBuilder();
        for (int i = 0; i < bands.length; i++) {
            if (i > 0) {
                arguments.append(",");
            }
            arguments.append(BandArithmetic.createExternalName(bands[i].getName()));
        }
        for (double value : spectrum) {
            arguments.append(",");
            arguments.append(value);
        }
        String functionName = method == Method.DISTANCE ? "distance" : "shape";
        if (bands.length == 1) {
            return String.format("%s(%s) < %s", functionName, arguments, tolerance);
        } else {
            return String.format("%s(%s)/%s < %s", functionName, arguments, bands.length, tolerance);
        }
    }

    static double[] getSpectrum(Band[] bands, int pixelX, int pixelY) throws IOException {
        final double[] pixel = new double[1];
        final double[] spectrum = new double[bands.length];
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            band.readPixels(pixelX, pixelY, 1, 1, pixel, ProgressMonitor.NULL);
            if (band.isPixelValid(pixelX, pixelY)) {
                spectrum[i] = pixel[0];
            } else {
                spectrum[i] = Double.NaN;
            }
        }
        return spectrum;
    }
}
