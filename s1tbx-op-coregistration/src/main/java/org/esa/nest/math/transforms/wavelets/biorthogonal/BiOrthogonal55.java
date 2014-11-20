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
package org.esa.nest.math.transforms.wavelets.biorthogonal;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * BiOrthogonal Wavelet of type 4.4 - Five vanishing moments in wavelet function
 * and five vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 17:40:01
 */
public class BiOrthogonal55 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior5.5/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:40:01
   */
  public BiOrthogonal55( ) {

    _name = "BiOrthogonal 5/5"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 12; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = 0.;
    _scalingDeCom[ 2 ] = 0.03968708834740544;
    _scalingDeCom[ 3 ] = 0.007948108637240322;
    _scalingDeCom[ 4 ] = -0.05446378846823691;
    _scalingDeCom[ 5 ] = 0.34560528195603346;
    _scalingDeCom[ 6 ] = 0.7366601814282105;
    _scalingDeCom[ 7 ] = 0.34560528195603346;
    _scalingDeCom[ 8 ] = -0.05446378846823691;
    _scalingDeCom[ 9 ] = 0.007948108637240322;
    _scalingDeCom[ 10 ] = 0.03968708834740544;
    _scalingDeCom[ 11 ] = 0.;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = -0.013456709459118716;
    _waveletDeCom[ 1 ] = -0.002694966880111507;
    _waveletDeCom[ 2 ] = 0.13670658466432914;
    _waveletDeCom[ 3 ] = -0.09350469740093886;
    _waveletDeCom[ 4 ] = -0.47680326579848425;
    _waveletDeCom[ 5 ] = 0.8995061097486484;
    _waveletDeCom[ 6 ] = -0.47680326579848425;
    _waveletDeCom[ 7 ] = -0.09350469740093886;
    _waveletDeCom[ 8 ] = 0.13670658466432914;
    _waveletDeCom[ 9 ] = -0.002694966880111507;
    _waveletDeCom[ 10 ] = -0.013456709459118716;
    _waveletDeCom[ 11 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal55

} // BiOrthogonal55
