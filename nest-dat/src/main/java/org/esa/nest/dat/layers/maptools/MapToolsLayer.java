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
package org.esa.nest.dat.layers.maptools;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.LayerSelection;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.dat.layers.maptools.components.*;

import java.awt.*;
import java.util.ArrayList;

/**

 */
public class MapToolsLayer extends Layer implements LayerSelection {

    private final Product product;
    private final RasterDataNode raster;
    private final MapToolsOptions options;

    private final ArrayList<MapToolsComponent> components = new ArrayList<MapToolsComponent>(5);

    public MapToolsLayer(LayerType layerType, PropertySet configuration) {
        super(layerType, configuration);
        setName("Mapping Tools");
        raster = (RasterDataNode) configuration.getValue("raster");
        options = (MapToolsOptions) configuration.getValue("options");
        options.setLayer(this);
        product = raster.getProduct();
        regenerate();
    }

    /**
     * Regenerates the layer. May be called to update the layer data.
     * The default implementation does nothing.
     */
    @Override
    public void regenerate() {
        components.clear();
        if(options.showNorthArrow()) {
            components.add(new NorthArrowComponent(raster));
        }
        if(options.showLatLonGrid()) {
            components.add(new LatLonGridComponent(raster));
        }
        if(options.showLookDirection()) {
            components.add(new LookDirectionComponent(raster));
        }
        if(options.showMapOverview()) {

        }
        if(options.showInfo()) {
            components.add(new InfoComponent(raster));
        }
        if(options.showScale()) {
            components.add(new ScaleComponent(raster));
        }
        if(options.showNestLogo()) {
            components.add(new LogoComponent(raster));
        }
    }

    @Override
    protected void renderLayer(final Rendering rendering) {

        final GeoCoding geoCoding = product.getGeoCoding();
        if(geoCoding == null) return;

        final Viewport vp = rendering.getViewport();
        final RasterDataNode raster = product.getRasterDataNode(product.getBandAt(0).getName());
        final ScreenPixelConverter screenPixel = new ScreenPixelConverter(vp, raster);
        if (!screenPixel.withInBounds()) {
            return;
        }
        
        final Graphics2D graphics = rendering.getGraphics();

        for(MapToolsComponent component : components) {
            component.render(graphics, screenPixel);
        }
    }

    public void selectRectangle(final Rectangle rect) {

    }

    public void selectPoint(final int x, final int y) {

    }

    public static Layer findMapToolsLayer(final Layer rootLayer) {
        return LayerUtils.getChildLayer(rootLayer, LayerUtils.SearchMode.DEEP, new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer.getLayerType() instanceof MapToolsLayerType;
            }
        });
    }

    public MapToolsOptions getMapToolsOptions() {
        return options;
    }
}