/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.esa.beam.binning.support.ObservationImpl;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.Iterator;
import java.util.NoSuchElementException;

// todo - nf20131031 - review with marcop/marcoz - this class should be instantiated using a builder so that we can have flexible parameterisation

/**
 * Abstract implementation of Iterator interface which iterates over {@link org.esa.beam.binning.Observation Observations}.
 * To better support a streaming processing, instances of {@link org.esa.beam.binning.Observation} can be  generated on
 * the fly each time {@link ObservationIterator#next() next()} is called.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
abstract class ObservationIterator implements Iterator<Observation> {

    private Observation next;
    private boolean nextValid;
    private SamplePointer pointer;
    private final GeoCoding gc;
    private final Product product;
    private final boolean productHasTime;
    private final DataPeriod dataPeriod;
    private final PreparedGeometry region;
    private final GeometryFactory geometryFactory;

    static ObservationIterator create(PlanarImage[] sourceImages, PlanarImage maskImage, Product product,
                                      float[] superSamplingSteps, Rectangle sliceRectangle, BinningContext binningContext) {

        SamplePointer pointer;
        if (superSamplingSteps.length == 1) {
            pointer = SamplePointer.create(sourceImages, new Rectangle[]{sliceRectangle});
        } else {
            Point2D.Float[] superSamplingPoints = SamplePointer.createSamplingPoints(superSamplingSteps);
            pointer = SamplePointer.create(sourceImages, new Rectangle[]{sliceRectangle}, superSamplingPoints);
        }
        if (maskImage == null) {
            return new NoMaskObservationIterator(product, pointer, binningContext);
        } else {
            return new FullObservationIterator(product, pointer, maskImage, binningContext);
        }
    }

    private ObservationIterator(Product product, SamplePointer pointer, BinningContext binningContext) {
        this.pointer = pointer;
        this.dataPeriod = binningContext.getDataPeriod();
        Geometry geometryRegion = binningContext.getRegion();
        if (geometryRegion != null) {
            this.region = PreparedGeometryFactory.prepare(binningContext.getRegion());
        } else {
            this.region = null;
        }
        this.product = product;
        this.productHasTime = product.getStartTime() != null || product.getEndTime() != null;
        this.gc = product.getGeoCoding();
        geometryFactory = new GeometryFactory();
    }

    public final SamplePointer getPointer() {
        return pointer;
    }

    @Override
    public final boolean hasNext() {
        ensureValidNext();
        return next != null;
    }

    @Override
    public final Observation next() {
        ensureValidNext();
        if (next == null) {
            throw new NoSuchElementException("EMPTY");
        }
        nextValid = false;
        return next;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException("Removing of elements is not allowed");
    }

    private void ensureValidNext() {
        if (!nextValid) {
            next = getNextObservation();
            nextValid = true;
        }
    }

    protected abstract Observation getNextObservation();

    protected Observation createObservation(int x, int y) {
        final SamplePointer pointer = getPointer();

        final Point2D.Float superSamplingPoint = pointer.getSuperSamplingPoint();
        final PixelPos pixelPos = new PixelPos(x + superSamplingPoint.x, y + superSamplingPoint.y);
        final GeoPos geoPos = getGeoPos(pixelPos);

        if (!acceptGeoPos(geoPos)) {
            return null;
        }

        double mjd = 0.0;
        if (productHasTime) {
            ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, y + 0.5);
            mjd = scanLineTime.getMJD();
            if (dataPeriod != null && dataPeriod.getObservationMembership(geoPos.lon, mjd) != DataPeriod.Membership.CURRENT_PERIOD) {
                return null;
            }
        }

        final float[] samples = pointer.createSamples();
        return new ObservationImpl(geoPos.lat, geoPos.lon, mjd, samples);
    }

    private boolean acceptGeoPos(GeoPos geoPos) {
        return region == null
                || region.contains(geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat)));

    }

    protected GeoPos getGeoPos(PixelPos pixelPos) {
        final GeoPos geoPos = new GeoPos();
        gc.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    static class FullObservationIterator extends ObservationIterator {

        private Raster maskTile;
        private final PlanarImage maskImage;

        FullObservationIterator(Product product, SamplePointer pointer, PlanarImage maskImage, BinningContext binningContext) {
            super(product, pointer, binningContext);
            this.maskImage = maskImage;
        }

        @Override
        protected Observation getNextObservation() {
            SamplePointer pointer = getPointer();
            while (pointer.canMove()) {
                pointer.move();
                if (isSampleValid(pointer.getX(), pointer.getY())) {
                    Observation observation = createObservation(pointer.getX(), pointer.getY());
                    if (observation != null) {
                        return observation;
                    }
                }
            }
            return null;
        }

        private boolean isSampleValid(int x, int y) {
            if (maskTile == null || !maskTile.getBounds().contains(x, y)) {
                int tileX = maskImage.XToTileX(x);
                int tileY = maskImage.YToTileY(y);
                maskTile = maskImage.getTile(tileX, tileY);
            }

            return maskTile.getSample(x, y, 0) != 0;
        }

    }

    static class NoMaskObservationIterator extends ObservationIterator {


        NoMaskObservationIterator(Product product, SamplePointer pointer, BinningContext binningContext) {
            super(product, pointer, binningContext);
        }

        @Override
        protected Observation getNextObservation() {
            SamplePointer pointer = getPointer();
            while (pointer.canMove()) {
                pointer.move();
                Observation observation = createObservation(pointer.getX(), pointer.getY());
                if (observation != null) {
                    return observation;
                }
            }
            return null;
        }
    }
}
