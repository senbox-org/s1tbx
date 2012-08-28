package org.esa.beam.binning;

import org.esa.beam.binning.support.ObservationImpl;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

import java.awt.image.Raster;
import java.util.Iterator;

/**
 * @author Marco Peters
 */
public class ObservationSlice2 extends ObservationSlice {

    private Raster maskTile;
    private GeoCoding gc;
    private int size;

    public ObservationSlice2(Raster[] sourceTiles, Raster maskTile, GeoCoding gc, int observationCapacity) {
        super(sourceTiles, observationCapacity);
        this.maskTile = maskTile;
        this.gc = gc;
        size = observationCapacity;
    }

    @Override
    public float[] createObservationSamples(int x, int y) {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public void addObservation(double lat, double lon, float[] samples) {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public Iterator<Observation> iterator() {
        return new ObservationIterator();
    }

    @Override
    public int getSize() {
        return size;
    }

    private class ObservationIterator implements Iterator<Observation> {

        private final int y1;
        private final int y2;
        private final int x1;
        private final int x2;
        private long counter;
        private int x;
        private int y;


        private ObservationIterator() {
            this.counter = 0;
            y1 = maskTile.getMinY();
            y2 = y1 + maskTile.getHeight();
            x1 = maskTile.getMinX();
            x2 = x1 + maskTile.getWidth();
            x = x1;
            y = y1;
        }

        @Override
        public boolean hasNext() {
            return counter < size;
        }

        @Override
        public Observation next() {
            counter++;
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();
            if (x == x2) {
                x = x1;
                y++;
            }
            ObservationImpl observation = null;
            if (maskTile.getSample(x, y, 0) != 0) {
                final float[] samples = ObservationSlice2.super.createObservationSamples(x, y);
                pixelPos.setLocation(x + 0.5f, y + 0.5f);
                gc.getGeoPos(pixelPos, geoPos);
                observation = new ObservationImpl(geoPos.lat, geoPos.lon, samples);
                // todo - consider super sampling
//                for (float dy : superSamplingSteps) {
//                    for (float dx : superSamplingSteps) {
//                        pixelPos.setLocation(x + dx, y + dy);
//                        geoCoding.getGeoPos(pixelPos, geoPos);
//                        observationSlice.addObservation(geoPos.lat, geoPos.lon, samples);
//                    }
//                }

            }
            x++;
            return observation;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removing of elements is not allowed");
        }
    }
}
