/*
 * $Id: IndexValidator.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 * Copyright (c) by Brockmann Consult 2003
 */

package org.esa.beam.util.math;

/**
 * An interface used as parameter to several methods which perform some actions on data arrays.
 * It is used to decide whether or not an array value shall be taken into account for a particular
 * computation.
 */
public interface IndexValidator {

    /**
     * The validator whose {@link #validateIndex} method always returns true.
     */
    IndexValidator TRUE = new IndexValidator() {
                public final boolean validateIndex(int index) {
                    return true;
                }
            };

    /**
     * If the given <code>index</code> or the value at the given <code>index</code> is valid, this method should return
     * <code>true</code>, otherwise <code>false</code>.
     *
     * @param index the index to validate
     *
     * @return <code>true</code>, if the index or the data behind the index is valid.
     */
    boolean validateIndex(int index);
}
