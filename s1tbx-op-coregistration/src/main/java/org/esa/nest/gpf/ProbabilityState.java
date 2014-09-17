/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.gpf;

import java.util.HashMap;

/**
 * Calculates the probabilities of each state in a random variable. Provides the
 * base for all functions of one variable. Additional functions include the
 * normaliseArrays function which converts all inputs so they start at 0, and
 * the mergeArrays function which creates an array of the joint state of the two
 * input arrays.
 * 
* @author apocock
 */
public class ProbabilityState {

    public final HashMap<Integer, Double> probMap;
    public final int maxState;

    /**
     * Constructor for the ProbabilityState class. Takes a data vector and
     * calculates the marginal probability of each state, storing each
     * state/probability pair in a HashMap.
     *     
* @param dataVector Input vector. It is discretised to the floor of each
     * value.
     */
    public ProbabilityState(double[] dataVector) {
        probMap = new HashMap<Integer, Double>();
        int vectorLength = dataVector.length;
        double doubleLength = dataVector.length;
//round input to integers
        int[] normalisedVector = new int[vectorLength];
        maxState = normaliseArray(dataVector, normalisedVector);
        HashMap<Integer, Integer> countMap = new HashMap<Integer, Integer>();
        for (int value : normalisedVector) {
            Integer tmpKey = value;
            Integer tmpValue = countMap.remove(tmpKey);
            if (tmpValue == null) {
                countMap.put(tmpKey, 1);
            } else {
                countMap.put(tmpKey, tmpValue + 1);
            }
        }
        for (Integer key : countMap.keySet()) {
            probMap.put(key, countMap.get(key) / doubleLength);
        }
    }//constructor(double[])

    /**
     * Takes an input vector and writes an output vector which is a normalised
     * version of the input, and returns the maximum state. A normalised array
     * has min value = 0, max value = old max value - min value and all values
     * are integers
     *     
* The length of the vectors must be the same, and outputVector must be
     * instantiated before calling this function.
     *
     * @param inputVector The vector to normalise.
     * @param outputVector The normalised vector. Must be instantiated to length
     * inputVector.length.
     * @return The maximum state from the normalised vector.
     */
    public static final int normaliseArray(double[] inputVector, int[] outputVector) {
        int minVal = 0;
        int maxVal = 0;
        int currentValue;
        int i;
        int vectorLength = inputVector.length;
        if (vectorLength > 0) {
            minVal = (int) Math.floor(inputVector[0]);
            maxVal = (int) Math.floor(inputVector[0]);
            for (i = 0; i < vectorLength; i++) {
                currentValue = (int) Math.floor(inputVector[i]);
                outputVector[i] = currentValue;
                if (currentValue < minVal) {
                    minVal = currentValue;
                }
                if (currentValue > maxVal) {
                    maxVal = currentValue;
                }
            }/*for loop over vector*/

            for (i = 0; i < vectorLength; i++) {
                outputVector[i] = outputVector[i] - minVal;
            }
            maxVal = (maxVal - minVal) + 1;
        }
        return maxVal;
    }//normaliseArray(double[],double[])

    /**
     * Takes in two arrays and writes the joint state of those arrays to the
     * output vector, returning the maximum joint state.
     *     
* The length of all vectors must be equal to firstVector.length
     * outputVector must be instantiated before calling this function.
     *
     * @param firstVector The first vector.
     * @param secondVector The second vector.
     * @param outputVector The merged vector. Must be instantiated to length
     * inputVector.length.
     * @return The maximum state from the merged vector.
     */
    public static final int mergeArrays(double[] firstVector, double[] secondVector, double[] outputVector) {
        int[] firstNormalisedVector;
        int[] secondNormalisedVector;
        int firstNumStates;
        int secondNumStates;
        int i;
        int[] stateMap;
        int stateCount;
        int curIndex;
        int vectorLength = firstVector.length;
        firstNormalisedVector = new int[vectorLength];
        secondNormalisedVector = new int[vectorLength];
        firstNumStates = normaliseArray(firstVector, firstNormalisedVector);
        secondNumStates = normaliseArray(secondVector, secondNormalisedVector);
        stateMap = new int[firstNumStates * secondNumStates];
        stateCount = 1;
        for (i = 0; i < vectorLength; i++) {
            curIndex = firstNormalisedVector[i] + (secondNormalisedVector[i] * firstNumStates);
            if (stateMap[curIndex] == 0) {
                stateMap[curIndex] = stateCount;
                stateCount++;
            }
            outputVector[i] = stateMap[curIndex];
        }
        return stateCount;
    }//mergeArrays(double[],double[],double[])

    /**
     * A helper function which prints out any given int vector. Mainly used to
     * help debug the rest of the toolbox.
     *     
* @param vector The vector to print out.
     */
    public static void printIntVector(int[] vector) {
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] > 0) {
                System.out.println("Val at i=" + i + ", is " + vector[i]);
            }
        }//for number of items in vector
    }//printIntVector(doublei[])

    /**
     * A helper function which prints out any given double vector. Mainly used
     * to help debug the rest of the toolbox.
     *     
* @param vector The vector to print out.
     */
    public static void printDoubleVector(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] > 0) {
                System.out.println("Val at i=" + i + ", is " + vector[i]);
            }
        }//for number of items in vector
    }//printDoubleVector(doublei[])
}//class ProbabilityState
