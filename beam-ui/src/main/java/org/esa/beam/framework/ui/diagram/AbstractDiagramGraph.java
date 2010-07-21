/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.Guardian;


public abstract class AbstractDiagramGraph implements DiagramGraph {
    private Diagram diagram;
    private DiagramGraphStyle style;

    protected AbstractDiagramGraph() {
        style = new DefaultDiagramGraphStyle();
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        this.diagram = diagram;
    }

    public void setStyle(DiagramGraphStyle style) {
        Guardian.assertNotNull("style", style);
        this.style = style;
        invalidate();
    }

    public DiagramGraphStyle getStyle() {
        return style;
    }

    protected void invalidate() {
        if (diagram != null) {
            diagram.invalidate();
        }
    }

    public void dispose() {
        diagram = null;
        style = null;
    }
}
