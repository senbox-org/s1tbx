package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;

import java.io.IOException;
import java.math.BigInteger;

public class NumberUtils {

    static final BigInteger ULONG_MASK = new BigInteger(Long.toHexString(0xffffffffffffffffL), 16);

    public static Number getNumericMember(CompoundData compoundData, int memberIndex) throws IOException {
        Type memberType = compoundData.getType().getMemberType(memberIndex);
        Number number;
        if (memberType == SimpleType.DOUBLE) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.FLOAT) {
            number = compoundData.getFloat(memberIndex);
        } else if (memberType == SimpleType.ULONG) {
            number = BigInteger.valueOf(compoundData.getLong(memberIndex)).and(ULONG_MASK);
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

    public static Class<? extends Number> getNumericMemberType(CompoundType compoundData, int memberIndex) {
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

    private NumberUtils() {
    }
}
