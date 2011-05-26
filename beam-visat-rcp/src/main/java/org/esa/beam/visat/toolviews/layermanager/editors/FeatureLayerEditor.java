/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.AbstractLayerEditor;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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
public class FeatureLayerEditor extends AbstractLayerEditor {

    private JSlider polyFillTransparency;
    private JSlider polyStrokeTransparency;
    private JSlider textTransparency;

    @Override
    protected FeatureLayer getCurrentLayer() {
        return (FeatureLayer) super.getCurrentLayer();
    }

    @Override
    public JComponent createControl() {

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

        JLabel fillLabel = new JLabel("Fill transparency:");
        control.add(fillLabel);
        polyFillTransparency = new JSlider(0, 255, 255);
        polyFillTransparency.setToolTipText("Set the opacity of fillings");
        polyFillTransparency.setLabelTable(sliderLabelTable);
        polyFillTransparency.setPaintLabels(true);
        polyFillTransparency.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getCurrentLayer().setPolyFillOpacity(1.0 - polyFillTransparency.getValue() / 255.0);
            }
        });
        control.add(polyFillTransparency);

        JLabel lineLabel = new JLabel("Line transparency:");
        control.add(lineLabel);
        polyStrokeTransparency = new JSlider(0, 255, 255);
        polyStrokeTransparency.setToolTipText("Set the transparency of lines");
        polyStrokeTransparency.setLabelTable(sliderLabelTable);
        polyStrokeTransparency.setPaintLabels(true);
        polyStrokeTransparency.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getCurrentLayer().setPolyStrokeOpacity(1.0 - polyStrokeTransparency.getValue() / 255.0);
            }
        });
        control.add(polyStrokeTransparency);

        JLabel labelLabel = new JLabel("Label transparency:");
        control.add(labelLabel);
        textTransparency = new JSlider(0, 255, 255);
        textTransparency.setToolTipText("Set the transparency of labels");
        textTransparency.setLabelTable(sliderLabelTable);
        textTransparency.setPaintLabels(true);
        textTransparency.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getCurrentLayer().setTextOpacity(1.0 - textTransparency.getValue() / 255.0);
            }
        });
        control.add(textTransparency);
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
    public void handleLayerContentChanged() {
        polyFillTransparency.setValue((int) Math.round((1.0 - getCurrentLayer().getPolyFillOpacity()) * 255));
        polyStrokeTransparency.setValue((int) Math.round((1.0 - getCurrentLayer().getPolyStrokeOpacity()) * 255));
        textTransparency.setValue((int) Math.round((1.0 - getCurrentLayer().getTextOpacity()) * 255));
    }
}