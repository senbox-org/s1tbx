package org.esa.beam.visat.actions;

import com.bc.layer.AbstractLayer;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.visat.VisatApp;

public class SelectTool extends AbstractTool {
    public static final String SELECT_TOOL_PROPERTY_NAME = "selectTool";

    private static final Delegator DRAG = new Delegator() {
        public void execute(AbstractTool delegate, ToolInputEvent event) {
            delegate.mouseDragged(event);
        }
    };
    private static final Delegator MOVE = new Delegator() {
        public void execute(AbstractTool delegate, ToolInputEvent event) {
            delegate.mouseMoved(event);
        }
    };
    private static final Delegator RELEASE = new Delegator() {
        public void execute(AbstractTool delegate, ToolInputEvent event) {
            delegate.mouseReleased(event);
        }
    };
    private static final Delegator PRESS = new Delegator() {
        public void execute(AbstractTool delegate, ToolInputEvent event) {
            delegate.mousePressed(event);
        }
    };
    private static final Delegator CLICK = new Delegator() {
        public void execute(AbstractTool delegateTool, ToolInputEvent event) {
            delegateTool.mouseClicked(event);
        }
    };

    @Override
    public void mouseClicked(ToolInputEvent e) {
        handleInputEvent(e, CLICK);
    }

    @Override
    public void mousePressed(ToolInputEvent e) {
        handleInputEvent(e, PRESS);
    }

    @Override
    public void mouseReleased(ToolInputEvent e) {
        handleInputEvent(e, RELEASE);
    }

    @Override
    public void mouseMoved(ToolInputEvent e) {
        handleInputEvent(e, MOVE);
    }

    @Override
    public void mouseDragged(ToolInputEvent e) {
        handleInputEvent(e, DRAG);
    }

    private AbstractTool getDelegateTool(Layer layer) {
        AbstractTool delegate = null;
        if (layer instanceof AbstractLayer) {
            AbstractLayer abstractLayer = (AbstractLayer) layer;
            final Object value = abstractLayer.getPropertyValue(SELECT_TOOL_PROPERTY_NAME);
            if (value instanceof AbstractTool) {
                delegate = (AbstractTool) value;
            }
        }
        return delegate;
    }

    private void handleInputEvent(ToolInputEvent e, Delegator method) {
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            final LayerModel layerModel = sceneView.getImageDisplay().getLayerModel();
            for (int i = 0; i < layerModel.getLayerCount(); i++) {
                final Layer layer = layerModel.getLayer(i);
                AbstractTool delegate = getDelegateTool(layer);
                if (delegate != null) {
                    method.execute(delegate, e);
                }
            }
        }
    }

    private interface Delegator {
        public void execute(AbstractTool delegate, ToolInputEvent event);
    }
}
