package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.assistant.AssistantPane;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefileAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.wms.WmsAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.image.ImageFileAssistantPage;

import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }
        LayerSource[] sources = new LayerSource[]{
                new LayerSource("Image layer from band"),
                new LayerSource("Image layer from file", new ImageFileAssistantPage()),
                new LayerSource("WMS", new WmsAssistantPage()),
                new LayerSource("WFS"),
                new LayerSource("Shapefile", new ShapefileAssistantPage()),

        };

        final AssistantPane pane = new AssistantPane(null, "Add Layer");
        pane.show(new SelectLayerSourceAssistantPage(sources));
    }

}
