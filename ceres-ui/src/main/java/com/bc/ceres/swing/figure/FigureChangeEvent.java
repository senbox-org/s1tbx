/*
 * $Id: $
 *
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
package com.bc.ceres.swing.figure;

import java.util.EventObject;

/**
 * This event occurs, when a figure has been changed.
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class FigureChangeEvent extends EventObject {
    public final static int FIGURE_CHANGED = 0;
    public final static int FIGURES_ADDED = 1;
    public final static int FIGURES_REMOVED = 2;

    private final int type;
    private final Figure[] figures;

    /**
     * Constructor.
     *
     * @param sourceFigure The source figure which caused the event.
     * @param type         The type of the event. Always one of {@link #FIGURES_ADDED},
     *                     {@link #FIGURES_REMOVED}, {@link #FIGURE_CHANGED}.
     * @param figures      The figures added or removed. Should be {@code null} if the event type
     *                     is {@link #FIGURE_CHANGED}.
     */
    public FigureChangeEvent(Figure sourceFigure, int type, Figure[] figures) {
        super(sourceFigure);
        this.type = type;
        this.figures = figures != null ? figures.clone() : null;
    }

    /**
     * @return The source figure which caused the event.
     */
    public Figure getSourceFigure() {
        return (Figure) getSource();
    }

    /**
     * @return The type of the event. Always one of {@link #FIGURES_ADDED}, {@link #FIGURES_REMOVED}, {@link #FIGURE_CHANGED}.
     */
    public int getType() {
        return type;
    }

    /**
     * @return The figures added or removed. Returns {@code null} if the event type is {@link #FIGURE_CHANGED}.
     */
    public Figure[] getFigures() {
        return figures;
    }

}
