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
