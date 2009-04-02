package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.assistant.AssistantPane;

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

public class LayerSourceAssistantPane extends AssistantPane implements LayerSourcePageContext {

    private final AppContext appContext;
    private final Map<String, Object> properties;

    public LayerSourceAssistantPane(Window parent, String title, AppContext appContext) {
        super(parent, title);
        this.appContext = appContext;
        properties = new HashMap<String, Object>();
    }

//    @Override
//    public void setCurrentPage(AssistantPage currentPage) {
//        if (currentPage instanceof AbstractLayerSourceAssistantPage) {
//            AbstractLayerSourceAssistantPage layerSourcePage = (AbstractLayerSourceAssistantPage) currentPage;
//            layerSourcePage.setContext(this);
//        }
//    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public LayerContext getLayerContext() {
        return appContext.getSelectedProductSceneView().getLayerContext();
    }

    @Override
    public Object getPropertyValue(String key) {
        return properties.get(key);
    }

    @Override
    public void setPropertyValue(String key, Object value) {
        properties.put(key, value);
    }
}
