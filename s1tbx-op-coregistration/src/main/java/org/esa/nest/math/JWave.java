/**
 * JWave - Java implementation of wavelet transform algorithms
 *
 * Copyright 2008-2014 Christian Scheiblich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This file is part of JWave.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 23.05.2008 17:42:23
 *
 */
package org.esa.nest.math;

import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.transforms.BasicTransform;
import org.esa.nest.math.transforms.DiscreteFourierTransform;
import org.esa.nest.math.transforms.FastWaveletTransform;
import org.esa.nest.math.transforms.WaveletPacketTransform;
import org.esa.nest.math.transforms.wavelets.Haar1;
import org.esa.nest.math.transforms.wavelets.Wavelet;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet1;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies2;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies3;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies4;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre1;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre2;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre3;

/**
 * Main class for doing little test runs for different transform types and
 * different wavelets without JUnit.
 *
 * @date 23.02.2010 14:26:47
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
/**
 * @author tucker date 31.01.2014 20:26:06
 */
public class JWave {

    /**
     * Constructor.
     *
     * @date 23.02.2010 14:26:47
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     */
    public JWave() {
    } // JWave

    /**
     * Main method for doing little test runs for different transform types and
     * different wavelets without JUnit. Requesting the transform type and the
     * type of wavelet to be used else usage is printed.
     *
     * @date 23.02.2010 14:26:47
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param args [transformType] [waveletType]
     */
    public static void main(String[] args) {

        try { // try everything ~8>

      // String waveletTypeList =
            // "Haar1, Daubechies2, Daubechies3, Daubechies4, Legendre1, Legendre2, Legendre3, Coiflet1";
            String waveletTypeList = "Haar1, Daubechies2, Daubechies4, Legendre1";

            if (args.length < 2 || args.length > 3) {
                System.err
                        .println("usage: JWave [transformType] {waveletType} {noOfSteps}");
                System.err.println("");
                System.err.println("transformType: DFT, FWT, WPT, DWT");
                System.err.println("waveletType : " + waveletTypeList);
                System.err.println("noOfSteps : "
                        + "no of steps forward and reverse; optional");
                return;
            } // if args

            String wType = args[ 1];
            Wavelet wavelet = null;
            if (wType.equalsIgnoreCase("haar02")) {
                wavelet = new Haar1();
            } else if (wType.equalsIgnoreCase("lege02")) {
                wavelet = new Legendre1();
            } else if (wType.equalsIgnoreCase("daub02")) {
                wavelet = new Daubechies2();
            } else if (wType.equalsIgnoreCase("daub03")) {
                wavelet = new Daubechies3();
            } else if (wType.equalsIgnoreCase("daub04")) {
                wavelet = new Daubechies4();
            } else if (wType.equalsIgnoreCase("lege04")) {
                wavelet = new Legendre2();
            } else if (wType.equalsIgnoreCase("lege06")) {
                wavelet = new Legendre3();
            } else if (wType.equalsIgnoreCase("coif06")) {
                wavelet = new Coiflet1();
            } else {
                System.err.println("usage: JWave [transformType] {waveletType}");
                System.err.println("");
                System.err.println("available wavelets are " + waveletTypeList);
                return;
            } // if wType

            String tType = args[ 0];
            BasicTransform bWave = null;

            if (tType.equalsIgnoreCase("dft")) {
                bWave = new DiscreteFourierTransform();
            } else if (tType.equalsIgnoreCase("fwt")) {
                bWave = new FastWaveletTransform(wavelet);
            } else if (tType.equalsIgnoreCase("wpt")) {
                bWave = new WaveletPacketTransform(wavelet);
            } else {
                System.err.println("usage: JWave [transformType] {waveletType}");
                System.err.println("");
                System.err.println("available transforms are DFT, FWT, WPT");
                return;
            } // if tType

            // instance of transform
            Transform t;

            if (args.length > 2) {

                String argNoOfSteps = args[ 2];
                int noOfSteps = Integer.parseInt(argNoOfSteps);

                t = new Transform(bWave, noOfSteps); // perform less steps than
                // possible

            } else {

                t = new Transform(bWave); // perform all steps

            }

            double[] arrTime = {1., 1., 1., 1., 1., 1., 1., 1.};

            System.out.println("");
            System.out.println("time domain:");
            for (int p = 0; p < arrTime.length; p++) {
                System.out.printf("%9.6f", arrTime[ p]);
            }
            System.out.println("");

            double[] arrFreqOrHilb = null;
            arrFreqOrHilb = t.forward(arrTime);

            if (bWave instanceof DiscreteFourierTransform) {
                System.out.println("frequency domain:");
            } else {
                System.out.println("Hilbert domain:");
            }
            for (int p = 0; p < arrTime.length; p++) {
                System.out.printf("%9.6f", arrFreqOrHilb[ p]);
            }
            System.out.println("");

            double[] arrReco = null;

            arrReco = t.reverse(arrFreqOrHilb);

            System.out.println("reconstruction:");
            for (int p = 0; p < arrTime.length; p++) {
                System.out.printf("%9.6f", arrReco[ p]);
            }
            System.out.println("");

        } catch (JWaveException e1) {

            // TODO Auto-generated catch block
            e1.printStackTrace();

        } // try

    } // main

} // class
