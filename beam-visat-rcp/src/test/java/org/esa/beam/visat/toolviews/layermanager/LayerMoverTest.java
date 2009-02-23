package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LayerMoverTest {

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


    private LayerMover layerMover;

    private Layer layer0;
    private Layer layer1;
    private Layer layer2;
    private Layer layer3;
    private Layer layer4;
    private Layer layer5;
    private Layer layer6;
    private Layer layer7;

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

        layerMover = new LayerMover(layer0);

    }

    @Test
    public void testMoveLayer1Up() {
        layerMover.moveUp(layer1);      // Not possible it's already the first node

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));

    }

    @Test
    public void testMoveLayer3Up() {
        layerMover.moveUp(layer3);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer3"));
        Assert.assertEquals(2, layer0.getChildIndex("layer2"));
    }

    @Test
    public void testMoveLayer5Up() {
        layerMover.moveUp(layer5);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer5"));
        Assert.assertEquals(1, layer3.getChildIndex("layer4"));

    }

    @Test
    public void testMoveLayer6Up() {
        layerMover.moveUp(layer6);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer6"));
        Assert.assertEquals(3, layer0.getChildIndex("layer3"));
    }

    @Test
    public void testMoveLayer2Down() {
        layerMover.moveDown(layer2);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer3"));
        Assert.assertEquals(2, layer0.getChildIndex("layer2"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer4Down() {
        layerMover.moveDown(layer4);

        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer5"));
        Assert.assertEquals(1, layer3.getChildIndex("layer4"));
    }

    @Test
    public void testMoveLayer6Down() {     // Not possible it's already the last node
        layerMover.moveDown(layer6);

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer2Left() {
        layerMover.moveLeft(layer2);   // Not possible; no parent to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer4Left() {
        layerMover.moveLeft(layer4);

        Assert.assertEquals(5, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer4"));
        Assert.assertEquals(4, layer0.getChildIndex("layer6"));
    }

    @Test
    public void testMoveLayer7Left() {
        layerMover.moveLeft(layer7);

        Assert.assertEquals(5, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
        Assert.assertEquals(1, layer0.getChildIndex("layer2"));
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
        Assert.assertEquals(3, layer0.getChildIndex("layer6"));
        Assert.assertEquals(4, layer0.getChildIndex("layer7"));
    }

    @Test
    public void testMoveLayer1Right() {
        layerMover.moveRight(layer1);    // Not possible; no place to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(0, layer0.getChildIndex("layer1"));
    }

    @Test
    public void testMoveLayer2Right() {
        layerMover.moveRight(layer2);    // Not possible; no place to move to

        Assert.assertEquals(3, layer0.getChildren().size());
        Assert.assertEquals(0, layer1.getChildIndex("layer2"));
    }

    @Test
    public void testMoveLayer3Right() {
        layerMover.moveRight(layer3);

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
        layerMover.moveRight(layer4);    // Not possible; no place to move to

        Assert.assertEquals(4, layer0.getChildren().size());
        Assert.assertEquals(2, layer3.getChildren().size());
        Assert.assertEquals(2, layer0.getChildIndex("layer3"));
    }

    @Test
    public void testMoveLayer6Right() {
        layerMover.moveRight(layer6);

        Assert.assertEquals(3, layer0.getChildren().size());
        Assert.assertEquals(3, layer3.getChildren().size());
        Assert.assertEquals(0, layer3.getChildIndex("layer4"));
        Assert.assertEquals(1, layer3.getChildIndex("layer5"));
        Assert.assertEquals(2, layer3.getChildIndex("layer6"));
    }

    private static Layer createLayer(String id) {
        final Layer layer = new Layer();
        layer.setId(id);
        layer.setName(id);
        return layer;
    }
}
