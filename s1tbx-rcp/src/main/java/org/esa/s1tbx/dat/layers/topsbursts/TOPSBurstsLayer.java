/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.layers.topsbursts;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import org.esa.s1tbx.dat.graphics.GraphicShape;
import org.esa.s1tbx.dat.layers.LayerSelection;
import org.esa.s1tbx.dat.layers.ScreenPixelConverter;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
    Overalys the TOPS burst boundaries
 */
public class TOPSBurstsLayer extends Layer implements LayerSelection {

    private final Product product;
    private final RasterDataNode raster;

    private final List<Swath> swathList = new ArrayList<>(5);

    private final static BasicStroke thickStroke = new BasicStroke(4);
    private final static BasicStroke thinStroke = new BasicStroke(2);

    public TOPSBurstsLayer(LayerType layerType, PropertySet configuration) {
        super(layerType, configuration);
        setName("TOPS Burst Boundaries");
        raster = configuration.getValue("raster");
        product = raster.getProduct();

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final MetadataElement burstBoundaryElem = absRoot.getElement("BurstBoundary");
            if (burstBoundaryElem != null) {
                final MetadataElement[] swathElems = burstBoundaryElem.getElements();
                for(MetadataElement swathElem : swathElems) {
                    swathList.add(new Swath(swathElem, product));
                }
            }
        }
        regenerate();
    }

    /**
     * Regenerates the layer. May be called to update the layer data.
     * The default implementation does nothing.
     */
    @Override
    public void regenerate() {
    }

    @Override
    protected void renderLayer(final Rendering rendering) {

        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) return;

        final ScreenPixelConverter screenPixel = new ScreenPixelConverter(rendering.getViewport(), raster);
        if (!screenPixel.withInBounds()) {
            return;
        }

        final Graphics2D g = rendering.getGraphics();

        for (Swath swath : swathList) {
            swath.render(g, screenPixel);
        }
    }

    public void selectRectangle(final Rectangle rect) {
    }

    public void selectPoint(final int x, final int y) {
    }

    private static class Swath {

        private final List<Burst> burstList = new ArrayList<>(10);
        private final String name;

        public Swath(final MetadataElement swathElem, final Product product) {
            this.name = swathElem.getName();

            final MetadataElement[] burstElems = swathElem.getElements();
            for(MetadataElement burstElem : burstElems) {
                burstList.add(new Burst(burstElem, product));
            }
        }

        public void render(final Graphics2D g, final ScreenPixelConverter screenPixel) {
            for(Burst burst : burstList) {
                burst.render(g, screenPixel);
            }
        }
    }

    private static class Burst {

        private final String name;
        private final Product product;
        private final List<PixelPos> firstLinePosList = new ArrayList<>(6);
        private final List<PixelPos> lastLinePosList = new ArrayList<>(6);

        public Burst(final MetadataElement burstElem, final Product product) {
            this.name = burstElem.getName();
            this.product = product;

            getBoundaryPoints(burstElem.getElement("FirstLineBoundaryPoints"), firstLinePosList);
            getBoundaryPoints(burstElem.getElement("LastLineBoundaryPoints"), lastLinePosList);
        }

        private void getBoundaryPoints(final MetadataElement lineBoundaryPoints, final List<PixelPos> posList) {
            if(lineBoundaryPoints == null) {
                return;
            }

            final GeoCoding geoCoding = product.getGeoCoding();
            final MetadataElement[] boundaryPoints = lineBoundaryPoints.getElements();
            for (MetadataElement boundaryPoint : boundaryPoints) {
                double lat = boundaryPoint.getAttributeDouble("lat");
                double lon = boundaryPoint.getAttributeDouble("lon");

                PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
                //if(!pixelPos.isValid()) {
                //    System.out.println("not valid "+pixelPos.toString());
                //    pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
               // }
                posList.add(pixelPos);
            }
        }

        public void render(final Graphics2D g, final ScreenPixelConverter screenPixel) {

            final Path2D.Float path = new Path2D.Float();
            Point.Double p = screenPixel.pixelToScreen(firstLinePosList.get(0).x, firstLinePosList.get(0).y);
            path.moveTo(p.x, p.y);
            for(int i=1; i < firstLinePosList.size(); ++i) {
                p = screenPixel.pixelToScreen(firstLinePosList.get(i).x, firstLinePosList.get(i).y);
                path.lineTo(p.x, p.y);
            }
            for(int i=lastLinePosList.size()-1; i >= 0; --i) {
                p = screenPixel.pixelToScreen(lastLinePosList.get(i).x, lastLinePosList.get(i).y);
                path.lineTo(p.x, p.y);
            }
            p = screenPixel.pixelToScreen(firstLinePosList.get(0).x, firstLinePosList.get(0).y);
            path.lineTo(p.x, p.y);

            g.setColor(Color.CYAN);
            g.setStroke(thinStroke);
            g.draw(path);
        }
    }
}
