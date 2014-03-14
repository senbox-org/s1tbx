/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.actions.masktools;

import com.bc.ceres.core.Assert;
import com.bc.jexp.ParseException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.StringUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utilities for the magic wand tool (interactor).
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class MagicWandModel implements Cloneable {

    public enum PickMode {
        SINGLE,
        PLUS,
        MINUS,
    }

    public enum SpectrumTransform {
        INTEGRAL,
        IDENTITY,
        DERIVATIVE,
    }

    public enum BandAccumulation {
        DISTANCE,
        AVERAGE,
        LIMITS,
    }

    static final String MAGIC_WAND_MASK_NAME = "magic_wand";

    private double tolerance;
    private double minTolerance;
    private double maxTolerance;
    private ArrayList<String> bandNames;
    private SpectrumTransform spectrumTransform;
    private BandAccumulation bandAccumulation;
    private boolean normalize;
    private PickMode pickMode;
    private ArrayList<double[]> plusSpectra;
    private ArrayList<double[]> minusSpectra;

    private transient ArrayList<Listener> listeners;

    public MagicWandModel() {
        pickMode = PickMode.SINGLE;
        bandAccumulation = BandAccumulation.DISTANCE;
        spectrumTransform = SpectrumTransform.IDENTITY;
        bandNames = new ArrayList<>();
        plusSpectra = new ArrayList<>();
        minusSpectra = new ArrayList<>();
        tolerance = 0.1;
        minTolerance = 0.0;
        maxTolerance = 1.0;
    }

    public void addListener(Listener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    public void removeListeners() {
        if (listeners != null) {
            listeners.clear();
            listeners = null;
        }
    }

    public void fireModelChanged(boolean recomputeMask) {
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.modelChanged(this, recomputeMask);
            }
        }
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public MagicWandModel clone() {
        try {
            MagicWandModel clone = (MagicWandModel) super.clone();
            clone.bandNames = new ArrayList<>(bandNames);
            clone.plusSpectra = new ArrayList<>(plusSpectra);
            clone.minusSpectra = new ArrayList<>(minusSpectra);
            clone.listeners = null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void assign(MagicWandModel other) {
        bandAccumulation = other.bandAccumulation;
        spectrumTransform = other.spectrumTransform;
        pickMode = other.pickMode;
        tolerance = other.tolerance;
        minTolerance = other.minTolerance;
        maxTolerance = other.maxTolerance;
        bandNames = new ArrayList<>(other.bandNames);
        plusSpectra = new ArrayList<>(other.plusSpectra);
        minusSpectra = new ArrayList<>(other.minusSpectra);
        fireModelChanged(true);
    }

    int getSpectraCount() {
        return plusSpectra.size() + minusSpectra.size();
    }

    int getPlusSpectraCount() {
        return plusSpectra.size();
    }

    int getMinusSpectraCount() {
        return minusSpectra.size();
    }

    int getBandCount() {
        return bandNames.size();
    }

    void addSpectrum(double... spectrum) {
        Assert.argument(spectrum.length == bandNames.size(), "spectrum size does not match # selected bands");
        if (pickMode == PickMode.SINGLE) {
            plusSpectra.clear();
            minusSpectra.clear();
            plusSpectra.add(spectrum);
        } else if (pickMode == PickMode.PLUS) {
            plusSpectra.add(spectrum);
        } else if (pickMode == PickMode.MINUS) {
            minusSpectra.add(spectrum);
        }
        fireModelChanged(true);
    }

    void clearSpectra() {
        plusSpectra.clear();
        minusSpectra.clear();
        fireModelChanged(true);
    }

    public List<String> getBandNames() {
        return Collections.unmodifiableList(bandNames);
    }

    public void setBandNames(String... bandNames) {
        setBandNames(Arrays.asList(bandNames));
    }

    public void setBandNames(List<String> bandNames) {
        plusSpectra.clear();
        minusSpectra.clear();
        this.bandNames = new ArrayList<>(bandNames);
        fireModelChanged(true);
    }

    public PickMode getPickMode() {
        return pickMode;
    }

    public void setPickMode(PickMode pickMode) {
        this.pickMode = pickMode;
        fireModelChanged(false);
    }

    public BandAccumulation getBandAccumulation() {
        return bandAccumulation;
    }

    public void setBandAccumulation(BandAccumulation bandAccumulation) {
        this.bandAccumulation = bandAccumulation;
        fireModelChanged(false);
    }

    public SpectrumTransform getSpectrumTransform() {
        return spectrumTransform;
    }

    public void setSpectrumTransform(SpectrumTransform spectrumTransform) {
        this.spectrumTransform = spectrumTransform;
        fireModelChanged(false);
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
        fireModelChanged(true);
    }

    public double getMinTolerance() {
        return minTolerance;
    }

    public void setMinTolerance(double minTolerance) {
        this.minTolerance = minTolerance;
        fireModelChanged(false);
    }

    public double getMaxTolerance() {
        return maxTolerance;
    }

    public void setMaxTolerance(double maxTolerance) {
        this.maxTolerance = maxTolerance;
        fireModelChanged(false);
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
        fireModelChanged(false);
    }

    public void setSpectralBandNames(Product product) {
        List<Band> bands = getSpectralBands(product);
        List<String> bandNames = new ArrayList<>();
        for (Band band : bands) {
            bandNames.add(band.getName());
        }
        if (!bandNames.isEmpty()) {
            setBandNames(bandNames.toArray(new String[bandNames.size()]));
        }
    }

    public static List<Band> getSpectralBands(Product product) {
        List<Band> bands = new ArrayList<>();
        for (Band band : product.getBands()) {
            if (band.getSpectralWavelength() > 0.0 || band.getSpectralBandIndex() >= 0) {
                bands.add(band);
            }
        }
        Collections.sort(bands, new SpectralBandComparator());
        return bands;
    }


    List<Band> getBands(Product product) {
        List<String> names = getBandNames();
        List<Band> bands = new ArrayList<>(names.size());
        for (String name : names) {
            Band band = product.getBand(name);
            if (band == null) {
                return null;
            }
            bands.add(band);
        }
        return bands;
    }

    static void setMagicWandMask(Product product, String expression) {

        String validMaskExpression;
        try {
            validMaskExpression = BandArithmetic.getValidMaskExpression(expression, new Product[]{product}, 0, null);
        } catch (ParseException e) {
            validMaskExpression = null;
        }

        if (validMaskExpression != null) {
            expression = "(" + validMaskExpression + ") && (" + expression + ")";
        }

        final Mask magicWandMask = product.getMaskGroup().get(MAGIC_WAND_MASK_NAME);
        if (magicWandMask != null) {
            magicWandMask.getImageConfig().setValue("expression", expression);
        } else {
            product.addMask(MAGIC_WAND_MASK_NAME,
                            expression, "Magic wand mask",
                            Color.RED, 0.5);
        }
    }

    String createMaskExpression() {
        final String plusPart;
        final String minusPart;
        if (getBandAccumulation() == BandAccumulation.DISTANCE) {
            plusPart = getDistancePart(bandNames, spectrumTransform, plusSpectra, tolerance, normalize);
            minusPart = getDistancePart(bandNames, spectrumTransform, minusSpectra, tolerance, normalize);
        } else if (getBandAccumulation() == BandAccumulation.AVERAGE) {
            plusPart = getAveragePart(bandNames, spectrumTransform, plusSpectra, tolerance, normalize);
            minusPart = getAveragePart(bandNames, spectrumTransform, minusSpectra, tolerance, normalize);
        } else if (getBandAccumulation() == BandAccumulation.LIMITS) {
            plusPart = getLimitsPart(bandNames, spectrumTransform, plusSpectra, tolerance, normalize);
            minusPart = getLimitsPart(bandNames, spectrumTransform, minusSpectra, tolerance, normalize);
        } else {
            throw new IllegalStateException("Unhandled method " + getBandAccumulation());
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

    private static String getDistancePart(List<String> bandNames, SpectrumTransform spectrumTransform, List<double[]> spectra, double tolerance, boolean normalize) {
        if (spectra.isEmpty()) {
            return null;
        }
        final StringBuilder part = new StringBuilder();
        for (int i = 0; i < spectra.size(); i++) {
            double[] spectrum = getSpectrum(spectra.get(i), normalize);
            if (i > 0) {
                part.append(" || ");
            }
            part.append(getDistanceSubPart(bandNames, spectrumTransform, spectrum, tolerance, normalize));
        }
        return part.toString();
    }

    private static String getAveragePart(List<String> bandNames, SpectrumTransform spectrumTransform, List<double[]> spectra, double tolerance, boolean normalize) {
        if (spectra.isEmpty()) {
            return null;
        }
        double[] avgSpectrum = getAvgSpectrum(bandNames.size(), spectra, normalize);
        return getDistanceSubPart(bandNames, spectrumTransform, avgSpectrum, tolerance, normalize);
    }

    private static String getLimitsPart(List<String> bandNames, SpectrumTransform spectrumTransform, List<double[]> spectra, double tolerance, boolean normalize) {
        if (spectra.isEmpty()) {
            return null;
        }
        double[] minSpectrum = getMinSpectrum(bandNames.size(), spectra, tolerance, normalize);
        double[] maxSpectrum = getMaxSpectrum(bandNames.size(), spectra, tolerance, normalize);
        return getLimitsSubPart(bandNames, spectrumTransform, minSpectrum, maxSpectrum, normalize);
    }

    private static double[] getSpectrum(double[] spectrum, boolean normalize) {
        if (normalize) {
            double[] normSpectrum = new double[spectrum.length];
            for (int i = 0; i < spectrum.length; i++) {
                normSpectrum[i] = getSpectrumValue(spectrum, i, true);
            }
            return normSpectrum;
        } else {
            return spectrum;
        }
    }

    private static double[] getAvgSpectrum(int numBands, List<double[]> spectra, boolean normalize) {
        double[] avgSpectrum = new double[numBands];
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                double value = getSpectrumValue(spectrum, i, normalize);
                avgSpectrum[i] += value;
            }
        }
        for (int i = 0; i < avgSpectrum.length; i++) {
            avgSpectrum[i] /= spectra.size();
        }
        return avgSpectrum;
    }

    private static double[] getMinSpectrum(int numBands, List<double[]> spectra, double tolerance, boolean normalize) {
        double[] minSpectrum = new double[numBands];
        Arrays.fill(minSpectrum, +Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                double value = getSpectrumValue(spectrum, i, normalize);
                minSpectrum[i] = Math.min(minSpectrum[i], value - tolerance);
            }
        }
        return minSpectrum;
    }

    private static double[] getMaxSpectrum(int numBands, List<double[]> spectra, double tolerance, boolean normalize) {
        double[] maxSpectrum = new double[numBands];
        Arrays.fill(maxSpectrum, -Double.MAX_VALUE);
        for (double[] spectrum : spectra) {
            for (int i = 0; i < spectrum.length; i++) {
                double value = getSpectrumValue(spectrum, i, normalize);
                maxSpectrum[i] = Math.max(maxSpectrum[i], value + tolerance);
            }
        }
        return maxSpectrum;
    }

    private static double getSpectrumValue(double[] spectrum, int i, boolean normalize) {
        return normalize ? spectrum[i] / spectrum[0] : spectrum[i];
    }

    private static String getDistanceSubPart(List<String> bandNames, SpectrumTransform spectrumTransform, double[] spectrum, double tolerance, boolean normalize) {
        if (bandNames.isEmpty()) {
            return "0";
        }
        final StringBuilder arguments = new StringBuilder();
        appendSpectrumBandNames(bandNames, normalize, arguments);
        appendSpectrumBandValues(spectrum, arguments);
        String functionName;
        if (spectrumTransform == SpectrumTransform.IDENTITY) {
            functionName = "distance";
        } else if (spectrumTransform == SpectrumTransform.DERIVATIVE) {
            functionName = "distance_deriv";
        } else if (spectrumTransform == SpectrumTransform.INTEGRAL) {
            functionName = "distance_integ";
        } else {
            throw new IllegalStateException("unhandled operator " + spectrumTransform);
        }
        if (bandNames.size() == 1) {
            return String.format("%s(%s) < %s", functionName, arguments, tolerance);
        } else {
            return String.format("%s(%s)/%s < %s", functionName, arguments, bandNames.size(), tolerance);
        }
    }

    private static String getLimitsSubPart(List<String> bandNames, SpectrumTransform spectrumTransform, double[] minSpectrum, double[] maxSpectrum, boolean normalize) {
        if (bandNames.isEmpty()) {
            return "0";
        }
        final StringBuilder arguments = new StringBuilder();
        appendSpectrumBandNames(bandNames, normalize, arguments);
        appendSpectrumBandValues(minSpectrum, arguments);
        appendSpectrumBandValues(maxSpectrum, arguments);
        String functionName;
        if (spectrumTransform == SpectrumTransform.IDENTITY) {
            functionName = "inrange";
        } else if (spectrumTransform == SpectrumTransform.DERIVATIVE) {
            functionName = "inrange_deriv";
        } else if (spectrumTransform == SpectrumTransform.INTEGRAL) {
            functionName = "inrange_integ";
        } else {
            throw new IllegalStateException("unhandled operator " + spectrumTransform);
        }
        return String.format("%s(%s)", functionName, arguments);
    }

    private static void appendSpectrumBandNames(List<String> bandNames, boolean normalize, StringBuilder arguments) {
        String firstName = BandArithmetic.createExternalName(bandNames.get(0));
        for (int i = 0; i < bandNames.size(); i++) {
            if (i > 0) {
                arguments.append(",");
            }
            String name = BandArithmetic.createExternalName(bandNames.get(i));
            if (normalize) {
                arguments.append(name).append("/").append(firstName);
            } else {
                arguments.append(name);
            }
        }
    }

    private static void appendSpectrumBandValues(double[] spectrum, StringBuilder arguments) {
        for (double value : spectrum) {
            arguments.append(",");
            arguments.append(value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MagicWandModel that = (MagicWandModel) o;

        if (Double.compare(that.maxTolerance, maxTolerance) != 0) return false;
        if (Double.compare(that.minTolerance, minTolerance) != 0) return false;
        if (normalize != that.normalize) return false;
        if (Double.compare(that.tolerance, tolerance) != 0) return false;
        if (bandAccumulation != that.bandAccumulation) return false;
        if (pickMode != that.pickMode) return false;
        if (spectrumTransform != that.spectrumTransform) return false;
        if (!ObjectUtils.equalObjects(bandNames, that.bandNames)) return false;
        if (!ObjectUtils.equalObjects(plusSpectra.toArray(), that.plusSpectra.toArray())) return false;
        if (!ObjectUtils.equalObjects(minusSpectra.toArray(), that.minusSpectra.toArray())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = pickMode.hashCode();
        result = 31 * result + spectrumTransform.hashCode();
        result = 31 * result + bandAccumulation.hashCode();
        result = 31 * result + plusSpectra.hashCode();
        result = 31 * result + minusSpectra.hashCode();
        result = 31 * result + bandNames.hashCode();
        temp = tolerance != +0.0d ? Double.doubleToLongBits(tolerance) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (normalize ? 1 : 0);
        temp = minTolerance != +0.0d ? Double.doubleToLongBits(minTolerance) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = maxTolerance != +0.0d ? Double.doubleToLongBits(maxTolerance) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public static MagicWandModel fromXml(String xml) {
        return (MagicWandModel) createXStream().fromXML(xml);
    }

    public String toXml() {
        return createXStream().toXML(this);
    }

    private static XStream createXStream() {
        final XStream xStream = new XStream();
        xStream.alias("magicWandSettings", MagicWandModel.class);
        xStream.registerConverter(new SingleValueConverter() {
            @Override
            public boolean canConvert(Class type) {
                return type.equals(double[].class);
            }

            @Override
            public String toString(Object obj) {
                return StringUtils.arrayToString(obj, ",");
            }

            @Override
            public Object fromString(String str) {
                return StringUtils.toDoubleArray(str, ",");
            }
        });
        return xStream;
    }

    public static interface Listener {
        void modelChanged(MagicWandModel model, boolean recomputeMask);
    }
}
