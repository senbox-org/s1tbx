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

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class MoveLayerRightActionTest extends AbstractMoveLayerTest {


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

    private MoveLayerRightAction layerRightAction;

    @Override
    @Before
    public void setupTreeModel() {
        super.setupTreeModel();
        layerRightAction = new MoveLayerRightAction(new DummyAppContext());

    }

    @Test
    public void testMoveLayer1Right() {
        layerRightAction.moveRight(layer1);    // Not possible; no place to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
    }

    @Test
    public void testMoveLayer2Right() {
        layerRightAction.moveRight(layer2);    // Not possible; no place to move to

        Assert.assertEquals(3, layer0.getChildren().size());
        Assert.assertEquals(0, layer1.getChildIndex("layer2"));
    }

    @Test
    public void testMoveLayer3Right() {
        layerRightAction.moveRight(layer3);

        Assert.assertEquals(3, layer0.getChildren().size());
        Assert.assertSame(layer1, layer0.getChildren().get(0));
        Assert.assertSame(layer2, layer0.getChildren().get(1));
        Assert.assertSame(layer6, layer0.getChildren().get(2));
        Assert.assertEquals(1, layer2.getChildren().size());
        Assert.assertSame(layer3, layer2.getChildren().get(0));
        Assert.assertEquals(0, layer2.getChildIndex("layer3"));
        Assert.assertSame(layer7, layer6.getChildren().get(0));
    }

    @Test
    public void testMoveLayer4Right() {
        layerRightAction.moveRight(layer4);    // Not possible; no place to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
    }

    @Test
    public void testMoveLayer6Right() {
        layerRightAction.moveRight(layer6);

        Assert.assertEquals(3, layer0.getChildren().size());
        Assert.assertEquals(3, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer4"));
        Assert.assertEquals(1, layer3.getChildIndex("layer5"));
        Assert.assertEquals(2, layer3.getChildIndex("layer6"));
    }

}