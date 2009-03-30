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

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FeatureLayerEditor implements LayerEditor {

    private FeatureLayer currentLayer;
    private JCheckBox fillCb;
    private AlphaComboBox fillAcb;
    private JCheckBox lineCb;
    private final StyleBuilder sb;
    private AlphaComboBox lineAcb;
    private JCheckBox labelCb;
    private AlphaComboBox labelAcb;
    private ApplyingActionListener applyingActionListener;

    public FeatureLayerEditor() {
        sb = new StyleBuilder();
    }


    @Override
    public JComponent createControl() {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnWeightX(0, 0.1);
        tableLayout.setColumnWeightX(1, 0.9);
        tableLayout.setRowWeightY(3, 1.0);
        tableLayout.setTablePadding(4, 4);
        JPanel control = new JPanel(tableLayout);
        applyingActionListener = new ApplyingActionListener();

        fillCb = new JCheckBox("Fill:", true);
        fillCb.setToolTipText("Toggle visibility of fillings");
        control.add(fillCb);
        fillAcb = new AlphaComboBox(0, 1, 1, 255);
        fillAcb.setToolTipText("Set opacity of fillings");
        control.add(fillAcb);

        lineCb = new JCheckBox("Line:", true);
        lineCb.setToolTipText("Toggle visibility of lines");
        control.add(lineCb);
        lineAcb = new AlphaComboBox(0, 1, 1, 255);
        lineAcb.setToolTipText("Set opacity of lines");
        control.add(lineAcb);

        labelCb = new JCheckBox("Label:", true);
        labelCb.setToolTipText("Toggle visibility of labels");
        control.add(labelCb);
        labelAcb = new AlphaComboBox(0, 1, 1, 255);
        labelAcb.setToolTipText("Set opacity of labels");
        control.add(labelAcb);


        addApplyListener();

        control.add(new JPanel()); // filler
        return control;
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
        fillCb.addActionListener(applyingActionListener);
        fillAcb.addActionListener(applyingActionListener);
        lineCb.addActionListener(applyingActionListener);
        lineAcb.addActionListener(applyingActionListener);
        labelCb.addActionListener(applyingActionListener);
        labelAcb.addActionListener(applyingActionListener);
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
            Fill fillCopy = (Fill) pages.pop();
            double opacity = 0.0;
            if (fillCb.isSelected()) {
                opacity = fillAcb.getValue();
            }
            fillCopy.setOpacity(sb.literalExpression(opacity));
            pages.push(fillCopy);
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.pop();
            double opacity = 0.0;
            if (lineCb.isSelected()) {
                opacity = lineAcb.getValue();
            }
            strokeCopy.setOpacity(sb.literalExpression(opacity));
            pages.push(strokeCopy);
        }

        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.pop();
            double opacity = 0.0;
            if (labelCb.isSelected()) {
                opacity = labelAcb.getValue();
            }
            Fill textFill = textCopy.getFill();
            if (textFill == null) {
                textFill = sb.createFill(Color.BLACK, opacity);
                textCopy.setFill(textFill);
            } else {
                textFill.setOpacity(sb.literalExpression(opacity));
            }
            pages.push(textCopy);

        }
    }

    private class RetrevingStyleVisitor extends DuplicatingStyleVisitor {

        @Override
        public void visit(Fill fill) {
            super.visit(fill);
            Fill fillCopy = (Fill) pages.pop();
            Expression opacityExpression = fillCopy.getOpacity();
            if (opacityExpression != null) {
                fillAcb.setValue(opacityExpression.evaluate(opacityExpression, Double.class));
            } else {
                fillAcb.setValue(1.0);
            }
            pages.push(fillCopy);
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.pop();
            Expression opacityExpression = strokeCopy.getOpacity();
            if (opacityExpression != null) {
                lineAcb.setValue(opacityExpression.evaluate(opacityExpression, Double.class));
            } else {
                lineAcb.setValue(1.0);
            }
            pages.push(strokeCopy);
        }

        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.pop();
            Fill textFill = textCopy.getFill();
            if (textFill != null) {
                Expression opacityExpression = textFill.getOpacity();
                if (opacityExpression != null) {
                    labelAcb.setValue(opacityExpression.evaluate(opacityExpression, Double.class));
                }
            } else {
                labelAcb.setValue(1.0);
            }
            pages.push(textCopy);

        }
    }

    private class ApplyingActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            applyStyling();
        }
    }

}