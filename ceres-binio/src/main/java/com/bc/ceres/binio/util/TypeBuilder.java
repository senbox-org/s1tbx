package com.bc.ceres.binio.util;

import com.bc.ceres.binio.*;
import com.bc.ceres.binio.internal.CompoundTypeImpl;
import com.bc.ceres.binio.internal.SequenceTypeImpl;
import com.bc.ceres.binio.internal.CompoundMemberImpl;
import com.bc.ceres.binio.internal.VarElementCountSequenceTypeImpl;

/**
 * A utility class which fosters the construction of complex type defintions.
 * Its usage should be self-explaining, e.g.:
 * <pre>
 * <p/>
 * import static binio.util.TypeBuilder.*;
 * ...
 * CompoundType scanlineType =
 *     COMP("Scanline",
 *        MEMBER("flags", USHORT),
 *        MEMBER("data", SEQ(DOUBLE, 512))
 *     );
 * };
 * <p/>
 * CompoundType datasetType =
 *     COMP("Dataset",
 *        MEMBER("lineCount", UINT),
 *        MEMBER("scanlines", SEQ(scanlineType))
 *     );
 * };
 * ...
 * </pre>
 */
public class TypeBuilder {
    public final static SimpleType BYTE = SimpleType.BYTE;
    public final static SimpleType UBYTE = SimpleType.UBYTE;
    public final static SimpleType SHORT = SimpleType.SHORT;
    public final static SimpleType USHORT = SimpleType.USHORT;
    public final static SimpleType INT = SimpleType.INT;
    public final static SimpleType UINT = SimpleType.UINT;
    public final static SimpleType LONG = SimpleType.LONG;
    public final static SimpleType ULONG = SimpleType.ULONG;
    public final static SimpleType FLOAT = SimpleType.FLOAT;
    public final static SimpleType DOUBLE = SimpleType.DOUBLE;

    public static SequenceType SEQ(Type elementType) {
        return new SequenceTypeImpl(elementType);
    }

    public static VarSequenceType SEQ(Type elementType, String memberName) {
        return new VarElementCountSequenceTypeImpl(elementType, memberName);
    }

    public static SequenceType SEQ(Type elementType, int elementCount) {
        return new SequenceTypeImpl(elementType, elementCount);
    }

    public static CompoundMember MEMBER(String name, Type type) {
        return new CompoundMemberImpl(name, type);
    }

    public static CompoundType COMP(String name, CompoundMember... members) {
        return new CompoundTypeImpl(name, members);
    }

    private TypeBuilder() {
    }

}
