package com.bc.ceres.glayer;

/**
 * Used to filter layers.
 *
 * @author Norman Fomferra
 */
public interface LayerFilter {
    boolean accept(Layer layer);
}
