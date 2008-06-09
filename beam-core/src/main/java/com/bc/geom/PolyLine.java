/*
 * $Id: PolyLine.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
package com.bc.geom;

import java.awt.geom.Line2D;
import java.util.ArrayList;


public class PolyLine {

    private float _x1;
    private float _y1;
    private boolean _started;
    private ArrayList<Line2D.Float> _lines;

    public PolyLine() {
        _started = false;
    }

    public void lineTo(final float x, final float y) {
        _lines.add(new Line2D.Float(_x1, _y1, x, y));
        setXY1(x, y);
    }

    public void moveTo(final float x, final float y) {
        if (_started) {
            throw new IllegalStateException("Polyline alredy started");
        }
        setXY1(x, y);
        _lines = new ArrayList<Line2D.Float>();
        _started = true;
    }

    private void setXY1(final float x, final float y) {
        _x1 = x;
        _y1 = y;
    }

    public double getDistance(final float x, final float y) {
        double smallestDistPoints = Double.MAX_VALUE;
        double pointsDist = smallestDistPoints;
        if (_lines != null && _lines.size() > 0) {
            for (final Line2D.Float line : _lines) {
                final double distPoints = line.ptSegDistSq(x, y);
                if (distPoints < smallestDistPoints) {
                    smallestDistPoints = distPoints;
                }
            }

            pointsDist = Math.sqrt(smallestDistPoints);
        }

        return pointsDist;
    }
}
