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
 * BiOrthogonal Wavelet of type 2.2 - Two vanishing moments in wavelet function
 * and two vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 16:15:01
 */
public class BiOrthogonal22 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior2.2/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 16:15:01
   */
  public BiOrthogonal22( ) {

    _name = "BiOrthogonal 2/2"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 6; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = -0.1767766952966369;
    _scalingDeCom[ 2 ] = 0.3535533905932738;
    _scalingDeCom[ 3 ] = 1.0606601717798214;
    _scalingDeCom[ 4 ] = 0.3535533905932738;
    _scalingDeCom[ 5 ] = -0.1767766952966369;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.3535533905932738;
    _waveletDeCom[ 2 ] = -0.7071067811865476;
    _waveletDeCom[ 3 ] = 0.3535533905932738;
    _waveletDeCom[ 4 ] = 0.;
    _waveletDeCom[ 5 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal22

} // BiOrthogonal22