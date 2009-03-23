package org.esa.beam.visat.toolviews.layermanager.layersrc;

import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }
//        DefaultLayerSource[] sources = new DefaultLayerSource[]{
//                new DefaultLayerSource("Image layer from band"),
//                new DefaultLayerSource("Image layer from file", new ImageFileAssistantPage()),
//                new DefaultLayerSource("WMS", new WmsAssistantPage()),
//                new DefaultLayerSource("WFS"),
//                new DefaultLayerSource("Shapefile", new ShapefileAssistantPage()),
//
//        };

//        final AssistantPane pane = new AssistantPane(null, "Add Layer");
//        pane.show(new SelectLayerSourceAssistantPage(sources));
    }

}
