/**
 * JWave - Java implementation of wavelet transform algorithms
 *
 * Copyright 2008-2014 Christian Scheiblich
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * This file is part of JWave.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 23.05.2008 17:42:23
 *
 */
package org.esa.nest.math.transforms.wavelets;

/**
 * Alfred Haar's orthonormal wavelet transform.
 * 
 * @date 08.02.2010 12:46:34
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Haar1 extends Wavelet {

  /**
   * Constructor setting up the orthonormal Haar wavelet coefficients and the
   * scaling coefficients; normed, due to ||*||_2 -- euclidean norm. See the
   * orthogonal version in class Haar1Orthogonal for more details.
   * 
   * @date 08.02.2010 12:46:34
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Haar1( ) {

    _name = "Haar"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 2; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    double sqrt2 = Math.sqrt( 2. );
    _scalingDeCom[ 0 ] = 1. / sqrt2; // 1.4142135623730951 w0 - normed by sqrt( 2 )
    _scalingDeCom[ 1 ] = 1. / sqrt2; // 1.4142135623730951 w1 - normed by sqrt( 2 )

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = _scalingDeCom[ 1 ]; // w1
    _waveletDeCom[ 1 ] = -_scalingDeCom[ 0 ]; // -w0

    // Copy to reconstruction filters due to orthogonality (orthonormality)!
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {
      _scalingReCon[ i ] = _scalingDeCom[ i ];
      _waveletReCon[ i ] = _waveletDeCom[ i ];
    } // i

  } // Haar1

  /**
   * The forward wavelet transform using the Alfred Haar's wavelet.
   * 
   * @date 10.02.2010 08:26:06
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.wavelets.Wavelet#forward(double[])
   */

  /**
   * The reverse wavelet transform using the Alfred Haar's wavelet. The arrHilb
   * array keeping coefficients of Hilbert domain should be of length 2 to the
   * power of p -- length = 2^p where p is a positive integer.
   * 
   * @date 10.02.2010 08:26:06
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.wavelets.Wavelet#reverse(double[])
   */

} // class
