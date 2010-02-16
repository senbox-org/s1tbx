/*
 * $Id: FractIndex.java,v 1.1 2006/10/10 14:47:32 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.processor.cloud.internal.util;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
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
