/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.snap.binning;

import com.bc.ceres.core.Assert;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.geotools.geometry.jts.JTS;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used to re-project temporal bins onto a rectangular grid.
 * Uses a {@link TemporalBinRenderer} to convert subsequent collections of bins (parts) into raster data.
 *
 * @author Marco Zühlke
 * @author Norman Fomferra
 */
public class Reprojector {

    private final PlanetaryGrid planetaryGrid;
    private final TemporalBinRenderer temporalBinRenderer;
    private final Rectangle rasterRegion;
    private int yGlobalUltimate;

    public static void reproject(PlanetaryGrid planetaryGrid,
                                 TemporalBinSource temporalBinSource,
                                 TemporalBinRenderer temporalBinRenderer) throws Exception {
        Reprojector reprojector = new Reprojector(planetaryGrid, temporalBinRenderer);
        final int partCount = temporalBinSource.open();
        reprojector.begin();
        for (int i = 0; i < partCount; i++) {
            final Iterator<? extends TemporalBin> part = temporalBinSource.getPart(i);
            reprojector.processPart(part);
            temporalBinSource.partProcessed(i, part);
        }
        reprojector.end();
        temporalBinSource.close();
    }

    Reprojector(PlanetaryGrid planetaryGrid, TemporalBinRenderer temporalBinRenderer) {
        Assert.notNull(planetaryGrid, "planetaryGrid");
        Assert.notNull(temporalBinRenderer, "binRenderer");
        this.planetaryGrid = planetaryGrid;
        this.temporalBinRenderer = temporalBinRenderer;
        this.rasterRegion = temporalBinRenderer.getRasterRegion();
    }

    /**
     * Computes the sub-region in pixel coordinates of a raster that fully covers the given binning grid for the given
     * region of interest in geo-graphical coordinates.
     *
     * @param planetaryGrid The binning grid.
     * @param roiGeometry   The region of interest in geo-graphical coordinates.
     * @return The sub-region in pixel coordinates.
     */
    public static Rectangle computeRasterSubRegion(PlanetaryGrid planetaryGrid, Geometry roiGeometry) {
        final double pixelSize = getRasterPixelSize(planetaryGrid);
        final int gridHeight = planetaryGrid.getNumRows();
        final int gridWidth = 2 * gridHeight;
        Rectangle outputRegion = new Rectangle(gridWidth, gridHeight);
        if (roiGeometry != null) {
            final Coordinate[] coordinates = getBoundsCoordinates(roiGeometry);

            double gxmin = Double.POSITIVE_INFINITY;
            double gxmax = Double.NEGATIVE_INFINITY;
            double gymin = Double.POSITIVE_INFINITY;
            double gymax = Double.NEGATIVE_INFINITY;
            for (Coordinate coordinate : coordinates) {
                gxmin = Math.min(gxmin, coordinate.x);
                gxmax = Math.max(gxmax, coordinate.x);
                gymin = Math.min(gymin, coordinate.y);
                gymax = Math.max(gymax, coordinate.y);
            }
            final int x = (int) Math.floor((180.0 + gxmin) / pixelSize);
            final int y = (int) Math.floor((90.0 - gymax) / pixelSize);
            final int width = (int) Math.ceil((gxmax - gxmin) / pixelSize);
            final int height = (int) Math.ceil((gymax - gymin) / pixelSize);
            final Rectangle unclippedOutputRegion = new Rectangle(x, y, width, height);
            outputRegion = unclippedOutputRegion.intersection(outputRegion);
        }
        return outputRegion;
    }

    private static Coordinate[] getBoundsCoordinates(Geometry roiGeometry) {
        // do not use ShapeWriter.toShape(Geometry) here, because it rounds
        GeneralPath shape = new GeneralPath();
        shape.moveTo((float) roiGeometry.getCoordinates()[0].x, (float) roiGeometry.getCoordinates()[0].y);

        for (int i = 1; i < roiGeometry.getNumPoints(); i++) {
            shape.lineTo((float) roiGeometry.getCoordinates()[i].x, (float) roiGeometry.getCoordinates()[i].y);
        }

        roiGeometry = JTS.shapeToGeometry(shape.getBounds2D(), new GeometryFactory());
        return roiGeometry.getCoordinates();
    }

