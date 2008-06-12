/*
 * $Id: Drawable.java,v 1.1 2006/10/10 14:47:22 norman Exp $
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
package org.esa.beam.framework.draw;

import java.awt.Graphics2D;

/**
 * Clients implementing this interface can be drawn on a <code>Graphics2D</code> drawing surface.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface Drawable {

    /**
     * Draws this <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     *
     * @param g2d the graphics context
     */
    void draw(Graphics2D g2d);
}
