package org.esa.beam.visat.toolviews.layermanager.layersrc.image;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.Property;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;


public class ImageFileLayerType extends LayerType{

    static final String PROPERTY_NAME_IMAGE = "image";
    static final String PROPERTY_NAME_IMAGE_FILE = "filePath";
    static final String PROPERTY_NAME_WORLD_TRANSFORM = "worldTransform";

    @Override
    public String getName() {
        return "Image Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        return new ImageFileLayer(this, configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer template = new PropertyContainer();

        final Property imageModel = Property.create(PROPERTY_NAME_IMAGE, RenderedImage.class);
        imageModel.getDescriptor().setTransient(true);
        template.addProperty(imageModel);

        final Property filePathModel = Property.create(PROPERTY_NAME_IMAGE_FILE, File.class);
        filePathModel.getDescriptor().setNotNull(true);
        template.addProperty(filePathModel);

        final Property worldTransformModel = Property.create(PROPERTY_NAME_WORLD_TRANSFORM, AffineTransform.class);
        worldTransformModel.getDescriptor().setNotNull(true);
        template.addProperty(worldTransformModel);

        return template;
    }
}
