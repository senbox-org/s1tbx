/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.transform;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.TransformException;

/**
 * A math transform which converts from grid (pixel) coordinates to geographical coordinates.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class GeoCodingMathTransform extends AbstractMathTransform implements MathTransform2D {

    private static final TG2P G2P = new TG2P();
    private static final TP2G P2G = new TP2G();
    private static final int DIMS = 2;

    private final GeoCoding geoCoding;
    private final T t;


    public GeoCodingMathTransform(GeoCoding geoCoding) {
        this(geoCoding, G2P);
    }

    private GeoCodingMathTransform(GeoCoding geoCoding, T t) {
        this.geoCoding = geoCoding;
        this.t = t;
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return new DefaultParameterDescriptorGroup(getClass().getSimpleName(), new GeneralParameterDescriptor[0]);
    }

    @Override
    public int getSourceDimensions() {
        return DIMS;
    }

    @Override
    public int getTargetDimensions() {
        return DIMS;
    }

    @Override
    public MathTransform2D inverse() {
        return new GeoCodingMathTransform(geoCoding, t == G2P ? P2G : G2P);
    }

    @Override
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) throws TransformException {
        t.transform(geoCoding, srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        GeoCodingMathTransform that = (GeoCodingMathTransform) o;
        if (t != that.t) {
            return false;
        }

        return geoCoding == that.geoCoding;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + geoCoding.hashCode();
        result = 31 * result + t.hashCode();
        return result;
    }

    private interface T {

        void transform(GeoCoding geoCoding, double[] srcPts, int srcOff,
                       double[] dstPts, int dstOff,
                       int numPts) throws TransformException;
    }

    private static class TP2G implements T {

        @Override
        public void transform(GeoCoding geoCoding,
                              double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            try {
                GeoPos geoPos = new GeoPos();
                PixelPos pixelPos = new PixelPos();
                for (int i = 0; i < numPts; i++) {
                    final int firstIndex = (DIMS * i);
                    final int secondIndex = firstIndex + 1;
                    pixelPos.x = srcPts[srcOff + firstIndex];
                    pixelPos.y = srcPts[srcOff + secondIndex];

                    geoCoding.getGeoPos(pixelPos, geoPos);

                    dstPts[dstOff + firstIndex] = geoPos.lon;
                    dstPts[dstOff + secondIndex] = geoPos.lat;
                }
            } catch (Exception e) {
                TransformException transformException = new TransformException();
                transformException.initCause(e);
                throw transformException;
            }
        }
    }

    private static class TG2P implements T {

        @Override
        public void transform(GeoCoding geoCoding,
                              double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            try {
                GeoPos geoPos = new GeoPos();
                PixelPos pixelPos = new PixelPos();
                for (int i = 0; i < numPts; i++) {
                    final int firstIndex = (DIMS * i);
                    final int secondIndex = firstIndex + 1;
                    geoPos.lon = srcPts[srcOff + firstIndex];
                    geoPos.lat = srcPts[srcOff + secondIndex];

                    geoCoding.getPixelPos(geoPos, pixelPos);

                    dstPts[dstOff + firstIndex] = pixelPos.x;
                    dstPts[dstOff + secondIndex] = pixelPos.y;
                }
            } catch (Exception e) {
                final TransformException transformException = new TransformException();
                transformException.initCause(e);
                throw transformException;
            }
        }
    }
}
