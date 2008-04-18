package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;


class SpectraDiagram extends Diagram {
    private Product product;
    private Band[] bands;

    public SpectraDiagram(Product product) {
        this.product = product;
        this.bands = new Band[0];
        setXAxis(new DiagramAxis("Wavelength", "nm"));
        setYAxis(new DiagramAxis("", "1"));
    }

    public Band[] getBands() {
        return bands;
    }

    public SpectrumGraph getCursorSpectrumGraph() {
        return getSpectrumGraph(null);
    }

    public SpectrumGraph getSpectrumGraph(Pin pin) {
        for (DiagramGraph graph : getGraphs()) {
            SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
            if (spectrumGraph.getPin() == pin) {
                return spectrumGraph;
            }
        }
        return null;
    }

    public void addCursorSpectrumGraph() {
        final SpectrumGraph cursorSpectrumGraph = getCursorSpectrumGraph();
        if (cursorSpectrumGraph == null) {
            addSpectrumGraph(null);
        }
    }

    public void addSpectrumGraph(Pin pin) {
        SpectrumGraph spectrumGraph = new SpectrumGraph(pin, getBands());
        DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) spectrumGraph.getStyle();
        if (pin != null) {
            Paint fillPaint = pin.getSymbol().getFillPaint();
            if (fillPaint instanceof Color) {
                style.setOutlineColor(((Color) fillPaint).darker());
            } else {
                style.setOutlineColor(pin.getSymbol().getOutlineColor());
            }
            style.setOutlineStroke(pin.getSymbol().getOutlineStroke());
            style.setFillPaint(fillPaint);
        } else {
            style.setOutlineColor(Color.BLACK);
            style.setOutlineStroke(new BasicStroke(1.5f));
            style.setFillPaint(Color.WHITE);
        }
        addGraph(spectrumGraph);
    }

    public void removeCursorSpectrumGraph() {
        final SpectrumGraph cursorSpectrumGraph = getCursorSpectrumGraph();
        if (cursorSpectrumGraph != null) {
            removeGraph(cursorSpectrumGraph);
        }
    }

    public void setBands(Band[] bands) {
        this.bands = bands;
        for (DiagramGraph graph : getGraphs()) {
            ((SpectrumGraph) graph).setBands(bands);
        }
        getYAxis().setUnit(getUnit(this.bands));
        adjustAxes(true);
        invalidate();
    }

    public void updateSpectra(int pixelX, int pixelY) {
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            try {
                ((SpectrumGraph) graph).readValues(pixelX, pixelY);
            } catch (IOException e) {
                // ignore
            }
        }
        adjustAxes(false);
        invalidate();
    }

    private static String getUnit(final Band[] bands) {
        String unit = null;
        for (final Band band : bands) {
            if (unit == null) {
                unit = band.getUnit();
            } else if (!unit.equals(band.getUnit())) {
                unit = " mixed units "; /*I18N*/
                break;
            }
        }
        return unit != null ? unit : "?";
    }

    @Override
    public void dispose() {
        if (product != null) {
            product = null;
            bands = null;
            for (DiagramGraph graph : getGraphs()) {
                SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
                spectrumGraph.dispose();   // todo - care! is SpectraDiagram always owner of its graphs?
            }
        }
        super.dispose();
    }

}
