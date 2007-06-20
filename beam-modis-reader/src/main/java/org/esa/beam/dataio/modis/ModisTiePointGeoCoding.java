/*
 * $Id: ModisTiePointGeoCoding.java,v 1.2 2006/12/08 13:48:35 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.modis;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.geom.PolyLine;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ModisTiePointGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public class ModisTiePointGeoCoding extends AbstractGeoCoding {

    private final Datum _datum;
    private TiePointGrid _latGrid;
    private TiePointGrid _lonGrid;
    private List _tpgcList;
    private boolean _cross180;
    private List _centerLineList;
    private int _lastCenterLineIndex;
    private Area _generalArea;
    private int _smallestValidIndex;
    private int _biggestValidIndex;
    private final Integer _NULL;

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latGrid the latitude grid, must not be <code>null</code>
     * @param lonGrid the longitude grid, must not be <code>null</code>
     */
    public ModisTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        this(latGrid, lonGrid, Datum.WGS_84); // todo  - check datum, is it really WGS84 for MODIS?
    }

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latGrid the latitude grid, must not be <code>null</code>
     * @param lonGrid the longitude grid, must not be <code>null</code>
     */
    public ModisTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, final Datum datum) {
        Guardian.assertNotNull("latGrid", latGrid);
        Guardian.assertNotNull("lonGrid", lonGrid);
        Guardian.assertNotNull("datum", datum);
        if (latGrid.getRasterWidth() != lonGrid.getRasterWidth() ||
            latGrid.getRasterHeight() != lonGrid.getRasterHeight() ||
            latGrid.getOffsetX() != lonGrid.getOffsetX() ||
            latGrid.getOffsetY() != lonGrid.getOffsetY() ||
            latGrid.getSubSamplingX() != lonGrid.getSubSamplingX() ||
            latGrid.getSubSamplingY() != lonGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("latGrid is not compatible with lonGrid");
        }
        _latGrid = latGrid;
        _lonGrid = lonGrid;
        _datum = datum;
        _lastCenterLineIndex = 0;
        _NULL = 0;
        init();
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon in the coodinate system determined by {@link #getDatum()}
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.x = -1;
        pixelPos.y = -1;

        if (_generalArea == null) {
            initGeneralArea();
        }
        if (!_generalArea.contains(geoPos.lon, geoPos.lat)) {
            return pixelPos;
        }

        final int index = getGeoCodingIndexfor(geoPos);
        _lastCenterLineIndex = index;
        final Object gc = _tpgcList.get(index);
        if (gc instanceof TiePointGeoCoding) {
            final TiePointGeoCoding geoCoding = (TiePointGeoCoding) gc;
            geoCoding.getPixelPos(geoPos, pixelPos);
        }

        if (pixelPos.x == -1 || pixelPos.y == -1) {
            return pixelPos;
        }
        pixelPos.y += (_lastCenterLineIndex * 10);
        return pixelPos;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the geographical position as lat/lon in the coodinate system determined by {@link #getDatum()}
     */
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final int index = computeIndex(pixelPos);
        final Object gc = _tpgcList.get(index);
        if (gc instanceof TiePointGeoCoding) {
            return ((TiePointGeoCoding) gc).getGeoPos(new PixelPos(pixelPos.x, pixelPos.y - 10 * index), geoPos);
        } else {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.setInvalid();
            return geoPos;
        }
    }

    /**
     * Gets the datum, the reference point or surface against which {@link org.esa.beam.framework.datamodel.GeoPos} measurements are made.
     *
     * @return the datum
     */
    public Datum getDatum() {
        return _datum;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    public void dispose() {
        for (int i = 0; i < _tpgcList.size(); i++) {
            final Object o = _tpgcList.get(i);
            if (!_NULL.equals(o)) {
                final TiePointGeoCoding geoCoding = (TiePointGeoCoding) o;
                geoCoding.dispose();
            }
        }
        _tpgcList.clear();
        _latGrid = null;
        _lonGrid = null;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    public boolean isCrossingMeridianAt180() {
        return _cross180;
    }

    private void init() {
        _cross180 = false;
        _tpgcList = new ArrayList();
        _centerLineList = new ArrayList();
        final float osX = _lonGrid.getOffsetX();
        final float osY = _lonGrid.getOffsetY() + 0.5f;
        final float ssX = _lonGrid.getSubSamplingX();
        final float ssY = _lonGrid.getSubSamplingY();

        final float[] latFloats = (float[]) _latGrid.getDataElems();
        final float[] lonFloats = (float[]) _lonGrid.getDataElems();

        final int w = _lonGrid.getRasterWidth();
        final int h = _lonGrid.getRasterHeight();
        final int sceneWidth = _lonGrid.getSceneRasterWidth();
        for (int y = 0; y < h; y += 2) {
            final float[] lats = new float[w * 2];
            final float[] lons = new float[w * 2];
            System.arraycopy(latFloats, y * w, lats, 0, w * 2);
            System.arraycopy(lonFloats, y * w, lons, 0, w * 2);
            final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
            if (range.getMin() < -90) {
                _tpgcList.add(_NULL);
                _centerLineList.add(_NULL);
            } else {
                final TiePointGrid latTPG = new TiePointGrid("lat" + y, w, 2, osX, osY, ssX, ssY, lats);
                final TiePointGrid lonTPG = new TiePointGrid("lon" + y, w, 2, osX, osY, ssX, ssY, lons, true);
                final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latTPG, lonTPG, _datum);
                _cross180 = _cross180 || geoCoding.isCrossingMeridianAt180();
                _tpgcList.add(geoCoding);
                _centerLineList.add(createCenterPolyLine(geoCoding, sceneWidth, 10));
            }
        }
        initSmallestAndLargestValidGeocodingIndices();
    }

    private void initSmallestAndLargestValidGeocodingIndices() {
        for (int i = 0; i < _tpgcList.size(); i++) {
            if (!_NULL.equals(_tpgcList.get(i))) {
                _smallestValidIndex = i;
                break;
            }
        }
        for (int i = _tpgcList.size() - 1; i > 0; i--) {
            if (!_NULL.equals(_tpgcList.get(i))) {
                _biggestValidIndex = i;
                break;
            }
        }
    }

    private void initGeneralArea() {
        final GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(_latGrid.getProduct(), null, 20);
        _generalArea = new Area();
        for (int i = 0; i < geoBoundaryPaths.length; i++) {
            final GeneralPath geoBoundaryPath = geoBoundaryPaths[i];
            _generalArea.add(new Area(geoBoundaryPath));
        }
    }

    private static PolyLine createCenterPolyLine(TiePointGeoCoding geoCoding, final int sceneWidth,
                                                 final int sceneHeight) {

        final double stepX = sceneWidth / 100.0;

        final PixelPos pixelPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final PolyLine polyLine = new PolyLine();

        pixelPos.setLocation(0, sceneHeight / 2f);

        for (pixelPos.x = 0; pixelPos.x < sceneWidth + 0.1; pixelPos.x += stepX) {
            geoCoding.getGeoPos(pixelPos, geoPos);
            if (pixelPos.x == 0) {
                polyLine.moveTo(geoPos.lon, geoPos.lat);
            } else {
                polyLine.lineTo(geoPos.lon, geoPos.lat);
            }
        }

        return polyLine;
    }

    private int computeIndex(PixelPos pixelPos) {
        final int y = (int) pixelPos.getY();
        final int index = y / 10;
        if (index < _smallestValidIndex) {
            return _smallestValidIndex;
        } else if (index > _biggestValidIndex) {
            return _biggestValidIndex;
        } else {
            return index;
        }
    }

    private int getGeoCodingIndexfor(GeoPos geoPos) {
        int index = _lastCenterLineIndex;
        index = getNextCenterLineIndex(index, 1);
        final PolyLine centerLine = (PolyLine) _centerLineList.get(index);
        double v = centerLine.getDistance(geoPos.lon, geoPos.lat);
        int vIndex = index;

        int direction = -1;
        if (index == _smallestValidIndex) {
            direction = +1;
        }
        while (true) {
            index += direction;
            index = getNextCenterLineIndex(index, direction);
            final PolyLine centerLine2 = (PolyLine) _centerLineList.get(index);
            final double v2 = centerLine2.getDistance(geoPos.lon, geoPos.lat);
            if (v2 < v) {
                if (index == _smallestValidIndex || index == _biggestValidIndex) {
                    return index;
                }
                v = v2;
                vIndex = index;
            } else if (direction == -1) {
                index++;
                direction = +1;
                if (index == _biggestValidIndex) {
                    return index;
                }
            } else if (direction == +1) {
                return vIndex;
            }
        }
    }

    private int getNextCenterLineIndex(int index, final int direction) {
        Object o = _centerLineList.get(index);
        while (_NULL.equals(o)) {
            index += direction;
            if (index < 0) {
                index = _centerLineList.size() - 1;
            }
            if (index >= _centerLineList.size()) {
                index = 0;
            }
            o = _centerLineList.get(index);
        }
        return index;
    }

    /**
     * Transfers the geo-coding of the {@link org.esa.beam.framework.datamodel.Scene srcScene} to the {@link org.esa.beam.framework.datamodel.Scene destScene} with respect to the given
     * {@link org.esa.beam.framework.dataio.ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final String latGridName = _latGrid.getName();
        final String lonGridName = _lonGrid.getName();
        final TiePointGrid latGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final TiePointGrid lonGrid = destScene.getProduct().getTiePointGrid(lonGridName);
        if (latGrid != null && lonGrid != null) {
            destScene.setGeoCoding(new ModisTiePointGeoCoding(latGrid, lonGrid, getDatum()));
            return true;
        }
        return false;
    }
}
