package org.esa.beam.visat.toolviews.nav;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Graphics;


public abstract class NavigationCanvas extends JPanel {
    private final NavigationToolView navigationWindow;

    public NavigationCanvas(NavigationToolView navigationWindow) {
        super(null);
        this.navigationWindow = navigationWindow;
    }

    public NavigationToolView getNavigationWindow() {
        return navigationWindow;
    }

    /**
     * This method ignores the given parameter. It is an empty implementation to
     * prevent from setting borders on this canvas.
     *
     * @param border is ignored
     */
    @Override
    public void setBorder(Border border) {
        if (border != null) {
            BeamLogManager.getSystemLogger().warning("NavigationCanvas.setBorder() called with "
                    + border.getClass().getCanonicalName());
            BeamLogManager.getSystemLogger().warning("borders not allowed");
        }
    }

    public abstract void handleViewChanged(ProductSceneView oldView, ProductSceneView newView);

    public abstract boolean isUpdatingImageDisplay();

    public abstract void updateImage();

    public abstract void updateSlider();
}
