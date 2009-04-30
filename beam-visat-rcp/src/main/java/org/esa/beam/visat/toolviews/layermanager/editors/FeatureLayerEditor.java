package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.swing.TableLayout;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Font;
import java.util.Hashtable;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FeatureLayerEditor implements LayerEditor {

    private FeatureLayer currentLayer;
    private JSlider polyFillOpacity;
    private JSlider polyStrokeOpacity;
    private JSlider textOpacity;


    @Override
    public JComponent createControl(AppContext appContext, Layer layer) {
        currentLayer = (FeatureLayer) layer;
        Hashtable<Integer, JLabel> sliderLabelTable = new Hashtable<Integer, JLabel>();
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

        JLabel fillLabel = new JLabel("Fill Opacity:");
        control.add(fillLabel);
        polyFillOpacity = new JSlider(0, 255, 255);
        polyFillOpacity.setToolTipText("Set the opacity of fillings");
        polyFillOpacity.setLabelTable(sliderLabelTable);
        polyFillOpacity.setPaintLabels(true);
        polyFillOpacity.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentLayer.setPolyFillOpacity(polyFillOpacity.getValue() / 255.0);

            }
        });
        control.add(polyFillOpacity);

        JLabel lineLabel = new JLabel("Line Opacity:");
        control.add(lineLabel);
        polyStrokeOpacity = new JSlider(0, 255, 255);
        polyStrokeOpacity.setToolTipText("Set the opacity of lines");
        polyStrokeOpacity.setLabelTable(sliderLabelTable);
        polyStrokeOpacity.setPaintLabels(true);
        polyStrokeOpacity.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentLayer.setPolyStrokeOpacity(polyStrokeOpacity.getValue() / 255.0);

            }
        });
        control.add(polyStrokeOpacity);

        JLabel labelLabel = new JLabel("Label Opacity:");
        control.add(labelLabel);
        textOpacity = new JSlider(0, 255, 255);
        textOpacity.setToolTipText("Set the opacity of labels");
        textOpacity.setLabelTable(sliderLabelTable);
        textOpacity.setPaintLabels(true);
        textOpacity.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentLayer.setTextOpacity(textOpacity.getValue() / 255.0);

            }
        });
        control.add(textOpacity);
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
    public void updateControl() {
        polyFillOpacity.setValue((int) (currentLayer.getPolyFillOpacity() * 255));
        polyStrokeOpacity.setValue((int) (currentLayer.getPolyStrokeOpacity() * 255));
        textOpacity.setValue((int) (currentLayer.getTextOpacity() * 255));
    }
}