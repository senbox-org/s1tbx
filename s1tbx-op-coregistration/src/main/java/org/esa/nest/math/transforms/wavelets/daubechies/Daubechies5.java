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
 * Ingrid Daubechies' orthonormal wavelet of ten coefficients and the scales;
 * normed, due to ||*||2 - euclidean norm.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 00:18:15
 */
public class Daubechies5 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db5/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 00:18:15
   */
  public Daubechies5( ) {

    _name = "Daubechies 5"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 10; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.003335725285001549;
    _scalingDeCom[ 1 ] = -0.012580751999015526;
    _scalingDeCom[ 2 ] = -0.006241490213011705;
    _scalingDeCom[ 3 ] = 0.07757149384006515;
    _scalingDeCom[ 4 ] = -0.03224486958502952;
    _scalingDeCom[ 5 ] = -0.24229488706619015;
    _scalingDeCom[ 6 ] = 0.13842814590110342;
    _scalingDeCom[ 7 ] = 0.7243085284385744;
    _scalingDeCom[ 8 ] = 0.6038292697974729;
    _scalingDeCom[ 9 ] = 0.160102397974125;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies5

} // Daubechies5
