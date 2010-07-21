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

package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Utility class.
 */
public class NumberUtils {

    // note: using BigInteger.valueOf(long) to create this mask does not work
    static final BigInteger ULONG_MASK = new BigInteger(Long.toHexString(0xffffffffffffffffL), 16);

    /**
     * Returns the numeric value of a compound member, which is of a {@link SimpleType}.
     *
     * @param compoundData the compound.
     * @param memberIndex  the index of the compound member of interest.
     *
     * @return the numeric value of the compound member of interest, or {@code null} if the
     *         compound member is not of a {@link SimpleType}.
     *
     * @throws IOException if an I/O error occurred.
     */
    public static Number getNumericMember(CompoundData compoundData, int memberIndex) throws IOException {
        final Type memberType = compoundData.getType().getMemberType(memberIndex);
        final Number number;

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

    /**
     * Returns the numeric type of a compound member, which is of a {@link SimpleType}.
     *
     * @param compoundData the compound.
     * @param memberIndex  the index of the compound member of interest.
     *
     * @return the numeric type of the compound member of interest, or {@code null} if the
     *         compound member is not of a {@link SimpleType}.
     */
    public static Class<? extends Number> getNumericMemberType(CompoundType compoundData, int memberIndex) {
        final Type memberType = compoundData.getMemberType(memberIndex);
        final Class<? extends Number> numberClass;

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
