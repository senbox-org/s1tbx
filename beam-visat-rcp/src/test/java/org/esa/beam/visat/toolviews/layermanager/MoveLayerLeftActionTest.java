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
public class MoveLayerLeftActionTest extends AbstractMoveLayerTest {


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

    private MoveLayerLeftAction layerLeftAction;

    @Override
    @Before
    public void setupTreeModel() {
        super.setupTreeModel();
        layerLeftAction = new MoveLayerLeftAction(new DummyAppContext());

    }

    @Test
    public void testMoveLayer2Left() {
        layerLeftAction.moveLeft(layer2);   // Not possible; no parent to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer4Left() {
        layerLeftAction.moveLeft(layer4);

        Assert.assertEquals(5, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer4"));
        Assert.assertEquals(4, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer7Left() {
        layerLeftAction.moveLeft(layer7);

        Assert.assertEquals(5, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
        Assert.assertEquals(4, layer0.getChildIndex("layer7"));
    }

}