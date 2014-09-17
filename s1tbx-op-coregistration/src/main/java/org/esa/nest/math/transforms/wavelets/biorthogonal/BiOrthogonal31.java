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
 * BiOrthogonal Wavelet of type 3.1 - Three vanishing moments in wavelet
 * function and one vanishing moment in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 16:50:58
 */
public class BiOrthogonal31 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior3.1/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 16:50:58
   */
  public BiOrthogonal31( ) {

    _name = "BiOrthogonal 3/1"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 4; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.3535533905932738;
    _scalingDeCom[ 1 ] = 1.0606601717798214;
    _scalingDeCom[ 2 ] = 1.0606601717798214;
    _scalingDeCom[ 3 ] = -0.3535533905932738;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = -0.1767766952966369;
    _waveletDeCom[ 1 ] = 0.5303300858899107;
    _waveletDeCom[ 2 ] = -0.5303300858899107;
    _waveletDeCom[ 3 ] = 0.1767766952966369;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal31

} // BiOrthogonal31