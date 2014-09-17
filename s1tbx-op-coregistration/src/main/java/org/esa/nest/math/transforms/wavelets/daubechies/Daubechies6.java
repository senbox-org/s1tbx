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
 * Ingrid Daubechies' orthonormal wavelet of 12 coefficients and the scales;
 * normed, due to ||*||2 - euclidean norm.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 00:21:31
 */
public class Daubechies6 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db6/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 00:21:31
   */
  public Daubechies6( ) {

    _name = "Daubechies 6"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 12; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.00107730108499558;
    _scalingDeCom[ 1 ] = 0.004777257511010651;
    _scalingDeCom[ 2 ] = 0.0005538422009938016;
    _scalingDeCom[ 3 ] = -0.031582039318031156;
    _scalingDeCom[ 4 ] = 0.02752286553001629;
    _scalingDeCom[ 5 ] = 0.09750160558707936;
    _scalingDeCom[ 6 ] = -0.12976686756709563;
    _scalingDeCom[ 7 ] = -0.22626469396516913;
    _scalingDeCom[ 8 ] = 0.3152503517092432;
    _scalingDeCom[ 9 ] = 0.7511339080215775;
    _scalingDeCom[ 10 ] = 0.4946238903983854;
    _scalingDeCom[ 11 ] = 0.11154074335008017;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies6

} // Daubechies6
