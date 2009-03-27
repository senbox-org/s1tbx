package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.jidesoft.combobox.ColorComboBox;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayer;
import org.geotools.styling.Fill;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.expression.Expression;

import javax.swing.JComponent;
import javax.swing.JLabel;
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
    private ColorComboBox fillCcb;
    private ColorComboBox lineCcb;
    private final StyleBuilder sb;

    public FeatureLayerEditor() {
        sb = new StyleBuilder();
    }


    @Override
    public JComponent createControl() {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setColumnWeightX(0, 0.1);
        tableLayout.setColumnWeightX(1, 0.9);
        JPanel control = new JPanel(tableLayout);

        fillCcb = new ColorComboBox();
        fillCcb.addActionListener(new ApplyingActionListener());
        JLabel fillCcbLabel = new JLabel("Filling:");
        fillCcbLabel.setLabelFor(fillCcb);
        control.add(fillCcbLabel);
        control.add(fillCcb);

        lineCcb = new ColorComboBox();
        lineCcb.addActionListener(new ApplyingActionListener());
        JLabel lineCcbLabel = new JLabel("Line:");
        lineCcbLabel.setLabelFor(lineCcb);
        control.add(lineCcbLabel);
        control.add(lineCcb);
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
            final Expression color = sb.literalExpression(fillCcb.getSelectedColor());
            Fill fillCopy = (Fill) pages.pop();
            fillCopy.setColor(color);
            pages.push(fillCopy);
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.pop();
            strokeCopy.setColor(sb.literalExpression(lineCcb.getSelectedColor()));
            pages.push(strokeCopy);
        }
    }

    private class RetrevingStyleVisitor extends DuplicatingStyleVisitor {

        @Override
        public void visit(Fill fill) {
            super.visit(fill);
            Fill fillCopy = (Fill) pages.pop();
            Expression colorExpression = fill.getColor();
            fillCcb.setSelectedColor(colorExpression.evaluate(colorExpression, Color.class));
            pages.push(fillCopy);
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.pop();
            Expression colorExpression = strokeCopy.getColor();
            lineCcb.setSelectedColor(colorExpression.evaluate(colorExpression, Color.class));
            pages.push(strokeCopy);

        }

    }

    private class ApplyingActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            applyStyling();
        }
    }


}