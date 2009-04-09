package org.esa.beam.worldmap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.glevel.TiledFileMultiLevelSource;

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
public class BlueMarbleWorldMapLayer {

    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.pview.worldImageDir";
    private static final String WORLD_MAP_LAYER_NAME = "World Map (NASA Blue Marble)";

    private BlueMarbleWorldMapLayer() {
    }

    /**
     * Creates a new world map layer. Its visibility state is initially set to not visible.
     *
     * @return The world map layer.
     */
    public static Layer createWorldMapLayer() {
        String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
        if (dirPath == null || dirPath.isEmpty()) {
            dirPath = getDirPathFromModule();
        }
        if (dirPath == null) {
            return null;
        }
        final MultiLevelSource multiLevelSource;
        try {
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        final ImageLayer worldMapLayer = new ImageLayer(multiLevelSource);
        worldMapLayer.setName(WORLD_MAP_LAYER_NAME);
        worldMapLayer.setVisible(false);
        final Style style = worldMapLayer.getStyle();
        style.setOpacity(1.0);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);

        return worldMapLayer;

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
