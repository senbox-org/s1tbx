/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.productlibrary;

import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.db.DBQuery;
import org.esa.nest.db.ProductDB;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.util.DialogUtils;
import org.esa.nest.util.SQLUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**

 */
public final class DatabasePane extends JPanel {

    private final JList missionJList = new JList();
    private final JList productTypeJList = new JList();
    private final JComboBox acquisitionModeCombo = new JComboBox(new String[] { DBQuery.ALL_MODES });
    private final JComboBox passCombo = new JComboBox(new String[] {
            DBQuery.ALL_PASSES, DBQuery.ASCENDING_PASS, DBQuery.DESCENDING_PASS });
    private final JTextField trackField = new JTextField();
    private final DateComboBox startDateBox = new DateComboBox();
    private final DateComboBox endDateBox = new DateComboBox();
    private final JComboBox polarizationCombo = new JComboBox(new String[] {
            DBQuery.ANY, DBQuery.DUALPOL, DBQuery.QUADPOL, "HH", "VV", "HV", "VH" });
    private final JComboBox calibrationCombo = new JComboBox(new String[] {
            DBQuery.ANY, DBQuery.CALIBRATED, DBQuery.NOT_CALIBRATED });
    private final JComboBox orbitCorrectionCombo = new JComboBox(new String[] {
            DBQuery.ANY, DBQuery.ORBIT_PRELIMINARY, DBQuery.ORBIT_PRECISE, DBQuery.ORBIT_VERIFIED });

    private final JComboBox metadataNameCombo = new JComboBox();
    private final JTextField metdataValueField = new JTextField();
    private final JTextArea metadataArea = new JTextArea();
    private final JButton addMetadataButton = new JButton("+");
    private final JButton updateButton = new JButton(UIUtils.loadImageIcon("icons/Update16.gif"));

    private ProductDB db;
    private DBQuery dbQuery = new DBQuery();
    private ProductEntry[] productEntryList = null;
    boolean modifyingCombos = false;

    private final List<DatabaseQueryListener> listenerList = new ArrayList<DatabaseQueryListener>(1);

