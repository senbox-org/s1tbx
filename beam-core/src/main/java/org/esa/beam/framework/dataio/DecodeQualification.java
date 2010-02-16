package org.esa.beam.framework.dataio;

/**
 * The qualification of a product reader for decoding a given input.
 */
public enum DecodeQualification {
    /**
     * The reader is intended to decode a given input.
     */
    INTENDED,
    /**
     * The reader is suitable to decode a given input, but has not specifically been designed for it.
     */
    SUITABLE,
    /**
     * The reader is unable to decode a given input.
     */
    UNABLE
}
