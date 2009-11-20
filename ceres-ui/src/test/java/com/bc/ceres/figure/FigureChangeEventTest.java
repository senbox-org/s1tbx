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
package com.bc.ceres.figure;

import static org.junit.Assert.*;

import com.bc.ceres.figure.support.DefaultFigureStyle;
import com.bc.ceres.figure.support.DefaultShapeFigure;

import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;

public class FigureChangeEventTest {
    
    private Figure figure;
    private Figure[] childs;

    @Before
    public void setUp() {
        Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        figure = new DefaultShapeFigure(rectangle, true, new DefaultFigureStyle());
        childs = new Figure[2];
        childs[0] = new DefaultShapeFigure(rectangle, true, new DefaultFigureStyle());
        childs[1] = new DefaultShapeFigure(rectangle, true, new DefaultFigureStyle());
    }


    @Test
    public void testCreateChangedEvent() {
        FigureChangeEvent event = FigureChangeEvent.createChangedEvent(figure);
        assertNotNull(event.getFigure());
        assertSame(figure, event.getFigure());
        assertNull(event.getChilds());
    }

    @Test
    public void testCreateAddedEvent() {
        FigureChangeEvent event = FigureChangeEvent.createAddedEvent(figure, childs);
        assertNotNull(event.getFigure());
        assertSame(figure, event.getFigure());
        Figure[] eventChilds = event.getChilds();
        assertNotNull(eventChilds);
        assertNotSame(childs, eventChilds);
        assertEquals(childs.length, eventChilds.length);
        assertSame(childs[0], eventChilds[0]);
        assertSame(childs[1], eventChilds[1]);
    }

    @Test
    public void testCreateRemovedEvent() {
        FigureChangeEvent event = FigureChangeEvent.createRemovedEvent(figure, childs);
        assertNotNull(event.getFigure());
        assertSame(figure, event.getFigure());
        Figure[] eventChilds = event.getChilds();
        assertNotNull(eventChilds);
        assertNotSame(childs, eventChilds);
        assertEquals(childs.length, eventChilds.length);
        assertSame(childs[0], eventChilds[0]);
        assertSame(childs[1], eventChilds[1]);
    }
}
