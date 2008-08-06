package com.bc.swing;

import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.view.ViewModel;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class ViewportConformanceTest {

    @Test
    public void testModelToViewTransform() {

        final Viewport viewport = new DefaultViewport();
        final Rectangle viewportBounds = new Rectangle(0, 0, 20, 10);
        viewport.setBounds(viewportBounds);

        final GraphicsPane graphicsPane = new GraphicsPane();
        graphicsPane.setBounds(viewportBounds);

        viewport.zoom(new Rectangle(2, 3, 4, 5));
        graphicsPane.zoom(new Rectangle(2, 3, 4, 5));
        AffineTransform expectedAT = createModelToViewTransform(graphicsPane);
        assertEquals(expectedAT, viewport.getModelToViewTransform());

        viewport.zoom(5);
        graphicsPane.zoom(5);
        expectedAT = createModelToViewTransform(graphicsPane);
        assertEquals(expectedAT, viewport.getModelToViewTransform());

        viewport.zoom(3, 2, 0.5);
        graphicsPane.zoom(3, 2, 0.5);
        expectedAT = createModelToViewTransform(graphicsPane);
        assertEquals(expectedAT, viewport.getModelToViewTransform());

        viewport.move(-3, 1);
        graphicsPane.getViewModel().setModelOffset(-3, 1);
        expectedAT = createModelToViewTransform(graphicsPane);
        assertEquals(expectedAT, viewport.getModelToViewTransform());
    }

    private AffineTransform createModelToViewTransform(GraphicsPane graphicsPane) {
        final ViewModel viewModel = graphicsPane.getViewModel();
        final AffineTransform expectedAT = new AffineTransform();
        expectedAT.scale(viewModel.getViewScale(), viewModel.getViewScale());
        expectedAT.translate(-viewModel.getModelOffsetX(), -viewModel.getModelOffsetY());
        return expectedAT;
    }


}
