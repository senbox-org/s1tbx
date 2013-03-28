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

package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.awt.Window;

import static junit.framework.Assert.*;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class AbstractMoveLayerTest {

    /*
          TreeStructure:
              layer0
                  |- layer1
                  |- layer2
                  |- layer3
                      |- layer4
                      |- layer5
                  |- layer6
                      |- layer7
       */

    protected Layer layer0;
    protected Layer layer1;
    protected Layer layer2;
    protected Layer layer3;
    protected Layer layer4;
    protected Layer layer5;
    protected Layer layer6;
    protected Layer layer7;

    @Before
    public void setupTreeModel() {
        layer0 = createLayer("layer0");
        layer1 = createLayer("layer1");
        layer2 = createLayer("layer2");
        layer3 = createLayer("layer3");
        layer4 = createLayer("layer4");
        layer5 = createLayer("layer5");
        layer6 = createLayer("layer6");
        layer7 = createLayer("layer7");

        layer0.getChildren().add(layer1);
        layer0.getChildren().add(layer2);
        layer0.getChildren().add(layer3);
        layer3.getChildren().add(layer4);
        layer3.getChildren().add(layer5);
        layer0.getChildren().add(layer6);
        layer6.getChildren().add(layer7);

    }


    @Test
    public void testDummy() {
        assertEquals(true, true);
    }

    private static Layer createLayer(String id) {
        final Layer layer = new CollectionLayer();
        layer.setId(id);
        layer.setName(id);
        return layer;
    }

    static class DummyAppContext implements AppContext {

        @Override
        public String getApplicationName() {
            return null;
        }

        @Override
        public Window getApplicationWindow() {
            return null;
        }

        @Override
        public ApplicationPage getApplicationPage() {
            return null;
        }

        @Override
        public Product getSelectedProduct() {
            return null;
        }

        @Override
        public void handleError(String message, Throwable e) {
        }

        @Override
        public PropertyMap getPreferences() {
            return null;
        }

        @Override
        public ProductManager getProductManager() {
            return null;
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return null;
        }
    }
}
