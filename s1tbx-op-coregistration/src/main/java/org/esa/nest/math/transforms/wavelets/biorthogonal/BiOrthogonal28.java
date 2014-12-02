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
 * BiOrthogonal Wavelet of type 2.8 - Two vanishing moments in wavelet function
 * and eight vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 16:37:35
 */
public class BiOrthogonal28 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior2.8/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 16:37:35
   */
  public BiOrthogonal28( ) {

    _name = "BiOrthogonal 2/8"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 18; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = 0.0015105430506304422;
    _scalingDeCom[ 2 ] = -0.0030210861012608843;
    _scalingDeCom[ 3 ] = -0.012947511862546647;
    _scalingDeCom[ 4 ] = 0.02891610982635418;
    _scalingDeCom[ 5 ] = 0.052998481890690945;
    _scalingDeCom[ 6 ] = -0.13491307360773608;
    _scalingDeCom[ 7 ] = -0.16382918343409025;
    _scalingDeCom[ 8 ] = 0.4625714404759166;
    _scalingDeCom[ 9 ] = 0.9516421218971786;
    _scalingDeCom[ 10 ] = 0.4625714404759166;
    _scalingDeCom[ 11 ] = -0.16382918343409025;
    _scalingDeCom[ 12 ] = -0.13491307360773608;
    _scalingDeCom[ 13 ] = 0.052998481890690945;
    _scalingDeCom[ 14 ] = 0.02891610982635418;
    _scalingDeCom[ 15 ] = -0.012947511862546647;
    _scalingDeCom[ 16 ] = -0.0030210861012608843;
    _scalingDeCom[ 17 ] = 0.0015105430506304422;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = 0.;
    _waveletDeCom[ 3 ] = 0.;
    _waveletDeCom[ 4 ] = 0.;
    _waveletDeCom[ 5 ] = 0.;
    _waveletDeCom[ 6 ] = 0.;
    _waveletDeCom[ 7 ] = 0.3535533905932738;
    _waveletDeCom[ 8 ] = -0.7071067811865476;
    _waveletDeCom[ 9 ] = 0.3535533905932738;
    _waveletDeCom[ 10 ] = 0.;
    _waveletDeCom[ 11 ] = 0.;
    _waveletDeCom[ 12 ] = 0.;
    _waveletDeCom[ 13 ] = 0.;
    _waveletDeCom[ 14 ] = 0.;
    _waveletDeCom[ 15 ] = 0.;
    _waveletDeCom[ 16 ] = 0.;
    _waveletDeCom[ 17 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal28

} // BiOrthogonal28