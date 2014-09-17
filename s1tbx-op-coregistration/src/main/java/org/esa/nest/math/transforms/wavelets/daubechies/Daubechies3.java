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
package org.esa.nest.math.transforms.wavelets.daubechies;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal wavelet of six coefficients and the scales;
 * normed, due to ||*||2 - euclidean norm.
 * 
 * @date 15.02.2014 22:23:20
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Daubechies3 extends Wavelet {

  /**
   * Constructor setting up the orthonormal Daubechie6 wavelet coeffs and the
   * scales; normed, due to ||*||2 - euclidean norm.
   * 
   * @date 25.03.2010 14:03:20
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Daubechies3( ) {

    _name = "Daubechies 3"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 6; // wavelength of mother wavelet

    // calculate the coefficients analytically 
    _scalingDeCom = new double[ _motherWavelength ]; // can be done in static way also; faster?
    double sqrt10 = Math.sqrt( 10. );
    double constA = Math.sqrt( 5. + 2. * sqrt10 );
    _scalingDeCom[ 0 ] = ( 1.0 + 1. * sqrt10 + 1. * constA ) / 16.; // h0 = 0.47046720778416373
    _scalingDeCom[ 1 ] = ( 5.0 + 1. * sqrt10 + 3. * constA ) / 16.; // h1 = 1.1411169158314438
    _scalingDeCom[ 2 ] = ( 10. - 2. * sqrt10 + 2. * constA ) / 16.; // h2 = 0.6503650005262325
    _scalingDeCom[ 3 ] = ( 10. - 2. * sqrt10 - 2. * constA ) / 16.; // h3 = -0.1909344155683274
    _scalingDeCom[ 4 ] = ( 5.0 + 1. * sqrt10 - 3. * constA ) / 16.; // h4 = -0.1208322083103962
    _scalingDeCom[ 5 ] = ( 1.0 + 1. * sqrt10 - 1. * constA ) / 16.; // h5 = 0.049817499736883764

    // normalize orthogonal space => orthonormal space!!!  
    double sqrt02 = Math.sqrt( 2. ) ; // 1.4142135623730951
    for( int i = 0; i < _motherWavelength; i++ )
      _scalingDeCom[ i ] /= sqrt02;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies3

} // class
