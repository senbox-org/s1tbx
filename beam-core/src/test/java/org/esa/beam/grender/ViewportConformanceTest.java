package org.esa.beam.grender;

import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.glayer.Assert2D;
import com.bc.view.DefaultViewModel;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

public class ViewportConformanceTest {

    @Test
    public void testModelToViewTransform() {

        final DefaultViewport viewport = new DefaultViewport();
        final DefaultViewModel viewModel = new DefaultViewModel();


        final Rectangle modelRect = new Rectangle(0, 0, 10, 20);
        double modelOffsetX = 2;
        double modelOffsetY = -3;
        double viewScale = 0.5;


        viewport.setModelOffset(modelOffsetX, modelOffsetY, viewScale);

        final Rectangle2D viewRect = viewport.getModelToViewTransform().createTransformedShape(modelRect).getBounds2D();

        viewModel.setModelOffset(modelOffsetX, modelOffsetY, viewScale);
        final AffineTransform transform = new AffineTransform();
        transform.scale(viewModel.getViewScale(), viewModel.getViewScale());
        transform.translate(-viewModel.getModelOffsetX(),
                            -viewModel.getModelOffsetY());
        final Rectangle2D expectedViewRect = transform.createTransformedShape(modelRect).getBounds2D();

        assertEquals(expectedViewRect.getX(), viewRect.getX(), 1e-8);
        assertEquals(expectedViewRect.getY(), viewRect.getY(), 1e-8);
        assertEquals(expectedViewRect.getY(), viewRect.getY(), 1e-8);
        assertEquals(expectedViewRect.getWidth(), viewRect.getWidth(), 1e-8);
        assertEquals(expectedViewRect.getHeight(), viewRect.getHeight(), 1e-8);



    }



}
