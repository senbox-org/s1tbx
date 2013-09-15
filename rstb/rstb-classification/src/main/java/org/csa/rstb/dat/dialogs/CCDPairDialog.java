/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.dat.dialogs;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.ProductSetPanel;
import org.esa.nest.dat.dialogs.SourceProductPanel;
import org.esa.nest.dat.util.ProductOpener;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.db.DBQuery;
import org.esa.nest.db.ProductDB;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Find CCD Pairs
 */
public class CCDPairDialog extends ModalDialog {

    private boolean ok = false;

    private SourceProductPanel sourcePanel;
    private ProductSetPanel productListPanel = new ProductSetPanel(VisatApp.getApp(), "Results");

    private final JRadioButton dateBeforeMasterBtn = new JRadioButton("Date before master", true);
    private final JRadioButton dateClosestBtn = new JRadioButton("Closest date");
    private final JTextField maxSlavesField = new JTextField("1");
    private JButton openBtn;

    private int maxSlaves = 1;

    public CCDPairDialog() {
        super(VisatApp.getApp().getMainFrame(), "Find CCD Pairs", ModalDialog.ID_OK_CANCEL_HELP, null);

        getButton(ID_OK).setText("Search");
        getButton(ID_CANCEL).setText("Close");

        initContent();
    }

    private void initContent() {
        final JPanel contentPane = new JPanel(new BorderLayout());

        sourcePanel = new SourceProductPanel(VisatApp.getApp());
        sourcePanel.initProducts();
        contentPane.add(sourcePanel, BorderLayout.NORTH);

        final ButtonGroup group = new ButtonGroup();
        group.add(dateBeforeMasterBtn);
        group.add(dateClosestBtn);

        final JPanel optionsPane = new JPanel(new GridBagLayout());
        optionsPane.setBorder(BorderFactory.createTitledBorder("Options"));
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.gridy++;
        DialogUtils.addComponent(optionsPane, gbc, "", dateBeforeMasterBtn);
        gbc.gridy++;
        DialogUtils.addComponent(optionsPane, gbc, "", dateClosestBtn);
        gbc.gridy++;
        maxSlavesField.setColumns(20);
        DialogUtils.addComponent(optionsPane, gbc, "Max Slaves:", maxSlavesField);
        contentPane.add(optionsPane, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        openBtn = DialogUtils.CreateButton("openButton", "Open", null, buttonPanel);
        openBtn.setEnabled(false);
        openBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                File[] files = productListPanel.getSelectedFiles();
                if(files.length == 0)                      // default to get all files
                    files = productListPanel.getFileList();
                final ProductOpener opener = new ProductOpener(VisatApp.getApp());
                opener.openProducts(files);
            }
        });
        buttonPanel.add(openBtn);
        productListPanel.add(buttonPanel, BorderLayout.EAST);
        contentPane.add(productListPanel, BorderLayout.SOUTH);

        setContent(contentPane);
    }

    private void validate() throws Exception {
        String maxSlaveStr = maxSlavesField.getText();
        if(maxSlaveStr.isEmpty())
            maxSlaveStr = "1";
        maxSlaves = Integer.parseInt(maxSlaveStr);
        final Product product = sourcePanel.getSelectedSourceProduct();
        if(product == null) {
            throw new Exception("Please select a product");
        }
        if(!OperatorUtils.isComplex(product)) {
            throw new Exception("Input product must be complex.\nPlease select a SLC product");  
        }
    }

    protected void onOK() {
        try {
            validate();

            final ProductEntry masterEntry = new ProductEntry(sourcePanel.getSelectedSourceProduct());
            final ProductEntry[] results = findCCDPairs(ProductDB.instance(), masterEntry,
                                              maxSlaves, dateClosestBtn.isSelected());

            if(results.length == 0) {
                openBtn.setEnabled(false);
                VisatApp.getApp().showWarningDialog("No CCD pairs found.\n"+
                        "Please make sure overlapping SLCs exist in the Product Library");   
            } else {
                productListPanel.setProductEntryList(results);
                openBtn.setEnabled(true);
                ok = true;
            }
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog("Error: "+e.getMessage());
        }
    }

    public boolean IsOK() {
        return ok;
    }

    private static ProductEntry[] findCCDPairs(final ProductDB db, final ProductEntry master,
                                       final int maxSlaves, final boolean anyDate) {
        final DBQuery dbQuery = new DBQuery();
        dbQuery.setFreeQuery(AbstractMetadata.PRODUCT+" <> '"+master.getName()+ '\'');
        dbQuery.setSelectionRect(master.getGeoBoundary());
        dbQuery.setSelectedPass(master.getPass());
        dbQuery.setSelectedSampleType("COMPLEX");
        try {
            final ProductEntry[] entries = dbQuery.queryDatabase(db);
            return getClosestDatePairs(entries, master, dbQuery, maxSlaves, anyDate);
        } catch(Throwable t) {
            VisatApp.getApp().showErrorDialog("Query database error:"+t.getMessage());
            return null;
        }
    }

    private static ProductEntry[] getClosestDatePairs(final ProductEntry[] entries,
                                                      final ProductEntry master, DBQuery dbQuery,
                                                      final int maxSlaves, final boolean anyDate) {
        final double masterTime = master.getFirstLineTime().getMJD();
        double cutoffTime = masterTime;
        if(dbQuery != null && dbQuery.getEndDate() != null) {
            final double endTime = ProductData.UTC.create(dbQuery.getEndDate().getTime(), 0).getMJD();
            if(endTime > masterTime)
                cutoffTime = endTime;
        }

        final List<ProductEntry> resultList = new ArrayList<ProductEntry>(maxSlaves);
        final Map<Double, ProductEntry> timesMap = new HashMap<Double, ProductEntry>();
        final List<Double> diffList = new ArrayList<Double>();
        // find all before masterTime
        for(ProductEntry entry : entries) {
            final double entryTime = entry.getFirstLineTime().getMJD();
            if(anyDate || entryTime < cutoffTime) {
                final double diff = Math.abs(masterTime - entryTime);
                timesMap.put(diff, entry);
                diffList.add(diff);
            }
        }
        Collections.sort(diffList);
        // select only the closest up to maxPairs
        for(Double diff : diffList) {
            resultList.add(timesMap.get(diff));
            if(resultList.size() >= maxSlaves)
                break;
        }

        return resultList.toArray(new ProductEntry[resultList.size()]);
    }
}