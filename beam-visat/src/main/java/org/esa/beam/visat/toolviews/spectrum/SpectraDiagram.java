package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;

import java.io.IOException;


class SpectraDiagram extends Diagram {
    private Product product;
    private Band[] bands;
    private double xMinAccum;
    private double xMaxAccum;
    private double yMinAccum;
    private double yMaxAccum;

    public SpectraDiagram(Product product) {
        this.product = product;
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

    public void setBands(Band[] bands) {
        this.bands = bands;
        for (DiagramGraph graph : getGraphs()) {
            ((SpectrumGraph) graph).setBands(bands);
        }
    }

    public void setAxesMinMaxAccumulatorsToAxesMinMax() {
        xMinAccum = getXAxis().getMinValue();
        xMaxAccum = getXAxis().getMaxValue();
        yMinAccum = getYAxis().getMinValue();
        yMaxAccum = getYAxis().getMaxValue();
    }

    public void updateYUnit() {
        getYAxis().setUnit(getUnit(bands));
    }

    public void updateSpectra(final int pixelX, final int pixelY) {
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            updateGraph((SpectrumGraph) graph, pixelX, pixelY);
        }
    }

    private void updateGraph(SpectrumGraph spectrumGraph, int pixelX, int pixelY) {
        try {
            spectrumGraph.readValues(pixelX, pixelY);
            handleGraphDataChanged(spectrumGraph);
        } catch (IOException e) {
            // todo - handle!
        }
    }

    private void handleGraphDataChanged(SpectrumGraph spectrumGraph) {
        final DiagramAxis xAxis = getXAxis();
        xMinAccum = Math.min(xMinAccum, spectrumGraph.getXMin());
        xMaxAccum = Math.max(xMaxAccum, spectrumGraph.getXMax());
        boolean xRangeValid = xMaxAccum > xMinAccum;
        if (xRangeValid) {
            xAxis.setValueRange(xMinAccum, xMaxAccum);
            xAxis.setOptimalSubDivision(4, 6, 5);
        } else {
            // todo - handle!
        }

        final DiagramAxis yAxis = getYAxis();
        yMinAccum = Math.min(yMinAccum, spectrumGraph.getYMin());
        yMaxAccum = Math.max(yMaxAccum, spectrumGraph.getYMax());
        boolean yRangeValid = yMaxAccum > yMinAccum;
        if (yRangeValid) {
            yAxis.setValueRange(yMinAccum, yMaxAccum);
            yAxis.setOptimalSubDivision(3, 6, 5);
        } else {
            // todo - handle!
        }
        if (xRangeValid && yRangeValid) {
            // todo - handle!
        }
    }

    public void resetAxesMinMaxAccumulators() {
        xMinAccum = +Double.MAX_VALUE;
        xMaxAccum = -Double.MAX_VALUE;
        yMinAccum = +Double.MAX_VALUE;
        yMaxAccum = -Double.MAX_VALUE;
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

    public void dispose() {
        if (product != null) {
            product = null;
            bands = null;
            for (DiagramGraph graph : getGraphs()) {
                SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
                spectrumGraph.dispose();
            }
        }
    }

}
