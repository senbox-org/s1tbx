package org.esa.snap.core.datamodel;

/**
 * @author Tonio Fincke
 */
public interface Resampler {

    String getName();

    String getDescription();

    void resample(Product multiSizeProduct);

}
