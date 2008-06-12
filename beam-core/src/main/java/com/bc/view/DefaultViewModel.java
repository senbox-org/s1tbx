/*
 * $Id: DefaultViewModel.java,v 1.2 2006/10/23 06:38:14 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.view;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simple default implementation for a {@link ViewModel}.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class DefaultViewModel implements ViewModel {
    /**
     * The x-offset in model coordinates of the upper left view pixel
     */
    private double modelOffsetX;
    /**
     * The y-offset in model coordinates of the upper left view pixel
     */
    private double modelOffsetY;
    /**
     * The current view scale
     */
    private double viewScale;
    /**
     * The maximum view scale. Minimum is given as 1.0 / viewScaleMax.
     */
    private Double viewScaleMax;
    /**
     * This view model's area. Enables scrolling with scroll bars.
     */
    private Rectangle2D modelArea;

    /**
     * The list of change listeners
     */
    private List<ViewModelChangeListener> viewModelChangeListenerList;


    public DefaultViewModel() {
        this.modelOffsetX = 0;
        this.modelOffsetY = 0;
        this.viewScale = 1;
        this.viewScaleMax = 16.0;
        this.modelArea = new Rectangle2D.Double();
        this.viewModelChangeListenerList = new ArrayList<ViewModelChangeListener>();
    }


    public double getModelOffsetX() {
        return modelOffsetX;
    }

    public double getModelOffsetY() {
        return modelOffsetY;
    }

    public void setModelOffset(double modelOffsetX, double modelOffsetY) {
        if (this.modelOffsetX != modelOffsetX ||
                this.modelOffsetY != modelOffsetY) {
            this.modelOffsetX = modelOffsetX;
            this.modelOffsetY = modelOffsetY;
            fireViewModelChanged();
        }
    }

    public void setModelOffset(double modelOffsetX, double modelOffsetY, double viewScale) {
        viewScale = maybeCropViewScale(viewScale);
        if (this.modelOffsetX != modelOffsetX ||
                this.modelOffsetY != modelOffsetY ||
                this.viewScale != viewScale) {
            this.modelOffsetX = modelOffsetX;
            this.modelOffsetY = modelOffsetY;
            this.viewScale = viewScale;
            fireViewModelChanged();
        }
    }

    public double getViewScale() {
        return viewScale;
    }

    public void setViewScale(double viewScale) {
        viewScale = maybeCropViewScale(viewScale);
        if (this.viewScale != viewScale) {
            this.viewScale = viewScale;
            fireViewModelChanged();
        }
    }

    public Double getViewScaleMax() {
        return viewScaleMax;
    }

    public void setViewScaleMax(Double viewScaleMax) {
        this.viewScaleMax = viewScaleMax;
    }

    public Rectangle2D getModelArea() {
        return new Rectangle2D.Double(modelArea.getX(), modelArea.getY(), modelArea.getWidth(), modelArea.getHeight());
    }

    public void setModelArea(Rectangle2D modelArea) {
        if (!this.modelArea.equals(modelArea)) {
            this.modelArea = new Rectangle2D.Double(modelArea.getX(),
                                                    modelArea.getY(),
                                                    modelArea.getWidth(),
                                                    modelArea.getHeight());
            fireViewModelChanged();
        }
    }

    public ViewModelChangeListener[] getViewModelChangeListeners() {
        final ViewModelChangeListener[] viewModelChangeListeners = new ViewModelChangeListener[viewModelChangeListenerList.size()];
        return viewModelChangeListenerList.toArray(viewModelChangeListeners);
    }

    public void addViewModelChangeListener(ViewModelChangeListener l) {
        if (l != null && !viewModelChangeListenerList.contains(l)) {
            viewModelChangeListenerList.add(l);
        }
    }

    public void removeViewModelChangeListener(ViewModelChangeListener l) {
        if (l != null) {
            viewModelChangeListenerList.remove(l);
        }
    }

    protected void fireViewModelChanged() {
        for (Object aViewModelChangeListenerList : viewModelChangeListenerList) {
            ViewModelChangeListener l = (ViewModelChangeListener) aViewModelChangeListenerList;
            l.handleViewModelChanged(this);
        }
    }

    public static double cropViewScale(double viewScale, Double viewScaleMax) {
        if (viewScaleMax != null && viewScaleMax > 1.0) {
            final double upperLimit = viewScaleMax;
            final double lowerLimit = 1.0 / upperLimit;
            if (viewScale < lowerLimit) {
                viewScale = lowerLimit;
            } else if (viewScale > upperLimit) {
                viewScale = upperLimit;
            }
        }
        return viewScale;
    }

    private double maybeCropViewScale(double viewScale) {
        return cropViewScale(viewScale, getViewScaleMax());
    }
}
