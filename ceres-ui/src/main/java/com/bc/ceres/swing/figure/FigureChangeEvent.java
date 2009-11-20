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

/**
 * This event gets emitted, when a figure is changed.
 *
 * @author Marco Zuehlke
 * @since ceres 0.10
 */
public class FigureChangeEvent {
    
    private final Figure figure;
    private final Figure[] childs;

    private FigureChangeEvent(Figure figure, Figure[] childs) {
        this.figure = figure;
        this.childs = childs;
    }
    
    public Figure getFigure() {
        return figure;
    }
    
    public Figure[] getChilds() {
        return childs;
    }
    
    public static FigureChangeEvent createChangedEvent(Figure figure) {
        return new FigureChangeEvent(figure, null);
    }
    
    public static FigureChangeEvent createAddedEvent(Figure parent, Figure[] childs) {
        return new FigureChangeEvent(parent, childs.clone());
    }

    public static FigureChangeEvent createRemovedEvent(Figure parent, Figure[] childs) {
        return new FigureChangeEvent(parent, childs.clone());
    }

}
