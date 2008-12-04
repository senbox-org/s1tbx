/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

import java.awt.geom.Point2D;

/**
 * Default point filter, rejecting any point (x, y) where x or y is
 * infinite or not a number.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class DefaultPointFilter implements PointFilter {

    @Override
    public boolean accept(Point2D point) {
        final double x = point.getX();
        final double y = point.getY();

        return !(Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y));
    }
}
