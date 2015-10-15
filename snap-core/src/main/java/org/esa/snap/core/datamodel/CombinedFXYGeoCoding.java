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
package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.Guardian;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * A geo-coding based on a combination of {@link GeoCoding GeoCodings}.
 * All the geocodings added must be wrapped in a {@link CombinedFXYGeoCoding.CodingWrapper CodingWrapper}
 * which describes the area in scene coordinates to which the geocoding is concerned.
 *
 * @author Sabine Embacher
 */
public class CombinedFXYGeoCoding extends AbstractGeoCoding {

    private CodingWrapper[] _codingWrappers;
    private final Datum _datum;
    private int _lastIndex;

    public CombinedFXYGeoCoding(final CodingWrapper[] codingWrappers) {
        Guardian.assertNotNull("codingWrappers", codingWrappers);
        final ArrayList<CodingWrapper> wrappers = new ArrayList<CodingWrapper>();
        for (int i = 0; i < codingWrappers.length; i++) {
            final CodingWrapper codingWrapper = codingWrappers[i];
            if (codingWrapper != null) {
                wrappers.add(codingWrapper);
            }
        }
        Guardian.assertGreaterThan("number of coding wrappers", wrappers.size(), 0);
        _codingWrappers = wrappers.toArray(new CodingWrapper[wrappers.size()]);
        _datum = _codingWrappers[0].getGeoGoding().getDatum();
        _lastIndex = 0;
    }

    /**
     * Returns the {@link CodingWrapper CodingWrappers} this {@link GeoCoding GeoCoding} consists of.
     *
     * @return array of {@link CodingWrapper CodingWrappers}.
     */
    public CodingWrapper[] getCodingWrappers() {
        return _codingWrappers;
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene,
                                     final Scene destScene,
                                     final ProductSubsetDef subsetDef) {
        final CodingWrapper[] wrappers = new CodingWrapper[_codingWrappers.length];
        int ccdCarryover = 0;
        for (int i = 0; i < _codingWrappers.length; i++) {
            final CodingWrapper wrapper = _codingWrappers[i];
            final Point location = wrapper.getLocation();
            final Dimension dimension = wrapper.getDimension();
            final FXYGeoCoding sourceGC = wrapper.getGeoGoding();

            float gcPixelOffsetX = sourceGC.getPixelOffsetX();
            float gcPixelOffsetY = sourceGC.getPixelOffsetY();
            float gcPixelSizeX = sourceGC.getPixelSizeX();
            float gcPixelSizeY = sourceGC.getPixelSizeY();
            int gcwPosX = location.x;
            int gcwPosXSrc = 0;
            int gcwPosY = location.y;
            int gcwWidth = dimension.width;
            int gcwWidthSrc = 0;
            int gcwHeight = dimension.height;

            if (subsetDef != null) {

                final Rectangle subsetRegion;
                // get region if set, otherwise set it to full size
                if (subsetDef.getRegion() != null) {
                    subsetRegion = subsetDef.getRegion();
                } else {
                    subsetRegion = new Rectangle(0, 0, srcScene.getRasterWidth(), srcScene.getRasterHeight());
                }

                if (!wrapper.intersects(subsetRegion)) {
                    wrappers[i] = null;
                    continue;
                }

                if (gcwPosX < subsetRegion.x) {
                    gcPixelOffsetX += (subsetRegion.x - gcwPosX) * gcPixelSizeX;
                }
                gcPixelOffsetX += ccdCarryover * gcPixelSizeX;
                if (gcwPosY < subsetRegion.y) {
                    gcPixelOffsetY += (subsetRegion.y - gcwPosY) * gcPixelSizeY;
                }

                final Rectangle intersection = wrapper.intersection(subsetRegion);
                intersection.x += ccdCarryover;
                intersection.width -= ccdCarryover;

                gcwPosXSrc = intersection.x - subsetRegion.x;
                gcwPosY = intersection.y - subsetRegion.y;
                gcwWidthSrc = intersection.width;
                gcwHeight = intersection.height;

                final int subSamplingX = subsetDef.getSubSamplingX();
                final int subSamplingY = subsetDef.getSubSamplingY();

                if (subSamplingX != 1) {
                    ccdCarryover = subSamplingX - (gcwWidthSrc % subSamplingX);
                }


                gcPixelSizeX *= subSamplingX;
                gcPixelSizeY *= subSamplingY;

                gcwPosX = gcwPosXSrc / subSamplingX;
                gcwPosY /= subSamplingY;
                gcwWidth = (gcwWidthSrc - 1) / subSamplingX + 1;
                gcwHeight = (gcwHeight - 1) / subSamplingY + 1;
            }

            final FXYGeoCoding gc = sourceGC.createCloneWithNewOffsetAndSize(gcPixelOffsetX,
                                                                             gcPixelOffsetY,
                                                                             gcPixelSizeX,
                                                                             gcPixelSizeY);
            wrappers[i] = new CodingWrapper(gc, gcwPosX, gcwPosY, gcwWidth, gcwHeight);
        }
        destScene.setGeoCoding(new CombinedFXYGeoCoding(wrappers));
        return true;
    }

