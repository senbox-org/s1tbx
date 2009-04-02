package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

/**
 * todo - api doc
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public interface LayerSource {

    boolean isApplicable(LayerSourcePageContext pageContext);

    boolean hasFirstPage();

    AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext);

    boolean canFinish(LayerSourcePageContext pageContext);

    boolean finish(LayerSourcePageContext pageContext);

    void cancel();
}
