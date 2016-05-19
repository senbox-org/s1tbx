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
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by luis on 23/09/2015.
 */
public class StatBaselines implements InSARStatistic {

    private TileCacheTableModel tableModel;
    private final static DecimalFormat df = new DecimalFormat("0.00");

    public String getName() {
        return "Baselines";
    }

    public Component createPanel() {
        tableModel = new TileCacheTableModel();
        return new JScrollPane(new JTable(tableModel));
    }

    public void update(final Product product) {

        if(InSARStatistic.isValidProduct(product)) {
            try {
                final InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(product);
                final InSARStackOverview.IfgPair[] slaves = stackOverview[0].getMasterSlave();

                for(InSARStackOverview.IfgPair slave : slaves) {
                    CachedBaseline baseline = new CachedBaseline(slave);
                    tableModel.addRow(baseline);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class TileCacheTableModel extends AbstractTableModel {
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
                    return baseline.hao;
                case 5:
                    return baseline.dopplerDifference;
            }
            return null;
        }

        public void addRow(CachedBaseline baseline) {
            data.add(baseline);
        }
    }

    private static class CachedBaseline {
        private final String productName;
        private final String perpendicularBaseline;
        private final String temporalBaseline;
        private final String coherence;
        private final String hao;
        private final String dopplerDifference;

        public CachedBaseline(InSARStackOverview.IfgPair slave) {
            this.perpendicularBaseline = df.format(slave.getPerpendicularBaseline());
            this.temporalBaseline = df.format(slave.getTemporalBaseline());
            this.coherence = df.format(slave.getCoherence());
            this.hao = df.format(slave.getHeightAmb());
            this.dopplerDifference = df.format(slave.getDopplerDifference());

            final MetadataElement absRoot = slave.getSlaveMetadata().getAbstractedMetadata();

            productName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
        }
    }
}


