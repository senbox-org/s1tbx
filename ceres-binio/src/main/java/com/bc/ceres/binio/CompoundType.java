package com.bc.ceres.binio;

public final class CompoundType extends Type implements MetadataAware {
    private final String name;
    private final Member[] members;
    private Object metadata;
    private int size;

    public CompoundType(String name, Member[] members) {
        this(name, members, null);
    }

    public CompoundType(String name, Member[] members, Object metadata) {
        this.name = name;
        this.members = members.clone();
        this.metadata = metadata;
        updateSize();
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public Member[] getMembers() {
        return members.clone();
    }

    public int getMemberCount() {
        return members.length;
    }

    public int getMemberIndex(String name) {
        // todo - OPT: trivial implementation, optimize using a Map<String, int>
        for (int i = 0; i < members.length; i++) {
            CompoundType.Member member = members[i];
            if (name.equalsIgnoreCase(member.getName())) {
                return i;
            }
        }
        return -1;
    }

    public Member getMember(int memberIndex) {
        return members[memberIndex];
    }

    public void setMember(int memberIndex, Member member) {
        members[memberIndex] = member;
        updateSize();
    }

    public String getMemberName(int memberIndex) {
        return getMember(memberIndex).getName();
    }

    public Type getMemberType(int memberIndex) {
        return getMember(memberIndex).getType();
    }

    public int getMemberSize(int memberIndex) {
        return getMember(memberIndex).getType().getSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public final boolean isCollectionType() {
        return true;
    }

    @Override
    public boolean isCompoundType() {
        return true;
    }

    @Override
    public void visit(TypeVisitor visitor) {
        for (Member member : members) {
            member.getType().visit(visitor);
        }
        visitor.accept(this);
    }

    private void updateSize() {
        int size = 0;
        for (Member member : members) {
            final int memberSize = member.getType().getSize();
            if (memberSize >= 0 && size >= 0) {
                size += memberSize;
            } else {
                size = -1;
                break;
            }
        }
        this.size = size;
    }

    public static final class Member implements MetadataAware {
        private final String name;
        private final Type type;
        private Object metadata;

        public Member(String name, Type type) {
            this(name, type, null);
        }

        public Member(String name, Type type, Object metadata) {
            this.name = name;
            this.type = type;
            this.metadata = metadata;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }
}