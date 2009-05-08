package org.esa.beam.visat.toolviews.layermanager.layersrc.image;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;


public class ImageFileLayerType extends LayerType{

    static final String PROPERTY_IMAGE = "image";
    static final String PROPERTY_IMAGE_FILE = "filePath";
    static final String PROPERTY_WORLD_TRANSFORM = "worldTransform";

    @Override
    public String getName() {
        return "Image Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new ImageFileLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = new ValueContainer();

        final ValueModel imageModel = createDefaultValueModel(PROPERTY_IMAGE, RenderedImage.class);
        imageModel.getDescriptor().setTransient(true);
        template.addModel(imageModel);

        final ValueModel filePathModel = createDefaultValueModel(PROPERTY_IMAGE_FILE, File.class);
        filePathModel.getDescriptor().setNotNull(true);
        template.addModel(filePathModel);

        final ValueModel worldTransformModel = createDefaultValueModel(PROPERTY_WORLD_TRANSFORM, AffineTransform.class);
        worldTransformModel.getDescriptor().setNotNull(true);
        template.addModel(worldTransformModel);

        return template;
    }
}
