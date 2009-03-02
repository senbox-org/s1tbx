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
public class MoveLayerUpActionTest extends AbstractMoveLayerTest {


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

    private MoveLayerUpAction layerUpAction;

    @Override
    @Before
    public void setupTreeModel() {
        super.setupTreeModel();
        layerUpAction = new MoveLayerUpAction(new DummyAppContext());

    }

    @Test
    public void testMoveLayer1Up() {
        layerUpAction.moveUp(layer1);      // Not possible it's already the first node

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));

    }

    @Test
    public void testMoveLayer3Up() {
        layerUpAction.moveUp(layer3);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer3"));
        Assert.assertEquals(2, layer0.getChildIndex("layer2"));
    }

    @Test
    public void testMoveLayer5Up() {
        layerUpAction.moveUp(layer5);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer5"));
        Assert.assertEquals(1, layer3.getChildIndex("layer4"));

    }

    @Test
    public void testMoveLayer6Up() {
        layerUpAction.moveUp(layer6);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer6"));
        Assert.assertEquals(3, layer0.getChildIndex("layer3"));
    }


}
