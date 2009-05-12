/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager.layersrc.image;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;

/**
 * A layer source for images.
 * <p/>
 * The image can either be associated with an "world-file" or
 * the orientation relative to the existing layers has to be given by hand.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ImageFileLayerSource implements LayerSource {

    static final String PROPERTY_NAME_IMAGE = "image";
    static final String PROPERTY_NAME_IMAGE_FILE_PATH = "imageFilePath";
    static final String PROPERTY_NAME_WORLD_FILE_PATH = "worldFilePath";
    static final String PROPERTY_NAME_WORLD_TRANSFORM = "worldTransform";

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean hasFirstPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return new ImageFileAssistantPage1();
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
        pageContext.setPropertyValue(PROPERTY_NAME_IMAGE, null);
    }

    static boolean insertImageLayer(LayerSourcePageContext pageContext) {
        AffineTransform transform = (AffineTransform) pageContext.getPropertyValue(
                PROPERTY_NAME_WORLD_TRANSFORM);
        RenderedImage image = (RenderedImage) pageContext.getPropertyValue(PROPERTY_NAME_IMAGE);
        String imageFilePath = (String) pageContext.getPropertyValue(PROPERTY_NAME_IMAGE_FILE_PATH);

        try {
            ProductSceneView sceneView = pageContext.getAppContext().getSelectedProductSceneView();
            final LayerType type = LayerType.getLayerType(ImageFileLayerType.class.getName());
            final ValueContainer configuration = type.getConfigurationTemplate();
            configuration.setValue(ImageFileLayerType.PROPERTY_NAME_IMAGE, image);
            configuration.setValue(ImageFileLayerType.PROPERTY_NAME_IMAGE_FILE, new File(imageFilePath));
            configuration.setValue(ImageFileLayerType.PROPERTY_NAME_WORLD_TRANSFORM, transform);
            Layer layer = type.createLayer(sceneView.getLayerContext(), configuration);
            layer.setName(FileUtils.getFileNameFromPath(imageFilePath));
            Layer rootLayer = sceneView.getRootLayer();
            rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), layer);
            return true;
        } catch (Exception e) {
            pageContext.showErrorDialog(e.getMessage());
            return false;
        }
    }
}
