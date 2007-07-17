package org.esa.beam.framework.gpf.internal;

import java.awt.Rectangle;
import java.util.List;

import org.esa.beam.framework.gpf.OperatorContext;

public interface OperatorComputationContext extends OperatorContext {
	
	public List<Rectangle> getLayoutRectangles();
	
	public void setLayoutRectangles(List<Rectangle> layoutRectangles);
	
	public List<Rectangle> getAffectedRectangles(Rectangle rectangle);
	
	public boolean isLayoutRectangle(Rectangle rectangle);
}
