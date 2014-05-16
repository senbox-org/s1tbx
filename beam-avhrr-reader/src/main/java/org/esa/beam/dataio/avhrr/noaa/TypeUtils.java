package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;

import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;

/**
 * Methods for building additional generic data types.
 *
 * @author Ralf Quast
 */
class TypeUtils {

    private TypeUtils() {
    }

    static FormatMetadata META() {
        return new FormatMetadata();
    }

    static Type STRING(int length) {
        return SEQUENCE(SimpleType.BYTE, length);
    }

    static CompoundMember FILL_MEMBER(int bytes) {
        return MEMBER("fill", STRING(bytes));
    }

    static CompoundMember META_MEMBER(String name, Type type, FormatMetadata metadata) {
        CompoundMember member = MEMBER(name, type);
        member.setMetadata(metadata);
        return member;
    }

    static CompoundMember STRING_MEMBER(String name, int length) {
        return STRING_MEMBER(name, length, META());
    }

    static CompoundMember STRING_MEMBER(String name, int length, FormatMetadata metadata) {
        CompoundMember member = MEMBER(name, STRING(length));
        metadata.setType("string");
        member.setMetadata(metadata);
        return member;
    }
}
