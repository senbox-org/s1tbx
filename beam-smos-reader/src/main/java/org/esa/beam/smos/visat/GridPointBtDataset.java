package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import org.esa.beam.dataio.smos.L1cSmosFile;

import java.io.IOException;
import java.math.BigInteger;


class GridPointBtDataset {
    final int gridPointIndex;
    final CompoundType btDataType;
    final String[] columnNames;
    final Class[] columnClasses;
    final Number[][] data;

    static GridPointBtDataset read(L1cSmosFile smosFile, int gridPointIndex) throws IOException {
        SequenceData btDataList = smosFile.getBtDataList(gridPointIndex);

        CompoundType type = (CompoundType) btDataList.getSequenceType().getElementType();
        int memberCount = type.getMemberCount();

        int btDataListCount = btDataList.getElementCount();

        String[] columnNames = new String[memberCount];
        Class[] columnClasses = new Class[memberCount];

        for (int j = 0; j < memberCount; j++) {
            columnNames[j] = type.getMemberName(j);
            columnClasses[j] = getNumbericMemberType(type, j);
        }

        Number[][] tableData = new Number[btDataListCount][memberCount];
        for (int i = 0; i < btDataListCount; i++) {
            CompoundData btData = btDataList.getCompound(i);
            for (int j = 0; j < memberCount; j++) {
                tableData[i][j] = getNumbericMember(btData, j);
            }
        }

        return new GridPointBtDataset(gridPointIndex, smosFile.getBtDataType(), columnNames, columnClasses, tableData);
    }

    // todo - move this to binio.utils (nf - 20081205)
    // todo - test this (nf - 20081205)
    public static Number getNumbericMember(CompoundData compoundData, int memberIndex) throws IOException {
        Type memberType = compoundData.getCompoundType().getMemberType(memberIndex);
        Number number;
        if (memberType == SimpleType.DOUBLE) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.FLOAT) {
            number = compoundData.getFloat(memberIndex);
        } else if (memberType == SimpleType.ULONG) {
            // This mask is used to obtain the value of an int as if it were unsigned.
            BigInteger mask = BigInteger.valueOf(0xffffffffffffffffL);
            BigInteger bi = BigInteger.valueOf(compoundData.getLong(memberIndex));
            number = bi.and(mask);
        } else if (memberType == SimpleType.LONG || memberType == SimpleType.UINT) {
            number = compoundData.getLong(memberIndex);
        } else if (memberType == SimpleType.INT || memberType == SimpleType.USHORT) {
            number = compoundData.getInt(memberIndex);
        } else if (memberType == SimpleType.SHORT || memberType == SimpleType.UBYTE) {
            number = compoundData.getShort(memberIndex);
        } else if (memberType == SimpleType.BYTE) {
            number = compoundData.getByte(memberIndex);
        } else {
            number = null;
        }
        return number;
    }

    // todo - move this to binio.utils (nf - 20081205)
    // todo - test this (nf - 20081205)
    public static Class<? extends Number> getNumbericMemberType(CompoundType compoundData, int memberIndex) {
        Type memberType = compoundData.getMemberType(memberIndex);
        Class<? extends Number> numberClass;
        if (memberType == SimpleType.DOUBLE) {
            numberClass = Double.class;
        } else if (memberType == SimpleType.FLOAT) {
            numberClass = Float.class;
        } else if (memberType == SimpleType.ULONG) {
            numberClass = BigInteger.class;
        } else if (memberType == SimpleType.LONG || memberType == SimpleType.UINT) {
            numberClass = Long.class;
        } else if (memberType == SimpleType.INT || memberType == SimpleType.USHORT) {
            numberClass = Integer.class;
        } else if (memberType == SimpleType.SHORT || memberType == SimpleType.UBYTE) {
            numberClass = Short.class;
        } else if (memberType == SimpleType.BYTE) {
            numberClass = Byte.class;
        } else {
            numberClass = null;
        }
        return numberClass;
    }

    GridPointBtDataset(int gridPointIndex, CompoundType btDataType, String[] columnNames, Class[] columnClasses, Number[][] data) {
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
