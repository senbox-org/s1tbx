package org.jlinda.core.coregistration.estimation.utils;

import java.util.*;


/**
 * A collection of mathematical utility functions.
 * <p/>
 * Copyright (c) 2008 Eric Eaton
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * @author Eric Eaton (EricEaton@umbc.edu) <br>
 *         University of Maryland Baltimore County
 * @version 0.1
 */
public class MathUtils {


    /**
     * Runs the reverse Cuthill-McKee algorithm on the adjacency lists
     * to provide an efficient ordering of the vertices
     *
     * @param adjacencyLists           a list of the adjacency lists of the graph
     * @param runTwiceForOptimalResult whether to run the algorithm twice to get the optimal result
     * @return a permutation of the vertices that is efficient for a sparse matrix
     */
    public static int[] reverseCuthillMcKee(int[][] adjacencyLists, boolean runTwiceForOptimalResult) {

        // find a vertex with the smallest branching factor and start there
        int minBranchingFactor = Integer.MAX_VALUE;
        int minBranchingFactorVertex = -1;
        for (int v = 0; v < adjacencyLists.length; v++) {
            int branchingFactor = adjacencyLists[v].length;
            if (branchingFactor < minBranchingFactor) {
                minBranchingFactor = branchingFactor;
                minBranchingFactorVertex = v;
            }
        }

        // first run of RCM
        int[] permutation = reverseCuthillMcKee(adjacencyLists, minBranchingFactorVertex);

        if (!runTwiceForOptimalResult) {
            return permutation;
        }

        // second run of RCM, starting at optimal first vertex
        return reverseCuthillMcKee(adjacencyLists, permutation[0]);
    }


    /**
     * Runs the reverse Cuthill-McKee algorithm on the adjacency lists
     * to provide an efficient ordering of the vertices
     *
     * @param adjacencyLists a list of the adjacency lists of the graph
     * @param startingVertex the vertex to start in the ordering
     * @return a permutation of the vertices that is efficient for a sparse matrix
     */
    private static int[] reverseCuthillMcKee(int[][] adjacencyLists, int startingVertex) {

        // error check args
        if (startingVertex < 0 || startingVertex >= adjacencyLists.length) {
            throw new IllegalArgumentException("startingVertex must be in [0," + adjacencyLists.length + ").");
        }

        int numVertices = adjacencyLists.length;
        HashSet<Integer> unvisitedVertices = new HashSet<Integer>();

        // determine the branching factors of each vertex
        int[] branchingFactors = new int[numVertices];
        for (int v = 0; v < numVertices; v++) {
            branchingFactors[v] = adjacencyLists[v].length;
            unvisitedVertices.add(v);
        }

        ArrayList<Integer> ordering = new ArrayList<Integer>();

        // start at the given vertex
        ordering.add(startingVertex);
        unvisitedVertices.remove(startingVertex);

        int i = 0;
        // keep looping until all vertices have been explored
        while (ordering.size() < numVertices) {

            // if we've reached the end of one component of the graph, start another
            if (i == ordering.size()) {
                // determine the lowest branching factor of the unvisitedVertices
                int minBranchingFactor = Integer.MAX_VALUE;
                int minBranchingFactorVertex = -1;
                for (int v : unvisitedVertices) {
                    if (branchingFactors[v] < minBranchingFactor) {
                        minBranchingFactor = branchingFactors[v];
                        minBranchingFactorVertex = v;
                    }
                }
                // start again with that vertex
                ordering.add(minBranchingFactorVertex);
                unvisitedVertices.remove(minBranchingFactorVertex);
            }

            int currVertex = ordering.get(i++);

            // sort the neighbors in increasing order
            int[] neighborDegrees = new int[adjacencyLists[currVertex].length];
            for (int nIdx = 0; nIdx < adjacencyLists[currVertex].length; nIdx++) {
                int neighbor = adjacencyLists[currVertex][nIdx];
                neighborDegrees[nIdx] = branchingFactors[neighbor];
            }
            int[] neighborOrder = MathUtils.sortOrder(neighborDegrees);

            // append unvisited neighbors to the ordering
            for (int nIdx : neighborOrder) {
                int neighbor = adjacencyLists[currVertex][nIdx];
                if (unvisitedVertices.contains(neighbor)) {
                    ordering.add(neighbor);
                    unvisitedVertices.remove(neighbor);
                }
            }
        }

        // return the reverse ordering
        int[] permutation = new int[ordering.size()];
        i = ordering.size() - 1;
        for (int v : ordering) {
            permutation[i--] = v;
        }
        return permutation;
    }


