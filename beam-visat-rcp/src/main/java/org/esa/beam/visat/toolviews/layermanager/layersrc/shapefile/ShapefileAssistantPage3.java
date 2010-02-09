package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.geotools.styling.Style;
import org.opengis.util.InternationalString;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

class ShapefileAssistantPage3 extends AbstractLayerSourceAssistantPage {

    private JComboBox styleList;
    private JPanel mapPanel;
    private SwingWorker<Layer, Object> worker;
    private boolean shapeFileLoaded;
    private JLabel infoLabel;
    private JLabel mapLabel;
    private ShapefileAssistantPage3.ResizeAdapter resizeAdapter;

    ShapefileAssistantPage3() {
        super("Layer Preview");
        shapeFileLoaded = false;
    }

    @Override
    public boolean validatePage() {
        return shapeFileLoaded;
    }

    @Override
    public Component createPageComponent() {
        mapPanel = new JPanel(new BorderLayout());
        mapLabel = new JLabel();
        mapLabel.setHorizontalAlignment(JLabel.CENTER);
        mapPanel.add(mapLabel, BorderLayout.CENTER);

        LayerSourcePageContext context = getContext();
        String filePath = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FILE_PATH);
        String fileName = new File(filePath).getName();

        infoLabel = new JLabel();

        styleList = new JComboBox();
        styleList.setRenderer(new StyleListCellRenderer());
        styleList.addItemListener(new StyleSelectionListener());
        styleList.setPreferredSize(new Dimension(100, styleList.getPreferredSize().height));

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b>", fileName)), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(mapPanel, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateMap();
            }
        });
        return panel;
    }

    @Override
    public boolean performFinish() {
        LayerSourcePageContext context = getContext();
        new ShapefileLayerLoader(context).execute();
        return true;
    }

    private void updateMap() {
        if (worker != null && !worker.isDone()) {
            try {
                worker.cancel(true);
            } catch (Throwable ignore) {
                // ok
            }
        }
        mapLabel.setText("<html><i>Loading map...</i></html>");
        addToMapPanel(mapLabel);
        final LayerSourcePageContext context = getContext();
        context.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        shapeFileLoaded = false;
        context.updateState();

        worker = new ShapeFilePreviewLoader(context);
        worker.execute();
    }

    private void addToMapPanel(Component component) {
        if (resizeAdapter == null && component instanceof LayerCanvas) {
            final LayerCanvas layerCanvas = (LayerCanvas) component;
            resizeAdapter = new ResizeAdapter(layerCanvas);
            mapPanel.addComponentListener(resizeAdapter);
        } else {
            mapPanel.removeComponentListener(resizeAdapter);
            resizeAdapter = null;
        }
        mapPanel.removeAll();
        mapPanel.add(component, BorderLayout.CENTER);
    }

    private class StyleSelectionListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            LayerSourcePageContext context = getContext();
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_SELECTED_STYLE, styleList.getSelectedItem());
            context.updateState();
            updateMap();
        }
    }

    private static class StyleListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = null;
            if (value != null) {
                Style style = (Style) value;
                InternationalString title = style.getDescription().getTitle();
                if (title != null) {
                    text = title.toString();
                }else {
                    text = "Default Styler";
                }
            }
            label.setText(text);
            return label;
        }
    }

    private class ShapeFilePreviewLoader extends ShapefileLoader {

        private ShapeFilePreviewLoader(LayerSourcePageContext context) {
            super(context);
        }

        @Override
        protected void done() {
            final LayerSourcePageContext context = getContext();
            context.getWindow().setCursor(Cursor.getDefaultCursor());
            final ProductSceneView sceneView = context.getAppContext().getSelectedProductSceneView();
            try {
                final Layer layer = get();
                final LayerCanvas layerCanvas = new LayerCanvas(layer);
                layerCanvas.getViewport().setModelYAxisDown(sceneView.getLayerCanvas().getViewport().isModelYAxisDown());
                addToMapPanel(layerCanvas);
                final Rectangle2D bounds = layer.getModelBounds();
                infoLabel.setText(String.format("Model bounds [%.3f : %.3f, %.3f : %.3f]",
                                                bounds.getMinX(), bounds.getMinY(),
                                                bounds.getMaxX(), bounds.getMaxY()));

                Style[] styles = (Style[]) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_STYLES);
                Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_SELECTED_STYLE);
                styleList.setModel(new DefaultComboBoxModel(styles));
                styleList.setSelectedItem(selectedStyle);
                shapeFileLoaded = true;
            } catch (ExecutionException e) {
                final String errorMessage = MessageFormat.format("<html><b>Error:</b> <i>{0}</i></html>",
                                                                 e.getMessage());
                e.printStackTrace();
                mapLabel.setText(errorMessage);
                addToMapPanel(mapLabel);
            } catch (InterruptedException ignore) {
                // ok
            } finally {
                context.updateState();
            }
        }

    }

    private static class ResizeAdapter extends ComponentAdapter {

        private final LayerCanvas layerCanvas;

        private ResizeAdapter(LayerCanvas layerCanvas) {
            this.layerCanvas = layerCanvas;
        }

        @Override
        public void componentResized(ComponentEvent e) {
            layerCanvas.zoomAll();
        }
    }

}