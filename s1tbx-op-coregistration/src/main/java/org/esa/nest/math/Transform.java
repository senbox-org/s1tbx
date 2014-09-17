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

import org.esa.nest.math.datatypes.Complex;
import org.esa.nest.math.exceptions.JWaveError;
import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.exceptions.JWaveFailure;
import org.esa.nest.math.tools.MathToolKit;
import org.esa.nest.math.transforms.BasicTransform;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Base class for transforms like DiscreteFourierTransform, FastBasicTransform,
 * and WaveletPacketTransform.
 *
 * @date 19.05.2009 09:43:40
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Transform {

    /**
     * Transform object of type base class
     */
    protected BasicTransform _transform;

    /**
     * Supplying a various number of little mathematical methods.
     *
     * @author Christian Scheiblich (cscheiblich@gmail.com) 19.02.2014 18:34:34
     */
    protected MathToolKit _mathToolKit;

    /**
     * Constructor; needs some object like DiscreteFourierTransform,
     * FastBasicTransform, WaveletPacketTransfom, ...
     *
     * @date 19.05.2009 09:50:24
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param transform Transform object
     */
    public Transform(BasicTransform transform) {

        _transform = transform;

        _mathToolKit = new MathToolKit();

    } // Transform

    /**
     * Constructor; needs some object like DiscreteFourierTransform,
     * FastBasicTransform, WaveletPacketTransfom, ... It take also a number of
     * iteration for decomposition
     *
     * @date 19.05.2009 09:50:24
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     */
    @Deprecated
    public Transform(BasicTransform transform, int iteration) {
        if (transform instanceof BasicTransform) {

            _transform = transform;

      // TODO realize the level transform in GOOD Software Engineering
            // style - after restructuring the code
      // ( (WaveletTransform)_transform ).set_iteration( iteration );
            try { // always break down these methods
                throw new JWaveError("THE ITERATION METHODS ARE BORKEN AT MOMENT");
            } catch (JWaveError e) {
                e.printStackTrace();
            } // try

        } else {
            throw new IllegalArgumentException("Can't use transform :"
                    + transform.getClass() + " with a specific level decomposition ;"
                    + " use Transform( TransformI transform ) constructor instead.");
        }

        _mathToolKit = new MathToolKit();

    } // Transform

    /**
     * Performs the forward transform of the specified BasicWave object.
     *
     * @date 10.02.2010 09:41:01
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param arrTime coefficients of time domain
     * @return coefficients of frequency or Hilbert domain
     * @throws JWaveException array is not of type 2^p
     */
    public double[] forward(double[] arrTime) throws JWaveException {

        if (!_mathToolKit.isBinary(arrTime.length)) {
            throw new JWaveFailure(
                    "given array length is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        return _transform.forward(arrTime);

    } // forward

    /**
     * Performs the reverse transform of the specified BasicWave object.
     *
     * @date 10.02.2010 09:42:18
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param arrFreq coefficients of frequency or Hilbert domain
     * @return coefficients of time domain
     * @throws JWaveException if array is not of type 2^p
     */
    public double[] reverse(double[] arrFreq) throws JWaveException {

        if (!_mathToolKit.isBinary(arrFreq.length)) {
            throw new JWaveFailure(
                    "given array length is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        return _transform.reverse(arrFreq);

    } // reverse

    /**
     * Performs the forward transform from time domain to frequency or Hilbert
     * domain for a given array depending on the used transform algorithm by
     * inheritance.
     *
     * @date 23.11.2010 19:19:24
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param arrTime coefficients of 1-D time domain
     * @return coefficients of 1-D frequency or Hilbert domain
     */
    public Complex[] forward(Complex[] arrTime) {

        return ((BasicTransform) _transform).forward(arrTime);

    } // forward

    /**
     * Performs the reverse transform from frequency or Hilbert domain to time
     * domain for a given array depending on the used transform algorithm by
     * inheritance.
     *
     * @date 23.11.2010 19:19:33
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param arrFreq coefficients of 1-D frequency or Hilbert domain
     * @return coefficients of 1-D time domain
     */
    public Complex[] reverse(Complex[] arrFreq) {

        return ((BasicTransform) _transform).reverse(arrFreq);

    } // reverse

    /**
     * Performs the 2-D forward transform of the specified BasicWave object.
     *
     * @date 10.02.2010 10:58:54
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param matrixTime coefficients of 2-D time domain; internal M(i),N(j)
     * @return coefficients of 2-D frequency or Hilbert domain
     * @throws JWaveException if matrix is not of type 2^p x 2^q
     */
    public double[][] forward(double[][] matrixTime) throws JWaveException {

        int M = matrixTime.length;

        if (!_mathToolKit.isBinary(M)) {
            throw new JWaveFailure(
                    "given matrix dimension "
                    + M
                    + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        for (int i = 0; i < M; i++) {
            if (!_mathToolKit.isBinary(matrixTime[ i].length)) {
                throw new JWaveFailure(
                        "given matrix dimension N(i)="
                        + matrixTime[ i].length
                        + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                        + "please use the Ancient Egyptian Decomposition for any other array length!");
            }
        }

        return _transform.forward(matrixTime);

    } // forward

    /**
     * Performs the 2-D reverse transform of the specified BasicWave object.
     *
     * @date 10.02.2010 10:59:32
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param matrixFreq coefficients of 2-D frequency or Hilbert domain;
     * internal M(i),N(j)
     * @return coefficients of 2-D time domain
     * @throws JWaveException if matrix is not of type 2^p x 2^q
     */
    public double[][] reverse(double[][] matrixFreq) throws JWaveException {

        int M = matrixFreq.length;

        if (!_mathToolKit.isBinary(M)) {
            throw new JWaveFailure(
                    "given matrix dimension "
                    + M
                    + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        for (int i = 0; i < M; i++) {
            if (!_mathToolKit.isBinary(matrixFreq[ i].length)) {
                throw new JWaveFailure(
                        "given matrix dimension N(i)="
                        + matrixFreq[ i].length
                        + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                        + "please use the Ancient Egyptian Decomposition for any other array length!");
            }
        }

        return _transform.reverse(matrixFreq);

    } // reverse

    /**
     * Performs the 3-D forward transform of the specified BasicWave object.
     *
     * @date 10.07.2010 18:15:22
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param matrixTime coefficients of 2-D time domain; internal
     * M(i),N(j),O(k)
     * @return coefficients of 2-D frequency or Hilbert domain
     * @throws JWaveException if space is not of type 2^p x 2^q x 2^r
     */
    public double[][][] forward(double[][][] spaceTime)
            throws JWaveException {

        int M = spaceTime.length;

        if (!_mathToolKit.isBinary(M)) {
            throw new JWaveFailure(
                    "given space dimension "
                    + M
                    + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        for (int i = 0; i < M; i++) { // M(i)

            int N = spaceTime[ i].length;

            if (!_mathToolKit.isBinary(N)) {
                throw new JWaveFailure(
                        "given space dimension N(i)="
                        + N
                        + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                        + "please use the Ancient Egyptian Decomposition for any other array length!");
            }

            for (int j = 0; j < N; j++) // // N(j)
            {
                if (!_mathToolKit.isBinary(spaceTime[ i][ j].length)) // O
                {
                    throw new JWaveFailure(
                            "given space dimension M(j)="
                            + spaceTime[ i][ j].length
                            + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                            + "please use the Ancient Egyptian Decomposition for any other array length!");
                }
            }

        } // i

        return _transform.forward(spaceTime);

    } // forward

    /**
     * Performs the 3-D reverse transform of the specified BasicWave object.
     *
     * @date 10.07.2010 18:15:33
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @param matrixFreq coefficients of 2-D frequency or Hilbert domain;
     * internal M(i),N(j),O(k)
     * @return coefficients of 2-D time domain
     * @throws JWaveException if space is not of type 2^p x 2^q x 2^r
     */
    public double[][][] reverse(double[][][] spaceFreq)
            throws JWaveException {

        int M = spaceFreq.length;

        if (!_mathToolKit.isBinary(M)) {
            throw new JWaveFailure(
                    "given space dimension "
                    + M
                    + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        for (int i = 0; i < M; i++) { // M(i)

            int N = spaceFreq[ i].length;

            if (!_mathToolKit.isBinary(N)) {
                throw new JWaveFailure(
                        "given space dimension N(i)="
                        + N
                        + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                        + "please use the Ancient Egyptian Decomposition for any other array length!");
            }

            for (int j = 0; j < N; j++) { // N(j)

                if (!_mathToolKit.isBinary(spaceFreq[ i][ j].length)) // O
                {
                    throw new JWaveFailure(
                            "given space dimension M(j)="
                            + spaceFreq[ i][ j].length
                            + " is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                            + "please use the Ancient Egyptian Decomposition for any other array length!");
                }

            } // j

        } // i

        return _transform.reverse(spaceFreq);

    } // reverse

    /**
     * Generates from a 1D signal a 2D output, where the second dimension are
     * the levels of the wavelet transform.
     *
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @date 17.08.2014 10:07:19
     * @param arrTime coefficients of time domain
     * @return matDeComp 2-D Hilbert spaces: [ 0 .. p ][ 0 .. N ] where p is the
     * exponent of N=2^p
     * @throws JWaveException if not available or signal is not of 2^p
     */
    public double[][] decompose(double[] arrTime) throws JWaveException {

        if (!_mathToolKit.isBinary(arrTime.length)) {
            throw new JWaveFailure(
                    "given array length is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        return _transform.decompose(arrTime);

    } // decompose

    /**
     * Generates from a 2-D decomposition a 1-D time series.
     *
     * @author Christian Scheiblich (cscheiblich@gmail.com)
     * @date 17.08.2014 10:07:19
     * @param matDeComp 2-D Hilbert spaces: [ 0 .. p ][ 0 .. N ] where p is the
     * exponent of N=2^p
     * @return a 1-D time domain signal
     * @throws JWaveException if not available or signal is not of 2^p
     */
    public double[] recompose(double[][] matDeComp) throws JWaveException {

        if (!_mathToolKit.isBinary(matDeComp[ 0].length)) {
            throw new JWaveFailure(
                    "given array length is not 2^p = 1, 2, 4, 8, 16, 32, .. "
                    + "please use the Ancient Egyptian Decomposition for any other array length!");
        }

        return _transform.recompose(matDeComp);

    } // recompose

} // class
