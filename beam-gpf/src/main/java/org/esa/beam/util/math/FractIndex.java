/*
 * $Id: FractIndex.java,v 1.1 2007/03/27 12:51:06 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.util.math;

//TODO make this public API

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class belongs to a preliminary API.
 * It is not (yet) intended to be used by clients and may change in the future.</i></p>
 */
public final class FractIndex {

    public int index;
    public double fraction;

    public static FractIndex[] createArray(int n) {
        final FractIndex[] fractIndexes = new FractIndex[n];
        for (int i = 0; i < fractIndexes.length; i++) {
            fractIndexes[i] = new FractIndex();
        }
        return fractIndexes;
    }
}
