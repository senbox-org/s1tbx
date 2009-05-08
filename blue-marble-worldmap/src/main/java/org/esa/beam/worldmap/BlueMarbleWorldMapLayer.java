package org.esa.beam.worldmap;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Rendering;
import org.esa.beam.glevel.TiledFileMultiLevelSource;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Provides a world map layer for the SMOS-Box.
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class BlueMarbleWorldMapLayer extends Layer {

    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.pview.worldImageDir";
    private static final String WORLD_MAP_LAYER_NAME = "World Map (NASA Blue Marble)";
    private ImageLayer layerDelegate;

    public BlueMarbleWorldMapLayer(ValueContainer configuration) {
        super(LayerType.getLayerType(BlueMarbleLayerType.class.getName()), configuration);
        final ImageLayer.Type imageLayerType = (ImageLayer.Type) LayerType.getLayerType(
                ImageLayer.Type.class.getName());
        final ValueContainer template = imageLayerType.getConfigurationTemplate();
        final MultiLevelSource multiLevelSource = createMultiLevelSource();

        try {
            template.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            template.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM,
                              multiLevelSource.getModel().getImageToModelTransform(0));
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
        layerDelegate = new ImageLayer(imageLayerType, template);
        final Style style = layerDelegate.getStyle();
        style.setOpacity(1.0);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        setName(WORLD_MAP_LAYER_NAME);
        setVisible(true);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        layerDelegate.render(rendering);

    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return layerDelegate.getModelBounds();
    }

    @Override
    protected void disposeLayer() {
        layerDelegate.dispose();
    }

    @Override
    public void regenerate() {
        layerDelegate.regenerate();
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
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return multiLevelSource;
    }

    public static boolean hasWorldMapChildLayer(Layer layer) {
        final List<Layer> rootChildren = layer.getChildren();
        for (Layer child : rootChildren) {
            if (WORLD_MAP_LAYER_NAME.equals(child.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String getDirPathFromModule() {
        final URL resource = BlueMarbleWorldMapLayer.class.getResource("image.properties");
        try {
            return new File(resource.toURI()).getParent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
