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
package org.esa.s1tbx.insar.rcp.toolviews;

import org.esa.snap.core.datamodel.Product;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by luis on 23/09/2015.
 */
public class StatBaselines implements InSARStatistic {

    private TileCacheTableModel tableModel;

    private static class CachedTileInfo {
        Object uid;
        String imageName;
        int level;
        int numTiles;
        long size;
        String comment;
    }

    private static class TileCacheTableModel extends AbstractTableModel {
        private final static String[] COLUM_NAMES = {"1", "2", "3", "4", "5"};
        private final static Class[] COLUM_CLASSES = {String.class, Integer.class, Long.class,
                Integer.class, String.class};
        List<CachedTileInfo> data = new ArrayList<>(50);

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUM_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUM_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUM_CLASSES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            CachedTileInfo cachedTileInfo = data.get(row);
            switch (column) {
                case 0:
                    return cachedTileInfo.imageName;
                case 1:
                    return cachedTileInfo.numTiles;
                case 2:
                    return cachedTileInfo.size / 1024;
                case 3:
                    return cachedTileInfo.level;
                case 4:
                    return cachedTileInfo.comment;
            }
            return null;
        }

        public void reset() {
            for (CachedTileInfo tileInfo : data) {
                tileInfo.numTiles = 0;
                tileInfo.size = 0;
            }
        }

        public void cleanUp() {
            Iterator<CachedTileInfo> iterator = data.iterator();
            while (iterator.hasNext()) {
                CachedTileInfo tileInfo = iterator.next();
                if (tileInfo.numTiles == 0) {
                    iterator.remove();
                }
            }
        }

        public void addRow(CachedTileInfo tileInfo) {
            data.add(tileInfo);
        }
    }

    public String getName() {
        return "Baselines";
    }

    public Component createPanel() {
        tableModel = new TileCacheTableModel();
        return new JScrollPane(new JTable(tableModel));
    }

    public void update(final Product product) {

    }
}


