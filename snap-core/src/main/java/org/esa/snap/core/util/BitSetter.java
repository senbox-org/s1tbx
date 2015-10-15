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
package org.esa.snap.core.util;

/**
 * Static function to manipulate bits inside an int or long.
 *
 * @author marcoz
 * @since 4.1
 */
public class BitSetter {

    /**
     * Tests if a flag with the given index is set in a 32-bit collection of flags.
     *
     * @param flags    a collection of a maximum of 32 flags
     * @param bitIndex the zero-based index of the flag to be tested
     * @return <code>true</code> if the flag is set
     */
    public static boolean isFlagSet(int flags, int bitIndex) {
        return (flags & (1 << bitIndex)) != 0;
    }

    /**
     * Tests if a flag with the given index is set in a 64-bit collection of flags.
     *
     * @param flags    a collection of a maximum of 64 flags
     * @param bitIndex the zero-based index of the flag to be tested
     * @return <code>true</code> if the flag is set
     */
    public static boolean isFlagSet(long flags, int bitIndex) {
        return (flags & (1L << bitIndex)) != 0;
    }

    /**
     * Sets a flag with the given index in a 32-bit collection of flags.
     *
     * @param flags    a collection of a maximum of 32 flags
     * @param bitIndex the zero-based index of the flag to be set
     * @return the collection of flags with the given flag set
     */
    public static int setFlag(int flags, int bitIndex) {
        return setFlag(flags, bitIndex, true);
    }

    /**
     * Sets a flag with the given index in a 64-bit collection of flags.
     *
     * @param flags    a collection of a maximum of 64 flags
     * @param bitIndex the zero-based index of the flag to be set
     * @return the collection of flags with the given flag set
     */
    public static long setFlag(long flags, int bitIndex) {
        return setFlag(flags, bitIndex, true);
    }

    /**
     * Sets a flag with the given index in a 32-bit collection of flags if a given condition is <code>true</code>.
     *
     * @param flags    a collection of a maximum of 32 flags
     * @param bitIndex the zero-based index of the flag to be set
     * @param cond     the condition
     * @return the collection of flags with the given flag possibly set
     */
    public static int setFlag(int flags, int bitIndex, boolean cond) {
        return cond ? (flags | (1 << bitIndex)) : (flags & ~(1 << bitIndex));
    }

    /**
     * Sets a flag with the given index in a 64-bit collection of flags if a given condition is <code>true</code>.
     *
     * @param flags    a collection of a maximum of 64 flags
     * @param bitIndex the zero-based index of the flag to be set
     * @param cond     the condition
     * @return the collection of flags with the given flag possibly set
     */
    public static long setFlag(long flags, int bitIndex, boolean cond) {
        return cond ? (flags | (1L << bitIndex)) : (flags & ~(1L << bitIndex));
    }
}
