package org.esa.beam.unmixing.ui;

import org.esa.beam.framework.ui.diagram.DefaultDiagramGraph;
import org.esa.beam.unmixing.Endmember;

class EndmemberGraph extends DefaultDiagramGraph {
    private Endmember endmember;

    public EndmemberGraph(Endmember endmember) {
        super("Wavelength", endmember.getWavelengths(), endmember.getName(), endmember.getRadiations());
    }

    public Endmember getEndmember() {
        return endmember;
    }

    @Override
    public void dispose() {
        endmember = null;
        super.dispose();
    }
}
