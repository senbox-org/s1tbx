package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public interface LayerSourceController {

    boolean isApplicable(AppAssistantPageContext pageContext);

    boolean hasFirstPage();
    
    AbstractAppAssistantPage getFirstPage(AppAssistantPageContext pageContext);

    boolean finish(AppAssistantPageContext pageContext);

    void cancel();
}
