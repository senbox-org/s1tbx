/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.datamodel.AbstractMetadata;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class Sentinel1Utils {
    public final static DateFormat sentinelDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd_HH:mm:ss");

    private Sentinel1Utils() {
    }

    public static ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, sentinelDateFormat);
    }

    /**
     * Get source product polarizations.
     *
     * @param absRoot Root element of the abstracted metadata of the source product.
     * @return The polarization array.
     */
    public static String[] getProductPolarizations(final MetadataElement absRoot) {

//        String swath = absRoot.getAttributeString(AbstractMetadata.SWATH);
//        if (swath.length() <= 1) {
//            swath = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
//        }
        String swath = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

        final MetadataElement[] elems = absRoot.getElements();
        final List<String> polarizations = new ArrayList<String>(4);
        for (MetadataElement elem : elems) {
            if (elem.getName().contains(swath)) {
                final String pol = elem.getAttributeString("polarization");
                if (!polarizations.contains(pol)) {
                    polarizations.add(pol);
                }
            }
        }
        return polarizations.toArray(new String[polarizations.size()]);
    }

    public static void updateBandNames(
            final MetadataElement absRoot, final java.util.List<String> selectedPolList, final String[] bandNames) {

        final boolean isGRD = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE).equals("GRD");
        final MetadataElement[] children = absRoot.getElements();
        for (MetadataElement child : children) {
            final String childName = child.getName();
            if (childName.startsWith(AbstractMetadata.BAND_PREFIX)) {
                final String pol = childName.substring(childName.lastIndexOf("_") + 1);
                final String sw_pol = childName.substring(childName.indexOf("_") + 1);
                if (selectedPolList.contains(pol)) {
                    String bandNameArray = "";
                    for (String bandName : bandNames) {
                        if (!isGRD && bandName.contains(sw_pol) || isGRD && bandName.contains(pol)) {
                            bandNameArray += bandName + " ";
                        }
                    }
                    child.setAttributeString(AbstractMetadata.band_names, bandNameArray);
                } else {
                    absRoot.removeElement(child);
                }
            }
        }
    }

    public static int[] getIntArray(final MetadataElement elem, final String tag) {

        MetadataAttribute attribute = elem.getAttribute(tag);
        if (attribute == null) {
            throw new OperatorException(tag + " attribute not found");
        }

        int[] array = null;
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            String dataStr = attribute.getData().getElemString();
            String[] items = dataStr.split(" ");
            array = new int[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    array[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException e) {
                    throw new OperatorException("Failed in getting" + tag + " array");
                }
            }
        }

        return array;
    }

    public static double[] getDoubleArray(final MetadataElement elem, final String tag) {

        MetadataAttribute attribute = elem.getAttribute(tag);
        if (attribute == null) {
            throw new OperatorException(tag + " attribute not found");
        }

        double[] array = null;
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            String dataStr = attribute.getData().getElemString();
            String[] items = dataStr.split(" ");
            array = new double[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    array[i] = Double.parseDouble(items[i]);
                } catch (NumberFormatException e) {
                    throw new OperatorException("Failed in getting" + tag + " array");
                }
            }
        }

        return array;
    }

    public static NoiseVector[] getNoiseVector(final Band band) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(band.getProduct());
        final MetadataElement bandAbsMetadata = AbstractMetadata.getBandAbsMetadata(absRoot, band);
        final String annotation = bandAbsMetadata.getAttributeString(AbstractMetadata.annotation);
        final MetadataElement origMetadata = AbstractMetadata.getOriginalProductMetadata(band.getProduct());
        final MetadataElement noiseElem = origMetadata.getElement("noise");
        final MetadataElement bandNoise = noiseElem.getElement(annotation);
        final MetadataElement noise = bandNoise.getElement("noise");
        final MetadataElement noiseVectorListElem = noise.getElement("noiseVectorList");

        return getNoiseVector(noiseVectorListElem);
    }

    public static NoiseVector[] getNoiseVector(final MetadataElement noiseVectorListElem) {

        final MetadataElement[] list = noiseVectorListElem.getElements();

        final List<NoiseVector> noiseVectorList = new ArrayList<NoiseVector>(5);
        for (MetadataElement noiseVectorElem : list) {
            final ProductData.UTC time = getTime(noiseVectorElem, "azimuthTime");
            final int line = Integer.parseInt(noiseVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = noiseVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final MetadataElement noiseLutElem = noiseVectorElem.getElement("noiseLut");
            final String noiseLUT = noiseLutElem.getAttributeString("noiseLut");

            final int[] pixelArray = new int[count];
            final float[] noiseLUTArray = new float[count];
            addToArray(pixelArray, 0, pixel, " ");
            addToArray(noiseLUTArray, 0, noiseLUT, " ");

            noiseVectorList.add(new NoiseVector(time, line, pixelArray, noiseLUTArray));
        }
        return noiseVectorList.toArray(new NoiseVector[noiseVectorList.size()]);
    }

    public static CalibrationVector[] getCalibrationVector(final MetadataElement calibrationVectorListElem,
                                                           final boolean outputSigmaBand,
                                                           final boolean outputBetaBand,
                                                           final boolean outputGammaBand,
                                                           final boolean outputDNBand) {

        final MetadataElement[] list = calibrationVectorListElem.getElements();

        final List<CalibrationVector> calibrationVectorList = new ArrayList<CalibrationVector>(5);
        for (MetadataElement calibrationVectorElem : list) {
            final ProductData.UTC time = getTime(calibrationVectorElem, "azimuthTime");
            final int line = Integer.parseInt(calibrationVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = calibrationVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final int[] pixelArray = new int[count];
            addToArray(pixelArray, 0, pixel, " ");

            float[] sigmaNoughtArray = null;
            if (outputSigmaBand) {
                final MetadataElement sigmaNoughtElem = calibrationVectorElem.getElement("sigmaNought");
                final String sigmaNought = sigmaNoughtElem.getAttributeString("sigmaNought");
                sigmaNoughtArray = new float[count];
                addToArray(sigmaNoughtArray, 0, sigmaNought, " ");
            }

            float[] betaNoughtArray = null;
            if (outputBetaBand) {
                final MetadataElement betaNoughtElem = calibrationVectorElem.getElement("betaNought");
                final String betaNought = betaNoughtElem.getAttributeString("betaNought");
                betaNoughtArray = new float[count];
                addToArray(betaNoughtArray, 0, betaNought, " ");
            }

            float[] gammaArray = null;
            if (outputGammaBand) {
                final MetadataElement gammaElem = calibrationVectorElem.getElement("gamma");
                final String gamma = gammaElem.getAttributeString("gamma");
                gammaArray = new float[count];
                addToArray(gammaArray, 0, gamma, " ");
            }

            float[] dnArray = null;
            if (outputDNBand) {
                final MetadataElement dnElem = calibrationVectorElem.getElement("dn");
                final String dn = dnElem.getAttributeString("dn");
                dnArray = new float[count];
                addToArray(dnArray, 0, dn, " ");
            }

            calibrationVectorList.add(new CalibrationVector(
                    time, line, pixelArray, sigmaNoughtArray, betaNoughtArray, gammaArray, dnArray));
        }
        return calibrationVectorList.toArray(new CalibrationVector[calibrationVectorList.size()]);
    }

    private static int addToArray(final int[] array, int index, final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        while (tokenizer.hasMoreTokens()) {
            array[index++] = Integer.parseInt(tokenizer.nextToken());
        }
        return index;
    }

    private static int addToArray(final float[] array, int index, final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        while (tokenizer.hasMoreTokens()) {
            array[index++] = Float.parseFloat(tokenizer.nextToken());
        }
        return index;
    }

    public static class NoiseVector {
        public final double timeMJD;
        public final int line;
        public final int[] pixels;
        public final float[] noiseLUT;

        public NoiseVector(final ProductData.UTC time, final int line, final int[] pixels, final float[] noiseLUT) {
            this.timeMJD = time.getMJD();
            this.line = line;
            this.pixels = pixels;
            this.noiseLUT = noiseLUT;
        }
    }

    public static class CalibrationVector {
        public final double timeMJD;
        public final int line;
        public final int[] pixels;
        public final float[] sigmaNought;
        public final float[] betaNought;
        public final float[] gamma;
        public final float[] dn;

        public CalibrationVector(final ProductData.UTC time,
                                 final int line,
                                 final int[] pixels,
                                 final float[] sigmaNought,
                                 final float[] betaNought,
                                 final float[] gamma,
                                 final float[] dn) {
            this.timeMJD = time.getMJD();
            this.line = line;
            this.pixels = pixels;
            this.sigmaNought = sigmaNought;
            this.betaNought = betaNought;
            this.gamma = gamma;
            this.dn = dn;
        }
    }
}
