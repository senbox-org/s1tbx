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

import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSource;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public abstract class AbstractLayerSourceAssistantPage extends AbstractAssistantPage {

    protected AbstractLayerSourceAssistantPage(String pageTitle) {
        super(pageTitle);
    }

    @Override
    public LayerSourcePageContext getContext() {
        return (LayerSourcePageContext) super.getContext();
    }
    
    @Override
    public void performCancel() {
        LayerSourcePageContext context = getContext();
        LayerSource layerSource = context.getLayerSource();
        if (layerSource != null) {
            layerSource.cancel(context);
        }
    }
}
