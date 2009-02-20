package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.mpage.MultiPagePane;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefilePage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.wms.WmsPage;

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
                new LayerSource("Image layer from file", new OpenImageFilePage()),
                new LayerSource("WMS", new WmsPage()),
                new LayerSource("WFS"),
                new LayerSource("Shapefile", new ShapefilePage()),

        };

        final MultiPagePane pane = new MultiPagePane(null, "Add Layer");
        pane.show(new SelectLayerSourcePage(sources));
    }

}
