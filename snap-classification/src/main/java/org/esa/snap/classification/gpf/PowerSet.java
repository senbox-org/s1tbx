package org.esa.snap.classification.gpf;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Math.min;

/**
 * Calculate the Power Set with min and max size
 */
public class PowerSet<E> implements Iterator<Set<E>>, Iterable<Set<E>> {

    private final int minSize;
    private final int maxSize;
    private final E[] arr;
    private long bits = 0;
    private long count = 0;

    /**
     * Build a PowerSet of the given {@link Set} to iterate over subsets whose size is between the given boundaries
     * @param set the set to create subsets from
     * @param minSize the minimal size of the subsets
     * @param maxSize the maximum size of the subsets
     */
    @SuppressWarnings("unchecked")
    public PowerSet(Set<E> set, int minSize, int maxSize) {
        this.minSize = min(minSize, set.size());
        this.maxSize = min(maxSize, set.size());
        arr = (E[]) set.toArray();
        for (int n = 0; n < minSize; n++) {
            bits |= (1L << n);
        }
        count = countBitSet(bits);
    }

    @Override
    public boolean hasNext() {
        return (bits & (1L << arr.length)) == 0;
    }

    @Override
    public Set<E> next() {
        // compute current subset
        Set<E> returnSet = new TreeSet<>();
        for (int i = 0; i < arr.length; i++) {
            if ((bits & (1L << i)) != 0) {
                returnSet.add(arr[i]);
            }
        }

        // compute next bit map for next subset
        do {
            if (count < minSize) {
                long maxFree = lowestIndex(bits) - 1;
                long missing = minSize - count;
                for (int n = 0; n < min(maxFree, missing); n++) {
                    bits |= (1L << n);
                }
            } else {
                bits++;
            }
            count = countBitSet(bits);
        } while ((count < minSize) || (count > maxSize));
        return returnSet;
    }

    /**
     * Lowest set bit in a long word
     * @param i the long word
     * @return lowest bit set
     */
    private static long lowestIndex(long i) {
        int n = 0;
        while (n < 64 && (i & 1L) == 0) {
            n++;
            i = i >>> 1;
        }
        return n;
    }

    /**
     * Compute the number of bit sets inside a word or a long word.<br/>
     * <a href="http://en.wikipedia.org/wiki/Hamming_weight">Hamming weight</a>
     * @param i the long word
     * @return number of set bits
     */
    private static long countBitSet(long i) {
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = ((i + (i >>> 4)) & 0x0F0F0F0F0F0F0F0FL);
        return (i * (0x0101010101010101L)) >>> 56;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Override
    public Iterator<Set<E>> iterator() {
        return this;
    }

}