    public boolean isCrossingMeridianAt180() {
        for (int i = 0; i < _codingWrappers.length; i++) {
            final CodingWrapper codingWrapper = _codingWrappers[i];
            if (codingWrapper.getGeoGoding().isCrossingMeridianAt180()) {
                return true;
            }
        }
        return false;
    }

    public boolean canGetPixelPos() {
        return true;
    }

    public boolean canGetGeoPos() {
        return true;
    }

    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        synchronized (this) {
            int index = _lastIndex;
            do {
                final CodingWrapper wrapper = _codingWrappers[index];
                pixelPos = wrapper.getPixelPos(geoPos, pixelPos);
                if (wrapper.contains(pixelPos)) {
                    _lastIndex = index;
                    break;
                }
                index++;
                if (index == _codingWrappers.length) {
                    index = 0;
                }
            } while (index != _lastIndex);
            return pixelPos;
        }
    }

    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        for (int i = 0; i < _codingWrappers.length; i++) {
            final CodingWrapper codingWrapper = _codingWrappers[i];
            if (codingWrapper.contains(pixelPos)) {
                return codingWrapper.getGeoPos(pixelPos, geoPos);
            }
        }
        if (geoPos == null) {
            geoPos = new GeoPos(0, 0);
        }
        return geoPos;
    }

    public Datum getDatum() {
        return _datum;
    }

    public void dispose() {
        for (int i = 0; i < _codingWrappers.length; i++) {
            _codingWrappers[i].dispose();
            _codingWrappers[i] = null;
        }
        _codingWrappers = null;
    }

    public static class CodingWrapper {

        private FXYGeoCoding _geoGoding;
        private Rectangle _region;
        private Point _location;
        private Dimension _dimension;

        public CodingWrapper(final FXYGeoCoding geoCoding, final int x, final int y, final int width,
                             final int height) {
            _geoGoding = geoCoding;
            _location = new Point(x, y);
            _dimension = new Dimension(width, height);
            _region = new Rectangle(_location, _dimension);
        }

        public FXYGeoCoding getGeoGoding() {
            return _geoGoding;
        }

        private Point getLocation() {
            return _location;
        }

        private Dimension getDimension() {
            return _dimension;
        }

        public Rectangle getRegion() {
            return _region;
        }

        private boolean contains(final PixelPos pixelPos) {
            return _region.contains(pixelPos);
        }

        private void dispose() {
            _geoGoding.dispose();
            _geoGoding = null;
            _region = null;
            _dimension = null;
            _location = null;
        }

        private PixelPos getPixelPos(final GeoPos geoPos, final PixelPos pixelPos) {
            final PixelPos pos = _geoGoding.getPixelPos(geoPos, pixelPos);
            pos.x += _location.x;
            pos.y += _location.y;
            return pos;
        }

        private GeoPos getGeoPos(final PixelPos pixelPos, final GeoPos geoPos) {
            final PixelPos localPixelPos = new PixelPos();
            localPixelPos.setLocation(pixelPos.getX() - _location.x,
                                      pixelPos.getY() - _location.y);
            return _geoGoding.getGeoPos(localPixelPos, geoPos);
        }

        public boolean intersects(final Rectangle rectangle) {
            return _region.intersects(rectangle);
        }

        public Rectangle intersection(final Rectangle rectangle) {
            return _region.intersection(rectangle);
        }
    }
}
