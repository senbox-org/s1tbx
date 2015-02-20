/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.nestwwview;

import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**

 */
public class ProductRenderablesInfo  {

    public AnalyticSurface owiAnalyticSurface = null;
    public AnalyticSurface rvlAnalyticSurface = null;

    public BufferWrapper owiAnalyticSurfaceValueBuffer = null;
    public BufferWrapper rvlAnalyticSurfaceValueBuffer = null;

    // NOTE: analytic surfaces are included in these lists:
    public ArrayList<Renderable> owiRenderableList;
    public ArrayList<Renderable> oswRenderableList;
    public ArrayList<Renderable> rvlRenderableList;

    public ProductRenderablesInfo() {
        super();
        owiRenderableList = new ArrayList<Renderable>();
        oswRenderableList = new ArrayList<Renderable>();
        rvlRenderableList = new ArrayList<Renderable>();
    }

    public void setAnalyticSurfaceAndBuffer (AnalyticSurface analyticSurface, BufferWrapper analyticSurfaceValueBuffer, String comp) {
        if (comp.equalsIgnoreCase("owi")) {
            owiAnalyticSurface = analyticSurface;
            owiAnalyticSurfaceValueBuffer = analyticSurfaceValueBuffer;
        }
        else if (comp.equalsIgnoreCase("rvl")) {
            rvlAnalyticSurface = analyticSurface;
            rvlAnalyticSurfaceValueBuffer = analyticSurfaceValueBuffer;
        }
    }
}