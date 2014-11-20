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
 * Orthonormal Legendre wavelet transform of 2 coefficients based on the
 * Legendre polynomial. But, actually for the smallest Legendre wavelet, the
 * wavelet is the mirrored Haar Wavelet.
 * 
 * @date 08.06.2010 09:32:08
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Legendre1 extends Wavelet {

  /**
   * Constructor setting up the orthonormal Legendre 2 wavelet coeffs and the
   * scales; normed, due to ||*||_2 -- euclidean norm. Actually these
   * coefficients are the mirrored ones of Alfred Haar's wavelet -- see class
   * Haar1 and Haar1Orthogonal.
   * 
   * @date 08.06.2010 09:32:08
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Legendre1( ) {
    
    _name = "Legendre 1"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 2; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -1.; // h0
    _scalingDeCom[ 1 ] = -1.; // h1

    // normalize orthogonal space => orthonormal space!!!  
    double sqrt02 = Math.sqrt( 2. ) ; // 1.4142135623730951
    for( int i = 0; i < _motherWavelength; i++ )
      _scalingDeCom[ i ] /= sqrt02;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Legendre1

} // class
