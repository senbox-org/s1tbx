package com.bc.ceres.binio;

public interface CompoundType extends Type, MetadataAware {
    int getMemberCount();

    CompoundMember[] getMembers();

    CompoundMember getMember(int memberIndex);

    String getMemberName(int memberIndex);

    Type getMemberType(int memberIndex);

    int getMemberSize(int memberIndex);

    int getMemberIndex(String name);

}
