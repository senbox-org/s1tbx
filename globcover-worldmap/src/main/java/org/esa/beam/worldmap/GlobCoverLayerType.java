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

package org.esa.beam.worldmap;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.glayer.WorldMapLayerType;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Thomas Storm
 * @author Marco Zühlke
 * @since BEAM 4.8
 */
@LayerTypeMetadata(name = "GlobCoverLayerType")
public class GlobCoverLayerType extends WorldMapLayerType {

    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.worldImageDir";
    private static final String WORLD_MAP_LAYER_NAME = "World Map (ESA GlobCover)";
    private static final String WORLD_MAP_LABEL = "ESA GlobCover";

    private volatile MultiLevelSource multiLevelSource;

    @Override
    public String getLabel() {
        return WORLD_MAP_LABEL;
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        if (ctx.getCoordinateReferenceSystem() instanceof AbstractIdentifiedObject) {
            AbstractIdentifiedObject crs = (AbstractIdentifiedObject) ctx.getCoordinateReferenceSystem();
            return DefaultGeographicCRS.WGS84.equals(crs, false);
        }
        return false;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        if (multiLevelSource == null) {
            synchronized (this) {
                if (multiLevelSource == null) {
                    multiLevelSource = createMultiLevelSource();
                }
            }
        }
        for (final Property model : super.createLayerConfig(ctx).getProperties()) {
            if (configuration.getProperty(model.getDescriptor().getName()) == null) {
                configuration.addProperty(model);
            }
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);

        final ImageLayer layer = new ImageLayer(this, multiLevelSource, configuration);
        layer.setName(WORLD_MAP_LAYER_NAME);
        layer.setVisible(true);

        return layer;
    }

    private static MultiLevelSource createMultiLevelSource() {
        String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
        if (dirPath == null || dirPath.isEmpty()) {
            dirPath = getDirPathFromModule();
        }
        if (dirPath == null) {
            throw new IllegalStateException("World image directory not found.");
        }
        final MultiLevelSource multiLevelSource;
        try {
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return multiLevelSource;
    }

    private static String getDirPathFromModule() {
        final URL resource = GlobCoverLayerType.class.getResource("image.properties");
        try {
            return new File(resource.toURI()).getParent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
