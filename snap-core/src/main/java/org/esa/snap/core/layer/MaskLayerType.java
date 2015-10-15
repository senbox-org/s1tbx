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

package org.esa.snap.core.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

import java.awt.image.RenderedImage;


/**
 * A layer used to display {@link Mask}s.
 *
 * @author Norman Fomferra
 * @version $ Revision: $ Date: $
 * @since BEAM 4.7
 */
@LayerTypeMetadata(name = "MaskLayerType", aliasNames = {"org.esa.snap.core.layer.MaskLayerType"})
public class MaskLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_MASK = "mask";

    public static Layer createLayer(RasterDataNode raster, Mask mask) {
        final MaskLayerType type = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        final PropertySet configuration = type.createLayerConfig(null);
        configuration.setValue(MaskLayerType.PROPERTY_NAME_MASK, mask);

        final Layer layer = type.createLayer(null, configuration);
        layer.setVisible(raster.getOverlayMaskGroup().contains(mask));

        return layer;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            multiLevelSource = createMultiLevelSource(configuration);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        configuration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_SHOWN, false);
        final ImageLayer layer = new ImageLayer(this, multiLevelSource, configuration);
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        layer.setName(mask.getName());

        return layer;
    }

    public static MultiLevelSource createMultiLevelSource(PropertySet configuration) {
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        return createMultiLevelSource(mask);
    }

    public static MultiLevelSource createMultiLevelSource(final Mask mask) {
        return new AbstractMultiLevelSource(mask.getSourceImage().getModel()) {
            @Override
            protected RenderedImage createImage(int level) {
                return ImageManager.createColoredMaskImage(mask.getSourceImage().getImage(level), mask.getImageColor(), 1.0 - mask.getImageTransparency());
            }

        };
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet layerConfig = super.createLayerConfig(ctx);

        layerConfig.addProperty(Property.create(PROPERTY_NAME_MASK, Mask.class));
        layerConfig.getProperty(PROPERTY_NAME_MASK).getDescriptor().setNotNull(true);

        return layerConfig;
    }
}