    /**
     * Generates a random permutation of the numbers {0, ..., n-1}
     *
     * @param n the length of the set of numbers to permute
     * @return a random permutation of the numbers {0, ..., n-1}
     */
    public static int[] permutation(int n) {
        return permutation(n, getRandomGenerator());
    }


    /**
     * Generates a random permutation of the numbers {0, ..., n-1}
     *
     * @param n    the length of the set of numbers to permute
     * @param rand the random number generator
     * @return a random permutation of the numbers {0, ..., n-1}
     */
    public static int[] permutation(int n, Random rand) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = i;
        }
        Collections.shuffle(Arrays.asList(values));
        return values;
    }

    /**
     * Appends an element to a vector.
     *
     * @param v1 the vector.
     * @param d  the element to append.
     * @return A vector containing all the elements of v1 followed
     *         by d.
     */
    public static int[] append(int[] v1, int d) {
        int[] newVector = new int[v1.length + 1];
        System.arraycopy(v1, 0, newVector, 0, v1.length);
        newVector[v1.length] = d;
        return newVector;
    }

    /**
     * Appends an element to a vector.
     *
     * @param v1 the vector.
     * @param d  the element to append.
     * @return A vector containing all the elements of v1 followed
     *         by d.
     */
    public static double[] append(double[] v1, double d) {
        double[] newVector = new double[v1.length + 1];
        System.arraycopy(v1, 0, newVector, 0, v1.length);
        newVector[v1.length] = d;
        return newVector;
    }


    /**
     * Appends two vectors.
     *
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return A vector containing all the elements of v1 followed
     *         by all the elements of v2.
     */
    public static double[] append(double[] v1, double[] v2) {
        double[] newVector = new double[v1.length + v2.length];
        System.arraycopy(v1, 0, newVector, 0, v1.length);
        System.arraycopy(v2, 0, newVector, v1.length, v2.length);
        return newVector;
    }

    /**
     * Appends two vectors.
     *
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return A vector containing all the elements of v1 followed
     *         by all the elements of v2.
     */
    public static int[] append(int[] v1, int[] v2) {
        int[] newVector = new int[v1.length + v2.length];
        System.arraycopy(v1, 0, newVector, 0, v1.length);
        System.arraycopy(v2, 0, newVector, v1.length, v2.length);
        return newVector;
    }

    /**
     * Performs vector addition.
     *
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return v1 + v2
     */
    public static double[] vectorAdd(double[] v1, double[] v2) {
        double[] result = new double[v1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = v1[i] + v2[i];
        }
        return result;
    }

    /**
     * Performs vector subtraction.
     *
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return v1 - v2
     */
    public static double[] vectorSubtract(double[] v1, double[] v2) {
        double[] result = new double[v1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = v1[i] - v2[i];
        }
        return result;
    }

    /**
     * Performs scalar multiplication on a vector.
     *
     * @param s a scalar value.
     * @param v the vector.
     * @return the scalar product of s and v.
     */
    public static double[] vectorMultiply(double s, double[] v) {
        double[] result = new double[v.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = s * v[i];
        }
        return result;
    }

    /**
     * Computes the L2 norm of the given vector.
     *
     * @param v the vector.
     * @return the L2 norm of v.
     */
    public static double vectorL2Norm(double[] v) {
        double norm = 0;
        for (double d : v) {
            norm += Math.pow(d, 2);
        }
        return Math.sqrt(norm);
    }

    /**
     * Normalizes the given vector.
     *
     * @param v the vector.
     * @return v with the values normalized.
     */
    public static double[] vectorNormalize(double[] v) {
        double[] result = new double[v.length];
        double sum = 0;
        for (double d : v) {
            sum += d;
        }
        for (int i = 0; i < v.length; i++) {
            result[i] = v[i] / sum;
        }
        return result;
    }


    static Random randGenerator = new Random(System.currentTimeMillis());

    /**
     * Retrieves the initialized random number generator.  Use of this
     * rand generator allows an entire program to use a single rand generator.
     *
     * @return the initialized random number generator.
     */
    public static Random getRandomGenerator() {
        return randGenerator;
    }

    /**
     * Initializes a random number generator with the given seed.  Repeated
     * calls to this initialize function create a new random generator.
     *
     * @param seed the seed for the random generator.
     */
    public static void initializeRandomGenerator(long seed) {
        randGenerator = new Random(seed);
    }


    /**
     * Gets a random number from a one dimensional Gaussian
     * distribution with the given mean and variance.
     *
     * @param mean  the mean of the Gaussian
     * @param stdev the standard deviation of the Gaussian
     * @return a random number from the specified distribution
     */
    public static double nextRandomGaussian(double mean, double stdev) {
        return nextRandomGaussian(getRandomGenerator(), mean, stdev);
    }


    /**
     * Gets a random number from a one dimensional Gaussian
     * distribution with the given mean and variance.
     *
     * @param randGenerator the random generator.
     * @param mean          the mean of the Gaussian
     * @param stdev         the standard deviation of the Gaussian
     * @return a random number from the specified distribution
     */
    public static double nextRandomGaussian(Random randGenerator, double mean, double stdev) {
        return mean + stdev * randGenerator.nextGaussian();
    }

    /**
     * Computes the root mean squared error between two vectors.
     *
     * @param a
     * @param b
     * @return the RMSE between a and b
     */
    public static double rmse(double[] a, double[] b) {
        if (a.length != b.length)
            throw new IllegalArgumentException("Arrays must be the same length.");

        double rmse = 0;
        int n = a.length;
        for (int i = 0; i < n; i++) {
            rmse += Math.pow(a[i] - b[i], 2);
        }
        rmse = Math.sqrt(rmse / (double) n);
        return rmse;
    }


    public static void main(String[] args) {
        sampleWithoutReplacement(new String[]{"A", "B", "C", "D"},
                new double[]{0.15, 0.35, 0.15, 0.35},
                3);
    }

    /*
     public static int[] sampleWithoutReplacement(int[] values, double[] probabilityDistribution, int numSamples) {
         Integer[] objs = new Integer[values.length];
         for (int i=0; i<objs.length; i++) {
             objs[i] = new Integer(values[i]);
         }
         Integer[] sampledObjs = sampleWithoutReplacement(objs, probabilityDistribution, numSamples);
         int[] sampledValues = new int[sampledObjs.length];
         for (int i=0; i<sampledObjs.length; i++) {
             sampledValues[i] = sampledObjs[i];
         }
         return sampledValues;
     }
     public static int[] sampleWithReplacement(int[] values, double[] probabilityDistribution, int numSamples) {
         Integer[] objs = new Integer[values.length];
         for (int i=0; i<objs.length; i++) {
             objs[i] = new Integer(values[i]);
         }
         Integer[] sampledObjs = sampleWithReplacement(objs, probabilityDistribution, numSamples);
         int[] sampledValues = new int[sampledObjs.length];
         for (int i=0; i<sampledObjs.length; i++) {
             sampledValues[i] = sampledObjs[i];
         }
         return sampledValues;
     }
     */

    /**
     * Samples from a set of weighted objects without replacement.
     *
     * @param objs                    the array of objects
     * @param probabilityDistribution the probability distribution over the set of objects (i.e. the weight assigned to each object)
     * @param numSamples              the desired number of samples
     */
    public static <T> Collection<T> sampleWithoutReplacement(T[] objs, double[] probabilityDistribution, int numSamples) {

        // ensure that the arguments are correct
        if (numSamples > objs.length) {
            throw new IllegalArgumentException("Cannot sample (without replacement) more items than we have.");
        }
        if (objs.length != probabilityDistribution.length) {
            throw new IllegalArgumentException("Each object must have an associated probability");
        }

        // if we're sampling the same number as we have, the answer is simple
        if (numSamples == objs.length) {
            Collection<T> chosenObjs = new ArrayList<T>();
            for (T obj : objs) {
                chosenObjs.add(obj);
            }
            return chosenObjs;
        }


        Collection<T> chosenObjs = new ArrayList<T>();

        for (int sample = 0; sample < numSamples; sample++) {

            // compute the cumulative distribution
            double normalizer = MathUtils.sum(probabilityDistribution);
            double[] cumulativeDistribution = new double[objs.length];
            for (int i = 1; i < probabilityDistribution.length; i++) {
                cumulativeDistribution[i] = cumulativeDistribution[i - 1] + probabilityDistribution[i - 1] / normalizer;
            }

            //  sample into the cumulative distribution
            double value = getRandomGenerator().nextDouble();

            // determine the index into the cumulative distribution
            int index = Arrays.binarySearch(cumulativeDistribution, value);
            if (index < 0) {
                index = (index + 2) * -1;
            }

            // if the probability of the selected object is 0, skip to the next one
            while (index < probabilityDistribution.length && probabilityDistribution[index] == 0) {
                index++;
            }

            // store the object
            chosenObjs.add(objs[index]);

            // zero it's probability so we don't choose it again
            probabilityDistribution[index] = 0;
        }

        return chosenObjs;
    }


    /**
     * Samples from a set of weighted objects with replacement.
     *
     * @param objs                    the array of objects
     * @param probabilityDistribution the probability distribution over the set of objects (i.e. the weight assigned to each object)
     * @param numSamples              the desired number of samples
     */
    public static <T> Collection<T> sampleWithReplacement(T[] objs, double[] probabilityDistribution, int numSamples) {

        // ensure that the arguments are correct
        if (objs.length != probabilityDistribution.length) {
            throw new IllegalArgumentException("Each object must have an associated probability");
        }

        // compute the cumulative distribution
        double normalizer = MathUtils.sum(probabilityDistribution);
        double[] cumulativeDistribution = new double[objs.length];
        for (int i = 1; i < probabilityDistribution.length; i++) {
            cumulativeDistribution[i] = cumulativeDistribution[i - 1] + probabilityDistribution[i - 1] / normalizer;
        }

        Collection<T> chosenObjs = new ArrayList<T>();

        for (int sample = 0; sample < numSamples; sample++) {

            //  sample into the cumulative distribution
            double value = getRandomGenerator().nextDouble();

            // determine the index into the cumulative distribution
            int index = Arrays.binarySearch(cumulativeDistribution, value);
            if (index < 0) {
                index = (index + 2) * -1;
            }

            // store the object
            chosenObjs.add(objs[index]);
        }

        return chosenObjs;
    }


    /**
     * Caps a value to a given range.
     *
     * @param value    the value to constrain.
     * @param minValue the min value.
     * @param maxValue the max value.
     * @return the value altered to lie in [minValue, maxValue].
     */
    public static double capValue(double value, double minValue, double maxValue) {
        if (value < minValue) return minValue;
        if (value > maxValue) return maxValue;
        return value;
    }


    /**
     * Caps a value to a given range.
     *
     * @param value    the value to constrain.
     * @param minValue the min value.
     * @param maxValue the max value.
     * @return the value altered to lie in [minValue, maxValue].
     */
    public static int capValue(int value, int minValue, int maxValue) {
        if (value < minValue) return minValue;
        if (value > maxValue) return maxValue;
        return value;
    }


    /**
     * Throws an exception whenever a value falls outside the given range.
     *
     * @param value
     * @param min   the minimal value
     * @param max   the maximal value
     * @throws IllegalArgumentException if the value is outside of the given range.
     */
    public static void rangeCheck(int value, int min, int max) {
        if (value < min || value > max)
            throw new IllegalArgumentException("Value falls out of required range [" + min + "," + max + "].");
    }


    /**
     * Throws an exception whenever a value falls outside the given range.
     *
     * @param value
     * @param min   the minimal value
     * @param max   the maximal value
     * @throws IllegalArgumentException if the value is outside of the given range.
     */
    public static void rangeCheck(double value, double min, double max) {
        if (value < min || value > max)
            throw new IllegalArgumentException("Value falls out of required range [" + min + "," + max + "].");
    }


    /**
     * Reduces the precision of a double to a specified number of decimal places.
     *
     * @param d                     number to reduce precision
     * @param decimalPlacesRequired the number of required decimal places
     * @return d reduced to the specified precision
     */
    public static final double round(double d, int decimalPlacesRequired) {
        double factor = Math.pow(10, decimalPlacesRequired);
        return Math.round((d * factor)) / factor;
    }

    /**
     * Rounds a value to the nearest number that is a specific multiple.
     *
     * @param value      the value to round
     * @param multipleOf the value that the final number should be a multiple of.
     * @return value rounded to the nearest number that is a multiple of multipleOf.
     */
    public static final int roundToMultiple(int value, int multipleOf) {
        return (int) Math.round((double) value / multipleOf) * multipleOf;
    }


    //////////////////////   ARRAY PROPERTIES   ///////////////////////


    /**
     * Gets the index of the minimum element in the array.  Returns the
     * first min index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the minimum element of v.
     */
    public static int minIndex(double[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        double minValue = minValue(v);

        // determine the indices with the min value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == minValue) {
                return i;
            }
        }
        return -1;  // unreachable code
    }


    /**
     * Gets the index of the minimum element in the array.  Returns the
     * first min index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the minimum element of v.
     */
    public static int minIndex(int[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        int minValue = minValue(v);

        // determine the indices with the min value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == minValue) {
                return i;
            }
        }
        return -1;  // unreachable code
    }

    /**
     * Gets the index of the minimum element in the array.  Randomly chooses
     * the min index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the minimum element of v.
     */
    public static int minIndexRand(double[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        double minValue = minValue(v);
        ArrayList<Integer> minIndices = new ArrayList<Integer>();

        // determine the indices with the min value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == minValue) {
                minIndices.add(i);
            }
        }

        // return the single min index
        if (minIndices.size() == 1) {
            return minIndices.get(0);
        }
        // select a random index to return
        else {
            int randIndex = getRandomGenerator().nextInt(minIndices.size());
            return minIndices.get(randIndex);
        }
    }


    /**
     * Gets the index of the minimum element in the array.  Randomly chooses
     * the min index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the minimum element of v.
     */
    public static int minIndexRand(int[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        int minValue = minValue(v);
        ArrayList<Integer> minIndices = new ArrayList<Integer>();

        // determine the indices with the min value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == minValue) {
                minIndices.add(i);
            }
        }

        // return the single min index
        if (minIndices.size() == 1) {
            return minIndices.get(0);
        }
        // select a random index to return
        else {
            int randIndex = getRandomGenerator().nextInt(minIndices.size());
            return minIndices.get(randIndex);
        }
    }


    /**
     * Gets the index of the maximum element in the array.  Returns the
     * first max index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the maximum element of v.
     */
    public static int maxIndex(double[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        double maxValue = maxValue(v);

        // determine the indices with the max value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == maxValue) {
                return i;
            }
        }
        return -1;  // unreachable code
    }


    /**
     * Gets the index of the maximum element in the array.  Returns the
     * first max index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the maximum element of v.
     */
    public static int maxIndex(int[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        int maxValue = maxValue(v);

        // determine the indices with the max value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == maxValue) {
                return i;
            }
        }
        return -1;  // unreachable code
    }

    /**
     * Gets the index of the maximum element in the array.  Randomly chooses
     * the max index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the maximum element of v.
     */
    public static int maxIndexRand(double[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        double maxValue = maxValue(v);
        ArrayList<Integer> maxIndices = new ArrayList<Integer>();

        // determine the indices with the max value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == maxValue) {
                maxIndices.add(i);
            }
        }

        // return the single max index
        if (maxIndices.size() == 1) {
            return maxIndices.get(0);
        }
        // select a random index to return
        else {
            int randIndex = getRandomGenerator().nextInt(maxIndices.size());
            return maxIndices.get(randIndex);
        }
    }


    /**
     * Gets the index of the maximum element in the array.  Randomly chooses
     * the max index if there are multiple matching elements.
     *
     * @param v
     * @return the index of the maximum element of v.
     */
    public static int maxIndexRand(int[] v) {
        // error check the arguments
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("v cannot be empty.");
        }

        // set up
        int maxValue = maxValue(v);
        ArrayList<Integer> maxIndices = new ArrayList<Integer>();

        // determine the indices with the max value
        for (int i = 0; i < v.length; i++) {
            if (v[i] == maxValue) {
                maxIndices.add(i);
            }
        }

        // return the single max index
        if (maxIndices.size() == 1) {
            return maxIndices.get(0);
        }
        // select a random index to return
        else {
            int randIndex = getRandomGenerator().nextInt(maxIndices.size());
            return maxIndices.get(randIndex);
        }
    }


    /**
     * Computes the maximum value in the given array.
     *
     * @param values
     * @return the maximum value in the given array.
     */
    public static int maxValue(int[] values) {
        int maxValue = Integer.MIN_VALUE;
        for (int val : values) {
            maxValue = (maxValue < val) ? val : maxValue;
        }
        return maxValue;
    }

    /**
     * Computes the maximum value in the given array.
     *
     * @param values
     * @return the maximum value in the given array.
     */
    public static double maxValue(double[] values) {
        double maxValue = Double.MIN_VALUE;
        for (double val : values) {
            maxValue = (maxValue < val) ? val : maxValue;
        }
        return maxValue;
    }

    /**
     * Computes the minimum value in the given array.
     *
     * @param values
     * @return the minimum value in the given array.
     */
    public static double minValue(double[] values) {
        double minValue = Double.MAX_VALUE;
        for (double val : values) {
            minValue = (minValue > val) ? val : minValue;
        }
        return minValue;
    }

    /**
     * Computes the minimum value in the given array.
     *
     * @param values
     * @return the minimum value in the given array.
     */
    public static int minValue(int[] values) {
        int minValue = Integer.MAX_VALUE;
        for (int val : values) {
            minValue = (minValue > val) ? val : minValue;
        }
        return minValue;
    }

    /**
     * Computes the mean of the values in the given array.
     *
     * @param values
     * @return the mean of the values in the given array.
     */
    public static double mean(double[] values) {
        return sum(values) / values.length;
    }

    /**
     * Computes the mean of the values in the given array.
     *
     * @param values
     * @return the mean of the values in the given array.
     */
    public static double mean(int[] values) {
        return ((double) sum(values)) / values.length;
    }

    /**
     * Sums the values in the given array.
     *
     * @param values
     * @return the sum of the values in the given array.
     */
    public static double sum(double[] values) {
        double sumValue = 0;
        for (double val : values) {
            sumValue += val;
        }
        return sumValue;
    }

    /**
     * Sums the values in the given array.
     *
     * @param values
     * @return the sum of the values in the given array.
     */
    public static int sum(int[] values) {
        int sumValue = 0;
        for (int val : values) {
            sumValue += val;
        }
        return sumValue;
    }

    /**
     * Adds corresponding elements of two arrays.
     *
     * @param d1 the first array
     * @param d2 the second array
     * @return the element-wise sum of d1 + d2
     */
    public static double[] arrayAdd(double[] d1, double d2[]) {
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Arrays cannot be null.");
        }
        if (d1.length != d2.length) {
            throw new IllegalArgumentException("Arrays must be the same length.");
        }
        double[] dSum = new double[d1.length];
        for (int i = 0; i < d1.length; i++) {
            dSum[i] = d1[i] + d2[i];
        }
        return dSum;
    }

    /**
     * Divides each element of an array by a number.
     *
     * @param d1          the first array
     * @param denominator the denominator for the devision
     * @return d1 / denominator
     */
    public static double[] arrayDivide(double[] d1, double denominator) {
        if (d1 == null) {
            throw new IllegalArgumentException("Array cannot be null.");
        }
        double[] dDivide = new double[d1.length];
        for (int i = 0; i < d1.length; i++) {
            dDivide[i] = d1[i] / denominator;
        }
        return dDivide;
    }


    /**
     * Sorts the given values and returns the order of the original indices.
     *
     * @param values the values to sort
     * @return an array of the original indices of values reordered according to the sort.
     */
    public static int[] sortOrder(int[] values) {
        SortOrder[] array = new SortOrder[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = new SortOrder<Integer>(values[i], i);
        }
        // randomize the array before sorting
        Collections.shuffle(Arrays.asList(array), getRandomGenerator());
        Arrays.sort(array);
        int[] order = new int[values.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = array[i].getIndex();
        }
        return order;
    }

    /**
     * Sorts the given values and returns the order of the original indices.
     *
     * @param values the values to sort
     * @return an array of the original indices of values reordered according to the sort.
     */
    public static int[] sortOrder(double[] values) {
        SortOrder[] array = new SortOrder[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = new SortOrder<Double>(values[i], i);
        }
        // randomize the array before sorting
        Collections.shuffle(Arrays.asList(array), getRandomGenerator());
        Arrays.sort(array);
        int[] order = new int[values.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = array[i].getIndex();
        }
        return order;
    }

    /**
     * Private class used by sortOrder
     */
    static class SortOrder<T extends Comparable<T>> implements Comparable<SortOrder<T>> {
        T m_object;
        int m_index;

        public SortOrder(T o, int index) {
            m_object = o;
            m_index = index;
        }

        public T getObject() {
            return m_object;
        }

        public int getIndex() {
            return m_index;
        }

        public int compareTo(SortOrder<T> o) {
            //if (!(o instanceof SortOrder))
            //	throw new IllegalArgumentException("Incomparable types of objects.");
            //return m_object.compareTo(((SortOrder)o).m_object);
            return m_object.compareTo(o.m_object);
        }
    }


    /**
     * Reverses the given array.
     *
     * @param array
     * @return the array with the elements in reverse order
     */
    public static int[] reverse(int[] array) {
        int[] reverseArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            reverseArray[array.length - 1 - i] = array[i];
        }
        return reverseArray;
    }


    /**
     * Reverses the given array.
     *
     * @param array
     * @return the array with the elements in reverse order
     */
    public static double[] reverse(double[] array) {
        double[] reverseArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            reverseArray[array.length - 1 - i] = array[i];
        }
        return reverseArray;
    }


    static double MACHINE_PRECISION = 0;

    static {
        MACHINE_PRECISION = 1.0;
        double one = 1.0;
        do {
            MACHINE_PRECISION /= 2;
            one = 1.0 + MACHINE_PRECISION;
        } while (one > 1.0);
        // loop terminated when the compute can't tell ONE from 1.0 giving us the machine precision
    }


    /**
     * Gets the computed machine precision.
     *
     * @return the computed machine precision.
     */
    public static double getMachinePrecision() {
        //return 1E-10;
        return MACHINE_PRECISION;
    }


    /**
     * Determines whether two numbers are approximately equal according to the machine precision.
     *
     * @param value1
     * @param value2
     * @return whether the two numbers are approximately equal
     */
    public static boolean isApproxEqual(double value1, double value2) {
        return isApproxEqual(value1, value2, getMachinePrecision());
    }


    /**
     * Determines whether two numbers are approximately equal according to the precision.
     *
     * @param value1
     * @param value2
     * @param precision
     * @return whether the two numbers are approximately equal
     */
    public static boolean isApproxEqual(double value1, double value2, double precision) {
        return (Math.abs(value1 - value2) <= precision);
    }


    /**
     * Computes the correlation between two arrays of the same length, p and q.
     * Computes the correlation between p and q as
     * r = (|p| * \sum_i(p[i]*q[i]) - \sum_i(p[i]) * \sum_i(q[i]))/
     * sqrt((|p| * \sum_i((p[i])^2) - (\sum_i(p[i]))^2) *
     * (|p| * \sum_i((p[i])^2) - (\sum_i(p[i]))^2))
     * This correlation can be tested for statistical significance via t-tests.
     * See e.g.: http://www.socialresearchmethods.net/kb/statcorr.htm
     *
     * @return The correlation between the elements of the two arrays.
     */
    public static double correlation(int[] p, int[] q) {

        if (p == null || q == null) {
            throw new IllegalArgumentException("p and q cannot be null");
        }
        if (p.length != q.length) {
            throw new IllegalArgumentException("p and q must be the same length");
        }

        // compute the sums and squared sums
        int sumP = 0;
        int sumQ = 0;
        int sumPSquared = 0;
        int sumQSquared = 0;
        int sumPQ = 0;
        for (int i = 0; i < p.length; i++) {
            sumP += p[i];
            sumQ += q[i];
            sumPSquared += p[i] * p[i];
            sumQSquared += q[i] * q[i];
            sumPQ += p[i] * q[i];
        }

        // compute the correlation
        double r = ((double) (p.length * sumPQ - sumP * sumQ)) /
                Math.sqrt(((long) (p.length * sumPSquared - sumP * sumP)) *
                        ((long) (p.length * sumQSquared - sumQ * sumQ)));

        return r;
    }


    /**
     * Computes the correlation between two arrays of the same length, p and q.
     * Computes the correlation between p and q as
     * r = (|p| * \sum_i(p[i]*q[i]) - \sum_i(p[i]) * \sum_i(q[i]))/
     * sqrt((|p| * \sum_i((p[i])^2) - (\sum_i(p[i]))^2) *
     * (|p| * \sum_i((p[i])^2) - (\sum_i(p[i]))^2))
     * This correlation can be tested for statistical significance via t-tests.
     * See e.g.: http://www.socialresearchmethods.net/kb/statcorr.htm
     *
     * @return The correlation between the elements of the two arrays.
     */
    public static double correlation(double[] p, double[] q) {

        if (p == null || q == null) {
            throw new IllegalArgumentException("p and q cannot be null");
        }
        if (p.length != q.length) {
            throw new IllegalArgumentException("p and q must be the same length");
        }

        // compute the sums and squared sums
        double sumP = 0;
        double sumQ = 0;
        double sumPSquared = 0;
        double sumQSquared = 0;
        double sumPQ = 0;
        for (int i = 0; i < p.length; i++) {
            sumP += p[i];
            sumQ += q[i];
            sumPSquared += p[i] * p[i];
            sumQSquared += q[i] * q[i];
            sumPQ += p[i] * q[i];
        }

        // compute the correlation
        double r = (p.length * sumPQ - sumP * sumQ) /
                Math.sqrt((p.length * sumPSquared - sumP * sumP) *
                        (p.length * sumQSquared - sumQ * sumQ));

        return r;
    }


    /**
     * Computes the pairwise agreement between two pairwise arrays of labelings.
     * The pairwise agreement is the number-of-pairs-in-agreement / the-total-number-of-pairs.
     * The two arrays must be the same length.
     *
     * @param p An array of labels.
     * @param q An array of labels.
     * @return The pairwise agreement between the labelings in p and q.
     */
    public static double pairwiseAgreement(int[] p, int[] q) {

        if (p == null || q == null) {
            throw new IllegalArgumentException("p and q cannot be null");
        }
        if (p.length != q.length) {
            throw new IllegalArgumentException("p and q must be the same length");
        }

        int numSamePairs = 0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] == q[i])
                numSamePairs++;
        }

        return ((double) numSamePairs) / p.length;
    }


    /**
     * Determines the unique values of v.  The values are returned in no particular order.
     *
     * @param v
     * @return the unique values of v in no particular order.
     */
    public static int[] uniqueValues(int[] v) {
        // form the values into a set, which automatically removes duplicates
        HashSet<Integer> uniqueValues = new HashSet<Integer>();
        for (int i = 0; i < v.length; i++) {
            uniqueValues.add(v[i]);
        }
        // convert the set back into an array
        int[] vUnique = new int[uniqueValues.size()];
        int i = 0;
        for (Integer uniqueValue : uniqueValues) {
            vUnique[i++] = uniqueValue;
        }
        return vUnique;
    }

    /**
     * Determines the unique values of v.  The values are returned in no particular order.
     *
     * @param v
     * @return the unique values of v in no particular order.
     */
    public static double[] uniqueValues(double[] v) {
        // form the values into a set, which automatically removes duplicates
        HashSet<Double> uniqueValues = new HashSet<Double>();
        for (int i = 0; i < v.length; i++) {
            uniqueValues.add(v[i]);
        }
        // convert the set back into an array
        double[] vUnique = new double[uniqueValues.size()];
        int i = 0;
        for (Double uniqueValue : uniqueValues) {
            vUnique[i++] = uniqueValue;
        }
        return vUnique;
    }


    /**
     * Computes the normalized confusion matrix for two vectors.
     *
     * @param p the first vector
     * @param q the second vector
     * @return the normalized confusion matrix for p and q
     */
    public static double[][] getConfusionMatrix(int[] p, int[] q) {

        if (p.length != q.length) {
            throw new IllegalArgumentException("p and q must be the same length.");
        }

        int[] classes = uniqueValues(append(p, q));
        int n = p.length;

        // compute the confusion matrix
        double[][] confusionMatrix = new double[classes.length][classes.length];
        for (int i = 0; i < n; i++) {
            // determine the classIdx of p[i]
            int piClassIdx;
            for (piClassIdx = 0; piClassIdx < classes.length; piClassIdx++) {
                if (p[i] == classes[piClassIdx]) break;
            }
            // determine the classIdx of q[i]
            int qiClassIdx;
            for (qiClassIdx = 0; qiClassIdx < classes.length; qiClassIdx++) {
                if (q[i] == classes[qiClassIdx]) break;
            }
            // increment the counter in the confusion matrix
            confusionMatrix[piClassIdx][qiClassIdx]++;
        }

        // normalize the confusion matrix
        for (int i = 0; i < confusionMatrix.length; i++) {
            for (int j = 0; j < confusionMatrix.length; j++) {
                confusionMatrix[i][j] /= n;
            }
        }

        return confusionMatrix;
    }


    /**
     * Computes the mutual information between two vectors.
     *
     * @param p the first vector.
     * @param q the second vector.
     * @return the mutual information between p and q.
     */
    public static double mutualInformation(int[] p, int[] q) {
        double[][] confusionMatrix = getConfusionMatrix(p, q);

        // get the row and col sums of the confusion matrix
        double[] rowsum = new double[confusionMatrix.length];
        double[] colsum = new double[confusionMatrix.length];
        for (int i = 0; i < confusionMatrix.length; i++) {
            for (int j = 0; j < confusionMatrix.length; j++) {
                rowsum[i] += confusionMatrix[i][j];
                colsum[j] += confusionMatrix[i][j];
            }
        }

        // compute the mutual information
        double mutualInformation = 0;
        for (int i = 0; i < confusionMatrix.length; i++) {
            for (int j = 0; j < confusionMatrix.length; j++) {
                double deltaMI = 0;
                // if entry is not 0, then the deltaMI shouldn't be 0
                if (confusionMatrix[i][j] != 0) {
                    deltaMI = confusionMatrix[i][j] *
                            log2(confusionMatrix[i][j] / (rowsum[i] * colsum[j]));
                }
                if (Double.isNaN(deltaMI)) {
                    throw new IllegalStateException("MI is NaN!");
                }
                mutualInformation += deltaMI;
            }
        }

        return mutualInformation;
    }


    /**
     * Defines a similarity metric measure
     */
    public enum SimilarityMetric {
        CORRELATION,
        MUTUAL_INFORMATION,
        ACCURACY
    }

    /**
     * Computes the similarity of the two vectors.
     *
     * @param targetV          the target vector that forms the basis for comparison.
     * @param v                the vector for comparison
     * @param similarityMetric the metric to use for computing the similarity
     * @return The similarity of the two models.
     */
    public static double computeSimilarity(int[] targetV, int[] v, SimilarityMetric similarityMetric) {

        switch (similarityMetric) {

            case CORRELATION:
                return MathUtils.correlation(targetV, v);

            case MUTUAL_INFORMATION:
                return MathUtils.mutualInformation(targetV, v);

            case ACCURACY:
                return MathUtils.pairwiseAgreement(targetV, v);

        }

        return Double.NaN;

    }


    ///////////////// Log Computations ///////////////////////


    final static double LOG2 = Math.log(2);

    /**
     * Computes the log-base-2 of a number.
     *
     * @param d
     * @return the log-base-2 of d
     */
    public static double log2(double d) {
        return Math.log(d) / LOG2;
    }


}