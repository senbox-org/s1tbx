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

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class MoveLayerDownActionTest extends AbstractMoveLayerTest {


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

    private MoveLayerDownAction layerDownAction;

    @Override
    @Before
    public void setupTreeModel() {
        super.setupTreeModel();
        layerDownAction = new MoveLayerDownAction(new DummyAppContext());

    }

    @Test
    public void testMoveLayer2Down() {
        layerDownAction.moveDown(layer2);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer3"));
        Assert.assertEquals(2, layer0.getChildIndex("layer2"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer4Down() {
        layerDownAction.moveDown(layer4);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer5"));
        Assert.assertEquals(1, layer3.getChildIndex("layer4"));
    }

    @Test
    public void testMoveLayer6Down() {     // Not possible it's already the last node
        layerDownAction.moveDown(layer6);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }


}