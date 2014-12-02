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
 * Ingrid Daubechies' orthonormal wavelet of eight coefficients and the scales;
 * normed, due to ||*||2 - euclidean norm.
 * 
 * @date 26.03.2010 07:35:31
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class Daubechies4 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db4/ Thanks!
   * 
   * @date 26.03.2010 07:35:31
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Daubechies4( ) {

    _name = "Daubechies 4"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 8; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.010597401784997278;
    _scalingDeCom[ 1 ] = 0.032883011666982945;
    _scalingDeCom[ 2 ] = 0.030841381835986965;
    _scalingDeCom[ 3 ] = -0.18703481171888114;
    _scalingDeCom[ 4 ] = -0.02798376941698385;
    _scalingDeCom[ 5 ] = 0.6308807679295904;
    _scalingDeCom[ 6 ] = 0.7148465705525415;
    _scalingDeCom[ 7 ] = 0.23037781330885523;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies4

} // class
