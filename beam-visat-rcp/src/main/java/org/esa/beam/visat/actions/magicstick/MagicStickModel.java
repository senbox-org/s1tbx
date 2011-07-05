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

    public enum Operator {
        INTEGRAL,
        IDENTITY,
        DERIVATIVE,
    }

    public enum Method {
        DISTANCE,
        AVERAGE,
        LIMITS,
    }

    static final String MAGIC_STICK_MASK_NAME = "magic_stick";

    private Mode mode;
    private Operator operator;
    private Method method;
    private ArrayList<double[]> plusSpectra;
    private ArrayList<double[]> minusSpectra;
    private double tolerance;

    MagicStickModel() {
        mode = Mode.SINGLE;
        method = Method.DISTANCE;
        operator = Operator.IDENTITY;
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

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
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
        if (getMethod() == Method.DISTANCE) {
            plusPart = getDistancePart(spectralBands, operator, plusSpectra, tolerance);
            minusPart = getDistancePart(spectralBands, operator, minusSpectra, tolerance);
        } else if (getMethod() == Method.AVERAGE) {
            plusPart = getAveragePart(spectralBands, operator, plusSpectra, tolerance);
            minusPart = getAveragePart(spectralBands, operator, minusSpectra, tolerance);
        } else if (getMethod() == Method.LIMITS) {
            plusPart = getLimitsPart(spectralBands, operator, plusSpectra, tolerance);
            minusPart = getLimitsPart(spectralBands, operator, minusSpectra, tolerance);
        } else {
            throw new IllegalStateException("Unhandled method " + getMethod());
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

    private static String getLimitsPart(Band[] spectralBands, Operator operator, List<double[]> spectra, double tolerance) {
        if (spectra.isEmpty()) {
            return null;
        }
        double[] minSpectrum = getMinSpectrum(spectralBands, spectra, tolerance);
        double[] maxSpectrum = getMaxSpectrum(spectralBands, spectra, tolerance);
        return getLimitsSubPart(spectralBands, operator, minSpectrum, maxSpectrum);
    }

    private static String getAveragePart(Band[] spectralBands, Operator operator, List<double[]> spectra, double tolerance) {
        if (spectra.isEmpty()) {
            return null;
        }
        double[] avgSpectrum = getAvgSpectrum(spectralBands, spectra);
        return getDistanceSubPart(spectralBands, operator, avgSpectrum, tolerance);
    }

    private static double[] getAvgSpectrum(Band[] spectralBands, List<double[]> spectra) {
        double[] avgSpectrum = new double[spectralBands.length];
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                avgSpectrum[i] += spectrum[i];
            }
        }
        for (int i = 0; i < avgSpectrum.length; i++) {
            avgSpectrum[i] /= spectra.size();
        }
        return avgSpectrum;
    }

    private static double[] getMinSpectrum(Band[] spectralBands, List<double[]> spectra, double tolerance) {
        double[] minSpectrum = new double[spectralBands.length];
        Arrays.fill(minSpectrum, +Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                minSpectrum[i] = Math.min(minSpectrum[i], spectrum[i] - tolerance);
            }
        }
        return minSpectrum;
    }

    private static double[] getMaxSpectrum(Band[] spectralBands, List<double[]> spectra, double tolerance) {
        double[] maxSpectrum = new double[spectralBands.length];
        Arrays.fill(maxSpectrum, -Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                maxSpectrum[i] = Math.max(maxSpectrum[i], spectrum[i] + tolerance);
            }
        }
        return maxSpectrum;
    }

    private static String getDistancePart(Band[] bands, Operator operator, List<double[]> spectra, double tolerance) {
        if (spectra.isEmpty()) {
            return null;
        }
        final StringBuilder part = new StringBuilder();
        for (int i = 0; i < spectra.size(); i++) {
            double[] spectrum = spectra.get(i);
            if (i > 0) {
                part.append(" || ");
            }
            part.append(getDistanceSubPart(bands, operator, spectrum, tolerance));
        }
        return part.toString();
    }

    private static String getDistanceSubPart(Band[] bands, Operator operator, double[] spectrum, double tolerance) {
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
        String functionName;
        if (operator == Operator.IDENTITY) {
            functionName = "distance";
        } else if (operator == Operator.DERIVATIVE) {
            functionName = "distance_deriv";
        } else if (operator == Operator.INTEGRAL) {
            functionName = "distance_integ";
        } else {
            throw new IllegalStateException("unhandled operator " + operator);
        }
        if (bands.length == 1) {
            return String.format("%s(%s) < %s", functionName, arguments, tolerance);
        } else {
            return String.format("%s(%s)/%s < %s", functionName, arguments, bands.length, tolerance);
        }
    }

    private static String getLimitsSubPart(Band[] bands, Operator operator, double[] minSpectrum,  double[] maxSpectrum) {
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
        for (double value : minSpectrum) {
            arguments.append(",");
            arguments.append(value);
        }
        for (double value : maxSpectrum) {
            arguments.append(",");
            arguments.append(value);
        }
        String functionName;
        if (operator == Operator.IDENTITY) {
            functionName = "inrange";
        } else if (operator == Operator.DERIVATIVE) {
            functionName = "inrange_deriv";
        } else if (operator == Operator.INTEGRAL) {
            functionName = "inrange_integ";
        } else {
            throw new IllegalStateException("unhandled operator " + operator);
        }
        return String.format("%s(%s)", functionName, arguments);
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
