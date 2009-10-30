package org.esa.beam.visat.toolviews.layermanager.layersrc.image;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;


public class ImageFileLayerType extends ImageLayer.Type {

    static final String PROPERTY_NAME_IMAGE_FILE = "filePath";
    static final String PROPERTY_NAME_WORLD_TRANSFORM = "worldTransform";

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        final File file = (File) configuration.getValue(PROPERTY_NAME_IMAGE_FILE);
        final AffineTransform transform = (AffineTransform) configuration.getValue(PROPERTY_NAME_WORLD_TRANSFORM);
        RenderedImage image = FileLoadDescriptor.create(file.getPath(), null, true, null);
        final Rectangle2D modelBounds = DefaultMultiLevelModel.getModelBounds(transform, image);
        final DefaultMultiLevelModel model = new DefaultMultiLevelModel(1, transform, modelBounds);
        final MultiLevelSource multiLevelSource = new DefaultMultiLevelSource(image, model);
        return new ImageLayer(this, multiLevelSource, configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
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
