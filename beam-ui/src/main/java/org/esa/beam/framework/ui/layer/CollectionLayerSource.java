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
package org.esa.beam.framework.ui.layer;

import org.esa.beam.framework.ui.layer.LayerSource;

/**
 * This layer source creates a new and empty collection layer.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 * 
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class CollectionLayerSource implements LayerSource {

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
        return new CollectionLayerAssistantPage();
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        return pageContext.getCurrentPage().performFinish();
    }
    
    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}
