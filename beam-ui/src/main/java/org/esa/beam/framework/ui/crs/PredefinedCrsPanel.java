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
package org.esa.beam.framework.ui.crs;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.TableLayout.Anchor;
import com.bc.ceres.swing.TableLayout.Fill;
import com.jidesoft.list.FilterableListModel;
import com.jidesoft.list.QuickListFilterField;
import com.jidesoft.utils.Lm;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class PredefinedCrsPanel extends JPanel {
    
    private final CrsInfoListModel crsListModel;
    private JTextArea infoArea;
    private JList crsList;
    private QuickListFilterField filterField;

    private CrsInfo selectedCrsInfo;


    // for testing the UI
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
        final JFrame frame = new JFrame("CRS Selection Panel");
        Container contentPane = frame.getContentPane();

        final CrsInfoListModel listModel = new CrsInfoListModel(CrsInfo.generateCRSList());
        PredefinedCrsPanel predefinedCrsForm = new PredefinedCrsPanel(listModel);
        contentPane.add(predefinedCrsForm);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    PredefinedCrsPanel(CrsInfoListModel model) {
        crsListModel = model;
        createUI();
    }

    private void createUI() {
        filterField = new QuickListFilterField(crsListModel);
        filterField.setHintText("Type here to filter CRS");
        filterField.setWildcardEnabled(true);
        final FilterableListModel listModel = filterField.getDisplayListModel();
        crsList = new JList(listModel);
        crsList.setVisibleRowCount(15);
        filterField.setList(crsList);
        crsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        crsList.setSelectedValue(crsListModel.getElementAt(0), true);

        final JLabel filterLabel = new JLabel("Filter:");
        final JLabel infoLabel = new JLabel("Well-Known Text (WKT):");
        final JScrollPane crsListScrollPane = new JScrollPane(crsList);
        crsListScrollPane.setPreferredSize(new Dimension(200, 150));
        infoArea = new JTextArea(15, 30);
        infoArea.setEditable(false);
        crsList.addListSelectionListener(new CrsListSelectionListener());
        final JScrollPane infoAreaScrollPane = new JScrollPane(infoArea);

        TableLayout tableLayout = new TableLayout(3);
        setLayout(tableLayout);
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTableWeightX(1);
        tableLayout.setTablePadding(4, 4);

        tableLayout.setRowWeightY(0, 0);        // no weight Y for first row
        tableLayout.setCellWeightX(0, 0, 0);    // filter label; no grow in X
        tableLayout.setRowWeightY(1, 1.0);      // second row grow in Y
        tableLayout.setCellColspan(1, 0, 2);    // CRS list; spans 2 cols
        tableLayout.setCellRowspan(1, 2, 2);    // info area; spans 2 rows
        tableLayout.setCellColspan(2, 0, 2);    // defineCrsBtn button; spans to cols

        add(filterLabel);
        add(filterField);
        add(infoLabel);
        add(crsListScrollPane);
        add(infoAreaScrollPane);
        addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                filterLabel.setEnabled((Boolean) evt.getNewValue());
                filterField.setEnabled((Boolean) evt.getNewValue());
                infoLabel.setEnabled((Boolean) evt.getNewValue());
                crsList.setEnabled((Boolean) evt.getNewValue());
                crsListScrollPane.setEnabled((Boolean) evt.getNewValue());
                infoArea.setEnabled((Boolean) evt.getNewValue());
                infoAreaScrollPane.setEnabled((Boolean) evt.getNewValue());
            }
        });
        crsList.getSelectionModel().setSelectionInterval(0,0);
    }

    private class CrsListSelectionListener implements ListSelectionListener {


        @Override
        public void valueChanged(ListSelectionEvent e) {
            final JList list = (JList) e.getSource();
            selectedCrsInfo = (CrsInfo) list.getSelectedValue();
            if (selectedCrsInfo != null) {
                try {
                    setInfoText(selectedCrsInfo.getDescription());
                } catch (Exception e1) {
                    String message = e1.getMessage();
                    if (message != null) {
                        setInfoText("Error while creating CRS:\n\n" + message);
                    }
                }
            }
        }

    }

    CrsInfo getSelectedCrsInfo() {
        return selectedCrsInfo;
    }


    private void setInfoText(String infoText) {
        infoArea.setText(infoText);
        infoArea.setCaretPosition(0);
    }
}
