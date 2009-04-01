package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public interface LayerSource {

    boolean isApplicable(LayerSourcePageContext pageContext);

    boolean hasFirstPage();

    AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext);

    boolean finish(LayerSourcePageContext pageContext);

    void cancel();
}
