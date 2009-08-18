/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.TableLayout.Anchor;
import com.bc.ceres.swing.TableLayout.Fill;
import com.jidesoft.list.FilterableListModel;
import com.jidesoft.list.QuickListFilterField;
import com.jidesoft.utils.Lm;
import org.opengis.referencing.crs.ProjectedCRS;

import javax.swing.JButton;
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

public class ProjectedCrsSelectionForm extends JPanel {

    private final ProjectedCrsSelectionFormModel crsSelectionFormModel;
    private JTextArea infoArea;
    private JList crsList;

    // for testing the UI
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
        final JFrame frame = new JFrame("CRS Selection Panel");
        Container contentPane = frame.getContentPane();

        final CrsInfoListModel crsInfoListModel = new CrsInfoListModel(CrsInfo.generateSupportedCRSList());
        final ProjectedCrsSelectionFormModel model = new ProjectedCrsSelectionFormModel(crsInfoListModel);
        ProjectedCrsSelectionForm projectedCRSSelectionForm = new ProjectedCrsSelectionForm(model);
        contentPane.add(projectedCRSSelectionForm);
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


    ProjectedCrsSelectionForm(ProjectedCrsSelectionFormModel model) {
        crsSelectionFormModel = model;
        creaeUI();
        updateUIState();
    }

    private void creaeUI() {
        final QuickListFilterField filterField = new QuickListFilterField(crsSelectionFormModel.getListModel());
        filterField.setHintText("Type here to filter Projections");
        filterField.setWildcardEnabled(true);
        final FilterableListModel listModel = filterField.getDisplayListModel();
        crsList = new JList(listModel);
        crsList.setVisibleRowCount(10);
        filterField.setList(crsList);
        crsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        crsList.setSelectedValue(crsSelectionFormModel.getSelectedCrs(), true);

        final JLabel filterLabel = new JLabel("Filter:");
        final JLabel infoLabel = new JLabel("CRS Info:");
        JScrollPane crsListScrollPane = new JScrollPane(crsList);
        crsListScrollPane.setPreferredSize(new Dimension(200, 150));
        infoArea = new JTextArea(10, 30);
        infoArea.setEditable(false);
        crsList.addListSelectionListener(new CrsListSelectionListener());
        JScrollPane infoAreaScrollPane = new JScrollPane(infoArea);
        final JButton defineCrsBtn = new JButton("Create User Defined Projection");

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
        add(defineCrsBtn);
    }

    private void selectedCrsChanged(final ProjectedCRS crs) {
        if (crs != null) {
            crsSelectionFormModel.setSelectedCrs(crs);
        } else {
            crsSelectionFormModel.setSelectedCrs(null);
        }
        updateUIState();
    }

    private void updateUIState() {
        final ProjectedCRS selectedCrs = crsSelectionFormModel.getSelectedCrs();
        if(crsList.getSelectedValue() != selectedCrs){
            crsList.setSelectedValue(selectedCrs, true);
        }
        if (selectedCrs != null) {
            infoArea.setText(selectedCrs.toString());
        } else {
            infoArea.setText("");
        }
    }


    private class CrsListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            final JList list = (JList) e.getSource();
            CrsInfo selectedValue = (CrsInfo) list.getSelectedValue();
            selectedCrsChanged(selectedValue != null ? selectedValue.getCrs() : null);
        }

    }
}
