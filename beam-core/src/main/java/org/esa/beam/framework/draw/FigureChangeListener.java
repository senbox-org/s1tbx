/*
 * $Id: FigureChangeListener.java,v 1.1 2006/10/10 14:47:22 norman Exp $
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

import java.util.EventListener;

/**
 * A listener interested in figure changes.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public interface FigureChangeListener extends EventListener {

    /**
     * Sent when an area is invalid
     */
    void figureInvalidated(FigureChangeEvent e);

    /**
     * Sent when a figure changed
     */
    void figureChanged(FigureChangeEvent e);

    /**
     * Sent when a figure was removed
     */
    void figureRemoved(FigureChangeEvent e);

    /**
     * Sent when requesting to remove a figure.
     */
    void figureRequestRemove(FigureChangeEvent e);

    /**
     * Sent when an update should happen.
     */
    void figureRequestUpdate(FigureChangeEvent e);
}