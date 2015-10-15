/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.internal;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.Range;

/**
 * Factory class, which is responsible for creating an instance of {@link SampleConverterFactory.SampleConverter}
 * depending on the given {@link RasterDataNode}. The returned instance is optimised for the data type of the given
 * {@link RasterDataNode}.
 *
 * @author Marco Peters
 * @since BEAM 4.9.1
 */
class SampleConverterFactory {


    private SampleConverterFactory() {
    }

    public static SampleConverter createConverter(RasterDataNode rasterDataNode) {

        boolean scaled = rasterDataNode.isScalingApplied();
        boolean signedByte = rasterDataNode.getDataType() == ProductData.TYPE_INT8;
        if (!signedByte && !scaled) {
            return new IdentityConverter(rasterDataNode);
        }
        if (signedByte) {
            if (scaled) {
                return new ScaledSignedByteConverter(rasterDataNode);
            } else {
                return new UnscaledSignedByteConverter(rasterDataNode);
            }
        }
        return new DefaultScaledConverter(rasterDataNode);
    }

    public static class DefaultScaledConverter extends SampleConverter {

        protected DefaultScaledConverter(RasterDataNode rasterDataNode) {
            super(rasterDataNode);
        }

        @Override
        public double toGeoPhysical(double rawSample) {
            double geoSample = rawSample;

            geoSample = scaleToGeoPhysical(geoSample);
            geoSample = cropToGeoPhysicalValueRange(geoSample);

            return geoSample;
        }

        @Override
        public double toRaw(double geoPhysicalSample) {
            double rawSample = geoPhysicalSample;
            rawSample = scaleToRaw(rawSample);
            rawSample = cropToRawValueRange(rawSample);

            return rawSample;
        }

    }

    public static class ScaledSignedByteConverter extends SampleConverter {

        protected ScaledSignedByteConverter(RasterDataNode rasterDataNode) {
            super(rasterDataNode);
        }

        @Override
        public double toGeoPhysical(double rawSample) {
            double geoSample = rawSample;
            //noinspection SillyAssignment
            geoSample = (byte) geoSample;
            geoSample = scaleToGeoPhysical(geoSample);
            geoSample = cropToGeoPhysicalValueRange(geoSample);

            return geoSample;
        }

        @Override
        public double toRaw(double geoPhysicalSample) {
            double rawSample = geoPhysicalSample;

            rawSample = scaleToRaw(rawSample);
            rawSample = cropToRawValueRange(rawSample);

            return rawSample;
        }

    }

    public static class UnscaledSignedByteConverter extends SampleConverter {

        protected UnscaledSignedByteConverter(RasterDataNode rasterDataNode) {
            super(rasterDataNode);
        }

        @Override
        public double toGeoPhysical(double rawSample) {
            double geoSample = rawSample;
            //noinspection SillyAssignment
            geoSample = (byte) geoSample;
            return geoSample;
        }

        @Override
        public double toRaw(double geoPhysicalSample) {
            double rawSample = geoPhysicalSample;
            rawSample = cropToRawValueRange(rawSample);

            return rawSample;
        }

    }


    private static class IdentityConverter extends SampleConverter {

        private IdentityConverter(RasterDataNode rasterDataNode) {
            super(rasterDataNode);
        }

        @Override
        public double toGeoPhysical(double rawSample) {
            return rawSample;
        }

        @Override
        public double toRaw(double geoPhysicalSample) {
            return cropToRawValueRange(geoPhysicalSample);
        }
    }

    /**
     * Abstract base class for the implementations considering the different data types.
     *
     * @author Marco Peters
     */
    public abstract static class SampleConverter {

        private RasterDataNode rasterDataNode;
        private Range rawValueRange;
        private Range geoPhysicalValueRange;

        protected SampleConverter(RasterDataNode rasterDataNode) {
            this.rasterDataNode = rasterDataNode;
            rawValueRange = getValueRange(rasterDataNode.getDataType());
            geoPhysicalValueRange = new Range(rasterDataNode.scale(rawValueRange.getMin()),
                                              rasterDataNode.scale(rawValueRange.getMax()));
        }

        /**
         * Converts the given raw sample value (e.g. digital counts) to a (geo-)physically scaled sample value.
         *
         * @param rawSample The raw sample value.
         *
         * @return The calibrated sample value.
         */
        public abstract double toGeoPhysical(double rawSample);

        /**
         * Converts the  (geo-)physically scaled sample value to a its corresponding raw sample value (e.g. digital counts).
         * This method prevents an overflow of the returned raw value.
         *
         * @param geoPhysicalSample The calibrated sample value.
         *
         * @return The raw sample value.
         */
        public abstract double toRaw(double geoPhysicalSample);

        protected double scaleToRaw(double sample) {
            return rasterDataNode.scaleInverse(sample);
        }

        protected double scaleToGeoPhysical(double sample) {
            return rasterDataNode.scale(sample);
        }

        protected double cropToRawValueRange(double sample) {
            return MathUtils.crop(sample, rawValueRange.getMin(), rawValueRange.getMax());
        }

        protected double cropToGeoPhysicalValueRange(double sample) {
            return MathUtils.crop(sample, geoPhysicalValueRange.getMin(), geoPhysicalValueRange.getMax());
        }

        private static Range getValueRange(int dataType) {
            Range range = new Range();
            switch (dataType) {
                case ProductData.TYPE_INT8:
                    range.setMinMax(Byte.MIN_VALUE, Byte.MAX_VALUE);
                    break;
                case ProductData.TYPE_INT16:
                    range.setMinMax(Short.MIN_VALUE, Short.MAX_VALUE);
                    break;
                case ProductData.TYPE_INT32:
                    range.setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    break;
                case ProductData.TYPE_UINT8:
                    range.setMinMax(0, Math.pow(2, 8) - 1);
                    break;
                case ProductData.TYPE_UINT16:
                    range.setMinMax(0, Math.pow(2, 16) - 1);
                    break;
                case ProductData.TYPE_UINT32:
                    range.setMinMax(0, Math.pow(2, 32) - 1);
                    break;
                case ProductData.TYPE_FLOAT32:
                    range.setMinMax(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
                    break;
                case ProductData.TYPE_FLOAT64:
                    range.setMinMax(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Can not retrieve value range for given type or type is unknown: %d",
                                          dataType));
            }
            return range;
        }

    }
}
