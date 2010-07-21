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

import com.bc.ceres.binio.*;

import java.io.IOException;
import java.io.PrintStream;

public class DataPrinter {
    private static final String INDENT = "    ";
    private final PrintStream stream;
    private final boolean debug;

    public DataPrinter() {
        this(System.out, false);
    }

    public DataPrinter(PrintStream stream, boolean debug) {
        this.stream = stream;
        this.debug = debug;
    }

    public void print(CollectionData data) throws IOException {
        if (data instanceof CompoundData) {
            CompoundData compoundData = (CompoundData) data;
            printCompound(compoundData);
        } else if (data instanceof SequenceData) {
            SequenceData sequenceData = (SequenceData) data;
            printSequence(sequenceData);
        }
    }

    private void printCompound(CompoundData compoundData) throws IOException {
        printCompound("", "", compoundData);
    }

    private void printSequence(SequenceData sequenceData) throws IOException {
        printSequence("", "", sequenceData);
    }

    private void printCompound(String prefix, String name, CompoundData compoundData) throws IOException {
        CompoundType compoundType = compoundData.getType();
        printComplexTypeName(prefix, name, compoundType, compoundData);
        final int memberCount = compoundType.getMemberCount();
        for (int i = 0; i < memberCount; i++) {
            CompoundMember member = compoundType.getMember(i);
            Type type = member.getType();
            if (type.isCompoundType()) {
                printCompound(prefix + INDENT, member.getName(), compoundData.getCompound(i));
            } else if (type.isSequenceType()) {
                printSequence(prefix + INDENT, member.getName(), compoundData.getSequence(i));
            } else if (type.isSimpleType()) {
                stream.print(prefix + INDENT + member.getName() + ":" + type.getName() + " = ");
                printSimple(compoundData, type, i);
                stream.println();
            }
        }
        stream.println(prefix + "}");
    }

    private void printSequence(String prefix, String name, SequenceData sequenceData) throws IOException {
        SequenceType sequenceType = sequenceData.getType();
        printComplexTypeName(prefix, name, sequenceType, sequenceData);
        Type type = sequenceType.getElementType();
        final int elementCount = sequenceType.getElementCount();
        for (int i = 0; i < elementCount; i++) {
            if (type.isCompoundType()) {
                printCompound(prefix + INDENT, i + " = " + sequenceType.getName(), sequenceData.getCompound(i));
            } else if (type.isSequenceType()) {
                printSequence(prefix + INDENT, i + " = " + type.getName(), sequenceData.getSequence(i));
            } else if (type.isSimpleType()) {
                if (i % 10 == 0) {
                    stream.print(prefix + INDENT + i + " = ");
                }
                printSimple(sequenceData, type, i);
                if (i % 10 == 9 || i == elementCount - 1) {
                    stream.println();
                } else {
                    stream.print(", ");
                }
            }
        }
        stream.println(prefix + "}");
    }

    private void printComplexTypeName(String prefix, String name, Type type, CollectionData data) {
        String namePart = name.isEmpty() ? "" : name + ":";
        stream.print(prefix + namePart + type.getName());
        if (debug) {
            stream.print(" (position=" + data.getPosition());
            stream.print(", size=" + data.getSize());
            stream.print(", type=" + data.getClass().getSimpleName() + ")");
        }
        stream.println(" {");
    }

    private void printSimple(CollectionData collectionData, Type type, int index) throws IOException {
        if (type.equals(SimpleType.BYTE)) {
            stream.print(collectionData.getByte(index));
        } else if (type.equals(SimpleType.UBYTE)) {
            stream.print(collectionData.getUByte(index));
        } else if (type.equals(SimpleType.SHORT)) {
            stream.print(collectionData.getShort(index));
        } else if (type.equals(SimpleType.USHORT)) {
            stream.print(collectionData.getUShort(index));
        } else if (type.equals(SimpleType.INT)) {
            stream.print(collectionData.getInt(index));
        } else if (type.equals(SimpleType.UINT)) {
            stream.print(collectionData.getUInt(index));
        } else if (type.equals(SimpleType.LONG)) {
            stream.print(collectionData.getLong(index));
        } else if (type.equals(SimpleType.ULONG)) {
            stream.print(collectionData.getLong(index));
        } else if (type.equals(SimpleType.FLOAT)) {
            stream.print(collectionData.getFloat(index));
        } else if (type.equals(SimpleType.DOUBLE)) {
            stream.print(collectionData.getDouble(index));
        }
    }
}
