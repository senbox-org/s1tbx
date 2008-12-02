package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.SmosFile;

import java.io.IOException;

import com.bc.ceres.binio.*;


class GridPointDataset {
    final int gridPointIndex;
    final CompoundType btDataType;
    final String[] columnNames;
    final Class[] columnClasses;
    final Number[][] data;

    static GridPointDataset read(SmosFile smosFile, int gridPointIndex) throws IOException {
        SequenceData btDataList = smosFile.getBtDataList(gridPointIndex);

        CompoundType type = (CompoundType) btDataList.getSequenceType().getElementType();
        int memberCount = type.getMemberCount();

        int btDataListCount = btDataList.getElementCount();

        String[] columnNames = new String[memberCount];
        Class[] columnClasses = new Class[memberCount];

        for (int j = 0; j < memberCount; j++) {
            columnNames[j] = type.getMemberName(j);
            Type memberType = type.getMemberType(j);
            Class columnClass;
            if (memberType == SimpleType.FLOAT) {
                columnClass = Float.class;
            } else if (memberType == SimpleType.DOUBLE) {
                columnClass = Double.class;
            } else if (memberType == SimpleType.ULONG || memberType == SimpleType.LONG) {
                columnClass = Long.class;
            } else {
                columnClass = Integer.class;
            }
            columnClasses[j] = columnClass;
        }

        Number[][] tableData = new Number[btDataListCount][memberCount];
        for (int i = 0; i < btDataListCount; i++) {
            CompoundData btData = btDataList.getCompound(i);
            for (int j = 0; j < memberCount; j++) {
                Type memberType = type.getMemberType(j);
                Number value;
                if (memberType == SimpleType.FLOAT) {
                    value = btData.getFloat(j);
                } else if (memberType == SimpleType.DOUBLE) {
                    value = btData.getDouble(j);
                } else if (memberType == SimpleType.ULONG || memberType == SimpleType.LONG) {
                    value = btData.getLong(j);
                } else {
                    value = btData.getInt(j);
                }
                tableData[i][j] = value;
            }
        }

        return new GridPointDataset(gridPointIndex, smosFile.getBtDataType(), columnNames, columnClasses, tableData);
    }

    GridPointDataset(int gridPointIndex, CompoundType btDataType, String[] columnNames, Class[] columnClasses, Number[][] data) {
        this.gridPointIndex = gridPointIndex;
        this.btDataType = btDataType;
        this.columnNames = columnNames;
        this.columnClasses = columnClasses;
        this.data = data;
    }

    int getColumnIndex(String name) {
        return btDataType.getMemberIndex(name);
    }
}
