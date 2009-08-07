/*
 * $Id: MapInfo.java,v 1.2 2006/12/04 11:23:09 norman Exp $
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
package org.esa.beam.gpf.common;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;



public class BeamGridGeometry  {

//    //reference pixel X
//    final double pixelX;
//    //reference pixel Y
//    final double pixelY;
//
//    final double easting;
//    final double northing;
//    final double pixelSizeX;
//    final double pixelSizeY;
//    final double orientation;
//    final double sceneWidth;
//    final double sceneHeight;
//
//    public GridGeometry(double pixelX,
//                        double pixelY,
//                        double easting,
//                        double northing,
//                        double pixelSizeX,
//                        double pixelSizeY,
//                        double sceneWidth,
//                        double sceneHeight,
//                        double orientation) {
//        this.pixelX = pixelX;
//        this.pixelY = pixelY;
//        this.easting = easting;
//        this.northing = northing;
//        this.pixelSizeX = pixelSizeX;
//        this.pixelSizeY = pixelSizeY;
//        this.sceneWidth = sceneWidth;
//        this.sceneHeight = sceneHeight;
//        this.orientation = orientation;
//    }
//
//    public AffineTransform getPixelToMapTransform() {
//        AffineTransform transform = new AffineTransform();
//        transform.translate(easting, northing);
//        transform.scale(pixelSizeX, -pixelSizeY);
//        transform.rotate(Math.toRadians(-orientation));
//        transform.translate(-pixelSizeX, -pixelSizeY);
//        return transform;
//    }

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

    public CoordinateReferenceSystem getCRS() {
        return envelope.getCoordinateReferenceSystem();
    }
    
    public final static int dimensionXIndex = 0;
    public final static int dimensionYIndex = 1;

}
