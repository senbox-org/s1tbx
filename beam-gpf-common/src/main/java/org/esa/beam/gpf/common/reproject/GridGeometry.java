/*
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.gpf.common.reproject;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


class GridGeometry {

    public static final int dimensionXIndex = 0;
    public static final int dimensionYIndex = 1;
    
    private final AffineTransform i2m;
    private final Rectangle2D bounds2D;
    private final CoordinateReferenceSystem modelCrs;

    GridGeometry(Rectangle2D bounds2D, CoordinateReferenceSystem modelCrs, AffineTransform grid2model) {
        this.i2m = grid2model;
        this.bounds2D = bounds2D;
        this.modelCrs = modelCrs;
    }

    public AffineTransform getGridToModel() {
        return i2m;
    }
    
    public Rectangle2D getBounds2D() {
        return bounds2D;
    }
    
    public CoordinateReferenceSystem getModelCRS() {
        return modelCrs;
    }

}
