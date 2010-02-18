package org.esa.beam.processor.binning.algorithm;

/**
 * This interface describes classes that are able to create
 * algorithm based on a string based description.
 */
public interface AlgorithmCreator {
    /**
     * Retrieves an algorithm object specified by the identifier string passed in.
     *
     * @param algoName the algorithm name
     * @throws IllegalArgumentException if the requested algorithm is unkown
     */
    public Algorithm getAlgorithm(String algoName) throws IllegalArgumentException;
}
