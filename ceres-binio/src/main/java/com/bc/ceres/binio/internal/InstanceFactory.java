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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A factory used to create the various data instances.
 */
public class InstanceFactory {

    public static CompoundInstance createCompound(DataContext context, CollectionData parent, CompoundType compoundType, long position, ByteOrder byteOrder) {
        if (compoundType.isSizeKnown()) {
            // COMPOUND(all members have known size)
            return new FixCompound(context, parent, compoundType, position);
        } else {
            // COMPOUND(some members have unknown size)
            return new VarCompound(context, parent, compoundType, position);
        }
    }

    static SequenceInstance createSequence(DataContext context, CollectionData parent, SequenceType sequenceType, long position, ByteOrder byteOrder) {
        final Type elementType = sequenceType.getElementType();
        if (elementType.isSimpleType()) {
            if (sequenceType.getElementCount() >= 0) {
                // SEQUENCE(elementType=SIMPLE, elementCount=N)
                return new FixSequenceOfSimples(context, parent, sequenceType, position);
            } else {
                // SEQUENCE(elementType=SIMPLE, elementCount=?)
                return new VarSequenceOfSimples(context, parent, sequenceType, position);
            }
        } else if (elementType.isSizeKnown()) {
            if (sequenceType.getElementCount() >= 0) {
                // SEQUENCE(elementType=COLLECTION(elementCount=N), elementCount=N)
                return new FixSequenceOfFixCollections(context, parent, sequenceType, position);
            } else {
                // SEQUENCE(elementType=COLLECTION(elementCount=N), elementCount=?)
                return new VarSequenceOfFixCollections(context, parent, sequenceType, position);
            }
        } else {
            if (sequenceType.getElementCount() >= 0) {
                // SEQUENCE(elementType=COLLECTION(elementCount=?), elementCount=N)
                return new FixSequenceOfVarCollections(context, parent, sequenceType, position);
            } else {
                // SEQUENCE(elementType=COLLECTION(elementCount=?), elementCount=?)
                throw new IllegalArgumentException("unsupported sequence type: " + sequenceType);
            }
        }
    }

    static CollectionInstance createCollection(DataContext context, CollectionData parent, Type type, long position, ByteOrder byteOrder) throws IOException {
        if (type.isCompoundType()) {
            return createCompound(context, parent, (CompoundType) type, position, byteOrder);
        } else if (type.isSequenceType()) {
            return createSequence(context, parent, AbstractSequence.resolveSequenceType(parent, (SequenceType) type), position, byteOrder);
        } else {
            throw new IllegalArgumentException("illegal type: " + type);
        }
    }

    static MemberInstance createMember(DataContext context, CompoundData parent, Type type, long position, ByteOrder byteOrder) throws IOException {
        if (type.isSimpleType()) {
            return createFixMember(context, parent, type, new Segment(position, type.getSize()), 0);
        } else {
            return createCollection(context, parent, type, position, byteOrder);
        }
    }

    static MemberInstance createFixMember(DataContext context, CollectionData parent, Type type, Segment segment, int segmentOffset) {
        if (!type.isSizeKnown()) {
            throw new IllegalArgumentException("illegal type: " + type);
        }
        if (type.isSimpleType()) {
            return new SimpleMember(context, parent, (SimpleType) type, segment, segmentOffset);
        } else if (type.isSequenceType()) {
            return createFixSequence(context, parent, (SequenceType) type, segment, segmentOffset);
        } else if (type.isCompoundType()) {
            return createFixCompound(context, parent, (CompoundType) type, segment, segmentOffset);
        } else {
            throw new IllegalArgumentException("illegal type: " + type);
        }
    }

    static CompoundInstance createFixCompound(DataContext context, CollectionData parent, CompoundType compoundType, Segment segment, int segmentOffset) {
        // COMPOUND(all members have known size)
        return new FixCompound(context, parent, compoundType, segment, segmentOffset);
    }

    static SequenceInstance createFixSequence(DataContext context, CollectionData parent, SequenceType sequenceType, Segment segment, int segmentOffset) {
        final Type elementType = sequenceType.getElementType();
        if (elementType.isSimpleType()) {
            // SEQUENCE(elementType=SIMPLE, elementCount=N)
            return new FixSequenceOfSimples(context, parent, sequenceType, segment, segmentOffset);
        } else {
            // SEQUENCE(elementType=COLLECTION(elementCount=N), elementCount=N)
            return new FixSequenceOfFixCollections(context, parent, sequenceType, segment, segmentOffset);
        }
    }

    private InstanceFactory() {
    }
}
