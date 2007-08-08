package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.OperatorContext;

import java.awt.Rectangle;
import java.util.List;

public interface OperatorComputationContext extends OperatorContext {

    /**
     * Gets a list of rectangles which define the regions of the tiles each {@link org.esa.beam.framework.datamodel.RasterDataNode} is splited.
     *
     * @return the layoutRectangles
     */
    public List<Rectangle> getLayoutRectangles();

    /**
     * Sets the list of rectangles which define the regions of the tiles each {@link org.esa.beam.framework.datamodel.RasterDataNode} is splited.
     *
     * @param layoutRectangles the layout rectangles to set
     */
    public void setLayoutRectangles(List<Rectangle> layoutRectangles);

    /**
     * Returns all rectangles that intersect with the given rectangle.
     *
     * @param rectangle the rectangle to intersect with
     *
     * @return a list of rectangles
     */
    public List<Rectangle> getAffectedRectangles(Rectangle rectangle);

    /**
     * Checks if the given rectangle is equal to one of the layout rectangles.
     *
     * @param rectangle the rectangle to check for
     *
     * @return {@code true}, if the given rectangle is equal to one of the layout rectangles, otherwise {@code false}
     */
    public boolean isLayoutRectangle(Rectangle rectangle);
}
