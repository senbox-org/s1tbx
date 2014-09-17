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
 * BiOrthogonal Wavelet of type 2.6 - Two vanishing moments in wavelet function
 * and six vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 16:31:32
 */
public class BiOrthogonal26 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior2.6/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 16:31:32
   */
  public BiOrthogonal26( ) {

    _name = "BiOrthogonal 2/6"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 14; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = -0.006905339660024878;
    _scalingDeCom[ 2 ] = 0.013810679320049757;
    _scalingDeCom[ 3 ] = 0.046956309688169176;
    _scalingDeCom[ 4 ] = -0.10772329869638811;
    _scalingDeCom[ 5 ] = -0.16987135563661201;
    _scalingDeCom[ 6 ] = 0.4474660099696121;
    _scalingDeCom[ 7 ] = 0.966747552403483;
    _scalingDeCom[ 8 ] = 0.4474660099696121;
    _scalingDeCom[ 9 ] = -0.16987135563661201;
    _scalingDeCom[ 10 ] = -0.10772329869638811;
    _scalingDeCom[ 11 ] = 0.046956309688169176;
    _scalingDeCom[ 12 ] = 0.013810679320049757;
    _scalingDeCom[ 13 ] = -0.006905339660024878;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = 0.;
    _waveletDeCom[ 3 ] = 0.;
    _waveletDeCom[ 4 ] = 0.;
    _waveletDeCom[ 5 ] = 0.3535533905932738;
    _waveletDeCom[ 6 ] = -0.7071067811865476;
    _waveletDeCom[ 7 ] = 0.3535533905932738;
    _waveletDeCom[ 8 ] = 0.;
    _waveletDeCom[ 9 ] = 0.;
    _waveletDeCom[ 10 ] = 0.;
    _waveletDeCom[ 11 ] = 0.;
    _waveletDeCom[ 12 ] = 0.;
    _waveletDeCom[ 13 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal26

} // BiOrthogonal26