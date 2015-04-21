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

package com.bc.ceres.binio;

import com.bc.ceres.binio.internal.CompoundMemberImpl;
import com.bc.ceres.binio.internal.CompoundTypeImpl;
import com.bc.ceres.binio.internal.GrowableSequenceTypeImpl;
import com.bc.ceres.binio.internal.SequenceTypeImpl;
import com.bc.ceres.binio.internal.VarElementCountSequenceTypeImpl;

/**
 * A utility class which fosters the construction of complex type defintions.
 * Its usage should be self-explaining, e.g.:
 * <pre>
 * import static binio.util.TypeBuilder.*;
 * ...
 * CompoundType scanlineType =
 *     COMPOUND("Scanline",
 *        MEMBER("flags", USHORT),
 *        MEMBER("samples", SEQUENCE(DOUBLE, 512))
 *     );
 * };
 * CompoundType datasetType =
 *     COMPOUND("Dataset",
 *        MEMBER("lineCount", UINT),
 *        MEMBER("scanlines", VAR_SEQUENCE(scanlineType, "lineCount"))
 *     );
 * };
 * ...
 * </pre>
 */
public class TypeBuilder {
    /**
     * The 8-bit signed integer type.
     */
    public final static SimpleType BYTE = SimpleType.BYTE;
    /**
     * The 8-bit unsigned integer type.
     */
    public final static SimpleType UBYTE = SimpleType.UBYTE;
    /**
     * The 16-bit signed integer type.
     */
    public final static SimpleType SHORT = SimpleType.SHORT;
    /**
     * The 16-bit unsigned integer type.
     */
    public final static SimpleType USHORT = SimpleType.USHORT;
    /**
     * The 32-bit unsigned integer type.
     */
    public final static SimpleType INT = SimpleType.INT;
    /**
     * The 32-bit unsigned integer type.
     */
    public final static SimpleType UINT = SimpleType.UINT;
    /**
     * The 64-bit signed integer type.
     */
    public final static SimpleType LONG = SimpleType.LONG;
    /**
     * The 64-bit unsigned integer type.
     */
    public final static SimpleType ULONG = SimpleType.ULONG;
    /**
     * The 32-bit IEEE floating point type.
     */
    public final static SimpleType FLOAT = SimpleType.FLOAT;
    /**
     * The 64-bit IEEE floating point type.
     */
    public final static SimpleType DOUBLE = SimpleType.DOUBLE;

    /**
     * Creates a sequence type for the given element type and count.
     * If element count is greater or equal to zero, a fixed-length sequence type
     * will be created.
     * If element count is less than zero, a growable sequence type
     * will be created.
     *
     * @param elementType  The sequence's element type.
     * @param elementCount The sequence's element count.
     *
     * @return A sequence type.
     */
    public static SequenceType SEQUENCE(Type elementType, int elementCount) {
        if (elementCount >= 0) {
            return new SequenceTypeImpl(elementType, elementCount);
        } else {
            return new GrowableSequenceTypeImpl(elementType);
        }
    }

    /**
     * Creates a dynamic sequence type whose element count is resolved
     * by a compound member determined by the given member name.
     * The member must have integer type.
     *
     * @param elementType The sequence's element type.
     * @param memberName  The parent compount's member name.
     *
     * @return A dynamic sequence type.
     */
    public static VarSequenceType VAR_SEQUENCE(Type elementType, String memberName) {
        return new VarElementCountSequenceTypeImpl(elementType, memberName);
    }

    /**
     * Creates a dynamic sequence type whose element count is resolved
     * by a compound member determined by the given member index.
     * The member must have integer type.
     *
     * @param elementType The sequence's element type.
     * @param memberIndex The parent compount's member index.
     *
     * @return A dynamic sequence type.
     */
    public static VarSequenceType VAR_SEQUENCE(Type elementType, int memberIndex) {
        return new VarElementCountSequenceTypeImpl(elementType, memberIndex);
    }

    /**
     * Creates a compound member.
     *
     * @param name The member's name.
     * @param type The member's type.
     *
     * @return The compound member.
     *
     * @see #COMPOUND(String, CompoundMember[])
     */
    public static CompoundMember MEMBER(String name, Type type) {
        return new CompoundMemberImpl(name, type);
    }

    /**
     * Creates a compound type.
     *
     * @param name    The compound's name.
     * @param members The compound's members.
     *
     * @return The compound type.
     */
    public static CompoundType COMPOUND(String name, CompoundMember... members) {
        return new CompoundTypeImpl(name, members);
    }

    private TypeBuilder() {
    }

}
