/*
 * $Id: FlagWrapper.java,v 1.1 2007/03/27 12:51:06 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

/**
 * This class wraps an array of flag values and allows
 * to set individual bits or query their state.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:06 $
 */
public abstract class FlagWrapper {

    /**
     * Sets a flag with the given index in a 32-bit collection of flags.
     *
     * @param flags    a collection of a maximum of 32 flags
     * @param bitIndex the zero-based index of the flag to be set
     * @return the collection of flags with the given flag possibly set
     */
    public static int setFlag(int flags, int bitIndex) {
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
        return cond ? (flags | (1 << bitIndex)) : flags;
    }


    /**
     * Sets a flag with the given bitIndex in a collection of flags.
     *
     * @param index    index into the array
     * @param bitIndex the zero-based index of the flag to be set
     */
    public void set(int index, int bitIndex) {
        set(index, bitIndex, true);
    }

    /**
     * Sets a flag with the given index in a collection of flags,
     * if the given condition is <code>true</code>.
     *
     * @param index    index into the array
     * @param bitIndex the zero-based index of the flag to be set
     * @param cond     the condition
     */
    public abstract void set(int index, int bitIndex, boolean cond);

    /**
     * Tests if a flag with the given index is set in a collection of flags.
     *
     * @param index    index into the array
     * @param bitIndex the zero-based index of the flag to be tested
     * @return <code>true</code> if the flag is set
     */
    public abstract boolean isSet(int index, int bitIndex);

    public static class Byte extends FlagWrapper {

        private byte[] flags;

        public Byte(byte[] flags) {
            this.flags = flags;
        }

        @Override
        public boolean isSet(int index, int bitIndex) {
            return (flags[index] & (1 << bitIndex)) != 0;
        }

        @Override
        public void set(int index, int bitIndex, boolean cond) {
            if (cond) {
                flags[index] = (byte) (flags[index] | (1 << bitIndex));
            }
        }
    }

    public static class Short extends FlagWrapper {

        private short[] flags;

        public Short(short[] flags) {
            this.flags = flags;
        }

        @Override
        public boolean isSet(int index, int bitIndex) {
            return (flags[index] & (1 << bitIndex)) != 0;
        }

        @Override
        public void set(int index, int bitIndex, boolean cond) {
            if (cond) {
                flags[index] = (short) (flags[index] | (1 << bitIndex));
            }
        }
    }

    public static class Int extends FlagWrapper {

        private int[] flags;

        public Int(int[] flags) {
            this.flags = flags;
        }

        @Override
        public boolean isSet(int index, int bitIndex) {
            return (flags[index] & (1 << bitIndex)) != 0;
        }

        @Override
        public void set(int index, int bitIndex, boolean cond) {
            if (cond) {
                flags[index] = (flags[index] | (1 << bitIndex));
            }
        }
    }

    public static class Long extends FlagWrapper {

        private long[] flags;

        public Long(long[] flags) {
            this.flags = flags;
        }

        @Override
        public boolean isSet(int index, int bitIndex) {
            return (flags[index] & (1L << bitIndex)) != 0;
        }

        @Override
        public void set(int index, int bitIndex, boolean cond) {
            if (cond) {
                flags[index] = (flags[index] | (1L << bitIndex));
            }
        }
    }

}
