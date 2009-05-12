package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.RoiImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;


/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class RoiLayerType extends ImageLayer.Type {

    public static final String ROI_LAYER_ID = "org.esa.beam.layers.roi";
    public static final String PROPERTY_NAME_COLOR = "color";
    public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "ROI";
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);

        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final MultiLevelSource multiLevelSource;
            if (raster.getROIDefinition() != null && raster.getROIDefinition().isUsable()) {
                final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
                final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                        ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
                multiLevelSource = RoiImageMultiLevelSource.create(raster, color, i2mTransform);
            } else {
                multiLevelSource = MultiLevelSource.NULL;
            }
            try {
                configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            } catch (ValidationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        final ImageLayer roiLayer = new ImageLayer(this, configuration);
        roiLayer.setName("ROI");
        roiLayer.setId(ROI_LAYER_ID);

        return roiLayer;
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = super.getConfigurationTemplate();

        final ValueModel rasterModel = createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        template.addModel(rasterModel);

        template.addModel(createDefaultValueModel(PROPERTY_NAME_COLOR, Color.class, Color.RED));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_TRANSPARENCY, Double.class, 0.5));

        return template;
    }
}