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
package org.esa.s1tbx.dat.toolviews.worldwind;

import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;

import java.util.ArrayList;
import java.util.HashMap;

/**

 */
public class ProductRenderablesInfo  {

    public AnalyticSurface owiAnalyticSurface = null;
    public AnalyticSurface rvlAnalyticSurface = null;

    public BufferWrapper owiAnalyticSurfaceValueBuffer = null;
    public BufferWrapper rvlAnalyticSurfaceValueBuffer = null;


    public HashMap<String, ArrayList<Renderable>> theRenderableListHash;
    public ProductRenderablesInfo() {
        super();

        theRenderableListHash = new HashMap<String, ArrayList<Renderable>>();
        theRenderableListHash.put("owi", new ArrayList<Renderable>());
        theRenderableListHash.put("osw", new ArrayList<Renderable>());
        theRenderableListHash.put("rvl", new ArrayList<Renderable>());
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
