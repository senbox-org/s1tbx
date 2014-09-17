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
 * Legendre's orthonormal wavelet of six coefficients and the scales; normed,
 * due to ||*||2 - euclidean norm.
 * 
 * @date 03.06.2010 22:04:35
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Legendre3 extends Wavelet {

  /**
   * Constructor setting up the orthonormal Legendre6 wavelet coeffs and the
   * scales; normed, due to ||*||2 - euclidean norm.
   * 
   * @date 03.06.2010 22:04:36
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Legendre3( ) {

    _name = "Legendre 3"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal 

    _motherWavelength = 6; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ]; // can be done in static way also; faster?
    _scalingDeCom[ 0 ] = -63. / 128.; // h0
    _scalingDeCom[ 1 ] = -35. / 128.; // h1
    _scalingDeCom[ 2 ] = -30. / 128.; // h2
    _scalingDeCom[ 3 ] = -30. / 128.; // h3
    _scalingDeCom[ 4 ] = -35. / 128.; // h4
    _scalingDeCom[ 5 ] = -63. / 128.; // h5

    // normalize orthogonal space => orthonormal space!!!  
    double sqrt02 = Math.sqrt( 2. ); // 1.4142135623730951
    for( int i = 0; i < _motherWavelength; i++ )
      _scalingDeCom[ i ] /= sqrt02;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Legendre3

} // class
