/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.toolviews.insar_statistics;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 23/09/2015.
 */
public class StatBaselines implements InSARStatistic {

    private BaselineTableModel tableModel;
    private JTable table;
    private final InSARStatisticsTopComponent parent;
    private CachedBaseline[] cachedBaselines;
    private Product cachedProduct;

    private final static DecimalFormat df = new DecimalFormat("0.00");
    private final static String sep = ", ";

    public StatBaselines(final InSARStatisticsTopComponent parent) {
        this.parent = parent;
    }

    public String getName() {
        return "Baselines";
    }

    public Component createPanel() {
        tableModel = new BaselineTableModel();
        table = new JTable(tableModel);
        return new JScrollPane(table);
    }

    public CachedBaseline[] getBaselines(final Product product) {
        if (cachedBaselines == null || cachedProduct != product) {
            try {
                final List<CachedBaseline> baselines = new ArrayList<>(50);

                final InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(product);
                if(stackOverview == null)
                    return null;

                final InSARStackOverview.IfgPair[] slaves = stackOverview[0].getMasterSlave();

                for (InSARStackOverview.IfgPair slave : slaves) {
                    baselines.add(new CachedBaseline(slave));
                }
                cachedProduct = product;
                cachedBaselines = baselines.toArray(new CachedBaseline[baselines.size()]);
            } catch (Exception e) {
                SystemUtils.LOG.severe("Error getting baselines: "+ e.getMessage());
            }
        }
        return cachedBaselines;
    }

    public void update(final Product product) {

        if (InSARStatistic.isValidProduct(product)) {
            tableModel.clear();

            CachedBaseline[] baselines = getBaselines(product);
            if(baselines != null) {
                for (CachedBaseline baseline : baselines) {
                    tableModel.addRow(baseline);
                }
            }
            table.repaint();
        } else {
            tableModel.clear();
            table.repaint();
        }
    }

    public void copyToClipboard() {
        SystemUtils.copyToClipboard(getText());
    }

    public void saveToFile() {
        saveToFile(getText());
    }

    public String getHelpId() {
        return "StatBaselines";
    }

    private String getText() {
        final StringBuilder str = new StringBuilder(300);

        for (int i = 0; i < tableModel.getColumnCount(); ++i) {
            str.append(tableModel.getColumnName(i));
            str.append(sep);
        }
        str.append('\n');

        for (CachedBaseline baseline : tableModel.data) {
            str.append(baseline.toString());
            str.append('\n');
        }

        return str.toString();
    }

    private static class BaselineTableModel extends AbstractTableModel {
        private final static String[] COLUMN_NAMES =
                {"Product", "Perp Baseline [m]", "Temp Baseline [days]", "Coherence", "Height of Ambiguity [m]", "Doppler Diff [Hz]"};
        private final static Class[] COLUMN_CLASSES =
                {String.class, String.class, String.class, String.class, String.class, String.class};

        private final List<CachedBaseline> data = new ArrayList<>(50);

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_CLASSES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            CachedBaseline baseline = data.get(row);
            switch (column) {
                case 0:
                    return baseline.productName;
                case 1:
                    return baseline.perpendicularBaseline;
                case 2:
                    return baseline.temporalBaseline;
                case 3:
                    return baseline.coherence;
                case 4:
                    return baseline.hoa;
                case 5:
                    return baseline.dopplerDifference;
            }
            return null;
        }

        public void clear() {
            data.clear();
        }

        public void addRow(CachedBaseline baseline) {
            data.add(baseline);
        }
    }

    public static class CachedBaseline {
        private final String productName;
        private final String perpendicularBaseline;
        private final String temporalBaseline;
        private final String coherence;
        private final String hoa;
        private final String dopplerDifference;
        private final InSARStackOverview.IfgPair slave;

        public CachedBaseline(InSARStackOverview.IfgPair slave) {
            this.slave = slave;
            this.perpendicularBaseline = df.format(slave.getPerpendicularBaseline());
            this.temporalBaseline = df.format(slave.getTemporalBaseline());
            this.coherence = df.format(slave.getCoherence());
            this.hoa = df.format(slave.getHeightAmb());
            this.dopplerDifference = df.format(slave.getDopplerDifference());

            final MetadataElement absRoot = slave.getSlaveMetadata().getAbstractedMetadata();

            productName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
        }

        public String toString() {
            return productName +
                    sep +
                    perpendicularBaseline +
                    sep +
                    temporalBaseline +
                    sep +
                    coherence +
                    sep +
                    hoa +
                    sep +
                    dopplerDifference;
        }

        public InSARStackOverview.IfgPair getIfgPair() {
            return slave;
        }
    }
}