    /**
     * @param planetaryGrid The planetary grid used for the binning.
     * @return The pixel size in degree of a raster resulting from the given {@code planetaryGrid}.
     */
    public static double getRasterPixelSize(PlanetaryGrid planetaryGrid) {
        return 180.0 / planetaryGrid.getNumRows();
    }

    void begin() throws Exception {
        yGlobalUltimate = rasterRegion.y - 1;
        temporalBinRenderer.begin();
    }

    void end() throws Exception {
        final int x1 = rasterRegion.x;
        final int x2 = x1 + rasterRegion.width - 1;
        final int y1 = rasterRegion.y;
        final int y2 = y1 + rasterRegion.height - 1;
        processRowsWithoutBins(x1, x2, yGlobalUltimate + 1, y2);
        temporalBinRenderer.end();
    }

    void processPart(Iterator<? extends TemporalBin> temporalBins) throws Exception {
        final int x1 = rasterRegion.x;
        final int x2 = x1 + rasterRegion.width - 1;
        final int y1 = rasterRegion.y;
        final int y2 = y1 + rasterRegion.height - 1;
        final int gridWidth = planetaryGrid.getNumRows() * 2;
        final int gridHeight = planetaryGrid.getNumRows();

        final List<TemporalBin> binRow = new ArrayList<TemporalBin>();
        int yUltimate = -1;
        while (temporalBins.hasNext()) {
            TemporalBin temporalBin = temporalBins.next();
            long temporalBinIndex = temporalBin.getIndex();
            int y = planetaryGrid.getRowIndex(temporalBinIndex);
            if (y != yUltimate) {
                if (yUltimate >= y1 && yUltimate <= y2) {
                    processRowsWithoutBins(x1, x2, yGlobalUltimate + 1, yUltimate - 1);
                    processRowWithBins(yUltimate, binRow, gridWidth, gridHeight);
                    yGlobalUltimate = yUltimate;
                }
                binRow.clear();
                yUltimate = y;
            }
            binRow.add(temporalBin);
        }

        if (yUltimate >= y1 && yUltimate <= y2) {
            // last row
            processRowsWithoutBins(x1, x2, yGlobalUltimate + 1, yUltimate - 1);
            processRowWithBins(yUltimate, binRow, gridWidth, gridHeight);
            yGlobalUltimate = yUltimate;
        }
    }

    private void processRowWithBins(int y,
                                    List<TemporalBin> binRow,
                                    int gridWidth,
                                    int gridHeight) throws Exception {

        Assert.argument(!binRow.isEmpty(), "!binRow.isEmpty()");

        final int x1 = rasterRegion.x;
        final int x2 = rasterRegion.x + rasterRegion.width - 1;
        final int y1 = rasterRegion.y;
        Vector resultVector = null;
        final double lat = 90.0 - (y + 0.5) * 180.0 / gridHeight;
        long lastBinIndex = -1;
        TemporalBin temporalBin = null;
        int rowIndex = -1;
        for (int x = x1; x <= x2; x++) {
            double lon = -180.0 + (x + 0.5) * 360.0 / gridWidth;
            long wantedBinIndex = planetaryGrid.getBinIndex(lat, lon);
            if (lastBinIndex != wantedBinIndex) {
                // search temporalBin for wantedBinIndex
                temporalBin = null;
                for (int i = rowIndex + 1; i < binRow.size(); i++) {
                    final long binIndex = binRow.get(i).getIndex();
                    if (binIndex == wantedBinIndex) {
                        temporalBin = binRow.get(i);
                        resultVector = temporalBin.toVector();
                        lastBinIndex = wantedBinIndex;
                        rowIndex = i;
                        break;
                    } else if (binIndex > wantedBinIndex) {
                        break;
                    }
                }
            }
            if (temporalBin != null) {
                temporalBinRenderer.renderBin(x - x1, y - y1, temporalBin, resultVector);
            } else {
                temporalBinRenderer.renderMissingBin(x - x1, y - y1);
            }
        }
    }

    private void processRowsWithoutBins(int x1, int x2, int yStart, int yEnd) throws Exception {
        for (int y = yStart; y <= yEnd; y++) {
            processRowWithoutBins(x1, x2, y - rasterRegion.y);
        }
    }

    private void processRowWithoutBins(int x1, int x2, int y) throws Exception {
        for (int x = x1; x <= x2; x++) {
            temporalBinRenderer.renderMissingBin(x - x1, y);
        }
    }
}
