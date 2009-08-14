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

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;


public class BeamGridGeometry  {

    public final static int dimensionXIndex = 0;
    public final static int dimensionYIndex = 1;
    
    private AffineTransform i2m;
    private Rectangle bounds;
    private Envelope2D envelope;
    
    public BeamGridGeometry(AffineTransform i2m, Rectangle bounds, CoordinateReferenceSystem crs) {
        this.i2m = i2m;
        this.bounds = bounds;
        envelope = new Envelope2D(crs, bounds);
    }
    
    public AffineTransform getImageToModel() {
        return i2m;
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    public Envelope2D getEnvelope() {
        return envelope;
    }

    public CoordinateReferenceSystem getModelCRS() {
        return envelope.getCoordinateReferenceSystem();
    }
}
