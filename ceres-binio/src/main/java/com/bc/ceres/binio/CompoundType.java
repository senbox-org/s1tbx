package com.bc.ceres.binio;

public final class CompoundType extends Type {
    private final String name;
    private final Member[] members;
    private int size;

    public CompoundType(String name, Member[] members) {
        this.name = name;
        this.members = members.clone();
        updateSize();
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

    public Type getMemberType(int memberIndex) {
        return members[memberIndex].getType();
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
            }
        }
        this.size = size;
    }

    public int getMemberSize(int memberIndex) {
        return getMember(memberIndex).getType().getSize();
    }

    public static final class Member {
        private final String name;
        private final Type type;

        public Member(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }
}