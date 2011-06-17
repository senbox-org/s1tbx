package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;

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

    static void  setMagicStickMask(Product product, String expression) {
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
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bands.length; i++) {
            Band b = bands[i];
            double v = spectrum[i];
            if (i > 0) {
                sb.append("+");
            }
            sb.append(String.format("(%s-%s)*(%s-%s)", b.getName(), v, b.getName(), v));
        }
        return String.format("sqrt(%s) < %s", sb, tolerance);
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
