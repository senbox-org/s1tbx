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

package org.esa.beam.visat.toolviews.layermanager.layersrc.image;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;

@LayerTypeMetadata(name = "ImageFileLayerType",
                   aliasNames = {"org.esa.beam.visat.toolviews.layermanager.layersrc.image.ImageFileLayerType"})
public class ImageFileLayerType extends ImageLayer.Type {

    static final String PROPERTY_NAME_IMAGE_FILE = "filePath";
    static final String PROPERTY_NAME_WORLD_TRANSFORM = "worldTransform";

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final File file = (File) configuration.getValue(PROPERTY_NAME_IMAGE_FILE);
        final AffineTransform transform = (AffineTransform) configuration.getValue(PROPERTY_NAME_WORLD_TRANSFORM);
        RenderedImage image = FileLoadDescriptor.create(file.getPath(), null, true, null);
        final Rectangle2D modelBounds = DefaultMultiLevelModel.getModelBounds(transform, image);
        final DefaultMultiLevelModel model = new DefaultMultiLevelModel(1, transform, modelBounds);
        final MultiLevelSource multiLevelSource = new DefaultMultiLevelSource(image, model);
        return new ImageLayer(this, multiLevelSource, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer template = new PropertyContainer();

        final Property filePathModel = Property.create(PROPERTY_NAME_IMAGE_FILE, File.class);
        filePathModel.getDescriptor().setNotNull(true);
        template.addProperty(filePathModel);

        final Property worldTransformModel = Property.create(PROPERTY_NAME_WORLD_TRANSFORM, AffineTransform.class);
        worldTransformModel.getDescriptor().setNotNull(true);
        template.addProperty(worldTransformModel);

        return template;
    }

}
