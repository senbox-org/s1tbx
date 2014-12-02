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
package org.esa.nest.math.transforms.wavelets.legendre;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Legendre's orthonormal wavelet of four coefficients and the scales; normed,
 * due to ||*||2 - euclidean norm.
 * 
 * @date 03.06.2010 21:19:04
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Legendre2 extends Wavelet {

  /**
   * Constructor setting up the orthonormal Legendre4 wavelet coeffs and the
   * scales; normed, due to ||*||2 - euclidean norm.
   * 
   * @date 03.06.2010 21:19:04
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Legendre2( ) {

    _name = "Legendre 2"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal - TODO: test 2 ! 

    _motherWavelength = 4; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ]; // can be done in static way also; faster?
    _scalingDeCom[ 0 ] = -5. / 8.; // s0
    _scalingDeCom[ 1 ] = -3. / 8.; // s1
    _scalingDeCom[ 2 ] = -3. / 8.; // s2
    _scalingDeCom[ 3 ] = -5. / 8.; // s3

    // normalize orthogonal space => orthonormal space!!!  
    double sqrt02 = Math.sqrt( 2. ) ; // 1.4142135623730951
    for( int i = 0; i < _motherWavelength; i++ )
      _scalingDeCom[ i ] /= sqrt02;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Legendre2

} // class
