package org.esa.snap.core.gpf.common.resample;

/**
 * @author Tonio Fincke
 */
public interface DataAccessor {

    int getSrcScalineStride();

    int getDstScalineStride();

    int getSrcOffset();

    int getDstOffset();

}
