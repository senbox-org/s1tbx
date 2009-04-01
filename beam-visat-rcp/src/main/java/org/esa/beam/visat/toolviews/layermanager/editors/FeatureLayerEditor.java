package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayer;
import org.geotools.styling.Fill;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.expression.Expression;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Font;
import java.util.Hashtable;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FeatureLayerEditor implements LayerEditor {

    private FeatureLayer currentLayer;
    private JSlider fillOpacity;
    private final StyleBuilder sb;
    private JSlider lineOpacity;
    private JSlider labelOpacity;
    private ApplyingChangeListener applyingChangeListener;

    public FeatureLayerEditor() {
        sb = new StyleBuilder();
    }


    @Override
    public JComponent createControl() {
        Hashtable sliderLabelTable = new Hashtable();
        sliderLabelTable.put(0, createSliderLabel("0%"));
        sliderLabelTable.put(127, createSliderLabel("50%"));
        sliderLabelTable.put(255, createSliderLabel("100%"));

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnWeightX(0, 0.4);
        tableLayout.setColumnWeightX(1, 0.6);
        tableLayout.setRowWeightY(3, 1.0);
        tableLayout.setTablePadding(4, 4);
        JPanel control = new JPanel(tableLayout);
        applyingChangeListener = new ApplyingChangeListener();

        JLabel fillLabel = new JLabel("Fill Opacity:");
        control.add(fillLabel);
        fillOpacity = new JSlider(0, 255, 255);
        fillOpacity.setToolTipText("Set the opacity of fillings");
        fillOpacity.setLabelTable(sliderLabelTable);
        fillOpacity.setPaintLabels(true);
        control.add(fillOpacity);

        JLabel lineLabel = new JLabel("Line Opacity:");
        control.add(lineLabel);
        lineOpacity = new JSlider(0, 255, 255);
        lineOpacity.setToolTipText("Set the opacity of lines");
        lineOpacity.setLabelTable(sliderLabelTable);
        lineOpacity.setPaintLabels(true);
        control.add(lineOpacity);

        JLabel labelLabel = new JLabel("Label Opacity:");
        control.add(labelLabel);
        labelOpacity = new JSlider(0, 255, 255);
        labelOpacity.setToolTipText("Set the opacity of labels");
        labelOpacity.setLabelTable(sliderLabelTable);
        labelOpacity.setPaintLabels(true);
        control.add(labelOpacity);


        addApplyListener();

        control.add(new JPanel()); // filler
        return control;
    }

    private JLabel createSliderLabel(String text) {
        JLabel label = new JLabel(text);
        Font oldFont = label.getFont();
        Font newFont = oldFont.deriveFont(oldFont.getSize2D() * 0.85f);
        label.setFont(newFont);
        return label;
    }

    @Override
    public synchronized void updateControl(Layer selectedLayer) {
        LayerType layerType = selectedLayer.getLayerType();
        if (currentLayer != selectedLayer && layerType instanceof FeatureLayer.Type) {
            currentLayer = (FeatureLayer) selectedLayer;
            currentLayer.getSLDStyle().accept(new RetrevingStyleVisitor());
        }
    }

    private void addApplyListener() {
        fillOpacity.addChangeListener(applyingChangeListener);
        lineOpacity.addChangeListener(applyingChangeListener);
        labelOpacity.addChangeListener(applyingChangeListener);
    }

    private synchronized void applyStyling() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DuplicatingStyleVisitor copyStyle = new ApplyingStyleVisitor();
                Style style = currentLayer.getSLDStyle();
                style.accept(copyStyle);
                currentLayer.setSLDStyle((Style) copyStyle.getCopy());
                return null;
            }
        };
        worker.execute();
    }

    private class ApplyingStyleVisitor extends DuplicatingStyleVisitor {

        @Override
        public void visit(Fill fill) {
            super.visit(fill);
            Fill fillCopy = (Fill) pages.peek();
            double opacity = fillOpacity.getValue() / 255.0;
            fillCopy.setOpacity(sb.literalExpression(opacity));
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.peek();
            double opacity = lineOpacity.getValue() / 255.0;
            strokeCopy.setOpacity(sb.literalExpression(opacity));
        }

        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.peek();
            double opacity = labelOpacity.getValue() / 255.0;
            Fill textFill = textCopy.getFill();
            if (textFill == null) {
                textFill = sb.createFill(Color.BLACK, opacity);
                textCopy.setFill(textFill);
            } else {
                textFill.setOpacity(sb.literalExpression(opacity));
            }
        }
    }

    private class RetrevingStyleVisitor extends DuplicatingStyleVisitor {

        @Override
        public void visit(Fill fill) {
            super.visit(fill);
            Fill fillCopy = (Fill) pages.peek();
            Expression opacityExpression = fillCopy.getOpacity();
            if (opacityExpression != null) {
                fillOpacity.setValue((int) (opacityExpression.evaluate(opacityExpression, Double.class) * 255));
            } else {
                fillOpacity.setValue(255);
            }
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.peek();
            Expression opacityExpression = strokeCopy.getOpacity();
            if (opacityExpression != null) {
                lineOpacity.setValue((int) (opacityExpression.evaluate(opacityExpression, Double.class) * 255));
            } else {
                lineOpacity.setValue(255);
            }
        }

        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.peek();
            Fill textFill = textCopy.getFill();
            if (textFill != null) {
                Expression opacityExpression = textFill.getOpacity();
                if (opacityExpression != null) {
                    labelOpacity.setValue((int) (opacityExpression.evaluate(opacityExpression, Double.class) * 255));
                }
            } else {
                labelOpacity.setValue(255);
            }
        }
    }

    private class ApplyingChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            applyStyling();
        }

    }

}