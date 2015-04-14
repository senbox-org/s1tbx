package org.esa.snap.binning;

/**
 * The compositing type defines the way multiple images (products) are composed into one.
 *
 * @author Marco Peters
 */
public enum CompositingType {

    /**
     * Represents the compositing type 'binning'
     */
    BINNING,
    /**
     * Represents the compositing type 'mosaicking'
     */
    MOSAICKING
}
