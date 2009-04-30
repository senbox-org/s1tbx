package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.dom.XStreamDomConverter;
import org.esa.beam.framework.draw.Figure;

class FigureDomConverter extends XStreamDomConverter {

    public FigureDomConverter() {
        super(Figure.class);
    }
}
