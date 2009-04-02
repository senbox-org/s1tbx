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

import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

/**
 * A layer source for images.
 * 
 * The image can either be associated with an "world-file" or 
 * the orientation relative to the existing layers has to be given by hand.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ImageFileLayerSource implements LayerSource {

    static final String PROPERTY_IMAGE = "ImageFileLayerSource.image";
    static final String PROPERTY_IMAGE_FILE_PATH = "ImageFileLayerSource.imageFilePath";
    static final String PROPERTY_WORLD_FILE_PATH = "ImageFileLayerSource.worldFilePath";
    static final String PROPERTY_WORLD_TRANSFORM = "ImageFileLayerSource.worldTransform";

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
    public void cancel() {
    }

    @Override
    public boolean finish(LayerSourcePageContext pageContext) {
        return false;
    }
}