    public DatabasePane() {
        try {
            missionJList.setFixedCellWidth(100);
            createPanel();

            missionJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if(modifyingCombos || event.getValueIsAdjusting()) return;
                    updateProductTypeCombo();
                    queryDatabase();
                }
            });
            productTypeJList.setFixedCellWidth(100);
            productTypeJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if(modifyingCombos || event.getValueIsAdjusting()) return;
                    queryDatabase();
                }
            });
            addComboListener(acquisitionModeCombo);
            addComboListener(passCombo);
            addComboListener(polarizationCombo);
            addComboListener(calibrationCombo);
            addComboListener(orbitCorrectionCombo);

            startDateBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if(modifyingCombos || event.getStateChange() == ItemEvent.DESELECTED) return;
                    queryDatabase();
                }
            });
            endDateBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if(modifyingCombos || event.getStateChange() == ItemEvent.DESELECTED) return;
                    queryDatabase();
                }
            });
            addMetadataButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addMetadataText();
                }
            });
            updateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    queryDatabase();
                }
            });
        } catch(Throwable t) {
            handleException(t);
        }
    }

    private void addComboListener(final JComboBox combo) {
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if(modifyingCombos || event.getStateChange() == ItemEvent.DESELECTED) return;
                queryDatabase();
            }
        });
    }

    /**
     * Adds a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be added.
     */
    public void addListener(final DatabaseQueryListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be removed.
     */
    public void removeListener(final DatabaseQueryListener listener) {
        listenerList.remove(listener);
    }

    private void notifyQuery() {
        for (final DatabaseQueryListener listener : listenerList) {
            listener.notifyNewProductEntryListAvailable();
        }
    }

    private static void handleException(Throwable t) {
        t.printStackTrace();
        final VisatApp app = VisatApp.getApp();
        if(app != null) {
            app.showErrorDialog(t.getMessage());
        }
    }

    private void createPanel() {
        setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        JLabel label;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(new JLabel("Mission:"), gbc);
        gbc.gridx = 1;
        this.add(new JLabel("Product Type:"), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        this.add(new JScrollPane(missionJList), gbc);
        gbc.gridx = 1;
        this.add(new JScrollPane(productTypeJList), gbc);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Acquisition Mode:", acquisitionModeCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Pass:", passCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Track:", trackField);
        label.setHorizontalAlignment(JLabel.RIGHT);

        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Start Date:", startDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "End Date:", endDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Polarization:", polarizationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Calibration:", calibrationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Orbit Correction:", orbitCorrectionCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        gbc.gridx = 0;
        this.add(metadataNameCombo, gbc);
        metadataNameCombo.setPrototypeDisplayValue("1234567890123456789");
        gbc.gridx = 1;
        this.add(metdataValueField, gbc);
        gbc.gridx = 2;
        this.add(addMetadataButton, gbc);
        addMetadataButton.setMaximumSize(new Dimension(3, 3));

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        this.add(metadataArea, gbc);
        metadataArea.setBorder(new LineBorder(Color.BLACK));
        metadataArea.setLineWrap(true);
        metadataArea.setRows(4);
        metadataArea.setToolTipText("Use AND,OR,NOT and =,<,>,<=,>-");
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(updateButton, gbc);
        updateButton.setMaximumSize(new Dimension(3, 3));

        DialogUtils.fillPanel(this, gbc);
    }

    private void connectToDatabase() throws Exception {
        db = ProductDB.instance();

        refresh();
    }

    public ProductDB getDB() {
        if(db == null) {
            queryDatabase();
        }
        return db;
    }

    public void refresh() {
        try {
            boolean origState = lockCombos(true);

            if(metadataNameCombo.getItemCount() == 0) {
                final String[] metadataNames = db.getMetadataNames();
                for(String name : metadataNames) {
                    metadataNameCombo.insertItemAt(name, metadataNameCombo.getItemCount());
                }
            }

            updateMissionCombo();
            lockCombos(origState);
        } catch(Throwable t) {
            handleException(t);
        }
    }

    private boolean lockCombos(boolean flag) {
        final boolean origState = modifyingCombos;
        modifyingCombos = flag;
        return origState;
    }

    private void updateMissionCombo() throws SQLException {
        boolean origState = lockCombos(true);
        try {
            missionJList.removeAll();
            missionJList.setListData(SQLUtils.prependString(DBQuery.ALL_MISSIONS, db.getAllMissions()));
        } finally {
            lockCombos(origState);
        }
    }

    private void updateProductTypeCombo() {
        boolean origState = lockCombos(true);
        try {
            productTypeJList.removeAll();
            acquisitionModeCombo.removeAllItems();

            final String selectedMissions[] = toStringArray(missionJList.getSelectedValues());
            String[] productTypeList;
            String[] acquisitionModeList;
            if(StringUtils.contains(selectedMissions, DBQuery.ALL_MISSIONS)) {
                productTypeList = db.getAllProductTypes();
                acquisitionModeList = db.getAllAcquisitionModes();
            } else {
                productTypeList = db.getProductTypes(selectedMissions);
                acquisitionModeList = db.getAcquisitionModes(selectedMissions);
            }
            productTypeJList.setListData(SQLUtils.prependString(DBQuery.ALL_PRODUCT_TYPES, productTypeList));
            final String[] modeItems = SQLUtils.prependString(DBQuery.ALL_MODES, acquisitionModeList);
            for(String item : modeItems) {
                acquisitionModeCombo.addItem(item);
            }

        } catch(Throwable t) {
            handleException(t);
        } finally {
            lockCombos(origState);
        }
    }

    private static String[] toStringArray(Object[] objects) {
        final String strArray[] = new String[objects.length];
        for(int i=0; i<objects.length; ++i) {
            strArray[i] = (String)objects[i];
        }
        return strArray;
    }

    public void setBaseDir(final File dir) {
        dbQuery.setBaseDir(dir);
        if(db != null)
            queryDatabase();
    }

    public void removeProducts(final File baseDir) {
        try {
            db.removeProducts(baseDir);
        } catch(Throwable t) {
            handleException(t);
        }
    }

    private void addMetadataText() {
        final String name = (String)metadataNameCombo.getSelectedItem();
        final String value = metdataValueField.getText();
        if(!name.isEmpty() && !value.isEmpty()) {
            if(metadataArea.getText().length() > 0)
                metadataArea.append(" AND ");
            metadataArea.append(name+"='"+value+"' ");
        }
    }

    private void setData() {
        dbQuery.setSelectedMissions(toStringArray(missionJList.getSelectedValues()));
        dbQuery.setSelectedProductTypes(toStringArray(productTypeJList.getSelectedValues()));
        dbQuery.setSelectedAcquisitionMode((String) acquisitionModeCombo.getSelectedItem());
        dbQuery.setSelectedPass((String)passCombo.getSelectedItem());
        dbQuery.setSelectedTrack(trackField.getText());
        dbQuery.setStartEndDate(startDateBox.getCalendar(), endDateBox.getCalendar());

        dbQuery.setSelectedPolarization((String)polarizationCombo.getSelectedItem());
        dbQuery.setSelectedCalibration((String)calibrationCombo.getSelectedItem());
        dbQuery.setSelectedOrbitCorrection((String)orbitCorrectionCombo.getSelectedItem());

        dbQuery.clearMetadataQuery();
        dbQuery.setFreeQuery(metadataArea.getText());
    }

    private void queryDatabase() {
        if(db == null) {
            try {
                connectToDatabase();
            } catch(Throwable t) {
                handleException(t);
            }
        }
        setData();

        if(productEntryList != null) {
            ProductEntry.dispose(productEntryList);
        }
        try {
            productEntryList = dbQuery.queryDatabase(db);
        } catch(Throwable t) {
            handleException(t);
        }

        notifyQuery();
    }

    public void setSelectionRect(final GeoPos[] selectionBox) {
        dbQuery.setSelectionRect(selectionBox);
        queryDatabase();
    }

    public ProductEntry[] getProductEntryList() {
        return productEntryList;
    }

    public DBQuery getDBQuery() {
        setData();
        return dbQuery;
    }

    public void setDBQuery(final DBQuery query) throws Exception {
        if(query == null) return;
        dbQuery = query;
        if(db == null) {
            connectToDatabase();
        }
        boolean origState = lockCombos(true);
        try {
            missionJList.setSelectedIndices(findIndices(missionJList, dbQuery.getSelectedMissions()));
            updateProductTypeCombo();
            productTypeJList.setSelectedIndices(findIndices(productTypeJList, dbQuery.getSelectedProductTypes()));
            acquisitionModeCombo.setSelectedItem(dbQuery.getSelectedAcquisitionMode());
            passCombo.setSelectedItem(dbQuery.getSelectedPass());
            startDateBox.setCalendar(dbQuery.getStartDate());
            endDateBox.setCalendar(dbQuery.getEndDate());

            polarizationCombo.setSelectedItem(dbQuery.getSelectedPolarization());
            calibrationCombo.setSelectedItem(dbQuery.getSelectedCalibration());
            orbitCorrectionCombo.setSelectedItem(dbQuery.getSelectedOrbitCorrection());

            metadataArea.setText(dbQuery.getFreeQuery());
        } finally {
            lockCombos(origState);
        }
    }

    private static int[] findIndices(final JList list, final String[] values) {
        final int size = list.getModel().getSize();
        final List<Integer> indices = new ArrayList<Integer>(size);
        for(int i=0; i < size; ++i) {
            final String str = (String)list.getModel().getElementAt(i);
            if(StringUtils.contains(values, str)) {
                indices.add(i);
            }
        }
        final int[] intIndices = new int[indices.size()];
        for(int i=0; i < indices.size(); ++i) {
            intIndices[i] = indices.get(i);
        }
        return intIndices;
    }
}
