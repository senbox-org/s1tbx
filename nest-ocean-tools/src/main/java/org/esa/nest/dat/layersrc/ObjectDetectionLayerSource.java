/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.layersrc;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSource;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;

import java.io.File;

/**
 * A source for {@link org.esa.nest.dat.layersrc.ObjectDetectionLayer}s.
 *
 */
public class ObjectDetectionLayerSource implements LayerSource {

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        final Product product = pageContext.getAppContext().getSelectedProduct();

        final File targetFile = ObjectDetectionLayer.getTargetFile(product);
        return targetFile != null;
    }

    @Override
    public boolean hasFirstPage() {
        return false;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return null;
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        final Product product = pageContext.getAppContext().getSelectedProduct();
        final Band band = product.getBand(pageContext.getAppContext().getSelectedProductSceneView().getRaster().getName());

        final ObjectDetectionLayer fieldLayer = ObjectDetectionLayerType.createLayer(product, band);
        pageContext.getLayerContext().getRootLayer().getChildren().add(0, fieldLayer);
        return true;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}