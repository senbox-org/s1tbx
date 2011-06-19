package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for the magicstick stick tool (interactor).
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicStickUtils {
    static final String MAGIC_STICK_MASK_NAME = "magic_stick";

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
            final int heigth = product.getSceneRasterHeight();
            product.getMaskGroup().add(Mask.BandMathsType.create(MAGIC_STICK_MASK_NAME,
                                                                 "Magic stick mask",
                                                                 width, heigth,
                                                                 expression,
                                                                 Color.RED, 0.5));
        }
    }

    static String createExpression(Band[] bands, double[] spectrum, double tolerance) {
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
        for (int i = 0; i < spectrum.length; i++) {
            arguments.append(",");
            arguments.append(spectrum[i]);
        }
        if (bands.length == 1) {
            return String.format("distance(%s) < %s", arguments, tolerance);
        } else {
            return String.format("distance(%s)/%s < %s", arguments, bands.length, tolerance);
        }
    }

    static String createExpression(Band[] bands, List<double[]> plusSpectra, List<double[]> minusSpectra, double tolerance) {
        String plusPart = getPart(bands, plusSpectra, tolerance);
        String minusPart = getPart(bands, minusSpectra, tolerance);
        if (!plusPart.isEmpty() && !minusPart.isEmpty()) {
            return String.format("(%s) && !(%s)", plusPart, minusPart);
        } else if (!plusPart.isEmpty()) {
            return plusPart;
        } else if (!minusSpectra.isEmpty()) {
            return String.format("!(%s)", minusPart);
        } else {
            return "0";
        }
    }

    private static String getPart(Band[] bands, List<double[]> spectra, double tolerance) {
        final StringBuilder part = new StringBuilder();
        for (int i = 0; i < spectra.size(); i++) {
            double[] spectrum = spectra.get(i);
            if (i > 0) {
                part.append(" || ");
            }
            part.append(createExpression(bands, spectrum, tolerance));
        }
        return part.toString();
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
