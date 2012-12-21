/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.nest.dataio.sentinel1.Sentinel1Constants;
import org.esa.nest.datamodel.AbstractMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class Sentinel1Utils
{

    private Sentinel1Utils()
    {
    }

    public static ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, Sentinel1Constants.sentinelDateFormat);
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

    public static NoiseVector[] getNoiseVector(final Band band) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(band.getProduct());
        final MetadataElement bandAbsMetadata = AbstractMetadata.getBandAbsMetadata(absRoot, band);
        final String annotation = bandAbsMetadata.getAttributeString(AbstractMetadata.annotation);
        final MetadataElement origMetadata = AbstractMetadata.getOriginalProductMetadata(band.getProduct());

        final MetadataElement noiseElem = origMetadata.getElement("noise");
        final MetadataElement bandNoise = noiseElem.getElement(annotation);
        final MetadataElement noise = bandNoise.getElement("noise");
        final MetadataElement noiseVectorListElem = noise.getElement("noiseVectorList");
        final MetadataElement[] list = noiseVectorListElem.getElements();

        final List<NoiseVector> noiseVectorList = new ArrayList<NoiseVector>(5);
        for(MetadataElement noiseVectorElem : list) {
            final ProductData.UTC time = getTime(noiseVectorElem, "azimuthTime");
            final int line = noiseVectorElem.getAttributeInt("line");

            final MetadataElement pixelElem = noiseVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = pixelElem.getAttributeInt("count");
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
        public final ProductData.UTC time;
        public final int line;
        public final int[] pixels;
        public final float[] noiseLUT;

        public NoiseVector(final ProductData.UTC time, final int line, final int[] pixels, final float[] noiseLUT) {
            this.time = time;
            this.line = line;
            this.pixels = pixels;
            this.noiseLUT = noiseLUT;
        }
    }
}
