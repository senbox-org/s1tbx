package org.jlinda.nest.gpf.unwrapping.external;

public class Mask{

    // flags convention from list.h from ghiglia pritt unwrapping book
    public static final byte POS_RESIDUE = 1;   // 0x1
    public static final short NEG_RESIDUE = 2;  // 0x2
    public static final short VISITED = 4;      // 0x4
    public static final short ACTIVE = 8;        // 0x8
    public static final short BRANCH_CUT = 16;  // 0x10
    public static final short BORDER = 32;      // 0x20
    public static final short UNWRAPPED = 64;   // 0x40
    public static final short POSTPONED = 128;  // 0x80
    public static final short RESIDUE = 3;      // 0x3
    public static final short AVOID = 48;       // 0x30
}
